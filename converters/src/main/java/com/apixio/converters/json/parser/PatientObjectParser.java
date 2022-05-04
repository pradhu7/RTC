package com.apixio.converters.json.parser;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apixio.converters.base.Parser;
import com.apixio.model.file.catalog.jaxb.generated.ApxCatalog.CatalogEntry;
import com.apixio.model.patient.BaseObject;
import com.apixio.model.patient.Document;
import com.apixio.model.patient.DocumentType;
import com.apixio.model.patient.ParserType;
import com.apixio.model.patient.ParsingDetail;
import com.apixio.model.patient.Patient;
import com.apixio.model.utility.PatientJSONParser;

public class PatientObjectParser extends Parser
{
    private static final String PARSER_VERSION = "1.0";
    private Patient patient;

    private static final Logger logger = LoggerFactory.getLogger(PatientObjectParser.class);

    @Override
    public void parse(InputStream apoDocument, CatalogEntry catalogEntry) throws Exception
    {
        PatientJSONParser jp = new PatientJSONParser();

        patient = jp.parsePatientData(new String(IOUtils.toByteArray(apoDocument), "UTF-8"));

        if (catalogEntry != null && catalogEntry.getDocumentUUID() != null)
        {
            if (patient.getDocuments() != null && patient.getDocuments().iterator().hasNext())
            {
                Document document = patient.getDocuments().iterator().next();
                document.setInternalUUID(UUID.fromString(catalogEntry.getDocumentUUID()));
            }
            else
            {
                throw new Exception("No documents found in APO.");
            }
        }
        else
            throw new Exception("No Document UUID found in Catalog file.");
        // add parsing detail for this data
        ParsingDetail currentParsingDetails = new ParsingDetail();
        currentParsingDetails.setContentSourceDocumentType(DocumentType.STRUCTURED_DOCUMENT);
        currentParsingDetails.setParser(ParserType.APO);
        currentParsingDetails.setParserVersion(PARSER_VERSION);
        currentParsingDetails.setParsingDateTime(DateTime.now());
        Map<String, String> metadata = currentParsingDetails.getMetadata();
        if (metadata == null)
            metadata = new HashMap<>();
        if (getSourceDocumentName() != null)
        {
            metadata.put("sourceDocumentName", getSourceDocumentName());
            currentParsingDetails.setMetadata(metadata);
            logger.info("Added source document in the parsing details metadata.");
        }
        this.patient.addParsingDetail(currentParsingDetails);
        populateParsingDetailsId(currentParsingDetails.getParsingDetailsId());


        populatePrimaryExternalId(patient);
    }
    
    private void populatePrimaryExternalId(Patient p) throws Exception
    {
        p.setPrimaryExternalID(getPrimaryExternalId(p.getExternalIDs()));
    }

    private void populateParsingDetailsId(UUID uuid)
    {
        if (patient != null)
        {
            addParsingDetailsToCodedBaseObjects(patient.getAdministrations(), uuid);
            addParsingDetailsToCodedBaseObjects(patient.getAllergies(), uuid);
            addParsingDetailsToCodedBaseObjects(patient.getAlternateContactDetails(), uuid);
            addParsingDetailsToCodedBaseObjects(patient.getAlternateDemographics(), uuid);
            addParsingDetailsToCodedBaseObjects(patient.getApixions(), uuid);
            addParsingDetailsToCodedBaseObjects(patient.getBiometricValues(), uuid);
            addParsingDetailsToCodedBaseObjects(patient.getCareSites(), uuid);
            addParsingDetailsToCodedBaseObjects(patient.getClinicalActors(), uuid);
            addParsingDetailsToCodedBaseObjects(patient.getDocuments(), uuid);
            addParsingDetailsToCodedBaseObjects(patient.getEncounters(), uuid);
            // addParsingDetailsToCodedBaseObjects(patient.getExternalIDs(), uuid);
            addParsingDetailsToCodedBaseObjects(patient.getFamilyHistories(), uuid);
            addParsingDetailsToCodedBaseObjects(patient.getLabs(), uuid);
            addParsingDetailsToCodedBaseObjects(patient.getPrescriptions(), uuid);
            addParsingDetailsToCodedBaseObject(patient.getPrimaryContactDetails(), uuid);
            addParsingDetailsToCodedBaseObject(patient.getPrimaryDemographics(), uuid);
            addParsingDetailsToCodedBaseObjects(patient.getProblems(), uuid);
            addParsingDetailsToCodedBaseObjects(patient.getProcedures(), uuid);
            addParsingDetailsToCodedBaseObjects(patient.getSocialHistories(), uuid);
            addParsingDetailsToCodedBaseObjects(patient.getSources(), uuid);
        }
    }

    private void addParsingDetailsToCodedBaseObject(BaseObject baseObject, UUID uuid)
    {
        if (baseObject != null)
        {
            if (baseObject.getParsingDetailsId() != null) logger.info("Overriding Parsing Details!!!");
            // if (baseObject.getParsingDetailsId() == null) // we want to override any default values set in the loader
            baseObject.setParsingDetailsId(uuid);
        }
    }

    private void addParsingDetailsToCodedBaseObjects(Iterable<? extends BaseObject> baseObjects, UUID uuid)
    {
        if (baseObjects != null)
        {
            for (BaseObject baseObject : baseObjects)
            {
                addParsingDetailsToCodedBaseObject(baseObject, uuid);
            }
        }
    }
    
    @Override
    public Patient getPatient() throws Exception
    {
        return patient;
    }
}
