package com.apixio.converters.axm.parser;

import com.apixio.converters.base.Parser;
import com.apixio.model.EitherStringOrNumber;
import com.apixio.model.file.catalog.jaxb.generated.ApxCatalog.CatalogEntry;
import com.apixio.utility.HashCalculator;
import com.apixio.model.patient.*;
import com.apixio.model.external.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.joda.time.DateTime;
import org.apache.log4j.Logger;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

import static com.apixio.converters.base.BaseUtils.isEmpty;

/**
 * Parses Apixio External Model (AXM) objects.
 */
public class AXMParser extends Parser {

    private static Logger log = Logger.getLogger(AXMParser.class);
    private static final String PARSER_VERSION = "0.1";

    private Patient patient;
    private UUID  parsingId;
    private Map<String, Source> dataSources = new HashMap<>();

    @Override
    public Patient getPatient() throws Exception {
        if (patient == null) {
            throw new Exception("Patient was not parsed");
        }
        return patient;
    }

    @Override
    public Iterable<Patient> getPatients() throws Exception {
        return Arrays.asList(getPatient());
    }

    @Override
    public void parse(InputStream axmStream, CatalogEntry catalogEntry) throws Exception {
        byte[] axmBytes = IOUtils.toByteArray(axmStream);

        AxmPackage axmPackage = AxmUtils.fromJson(new String(axmBytes, StandardCharsets.UTF_8));
        AxmPatient axm = axmPackage.getPatient();

        patient = new Patient();

        // dedupe and set external IDs
        Set<ExternalID> ids = new HashSet<>();
        for (AxmExternalId id : axm.getExternalIds()) {
            ids.add(convertId(id));
        }
        patient.setExternalIDs(ids);

        // choose primary external id
        patient.setPrimaryExternalID(getPrimaryExternalId(patient.getExternalIDs()));

        // parsing details
        ParsingDetail parsingDetail = new ParsingDetail();
        parsingDetail.setContentSourceDocumentType(DocumentType.STRUCTURED_DOCUMENT);
        parsingDetail.setParser(ParserType.AXM);
        parsingDetail.setParserVersion(PARSER_VERSION);
        parsingDetail.setParsingDateTime(DateTime.now());
        parsingDetail.setSourceFileHash(HashCalculator.getFileHash(axmBytes));
        parsingDetail.setSourceFileArchiveUUID(UUID.fromString(catalogEntry.getDocumentUUID()));
        parsingDetail.setSourceUploadBatch(catalogEntry.getBatchId());
        parsingId = parsingDetail.getParsingDetailsId();
        patient.addParsingDetail(parsingDetail);

        if (axm.getDemographics() != null) { addDemographics(axm.getDemographics()); }
        for (AxmProblem p : axm.getProblems()) { addProblem(p); }
        for (AxmEncounter e : axm.getEncounters()) { addEncounter(e); }
        for (AxmProvider p : axm.getProviders()) { addProvider(p); }
        for (AxmCoverage c : axm.getCoverages()) { addCoverage(c); }
        for (AxmProcedure p : axm.getProcedures()) { addProcedure(p); }
        for (AxmPrescription p : axm.getPrescriptions()) { addPrescription(p); }
        for (AxmBiometricValue p : axm.getBiometricValues()) { addBiometric(p); }
        for (AxmSocialHistory p : axm.getSocialHistories()) { addSocialHistory(p); }
        for (AxmLabResult l : axm.getLabResults()) { addLabResult(l); }
        for (AxmDocument d : axm.getDocuments()) { addDocument(d); }
    }

    private void addDemographics(AxmDemographics axm) {
        Demographics demo = new Demographics();
        patient.setPrimaryDemographics(demo);
        UUID sourceId = populateBaseObject(demo, axm);

        Name name = new Name();
        if (!isEmpty(axm.getGivenNames())) {
            List<String> givenNames = new ArrayList<>();
            for (String givenName : axm.getGivenNames()) {
                givenNames.add(givenName);
            }
            name.setGivenNames(givenNames);
        }

        if (!isEmpty(axm.getFamilyNames())) {
            List<String> familyNames = new ArrayList<>();
            for (String familyName : axm.getFamilyNames()) {
                familyNames.add(familyName);
            }
            name.setFamilyNames(familyNames);
        }

        if (!isEmpty(axm.getPrefixes())) {
            List<String> prefixes = new ArrayList<>();
            for (String prefix : axm.getPrefixes()) {
                prefixes.add(prefix);
            }
            name.setPrefixes(prefixes);
        }

        if (!isEmpty(axm.getSuffixes())) {
            List<String> suffixes = new ArrayList<>();
            for (String suffix : axm.getSuffixes()) {
                suffixes.add(suffix);
            }
            name.setSuffixes(suffixes);
        }

        name.setNameType(NameType.CURRENT_NAME);
        demo.setName(name);

        // AXM gender values should always be consistent with APO gender values.
        demo.setGender(Gender.valueOf(axm.getGender().toString()));

        if (axm.getDateOfBirth() != null) {
            demo.setDateOfBirth(axm.getDateOfBirth().toDateTimeAtStartOfDay());
        }

        if (axm.getDateOfDeath() != null) {
            demo.setDateOfDeath(axm.getDateOfDeath().toDateTimeAtStartOfDay());
        }
    }

    private ClinicalCode toClinicalCode(AxmCodeOrName codeOrName) {
        if (codeOrName.isCode()) {
            return convertCode(codeOrName.getCode());
        } else {
            ClinicalCode ccode = new ClinicalCode();
            ccode.setDisplayName(codeOrName.getName());
            return ccode;
        }
    }

    private void addDocument(AxmDocument axm) {
        Document d = new Document();
        patient.addDocument(d);
        UUID sourceId = populateBaseObject(d, axm);

        // the only mandatory field for metadata updates is document uuid.
        // although in the future it's possible content would come as well and
        // we would want to use the auto-generated one?
        // this will fail if not an ID (as it should -- maybe AXM should actually use UUID?)
        if (axm.getDocumentUuid() != null) {
            d.setInternalUUID(UUID.fromString(axm.getDocumentUuid()));
            // APXParser does the below but it is inaccurate here:
            // currentParsingDetails.setSourceFileArchiveUUID(fileArchiveUUID);
        }

        if (axm.getCodeOrName() != null) {
            d.setCode(toClinicalCode(axm.getCodeOrName()));
        }

        // While not the primary use case, in the future we can move content to be uploaded like this
        // to cut down on the number of source objects
        if (axm.getContent() != null) {
            byte[] fileContent = axm.getContent().getBytes();
            List<DocumentContent> dcs = new ArrayList<>();
            d.setDocumentContents(dcs);
            DocumentContent dc = new DocumentContent();
            dcs.add(dc);
            try{
                String fileContentHash = HashCalculator.getFileHash(fileContent);
                dc.setHash(fileContentHash);
                // APXParser does this but it is inaccurate here:
                // currentParsingDetails.setSourceFileHash(fileContentHash);
            }catch(Exception e){
                log.warn("Error while calculating Hash of the file:"+e.getMessage());
            }
            dc.setLength(fileContent.length);
            dc.setMimeType(axm.getMimeType());

            // The below isn't even used by APXParser,
            // but we do have a uri, so, populate it if you have it
            if (axm.getUri() != null) {
                dc.setUri(axm.getUri());
            }
        }

        if (axm.getDate() != null) {
            d.setDocumentDate(axm.getDate().toDateTimeAtStartOfDay());
        }

        if (axm.getEncounterId() != null) {
            AxmEncounter encounter = new AxmEncounter();
            encounter.setOriginalId(axm.getEncounterId());
            UUID encounterUuid = addEncounter(encounter);
            d.setSourceEncounter(encounterUuid);
        }

        if (axm.getOriginalId() != null) {
            d.setOriginalId(convertId(axm.getOriginalId()));
        }

        if (!isEmpty(axm.getProviderInRoles())) {
            List<ClinicalActor> actors = parseProviders(axm.getProviderInRoles(), sourceId);
            addClinicalActors(d, actors);
        }

        if (axm.getTitle() != null) {
            d.setDocumentTitle(axm.getTitle());
        }

    }
    private void addPrescription(AxmPrescription axm) {
        Prescription p = new Prescription();
        patient.addPrescription(p);
        UUID sourceId = populateBaseObject(p, axm);

        p.setOriginalId(convertId(axm.getOriginalId()));
        if (axm.getCodeOrName() != null) {
            p.setCode(toClinicalCode(axm.getCodeOrName()));
        }
        if (!isEmpty(axm.getProviderInRoles())) {
            List<ClinicalActor> actors = parseProviders(axm.getProviderInRoles(), sourceId);
            addClinicalActors(p, actors);
        }
        // TODO: Not sure yet how to do this with AXM as designed -- needs testing
        // p.setSourceEncounter();
        if (axm.getPrescriptionDate() != null) {
            p.setPrescriptionDate(axm.getPrescriptionDate().toDateTimeAtStartOfDay());
        }

        p.setQuantity(axm.getQuantity());
        p.setFrequency(axm.getFrequency());
        if (axm.getEndDate() != null) {
            p.setEndDate(axm.getEndDate().toDateTimeAtStartOfDay());
        }
        if (axm.getFillDate() != null) {
            p.setFillDate(axm.getFillDate().toDateTimeAtStartOfDay());
        }
        p.setSig(axm.getSig());
        p.setAmount(axm.getAmount());
        p.setDosage(axm.getDosage());
        p.setActivePrescription(axm.getActivePrescription());
        p.setRefillsRemaining(axm.getRefillsRemaining());

        if (axm.getAssociatedMedication() != null) {
            AxmMedication axmMed = axm.getAssociatedMedication();
            Medication m = new Medication();
            p.setAssociatedMedication(m);
            // use the source ID from the parent
            UUID medicationSourceId = populateBaseObject(m, axmMed,sourceId);
            if (axmMed.getOriginalId() != null) {
                m.setOriginalId(convertId(axmMed.getOriginalId()));
            }
            if (axmMed.getCodeOrName() != null) {
                m.setCode(toClinicalCode(axmMed.getCodeOrName()));
            }
            if (!isEmpty(axmMed.getProviderInRoles())) {
                List<ClinicalActor> actors = parseProviders(axmMed.getProviderInRoles(), medicationSourceId);
                addClinicalActors(m, actors);
            }
            // TODO: Not sure yet how to do this with AXM as designed -- needs testing
            // m.setSourceEncounter();
            m.setBrandName(axmMed.getBrandName());
            m.setGenericName(axmMed.getGenericName());
            m.setIngredients(axmMed.getIngredients());
            m.setStrength(axmMed.getStrength());
            m.setForm(axmMed.getForm());
            m.setRouteOfAdministration(axmMed.getRouteOfAdministration());
            m.setUnits(axmMed.getUnits());
        }
    }

    private void addBiometric(AxmBiometricValue axm) {
        BiometricValue b = new BiometricValue();
        patient.addBiometricValue(b);
        UUID sourceId = populateBaseObject(b, axm);

        b.setOriginalId(convertId(axm.getOriginalId()));
        if (!isEmpty(axm.getProviderInRoles())) {
            List<ClinicalActor> actors = parseProviders(axm.getProviderInRoles(), sourceId);
            addClinicalActors(b, actors);
        }

        b.setName(axm.getName());
        b.setValue(new EitherStringOrNumber(axm.getValue()));
        b.setUnitsOfMeasure(axm.getUnits());

        if (axm.getResultDate() != null) {
            b.setResultDate(axm.getResultDate().toDateTimeAtStartOfDay());
        }

        if (axm.getCodeOrName() != null) {
            b.setCode(toClinicalCode(axm.getCodeOrName()));
        }
    }

    private void addSocialHistory(AxmSocialHistory axm) {
        SocialHistory s = new SocialHistory();
        patient.addSocialHistory(s);
        UUID sourceId = populateBaseObject(s, axm);

        s.setOriginalId(convertId(axm.getOriginalId()));
        if (!isEmpty(axm.getProviderInRoles())) {
            List<ClinicalActor> actors = parseProviders(axm.getProviderInRoles(), sourceId);
            addClinicalActors(s, actors);
        }

        s.setCode(convertCode(axm.getGroupCode()));
        s.setType(convertCode(axm.getItemCode()));
        s.setFieldName(axm.getItemName());
        s.setValue(axm.getItemValue());
        s.setDate(axm.getDate().toDateTimeAtStartOfDay());

        if (axm.getEncounterId() != null) {
            AxmEncounter encounter = new AxmEncounter();
            encounter.setOriginalId(axm.getEncounterId());
            UUID encounterUuid = addEncounter(encounter);
            s.setSourceEncounter(encounterUuid);
        }

    }

    private void addLabResult(AxmLabResult axm) {
        LabResult l = new LabResult();
        patient.addLabResult(l);
        UUID sourceId = populateBaseObject(l, axm);

        l.setOriginalId(convertId(axm.getOriginalId()));

        if (!isEmpty(axm.getProviderInRoles())) {
            List<ClinicalActor> actors = parseProviders(axm.getProviderInRoles(), sourceId);
            addClinicalActors(l, actors);
        }

        l.setLabName(axm.getName() == null ? "N/A" : axm.getName());

        if (axm.getSampleDate() != null) {
            l.setSampleDate(axm.getSampleDate().toDateTimeAtStartOfDay());
        }
        if (axm.getCode() != null) {
            l.setCode(convertCode(axm.getCode()));
        }
        if (axm.getPanelCode() != null) {
            l.setPanel(convertCode(axm.getPanelCode()));
        }
        l.setValue(new EitherStringOrNumber(axm.getValue()));
        l.setUnits(axm.getUnits());
        l.setRange(axm.getRange());
        l.setFlag(LabFlags.valueOf(axm.getFlag().toString()));
        l.setLabNote(axm.getNote());

    }


    private void addProcedure(AxmProcedure axm) {
        Procedure p = new Procedure();
        patient.addProcedure(p);
        UUID sourceId = populateBaseObject(p, axm);

        p.setOriginalId(convertId(axm.getOriginalId()));

        if (!isEmpty(axm.getProviderInRoles())) {
            List<ClinicalActor> actors = parseProviders(axm.getProviderInRoles(), sourceId);
            addClinicalActors(p, actors);
        }

        p.setProcedureName(axm.getName() == null ? "N/A" : axm.getName());

        if (axm.getStartDate() != null) {
            p.setPerformedOn(axm.getStartDate().toDateTimeAtStartOfDay());
        }

        if (axm.getEndDate() != null) {
            p.setEndDate(axm.getEndDate().toDateTimeAtStartOfDay());
        }

        if (axm.getCodeOrName() != null) {
            p.setCode(toClinicalCode(axm.getCodeOrName()));
        }

        p.setInterpretation(axm.getInterpretation());

        if (axm.getBodyPart() != null) {
            Anatomy bodySite = new Anatomy();
            ClinicalCode code = toClinicalCode(axm.getBodyPart());
            bodySite.setCode(code);
            bodySite.setAnatomicalStructureName(code.getDisplayName());
            p.setBodySite(bodySite);
        }

        if (!isEmpty(axm.getDiagnoses())) {
            List<ClinicalCode> diagnoses = new ArrayList<>();
            for (AxmCodeOrName diagnosis : axm.getDiagnoses()) {
                diagnoses.add(toClinicalCode(diagnosis));
            }
            p.setSupportingDiagnosis(diagnoses);
        }
    }

    private void addProblem(AxmProblem axm) {
        Problem p = new Problem();
        patient.addProblem(p);
        UUID sourceId = populateBaseObject(p, axm);

        p.setOriginalId(convertId(axm.getOriginalId()));

        if (!isEmpty(axm.getProviderInRoles())) {
            List<ClinicalActor> actors = parseProviders(axm.getProviderInRoles(), sourceId);
            addClinicalActors(p, actors);
        }

        p.setProblemName(axm.getName() == null ? "N/A" : axm.getName());

        if (axm.getResolution() != null) {
            p.setResolutionStatus(ResolutionStatus.valueOf(axm.getResolution().toString()));
        }

        p.setTemporalStatus(axm.getTemporalStatus());

        if (axm.getStartDate() != null) {
            p.setStartDate(axm.getStartDate().toDateTimeAtStartOfDay());
        }

        if (axm.getEndDate() != null) {
            p.setEndDate(axm.getEndDate().toDateTimeAtStartOfDay());
        }

        if (axm.getDiagnosisDate() != null) {
            p.setDiagnosisDate(axm.getDiagnosisDate().toDateTimeAtStartOfDay());
        }

        if (axm.getCodeOrName() != null) {
            p.setCode(toClinicalCode(axm.getCodeOrName()));
        }
    }

    private UUID addEncounter(AxmEncounter axm) {
        Encounter e = new Encounter();
        patient.addEncounter(e);
        UUID sourceId = populateBaseObject(e, axm);

        e.setOriginalId(convertId(axm.getOriginalId()));

        if (axm.getStartDate() != null) {
            e.setEncounterStartDate(axm.getStartDate().toDateTimeAtStartOfDay());
        }

        if (axm.getEndDate() != null) {
            e.setEncounterEndDate(axm.getEndDate().toDateTimeAtStartOfDay());
        }

        e.setEncType(EncounterType.FACE_TO_FACE);

        if (axm.getCareSite() != null) {
            CareSite careSite = new CareSite();
            careSite.setParsingDetailsId(parsingId);
            careSite.setCareSiteName(axm.getCareSite());
            e.setSiteOfService(careSite);
        }

        if (!isEmpty(axm.getProviderInRoles())) {
            List<ClinicalActor> actors = parseProviders(axm.getProviderInRoles(), sourceId);
            addClinicalActors(e, actors);
        }

        if (axm.getCodeOrName() != null) {
            e.setCode(toClinicalCode(axm.getCodeOrName()));
        }
        return e.getInternalUUID();
    }

    private void addProvider(AxmProvider axm) {
        ClinicalActor provider = new ClinicalActor();
        patient.addClinicalActor(provider);
        UUID sourceId = populateBaseObject(provider, axm);

        ExternalID providerId = convertId(axm.getOriginalId());
        provider.setOriginalId(providerId);
        provider.setPrimaryId(providerId);

        if (!isEmpty(axm.getAlternateIds())) {
            List<ExternalID> alternateIds = new ArrayList<>();
            for (AxmExternalId id : axm.getAlternateIds()) {
                alternateIds.add(convertId(id));
            }
            provider.setAlternateIds(alternateIds);
        }

        Name name = new Name();

        if (!isEmpty(axm.getGivenNames())) {
            List<String> givenNames = new ArrayList<>();
            for (String givenName : axm.getGivenNames()) {
                givenNames.add(givenName);
            }
            name.setGivenNames(givenNames);
        }

        if (!isEmpty(axm.getFamilyNames())) {
            List<String> familyNames = new ArrayList<>();
            for (String familyName : axm.getFamilyNames()) {
                familyNames.add(familyName);
            }
            name.setFamilyNames(familyNames);
        }

        if (!isEmpty(axm.getPrefixes())) {
            List<String> prefixes = new ArrayList<>();
            for (String prefix : axm.getPrefixes()) {
                prefixes.add(prefix);
            }
            name.setPrefixes(prefixes);
        }

        if (!isEmpty(axm.getSuffixes())) {
            List<String> suffixes = new ArrayList<>();
            for (String suffix : axm.getSuffixes()) {
                suffixes.add(suffix);
            }
            name.setSuffixes(suffixes);
        }

        name.setNameType(NameType.CURRENT_NAME);
        provider.setActorGivenName(name);

        provider.setTitle(axm.getTitle());

        if (axm.getOrgName() != null) {
            Organization org = new Organization();
            org.setName(axm.getOrgName());
            provider.setAssociatedOrg(org);
        }

        ContactDetails cd = new ContactDetails();
        provider.setContactDetails(cd);

        if (!isEmpty(axm.getEmails())) {
            List<String> alternateEmails = new ArrayList<>();
            cd.setAlternateEmails(alternateEmails);
            for (String email : axm.getEmails()) {
                if (cd.getPrimaryEmail() == null) {
                    cd.setPrimaryEmail(email);
                } else {
                    alternateEmails.add(email);
                }
            }
        }

        if (axm.getPhoneNumber() != null) {
            cd.setPrimaryPhone(convertPhone(axm.getPhoneNumber()));
        }

        provider.setRole(ActorRole.ATTENDING_PHYSICIAN);
    }

    private void addCoverage(AxmCoverage axm) {
        Coverage c = new Coverage();
        patient.addCoverage(c);
        UUID sourceId = populateBaseObject(c, axm);

        //c.setOriginalId(convertId(axm.getOriginalId()));

        if (axm.getPlanName() != null) {
            c.setHealthPlanName(axm.getPlanName());
        } else {
            c.setHealthPlanName("N/A");
        }

        if (axm.getStartDate() != null) {
            c.setStartDate(axm.getStartDate());
        }

        if (axm.getEndDate() != null) {
            c.setEndDate(axm.getEndDate());
        }

        if (axm.getSubscriberId() != null) {
            c.setSubscriberID(convertId(axm.getSubscriberId()));
        }

        if (axm.getBeneficiaryId() != null) {
            c.setBeneficiaryID(convertId(axm.getBeneficiaryId()));
        }

        if (axm.getGroupId() != null) {
            c.setGroupNumber(convertId(axm.getGroupId()));
        }

        if (axm.getSequenceNumber() != null) {
            c.setSequenceNumber(axm.getSequenceNumber());
        } else {
            c.setSequenceNumber(1);
        }
        
        // based on a discussion with product - this behavior is incorrect. If no value is available
        // the correct behaviour should be to default to UNK and not HMO. HMO is a very particular type
        // of health insurance. As we move more towards using our insights to make decisions at the point of
        // care - knowing the exact type or more importantly - the unavailabilty of this information becomes
        // very important.
        // TODO: Verify that changing the code doesn't affect downstream applications and make the change.
        if (axm.getCoverageType() != null) {
            c.setType(CoverageType.valueOf(axm.getCoverageType().toString()));
        } else {
            c.setType(CoverageType.HMO);
        }

        /*
        if (axm.getCodeOrName() != null) {
            c.setCode(toClinicalCode(axm.getCodeOrName()));
        }
        */
    }

    private List<ClinicalActor> parseProviders(Iterable<AxmProviderInRole> axmProviders, UUID sourceId) {
        List<ClinicalActor> actors = new ArrayList<>();
        for (AxmProviderInRole providerInRole : axmProviders) {
            ClinicalActor actor = new ClinicalActor();
            ExternalID actorId = new ExternalID();
            actorId.setId(providerInRole.getProviderId().getId());
            actorId.setAssignAuthority(providerInRole.getProviderId().getAssignAuthority());
            actor.setOriginalId(actorId);
            actor.setPrimaryId(actorId);
            actor.setRole(ActorRole.valueOf(providerInRole.getActorRole().toString()));
            actor.setParsingDetailsId(parsingId);
            actor.setSourceId(sourceId);
            actors.add(actor);
        }
        return actors;
    }

    /**
     * Adds clinical actors to both patient object and coded object ensuring referential integrity.
     */
    private void addClinicalActors(CodedBaseObject coded, Iterable<ClinicalActor> actors) {
        for (ClinicalActor actor : actors) {
            patient.addClinicalActor(actor);
            UUID actorId = actor.getClinicalActorId();
            if (coded.getPrimaryClinicalActorId() == null) {
                coded.setPrimaryClinicalActorId(actorId);
            } else {
                coded.getSupplementaryClinicalActorIds().add(actorId);
            }
        }
    }

    /**
     * Populates properties common to all base objects.
     *
     * @return source uuid.
     */
    private UUID populateBaseObject(BaseObject obj, AxmRow row) {
        Source s = addSource(row.getSource());
        UUID sourceId = s.getInternalUUID();
        return populateBaseObject(obj,row,sourceId);
    }

    /**
     * Populates properties common to all base objects.
     *
     * @return source uuid.
     */
    private UUID populateBaseObject(BaseObject obj, AxmRow row, UUID sourceId) {
        obj.setSourceId(sourceId);
        obj.setParsingDetailsId(parsingId);

        for (String key : row.getMetaKeys()) {
            obj.setMetaTag(key, row.getMetaTag(key));
        }

        if (row.getEditType() != null) {
            obj.setEditType(EditType.valueOf(row.getEditType().toString()));
        }
        return sourceId;
    }

    /**
     * Parses a Source object and adds it to a patient if the source is unique.
     */
    private Source addSource(AxmSource s) {
        String sourceUniqueKey = s.getSystem() + "^" + s.getType() + "^" + s.getDate().toString();
        if (s.getDciStartDate() != null)
            sourceUniqueKey = sourceUniqueKey + "^" + s.getDciStartDate() + "^" + s.getDciEndDate();
        if (!dataSources.containsKey(sourceUniqueKey)) {
            Source source = new Source();
            source.setSourceId(source.getInternalUUID());
            source.setParsingDetailsId(parsingId);
            source.setSourceSystem(s.getSystem());
            source.setSourceType(s.getType());
            source.setCreationDate(s.getDate().toDateTimeAtStartOfDay());
            source.setDciStart(s.getDciStartDate());
            source.setDciEnd(s.getDciEndDate());
            dataSources.put(sourceUniqueKey, source);
            patient.addSource(source);
        }
        return dataSources.get(sourceUniqueKey);
    }

    private ExternalID convertId(AxmExternalId axm) {
        ExternalID eid = new ExternalID();
        eid.setId(axm.getId());
        eid.setAssignAuthority(axm.getAssignAuthority());
        return eid;
    }

    private TelephoneNumber convertPhone(AxmPhoneNumber axm) {
        TelephoneNumber number = new TelephoneNumber();
        number.setPhoneNumber(axm.getNumber());
        number.setPhoneType(TelephoneType.valueOf(axm.getPhoneType().toString()));
        return number;
    }

    private ClinicalCode convertCode(AxmClinicalCode axm) {
        ClinicalCode cc = new ClinicalCode();
        cc.setDisplayName(axm.getName() == null ? "N/A" : axm.getName());
        cc.setCode(axm.getCode());
        cc.setCodingSystem(axm.getSystem());
        cc.setCodingSystemOID(axm.getSystemOid());
        cc.setCodingSystemVersions(axm.getSystemVersion());

        // coding system name override if resolves by oid
        String systemOid = axm.getSystemOid();
        CodingSystem system = CodingSystem.byOid(systemOid);
        if (system != null) {
            cc.setCodingSystem(system.name());
        }

        return cc;
    }
}
