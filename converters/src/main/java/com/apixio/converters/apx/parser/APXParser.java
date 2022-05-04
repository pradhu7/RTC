package com.apixio.converters.apx.parser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import com.apixio.converters.base.BaseUtils;
import com.apixio.converters.base.Parser;
import com.apixio.converters.zip.ZipUtils;
import com.apixio.model.file.catalog.jaxb.generated.ApxCatalog;
import com.apixio.model.file.catalog.jaxb.generated.ApxCatalog.CatalogEntry;
import com.apixio.model.file.catalog.jaxb.generated.ApxCatalog.CatalogEntry.Patient.PatientId;
import com.apixio.model.patient.ClinicalActor;
import com.apixio.model.patient.Demographics;
import com.apixio.model.patient.Document;
import com.apixio.model.patient.DocumentContent;
import com.apixio.model.patient.DocumentType;
import com.apixio.model.patient.Encounter;
import com.apixio.model.patient.ExternalID;
import com.apixio.model.patient.Gender;
import com.apixio.model.patient.Name;
import com.apixio.model.patient.NameType;
import com.apixio.model.patient.Organization;
import com.apixio.model.patient.ParserType;
import com.apixio.model.patient.ParsingDetail;
import com.apixio.model.patient.Patient;
import com.apixio.model.patient.Source;
import com.apixio.model.external.CaretParser;
import com.apixio.model.external.CaretParserException;
import com.apixio.utility.HashCalculator;

public class APXParser extends Parser {

    private static Logger log = Logger.getLogger(APXParser.class);

    private static final String PARSER_VERSION = "1.0";
    private URI sourceDocumentUri;
    private CatalogEntry catalogEntry;
    private ParsingDetail currentParsingDetails;
    private Source primarySource;
    private Patient patient = new Patient();
    //This "Object" value type is actually either byte[] (file content)or string (file name). May be we can use EitherOr Object to make sure either one of the types come in here.
    private Hashtable<String, Object> documents = null;

    private UUID associatedEncounterUUID = null;

    private UUID primaryClinicalActorUUID = null;
    //All these constants can be an enum.
    public static final String keyConstantFileName = "fileName";
    public static final String keyConstantFileContent = "fileContent";
    public static final String keyConstantCatFileName = "catFileName";
    public static final String keyConstantCatFileContent = "catFileContent";

    @Override
    public Patient getPatient() throws Exception {
        return patient;
    }

    @Override
    public void parse(InputStream apxDocumentPackage, CatalogEntry catalogEntry) throws Exception {
        if(this.documents==null){
            //This map has file name as key and its contents as value.
            Hashtable<String, byte[]> extractedDocs = ZipUtils.extractPackage(apxDocumentPackage);
            if(extractedDocs!=null){
                //From this map, prepare a map with specific labels as keys (labels defined by the static final constants) and the respective contents as value.
                this.documents = new Hashtable<String, Object>();
                for(String key : extractedDocs.keySet()){

                    byte[] content = extractedDocs.get(key);
                    //A hack. I don't know how else we can determine it is a catalog file. (I copied from the existing logic) - Nethra.
                    //Another way is to determine the root node and find out through that whether it's a catalog file - Nethra (from Shamshad's suggestion)
                    if(key.toLowerCase().endsWith(".apx")){
                        this.documents.put(APXParser.keyConstantCatFileName, key);
                        this.documents.put(keyConstantCatFileContent, content);
                    }
                    else{
                        this.documents.put(APXParser.keyConstantFileName, key);
                        this.documents.put(keyConstantFileContent, content);
                    }
                }
            }
        }
        this.catalogEntry = catalogEntry;
        if(this.catalogEntry==null){
            if (!loadCatalogFile())
                throw new Exception("Loading catalog file failed.");
        }

        if(this.catalogEntry!=null) {
            populateParsingDetails();
            populatePrimarySource();
            populatePatient();
            populateEncounters();
            populateAuthor();
            populateDocument();
        }
    }

    public void setDocuments(String name, Object obj) {
        if(name!=null && obj!=null){
            if(this.documents==null)
                this.documents = new Hashtable<String, Object>();

            this.documents.put(name, obj);
        }
    }

    private boolean loadCatalogFile() throws Exception {
        boolean success = false;
        try
        {
            InputStream apxDocument = getApxDocumentStream();
            JAXBContext jaxbContext = JAXBContext.newInstance(ApxCatalog.class);
            Unmarshaller unMarshaller = jaxbContext.createUnmarshaller();

            ApxCatalog apx = (ApxCatalog) unMarshaller.unmarshal(apxDocument);
            if (apx != null && apx.getCatalogEntry() != null)
            {
                // we are just looking at the first one
                catalogEntry = apx.getCatalogEntry();
                success = true;
            }
        }
        catch(Exception e)
        {
            log.fatal("SEVERE ERROR: Cannot parse the document as it is not complying to the XSD.", e);
        }
        return success;
    }

    private InputStream getApxDocumentStream() {
        InputStream apxDocumentStream = null;
        if (documents != null) {
            apxDocumentStream = new ByteArrayInputStream((byte[])documents.get(keyConstantCatFileContent));
        }
        return apxDocumentStream;
    }

    private void populateParsingDetails() {
        currentParsingDetails = new ParsingDetail();
        // do we use structured even though this is just a catalog for an unstructured document?
        // yes that is whats happening. not sure it makes sense. -lilith
        currentParsingDetails.setContentSourceDocumentType(DocumentType.STRUCTURED_DOCUMENT);
        currentParsingDetails.setContentSourceDocumentURI(sourceDocumentUri);
        currentParsingDetails.setParser(ParserType.APX);
        currentParsingDetails.setParserVersion(PARSER_VERSION);
        currentParsingDetails.setParsingDateTime(DateTime.now());
        currentParsingDetails.setSourceUploadBatch(catalogEntry.getBatchId());
        Map<String, String> metadata = currentParsingDetails.getMetadata();
        if(metadata==null)
            metadata = new HashMap<String, String>();
        if(getSourceDocumentName()!=null){
            metadata.put("sourceDocumentName", getSourceDocumentName());
            currentParsingDetails.setMetadata(metadata);
        }
        this.patient.addParsingDetail(currentParsingDetails);
    }

    private void populatePrimarySource() throws CaretParserException {
        primarySource = new Source();
        primarySource.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
        //primarySource.setClinicalActorId(clinicalActorId) -- We'll need to add an actor and reference it
        primarySource.setCreationDate(getDate(catalogEntry.getCreationDate())); // should we support source date?
        primarySource.setOrganization(getOrganization(catalogEntry.getOrganization()));
        primarySource.setOriginalId(BaseUtils.getExternalIdFromCatalog(catalogEntry.getDocumentId()));
        primarySource.setSourceSystem(catalogEntry.getSourceSystem());
        //primarySource.setSourceType(sourceType)
        this.patient.addSource(primarySource);
    }

    private void populateEncounters() throws Exception {
        if(catalogEntry.getEncounter() != null){
            Encounter encounter = new Encounter();
            //encounter.setEncounterId(UUID.fromString(catalogEntry.getEncounter().getEncounterId()));
            String encounterId = catalogEntry.getEncounter().getEncounterId();
            if (encounterId != null) {
                encounter.setOriginalId(CaretParser.toExternalID(encounterId));
            }
            encounter.setEncounterDate(getDate(catalogEntry.getEncounter().getEncounterDate()));
            encounter.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
            encounter.setSourceId(primarySource.getSourceId());
            patient.addEncounter(encounter);
            associatedEncounterUUID = encounter.getEncounterId();
        }
    }

    private void populateAuthor() {
        if(catalogEntry.getAuthorId() != null){
            ClinicalActor actor = new ClinicalActor();
            actor.setPrimaryId(BaseUtils.getExternalID(catalogEntry.getAuthorId().getId(), catalogEntry.getAuthorId().getAssignAuthority()));
            actor.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
            actor.setSourceId(primarySource.getSourceId());
            patient.addClinicalActor(actor);
            primaryClinicalActorUUID = actor.getInternalUUID();
        }
    }

    private void populateDocument() throws Exception {
        // Here we get the HTML for the document, create a document object, and hang it on the patient
        Document document = new Document();

        if (catalogEntry !=null && catalogEntry.getDocumentUUID() !=null) {
            UUID fileArchiveUUID = UUID.fromString(catalogEntry.getDocumentUUID());
            document.setInternalUUID(fileArchiveUUID);
            currentParsingDetails.setSourceFileArchiveUUID(fileArchiveUUID);
        }
        else {
            throw new Exception("No Document UUID found in Catalog file.");
        }

        document.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
        document.setSourceId(primarySource.getSourceId());
        document.setSourceEncounter(associatedEncounterUUID);
        document.setPrimaryClinicalActorId(primaryClinicalActorUUID);
        document.setDocumentDate(getDate(catalogEntry.getCreationDate()));
        document.setDocumentTitle(catalogEntry.getDescription());
        document.setOriginalId(BaseUtils.getExternalIdFromCatalog(catalogEntry.getDocumentId()));
        document.setMetadata(getMapFromString(catalogEntry.getMetaTags()));

        if (catalogEntry.getDocumentType() != null) {
            document.setMetaTag("DOCUMENT_TYPE", catalogEntry.getDocumentType());
        }

        DocumentContent documentContent = new DocumentContent();
        documentContent.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
        documentContent.setSourceId(primarySource.getSourceId());

        byte[] fileContent = (byte[])getDocument(keyConstantFileContent);
        if(fileContent==null){
            throw new Exception("File content is null.");
        }
        try{
            String fileContentHash = HashCalculator.getFileHash(fileContent);
            documentContent.setHash(fileContentHash);
            currentParsingDetails.setSourceFileHash(fileContentHash);
        }catch(Exception e){
            log.warn("Error while calculating Hash of the file:"+e.getMessage());
        }
        documentContent.setLength(fileContent.length);
        documentContent.setMimeType(catalogEntry.getMimeType());
        documentContent.setUri(sourceDocumentUri);

        document.getDocumentContents().add(documentContent);
        patient.addDocument(document);
    }

    private Map<String, String> getMapFromString(String metaTags) {
        Map<String,String> map = new HashMap<String,String>();
        if (!StringUtils.isBlank(metaTags)) {
            String[] mapEntries = metaTags.split(";", -1);
            for (String mapEntry : mapEntries) {
                String[] keyValue = mapEntry.split("=", -1);
                if (keyValue.length == 2) {
                    map.put(keyValue[0], keyValue[1]);
                }
            }
        }
        return map;
    }

    private Object getDocument(String fileLocation) {

        Object document = null;
        if (documents != null && documents.containsKey(fileLocation))
            document = documents.get(fileLocation);
        return document;
    }

    private void populatePatient() throws Exception {
        if (catalogEntry.getPatient() != null)
        {
            ApxCatalog.CatalogEntry.Patient catalogPatient = catalogEntry.getPatient();
            patient.setPrimaryDemographics(getDemographics(catalogPatient));
            patient.setExternalIDs(BaseUtils.getExternalIdsFromCatalog(catalogPatient.getPatientId()));
            patient.setPrimaryExternalID(getPrimaryExternalId(patient.getExternalIDs()));
        }
    }

    private Demographics getDemographics (ApxCatalog.CatalogEntry.Patient catalogPatient) {
        Demographics demographics = new Demographics();
        demographics.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
        demographics.setSourceId(primarySource.getSourceId());

        demographics.setName(getPatientName(catalogPatient));
        demographics.setDateOfBirth(getDate(catalogPatient.getPatientDOB()));
        demographics.setGender(getGender(catalogPatient.getPatientGender()));

        if (demographics.getName() == null
                && demographics.getDateOfBirth() == null
                && demographics.getGender() == Gender.UNKNOWN)
            return null;

        return demographics;
    }

    private Gender getGender(String genderText) {
        Gender gender = Gender.UNKNOWN;
        if (genderText != null) {
            if (genderText.equalsIgnoreCase("M") || genderText.equalsIgnoreCase("MALE"))
                gender = Gender.MALE;
            else if (genderText.equalsIgnoreCase("F") || genderText.equalsIgnoreCase("FEMALE"))
                gender = Gender.FEMALE;
            else if (genderText.equalsIgnoreCase("O") || genderText.equalsIgnoreCase("OTHER"))
                gender = Gender.TRANSGENDER;
        }
        return gender;
    }

    private Name getPatientName(ApxCatalog.CatalogEntry.Patient catalogPatient) {
        Name name = new Name();
        name.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
        name.setSourceId(primarySource.getSourceId());
        name.setNameType(NameType.CURRENT_NAME);
        if ((catalogPatient.getPatientFirstName() == null || catalogPatient.getPatientFirstName().trim().isEmpty())
                && (catalogPatient.getPatientMiddleName() == null || catalogPatient.getPatientMiddleName().trim().isEmpty())
                && (catalogPatient.getPatientLastName() == null || catalogPatient.getPatientLastName().trim().isEmpty()))
            return null;
        name.getGivenNames().add(catalogPatient.getPatientFirstName());
        name.getGivenNames().add(catalogPatient.getPatientMiddleName());
        name.getFamilyNames().add(catalogPatient.getPatientLastName());
        // TODO: would be nice to have name.isEmpty()
        return name;
    }

    private List<String> getListForString(String patientFirstName) {
        List<String> list = new LinkedList<String>();
        list.add(patientFirstName);
        return list;
    }

    private DateTime getDate(String creationDate) {
        DateTime date = null;
        if (creationDate != null) {
            date = BaseUtils.parseDateTime(creationDate);
        }
        return date;
    }

    private Organization getOrganization(String organizationName) {
        Organization organization = new Organization();
        organization.setName(organizationName);
        return organization;
    }

    public void setCatalogEntry(CatalogEntry catalogEntry) {
        this.catalogEntry = catalogEntry;
    }

}
