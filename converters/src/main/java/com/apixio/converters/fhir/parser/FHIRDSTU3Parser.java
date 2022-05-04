package com.apixio.converters.fhir.parser;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.apixio.converters.base.BaseUtils;
import com.apixio.converters.base.Parser;
import com.apixio.model.EitherStringOrNumber;
import com.apixio.model.file.catalog.jaxb.generated.ApxCatalog;
import com.apixio.model.patient.*;
import com.apixio.model.patient.Medication;
import com.apixio.utility.HashCalculator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Procedure;
import org.hl7.fhir.dstu3.model.MedicationStatement;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.apixio.converters.fhir.utils.FHIRDSTU3Utils;
import java.io.InputStream;
import java.util.*;

// TODO: Open Issues
// 1. How is the array of resources created?
// 2. Will this only be "non-document" resources?
// 3. Will Practitioner data be filtered and loaded for each patient by integration?
// 4. How will referenced Medication data be supplied?
// 5. should we store the "id" as another id even though that field is non-standard?
// 6. Data loaded separately should link up, meaning reference id would have to be used.
// 7. should we do a pass through to create encounters and providers first so the objects will exist?
// 8. To get true bi-directional APO<-->FHIR support, all non-supported fields but me stored in well-defined metadata locations
// 9. Only add Care Sites once per name at the global patient level.
// 10. Should we only create one Clinical Actor per Practitioner or one per role?
// 11. Should we populate clinical actors and encounters first?
// 12. Should MedicationRequest always be imported "via" a MedicationStatement? What if we have both for the same Medication?
// 13. getCodedBaseObject should enable preference of particular code systems by type (ex: LOINC) as there are proprietary codes that are of little use.
// 14. Figure out if addition of Enumerations will break existing Data clients if they don't use latest model.

public class FHIRDSTU3Parser extends Parser {

    private static final String PARSER_VERSION = "1.0";
    private com.apixio.model.patient.Patient patient  = new com.apixio.model.patient.Patient();
    private Source primarySource = new Source();
    private ParsingDetail currentParsingDetails = new ParsingDetail();
    private HashMap<String, UUID> clinicalActors = new HashMap<>();
    private HashMap<String, UUID> encounters = new HashMap<>();
    private HashMap<String, CareSite> careSites = new HashMap<>();
    private HashMap<String,Map<String,IBaseResource>> resourceMap = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(FHIRDSTU3Parser.class);

    @Override
    public void parse(InputStream document, ApxCatalog.CatalogEntry catalogEntry) throws Exception {
        parse(document);

        // TODO: Populate metadata and patient identifiers from the catalog file
        populateContextFromCatalog(catalogEntry);
    }

    // Convert the FHIR resources from a list of FHIR Resources JSON Strings
    // to a patient with no additional context
    public void parse(InputStream document) throws Exception {
        byte[] docBytes = BaseUtils.getBytesFromInputStream(document);
        populateParsingDetails(docBytes);
        populatePrimarySource();

        // Create a FHIR context and parser
        FhirContext ctx = FhirContext.forDstu3();
        IParser fhirParser = ctx.newJsonParser();

        // Convert the InputStream array into a List of JSON resources
        ObjectMapper objectMapper = new ObjectMapper();
        // TODO: We can support single FHIR Resources and "Bundle" resources in the future by relaxing this deserializer
        List<Map<String,Object>> resourceList = objectMapper.readValue(docBytes, ArrayList.class);
        // Loop through resource JSON to convert to IBaseResource and create map of resources by type and id
        // TODO: What if there is a failure in extracting only some resources? Currently fails entire parser.
        for (Map<String,Object> resource : resourceList) {
            String resourceString = objectMapper.writeValueAsString(resource.get("resource"));
            IBaseResource baseResource = fhirParser.parseResource(resourceString);
            String resourceType = baseResource.getIdElement().getResourceType();
            Map<String,IBaseResource> resourcesByType = resourceMap.getOrDefault(resourceType,new HashMap<String, IBaseResource>());
            resourcesByType.put(baseResource.getIdElement().getIdPart(),baseResource);
            resourceMap.put(resourceType,resourcesByType);
        }

        Set<String> unsupportedSections = new HashSet<String>();
        // Loop through each type of resource
        for (String resourceType: resourceMap.keySet()) {
            // parse each resource of this type
            for (IBaseResource baseResource : resourceMap.get(resourceType).values()) {
                switch (resourceType) {
                    case "AllergyIntolerance": {
                        parseAllergyIntolerance((AllergyIntolerance) baseResource);
                        break;
                    }
                    case "ClinicalImpression": {
                        parseClinicalImpression((ClinicalImpression) baseResource);
                        break;
                    }
                    case "Condition": {
                        parseCondition((Condition) baseResource);
                        break;
                    }
                    case "DiagnosticReport": {
                        parseDiagnosticReport((DiagnosticReport) baseResource);
                        break;
                    }
                    case "DocumentReference": {
                        parseDocumentReference((DocumentReference) baseResource);
                        break;
                    }
                    case "Encounter": {
                        parseEncounter((Encounter) baseResource);
                        break;
                    }
                    case "Goal": {
                        parseGoal((Goal) baseResource);
                        break;
                    }
                    case "Immunization": {
                        parseImmunization((Immunization) baseResource);
                        break;
                    }
                    case "MedicationRequest": {
                        parseMedicationRequest((MedicationRequest) baseResource);
                        break;
                    }
                    case "MedicationStatement": {
                        parseMedicationStatement((MedicationStatement) baseResource);
                        break;
                    }
                    case "Observation": {
                        parseObservation((Observation) baseResource);
                        break;
                    }
                    case "Patient": {
                        parsePatient((Patient) baseResource);
                        break;
                    }
                    case "Procedure": {
                        parseProcedure((Procedure) baseResource);
                        break;
                    }
                    case "Practitioner":
                    case "Medication":
                    case "Location":
                    case "Organization":
    //                    logger.info("Skipping referenced resource: " + resourceType);
                        break;
                    default: {
    //                    logger.error("Unsupported type: " + resourceType);
                        unsupportedSections.add(resourceType);
                    }
                }
            }
        }
        logger.error("Unsupported sections found: " + String.join(",", unsupportedSections));
    }

    private IBaseResource getReferencedResource(String reference) {
        IBaseResource referencedResource = null;
        if (reference == null) {
            logger.info("Referenced resource is null");
        } else {
            String[] referenceParts = reference.split("/");
            if (referenceParts.length != 2) {
                logger.error("Malformed reference: " + reference);
            } else {
                String resourceType = referenceParts[0];
                String resourceId = referenceParts[1];
                if (!resourceMap.containsKey(resourceType)) {
                    logger.error("No resources of type: " + resourceType);
                    return null;
                } else if (!resourceMap.get(resourceType).containsKey(resourceId)) {
                    logger.error("No resource of type " + resourceType + " with id: " + resourceId);
                } else {
                    referencedResource = resourceMap.get(resourceType).get(resourceId);
                }
            }
        }
        return referencedResource;
    }

    @Override
    public com.apixio.model.patient.Patient getPatient() throws Exception {
        return patient;
    }

    private void populateParsingDetails(byte[] docBytes) {
        currentParsingDetails.setContentSourceDocumentType(DocumentType.STRUCTURED_DOCUMENT);
        currentParsingDetails.setParser(ParserType.FHIR_DSTU3);
        currentParsingDetails.setParserVersion(PARSER_VERSION);
        currentParsingDetails.setParsingDateTime(DateTime.now());
        String fileContentHash = HashCalculator.getFileHash(docBytes);
        currentParsingDetails.setSourceFileHash(fileContentHash);
        Map<String, String> metadata = currentParsingDetails.getMetadata();
        if(metadata==null)
            metadata = new HashMap<String, String>();
        if(getSourceDocumentName()!=null){
            metadata.put("sourceDocumentName", getSourceDocumentName());
            currentParsingDetails.setMetadata(metadata);
            logger.info("Added source document in the parsing details metadata.");
        }
        this.patient.addParsingDetail(currentParsingDetails);
    }

    // TODO: How do we get global details from an array of resources, especially things like id, actor and organization.
    private void populatePrimarySource() {
        primarySource.setSourceId(primarySource.getInternalUUID());
        primarySource.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
//        primarySource.setClinicalActorId(getAuthorClinicalActorId(ccdaDocument.getAuthors()));
        primarySource.setSourceType("FHIR_DSTU3");
        this.patient.addSource(primarySource);
    }

    private void populateContextFromCatalog(ApxCatalog.CatalogEntry catalogEntry) throws Exception {
        ApxCatalog.CatalogEntry.Patient catalogPatient = catalogEntry.getPatient();
        // Add the IDs found in the catalog file
        for (ExternalID externalID : BaseUtils.getExternalIdsFromCatalog(catalogPatient.getPatientId())) {
            patient.addExternalId(externalID);
        }
        // Set the primary ID now that we've extended the list (note this will remain unset if parser used without catalog)
        patient.setPrimaryExternalID(getPrimaryExternalId(patient.getExternalIDs()));

        primarySource.setCreationDate(DateTime.parse(catalogEntry.getCreationDate()));
//        primarySource.setOrganization(getCustodianOrganization());
        ExternalID originalId = new ExternalID();
        originalId.setAssignAuthority(catalogEntry.getDocumentId());
        primarySource.setOriginalId(originalId);
        primarySource.setSourceSystem(catalogEntry.getSourceSystem());
        currentParsingDetails.setSourceFileArchiveUUID(UUID.fromString(catalogEntry.getDocumentUUID()));
        currentParsingDetails.setSourceUploadBatch(catalogEntry.getBatchId());
    }

    private <T extends CodedBaseObject> T getCodedBaseObject(Class<T> objectClass, List<Identifier> ids, List<Coding> codes) throws InstantiationException, IllegalAccessException {
        T codedBaseObject = objectClass.newInstance();
        populateCodedBaseObject(codedBaseObject, ids, codes);
        return codedBaseObject;
    }

    private <T extends CodedBaseObject> void populateCodedBaseObject(CodedBaseObject codedBaseObject, List<Identifier> ids, List<Coding> codes) throws InstantiationException, IllegalAccessException {
        codedBaseObject.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
        codedBaseObject.setSourceId(primarySource.getSourceId());
        if (ids != null) {
            FHIRDSTU3Utils.setBaseObjectIds(codedBaseObject, ids);
        }
        if (codes != null) {
            FHIRDSTU3Utils.setBaseObjectCodes(codedBaseObject, codes);
        }
    }

    private void parseAllergyIntolerance(AllergyIntolerance allergyIntolerance) throws Exception {
        Allergy allergy = getCodedBaseObject(Allergy.class, allergyIntolerance.getIdentifier(), allergyIntolerance.getCode().getCoding());
        allergy.setAllergen(allergyIntolerance.getCode().getText());
        allergy.setDiagnosisDate(new DateTime(allergyIntolerance.getAssertedDateElement().getValue()));
        //allergy.setReactionDate(reactionDate)

        // NOTE: Delimiter used should be reversible for creation of outbound FHIR resources
        allergy.setReaction(FHIRDSTU3Utils.getAllergyReactionCode(allergyIntolerance));
        allergy.setReactionSeverity(FHIRDSTU3Utils.getAllergySeverities(allergyIntolerance));

        //allergy.setSourceEncounter(sourceEncounter)
        //allergy.setPrimaryClinicalActor(primaryClinicalActor);
        //allergy.setSupplementaryClinicalActors(supplementaryClinicalActors)

        //allergy.setMetadata(metadata;)
        //allergy.setMetaTag(tag, value)

        patient.addAllergy(allergy);
    }

    // TODO: Current pipeline does not support creating multiple documents from one input
    private void parseClinicalImpression(ClinicalImpression clinicalImpression) throws Exception {
        // TODO: for now we will generate documents outside of the pipeline for two reasons
        //  1. The etl-v2 pipeline doesn't support generating multiple documents from one source file
        //  2. Our first FHIR integration, with Epic, requires note stitching and custom metadata which may be nonstandard for FHIR
        logger.error("Creating documents from ClinicalImpression is not yet supported in etl-v2 pipeline");
//        Document document = getCodedBaseObject(Document.class, clinicalImpression.getIdentifier(), clinicalImpression.getCode().getCoding());
//        document.setStringContent(clinicalImpression.getSummary());
    }

    private void parseCondition(Condition condition) throws Exception {
        Problem problem = getCodedBaseObject(Problem.class, condition.getIdentifier(), condition.getCode().getCoding());
        problem.setProblemName(condition.getCode().getText());
        problem.setResolutionStatus(ResolutionStatus.valueOf(condition.getClinicalStatus().getDisplay().toUpperCase()));
        if (condition.getOnsetPeriod() != null) {
            problem.setStartDate(new DateTime(condition.getOnsetPeriod().getStart()));
        }
        if (condition.getAbatementPeriod() != null) {
            problem.setEndDate(new DateTime(condition.getAbatementPeriod().getEnd()));
        }
        problem.setDiagnosisDate(new DateTime(condition.getAssertedDate()));
        // TODO: Add additional detail when example data is obtained
//        problem.setSeverity(severity);
//        problem.setTemporalStatus(temporalStatus);
//        problem.setSourceEncounter(sourceEncounter);
//        problem.setPrimaryClinicalActorId(primaryClinicalActor);
//        problem.setSupplementaryClinicalActors(supplementaryClinicalActors);
        patient.addProblem(problem);
    }

    private void parseDiagnosticReport(DiagnosticReport diagnosticReport) {
        // DiagnosticReport resources may reference an Observation of type "Narrative" for various types of reports
        // Those would map to documents, which aren't supported. It isn't yet clear if the context for the DiagnosticReport
        // for these and other types of referenced Observations is needed or if the Observation itself is sufficient.
        // TODO: for now we will generate documents outside of the pipeline for two reasons
        //  1. The etl-v2 pipeline doesn't support generating multiple documents from one source file
        //  2. Our first FHIR integration, with Epic, requires note stitching and custom metadata which may be nonstandard for FHIR
        logger.error("Creating documents from DiagnosticReport is not yet supported in etl-v2 pipeline");
    }

    private void parseDocumentReference(DocumentReference documentReference) {
        // TODO: for now we will generate documents outside of the pipeline for two reasons
        //  1. The etl-v2 pipeline doesn't support generating multiple documents from one source file
        //  2. Our first FHIR integration, with Epic, requires note stitching and custom metadata which may be nonstandard for FHIR
        logger.error("Creating documents from DocumentReference is not yet supported in etl-v2 pipeline");
    }

    private void parseEncounter(Encounter fhirEncounter) throws InstantiationException, IllegalAccessException {
        com.apixio.model.patient.Encounter encounter = null;
        String encounterFhirId = fhirEncounter.getIdElement().getIdPart();
        List<Coding> codings = new ArrayList<Coding>();
        for (CodeableConcept codeableConcept : fhirEncounter.getType()) {
            codings.addAll(codeableConcept.getCoding());
        }
        // Check if this encounter ID has already been stubbed
        if (encounters.containsKey(encounterFhirId)) {
            encounter = patient.getEncounterById(encounters.get(encounterFhirId));
            populateCodedBaseObject(encounter, fhirEncounter.getIdentifier(), codings);
        } else {
            encounter = getCodedBaseObject(com.apixio.model.patient.Encounter.class, fhirEncounter.getIdentifier(), codings);
            encounters.put(encounterFhirId, encounter.getEncounterId());
            patient.addEncounter(encounter);
        }
        encounter.setEncType(FHIRDSTU3Utils.getEncTypeFromClass(fhirEncounter.getClass_()));
        encounter.setChiefComplaints(FHIRDSTU3Utils.getClinicalCodesFromCodeableConcepts(fhirEncounter.getReason()));
        encounter.setEncounterStartDate(new DateTime(fhirEncounter.getPeriod().getStart()));
        encounter.setEncounterEndDate(new DateTime(fhirEncounter.getPeriod().getEnd()));
        CareSite siteOfService = getCareSiteFromReference(fhirEncounter.getLocationFirstRep().getLocation());
        encounter.setSiteOfService(siteOfService);

        for (Encounter.EncounterParticipantComponent participant : fhirEncounter.getParticipant()) {
            UUID clinicalActorId = getClinicalActorFromEncounterParticipantComponent(participant);
            // TODO: Integrate health care provider taxonomy code system
            if (encounter.getPrimaryClinicalActorId() == null) {
                encounter.setPrimaryClinicalActorId(clinicalActorId);
            } else {
                encounter.getSupplementaryClinicalActorIds().add(clinicalActorId);
            }
        }
    }

    private void parseGoal(Goal goal) {
        logger.warn("Goal not yet supported");
    }

    private void parseImmunization(Immunization immunization) throws Exception {
        // TODO: Where can we put the lot number
        // TODO: Where can we put the manufacturer
        // TODO: Where can we put the "site" of immunization, such as "Right arm"
        Administration administration = getCodedBaseObject(Administration.class, immunization.getIdentifier(),immunization.getVaccineCode().getCoding());
        if (administration.getCode() == null) {
            administration.setCode(FHIRDSTU3Utils.getClinicalCodeFromDisplay(immunization.getVaccineCode().getText()));
        } else if (administration.getCode().getDisplayName() == null) {
            administration.getCode().setDisplayName(immunization.getVaccineCode().getText());
        }
        administration.setAdminDate(new DateTime(immunization.getDate()));
//        administration.setAmount();
        administration.setDosage(FHIRDSTU3Utils.getDisplayFromDoseQuantity(immunization.getDoseQuantity()));
//        administration.setStartDate();
//        administration.setEndDate();
//        administration.setQuantity();
//        administration.setMedicationSeriesNumber();
        Medication medication = new Medication();
        medication.setRouteOfAdministration(immunization.getRoute().getText());
        // set the medication code to the vaccine code
        medication.setCode(administration.getCode());
        administration.setMedication(medication);
        for (Immunization.ImmunizationPractitionerComponent practitioner : immunization.getPractitioner()) {
            UUID clinicalActorId = getClinicalActorFromImmunizationPractitionerComponent(practitioner);
            if (administration.getPrimaryClinicalActorId() == null) {
                administration.setPrimaryClinicalActorId(clinicalActorId);
            } else {
                administration.getSupplementaryClinicalActorIds().add(clinicalActorId);
            }
        }
        administration.setSourceEncounter(getEncounterFromReference(immunization.getEncounter().getReference()));
        patient.addAdministration(administration);
    }

    private void parseMedicationRequest(MedicationRequest medicationRequest) throws Exception {
        Prescription prescription = getCodedBaseObject(Prescription.class, medicationRequest.getIdentifier(),medicationRequest.getCategory().getCoding());
        prescription.setPrescriptionDate(new DateTime(medicationRequest.getAuthoredOn()));
        Medication medication = getMedicationFromReference(medicationRequest.getMedicationReference());
        medication.setRouteOfAdministration(FHIRDSTU3Utils.getRouteofAdministration(medicationRequest.getDosageInstruction()));
        prescription.setAssociatedMedication(medication);
        prescription.setDosage(FHIRDSTU3Utils.getDosage(medicationRequest.getDosageInstruction()));
        prescription.setFrequency(FHIRDSTU3Utils.getFrequency(medicationRequest.getDosageInstruction()));
        prescription.setSig(FHIRDSTU3Utils.getInstructions(medicationRequest.getDosageInstruction()));

        // Get the prescribing doctor
        if (medicationRequest.getRequester().getAgent()!= null) {
            String reference = medicationRequest.getRequester().getAgent().getReference();
            String display = medicationRequest.getRequester().getAgent().getDisplay();
            // TODO: What role should we use for the source of information? using reviewer == "REVIEWING_PHYSICIAN" for now
            prescription.setPrimaryClinicalActorId(getClinicalActor(reference, display, "requester"));
        }
        // If this was recorded by a separate provider, get them
        if (medicationRequest.getRecorder() != null) {
            String reference = medicationRequest.getRecorder().getReference();
            String display = medicationRequest.getRecorder().getDisplay();
            // TODO: What role should we use for the source of information? using reviewer == "REVIEWING_PHYSICIAN" for now
            prescription.setSupplementaryClinicalActors(Collections.singletonList(getClinicalActor(reference, display, "recorder")));
        }
        if (medicationRequest.hasDispenseRequest()) {
            if (medicationRequest.getDispenseRequest().hasQuantity()) {
                prescription.setQuantity(medicationRequest.getDispenseRequest().getQuantity().getValue().doubleValue());
            }
        }
        patient.addPrescription(prescription);
    }

    // MedicationStatement is used for:
    //   - the recording of non-prescription and/or recreational drugs
    //   - the recording of an intake medication list upon admission to hospital
    //   - the summarization of a patient's "active medications" in a patient profile
    private void parseMedicationStatement(MedicationStatement medicationStatement) throws Exception {
        // TODO: Some MedicationStatement resources are "basedOn" a MedicationRequest and thus could lead to duplicate data
        for (Reference referencedMedicationRequest : medicationStatement.getBasedOn()) {
            if (getReferencedResource(referencedMedicationRequest.getReference()) != null) {
                logger.info("Skipping MedicationStatement that is based on an available MedicationRequest with id: " + referencedMedicationRequest.getReference());
                return;
            } else {
                logger.error("Referenced MedicationStatement not found in resources: " + referencedMedicationRequest.getReference());
            }
        }
        // Use the category code for this prescription. The actual medication code will be in the referenced medication
        // Note on AXM that the medication code is being used in the Prescription CodedBaseObject
        Prescription prescription = getCodedBaseObject(Prescription.class, medicationStatement.getIdentifier(),medicationStatement.getCategory().getCoding());
        if (medicationStatement.hasEffectivePeriod()) {
            if (medicationStatement.getEffectivePeriod().hasStart()) {
                prescription.setPrescriptionDate(new DateTime(medicationStatement.getEffectivePeriod().getStart()));
            }
            if (medicationStatement.getEffectivePeriod().hasEnd()) {
                prescription.setEndDate(new DateTime(medicationStatement.getEffectivePeriod().getEnd()));
            }
        }
        Medication medication = getMedicationFromReference(medicationStatement.getMedicationReference());
        medication.setRouteOfAdministration(FHIRDSTU3Utils.getRouteofAdministration(medicationStatement.getDosage()));
        prescription.setAssociatedMedication(medication);
        prescription.setDosage(FHIRDSTU3Utils.getDosage(medicationStatement.getDosage()));
        prescription.setFrequency(FHIRDSTU3Utils.getFrequency(medicationStatement.getDosage()));
        prescription.setSig(FHIRDSTU3Utils.getInstructions(medicationStatement.getDosage()));

        if (medicationStatement.getInformationSource() != null) {
            String reference = medicationStatement.getInformationSource().getReference();
            String display = medicationStatement.getInformationSource().getDisplay();
            // TODO: What role should we use for the source of information? using reviewer == "REVIEWING_PHYSICIAN" for now
            prescription.setPrimaryClinicalActorId(getClinicalActor(reference, display, "reviewer"));
        }
        patient.addPrescription(prescription);
    }


    private void parseObservation(Observation observation) throws Exception{
        Set<String> observationCategories = new HashSet<>();
        for (CodeableConcept category : observation.getCategory()) {
            observationCategories.add(category.getText());
        }
        // TODO: Other categories observed: "Point of Care Testing", "Imaging", "Procedure", "Cardiac Nuclear Medicine", "Pathology and Cytology", "Echocardiography"
        if (observationCategories.contains("Social History")) {
            parseObservationSocialHistory(observation);
        } else if (observationCategories.contains("Lab") ||
                observationCategories.contains("Labs") ||
                observationCategories.contains("Laboratory")) {
            parseObservationLabResult(observation);
        } else if (observationCategories.contains("Vital Signs")) {
            parseObservationBiometricValue(observation);
        } else if (observationCategories.contains("Imaging")) {
            logger.info("Skipping Imaging observation that is only used via Reference");
        } else {
            logger.error("Unknown category type in observation: " + String.join(",", observationCategories));
        }
    }

    private void parseObservationBiometricValue(Observation observation) throws Exception {
        // If this observation has components, add one biometric value for each.
        if (observation.getComponent().size() > 0) {
            for (Observation.ObservationComponentComponent component : observation.getComponent()) {
                addBiometricValue(observation, component.getCode().getText(), component.getCode().getCoding(), component.getValue());
            }
        } else {
            addBiometricValue(observation, observation.getCode().getText(), observation.getCode().getCoding(), observation.getValue());
        }

    }

    private void addBiometricValue(Observation observation, String name, List<Coding> codes, Type value) throws Exception {
        BiometricValue biometricValue = getCodedBaseObject(BiometricValue.class, observation.getIdentifier(), codes);
        biometricValue.setName(name);
        biometricValue.setResultDate(FHIRDSTU3Utils.getDateTimeFromEffective(observation.getEffective()));
        if (value instanceof Quantity) {
            Quantity quantity = (Quantity) value;
            biometricValue.setValue(new EitherStringOrNumber(quantity.getValue()));
            biometricValue.setUnitsOfMeasure(quantity.getUnit());
        } else if (value instanceof CodeableConcept) {
            CodeableConcept codeableConcept = (CodeableConcept) value;
            biometricValue.setValue(new EitherStringOrNumber(codeableConcept.getText()));
        }
        populateClinicalActorsFromReferences(biometricValue, observation.getPerformer(), "performer");
        biometricValue.setSourceEncounter(getEncounterFromReference(observation.getContext().getReference()));

        patient.addBiometricValue(biometricValue);
    }

    private void parseObservationLabResult(Observation observation) throws Exception {
        LabResult labResult = getCodedBaseObject(LabResult.class, observation.getIdentifier(), observation.getCode().getCoding());
        labResult.setLabName(observation.getCode().getText());
        labResult.setSampleDate(FHIRDSTU3Utils.getDateTimeFromEffective(observation.getEffective()));
        if (observation.hasValueQuantity()) {
            labResult.setValue(new EitherStringOrNumber(observation.getValueQuantity().getValue()));
            labResult.setUnits(observation.getValueQuantity().getUnit());
        } else if (observation.hasValueStringType()) {
            labResult.setValue(new EitherStringOrNumber(observation.getValueStringType().getValue()));
        } else if (observation.hasValueCodeableConcept()) {
            labResult.setValue(new EitherStringOrNumber(observation.getValueCodeableConcept().getText()));
        } else if (observation.getValue() != null) {
            logger.error("Uknown value type in Lab Result");
        } else {
            logger.error("Lab Result value is null.");
        }

        labResult.setFlag(FHIRDSTU3Utils.getLabFlagFromInterpretation(observation.getInterpretation()));

        // TODO: What if there are multiple reference ranges?
        if (observation.getReferenceRange().size() > 0) {
            labResult.setRange(observation.getReferenceRangeFirstRep().getText());
        }
        // TODO: Find these additional fields.
//        labResult.setSpecimen();
        if (observation.hasSpecimen()) {
            // Note specimen will likely need to be obtained via Reference
            logger.info("Found Specimen: " + observation.getSpecimen().toString());
        }
//        labResult.setSequenceNumber();
//        labResult.setPanel();
//        labResult.setSuperPanel();

        labResult.setLabNote(observation.getComment());
        populateClinicalActorsFromReferences(labResult, observation.getPerformer(), "orderer");
        labResult.setSourceEncounter(getEncounterFromReference(observation.getContext().getReference()));
        patient.addLabResult(labResult);
    }

    private void parseObservationSocialHistory(Observation observation) throws Exception {
        List<Coding> codings = new ArrayList<Coding>();
        for (CodeableConcept codeableConcept : observation.getCategory()) {
            codings.addAll(codeableConcept.getCoding());
        }
        SocialHistory socialHistory = getCodedBaseObject(SocialHistory.class, observation.getIdentifier(), codings);
        socialHistory.setType(FHIRDSTU3Utils.getFirstClinicalCode(observation.getCode().getCoding()));
        socialHistory.setFieldName(observation.getCode().getText());
        socialHistory.setDate(FHIRDSTU3Utils.getDateTimeFromEffective(observation.getEffective()));
        if (observation.hasValueQuantity()) {
            socialHistory.setValue(observation.getValueQuantity().getValue().toString());
        } else if (observation.hasValueStringType()) {
            socialHistory.setValue(observation.getValueStringType().getValue());
        } else if (observation.hasValueCodeableConcept()) {
            socialHistory.setValue(observation.getValueCodeableConcept().getText());
        } else if (observation.getValue() != null) {
            logger.error("Unknown value type in Social History");
        } else {
            logger.error("Social History value is null.");
        }
        populateClinicalActorsFromReferences(socialHistory, observation.getPerformer(), "recorder");
        socialHistory.setSourceEncounter(getEncounterFromReference(observation.getContext().getReference()));

        patient.addSocialHistory(socialHistory);
    }

    // TODO: Do we care about managing organization?
    private void parsePatient(Patient fhirPatient) {
        patient.setExternalIDs(FHIRDSTU3Utils.getEidsFromIdentifierList(fhirPatient.getIdentifier()));
        Demographics demographics = new Demographics();
        List<Demographics> alternateDemographicsList = new ArrayList<>();
        List<Name> names = FHIRDSTU3Utils.getNames(fhirPatient.getName());
        for (Name name : names) {
            if (demographics.getName() == null) {
                demographics.setName(name);
            } else {
                // The only reason we would have alternate demographics on a patient is if they have multiple names
                Demographics alternateDemographics = new Demographics();
                alternateDemographics.setName(name);
                alternateDemographicsList.add(alternateDemographics);
            }
        }
        if (alternateDemographicsList.size() > 0) {
            patient.setAlternateDemographics(alternateDemographicsList);
        }
        demographics.setDateOfBirth(new DateTime(fhirPatient.getBirthDate()));
        if (fhirPatient.hasDeceasedDateTimeType()) {
            demographics.setDateOfDeath(new DateTime(fhirPatient.getDeceasedDateTimeType().getValue()));
        }
        demographics.setGender(Gender.valueOf(fhirPatient.getGender().getDisplay().toUpperCase()));
        demographics.setLanguages(FHIRDSTU3Utils.getLangaugesFromCommunications(fhirPatient.getCommunication()));
        demographics.setMaritalStatus(FHIRDSTU3Utils.getMaritalStatus(fhirPatient.getMaritalStatus()));
        for (Extension extension : fhirPatient.getExtension()) {
            if (extension.getUrl().equals("http://hl7.org/fhir/us/core/StructureDefinition/us-core-race")) {
                for (Extension raceExtension : extension.getExtension()) {
                    if (raceExtension.getValue() instanceof Coding) {
                        demographics.setRace(FHIRDSTU3Utils.getClinicalCode((Coding) raceExtension.getValue()));
                    }
                }
            } else if (extension.getUrl().equals("http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity")) {
                for (Extension ethnicityExtension : extension.getExtension()) {
                    if (ethnicityExtension.getValue() instanceof Coding) {
                        demographics.setEthnicity(FHIRDSTU3Utils.getClinicalCode((Coding) ethnicityExtension.getValue()));
                    }
                }
            }
        }
        patient.setPrimaryDemographics(demographics);
        patient.setPrimaryContactDetails(FHIRDSTU3Utils.getContactDetails(fhirPatient.getTelecom(), fhirPatient.getAddress()));
        // Set the PCP in metadata
        Map<String, String> metadata = new HashMap<String, String>();
        for (Reference practitionerReference : fhirPatient.getGeneralPractitioner()) {
            String resourceReference = practitionerReference.getReference();
            Practitioner practitioner = (Practitioner) getReferencedResource(resourceReference);
            if (practitioner != null) {
                for (ExternalID id : FHIRDSTU3Utils.getEidsFromIdentifierList(practitioner.getIdentifier())) {
                    // TODO: This will only work for Epic currently - store a mapping of id assign authorities by organization
                    if (id.getAssignAuthority().equals("EPIC")) {
                        metadata.put("PRIMARY_CARE_PROVIDER",id.getId() + "^^NPI");
                    }
                }
            } else {
                logger.error("Could not get practitioner referenced by patient.generalPractitioner");
            }
        }
        patient.setMetadata(metadata);
    }

    private void parseProcedure(Procedure fhirProcedure) throws Exception {
        com.apixio.model.patient.Procedure procedure = getCodedBaseObject(com.apixio.model.patient.Procedure.class, fhirProcedure.getIdentifier(),fhirProcedure.getCode().getCoding());
        if (procedure.getCode() == null) {
            procedure.setCode(FHIRDSTU3Utils.getClinicalCodeFromDisplay(fhirProcedure.getCode().getText()));
        }
        procedure.setProcedureName(fhirProcedure.getCode().getText());
        if (fhirProcedure.hasPerformedPeriod()) {
            procedure.setPerformedOn(new DateTime(fhirProcedure.getPerformedPeriod().getStart()));
            procedure.setEndDate(new DateTime(fhirProcedure.getPerformedPeriod().getEnd()));
        } else if (fhirProcedure.hasPerformedDateTimeType()) {
            procedure.setPerformedOn(new DateTime(fhirProcedure.getPerformedDateTimeType().getValue()));
        }
        procedure.setSupportingDiagnosis(FHIRDSTU3Utils.getClinicalCodesFromCodeableConcepts(fhirProcedure.getReasonCode()));
        procedure.setSourceEncounter(getEncounterFromReference(fhirProcedure.getContext().getReference()));
        for (Procedure.ProcedurePerformerComponent performer : fhirProcedure.getPerformer()) {
            UUID clinicalActorId = getClinicalActorFromProcedurePerformerComponent(performer);
            // TODO: Integrate health care provider taxonomy code system
            if (procedure.getPrimaryClinicalActorId() == null) {
                procedure.setPrimaryClinicalActorId(clinicalActorId);
            } else {
                procedure.getSupplementaryClinicalActorIds().add(clinicalActorId);
            }
        }
        // TODO: Should we set the "interpretation" from the referenced DiagnosticReport?
        patient.addProcedure(procedure);
    }

    private CareSite getCareSiteFromReference(Reference locationReference) {
        String locationFhirId = locationReference.hasReference() ? locationReference.getReference().split("/")[1] : null;
        CareSite careSite = null;
        if (careSites.containsKey(locationFhirId)) {
            careSite = careSites.get(locationFhirId);
        } else if (locationReference.hasReference()) {
            careSite = new CareSite();
            Location location = (Location) getReferencedResource(locationReference.getReference());
            if (location != null) {
                // TODO: Our model doesn't currently support managingOrganization, partOf (Location) or telephone
                FHIRDSTU3Utils.setBaseObjectIds(careSite, location.getIdentifier());
                careSite.setCareSiteName(location.getName());
                careSite.setAddress(FHIRDSTU3Utils.getApixioAddressfromFHIRAddress(location.getAddress()));
                careSite.setCareSiteType(FHIRDSTU3Utils.getCareSiteTypeFromCode(location.getExtension()));

            } else {
                logger.error("Referenced Location not found in resources: " + locationReference.getReference() +
                        ". Generating from display: " + locationReference.getDisplay());
                careSite.setCareSiteName(locationReference.getDisplay());
            }
            // Add to global list of care sites and add to patient
            patient.addCareSite(careSite);
            careSites.put(locationFhirId, careSite);
        }
        return careSite;
    }

    private UUID getEncounterFromReference(String reference) {
        String encounterFhirId = reference != null ? reference.split("/")[1] : null;
        UUID encounterId = null;
        if (encounters.containsKey(encounterFhirId)) {
            encounterId = encounters.get(encounterFhirId);
        } else {
            com.apixio.model.patient.Encounter encounter = new com.apixio.model.patient.Encounter();
            encounter.setSourceId(primarySource.getSourceId());
            encounter.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
            patient.addEncounter(encounter);
            encounterId = encounter.getEncounterId();
            encounters.put(encounterFhirId, encounterId);
        }
        return encounterId;
    }

    private Medication getMedicationFromReference(Reference medicationReference) throws Exception{
        org.hl7.fhir.dstu3.model.Medication fhirMedication = (org.hl7.fhir.dstu3.model.Medication) getReferencedResource(medicationReference.getReference());
        if (fhirMedication != null) {
            // TODO: This is a spoofed ID as there isn't a fully qualified ID available on the Medication resource
            // While we are using a real FHIR ID for a medication, there is no scoping for system so there could be collisions
            Identifier resourceIdentifier = new Identifier();
            resourceIdentifier.setId(fhirMedication.getIdElement().getIdPart());
            resourceIdentifier.setSystem("FHIR");
            Medication medication = getCodedBaseObject(Medication.class, Collections.singletonList(resourceIdentifier), fhirMedication.getCode().getCoding());
            if (fhirMedication.getIsBrand()) {
                medication.setBrandName(fhirMedication.getCode().getText());
            } else {
                medication.setGenericName(fhirMedication.getCode().getText());
            }
            List<String> ingredients = new ArrayList<>();
            for (org.hl7.fhir.dstu3.model.Medication.MedicationIngredientComponent ingredient : fhirMedication.getIngredient()) {
                // Some ingredients require resolution of a referenced medication
                if (ingredient.hasItemReference()) {
                    Medication ingredientMedication = getMedicationFromReference(ingredient.getItemReference());
                    String ingredientName = ingredientMedication.getBrandName() != null ? ingredientMedication.getBrandName() : ingredientMedication.getGenericName();
                    ingredients.add(ingredientName);
                } else if (ingredient.hasItemCodeableConcept()) {
                    ingredients.add(ingredient.getItemCodeableConcept().getText());
                } else {
                    logger.error("Ingredient has no referenced medication or codeable concept.");
                }
            }
            medication.setIngredients(ingredients);
            medication.setForm(fhirMedication.getForm().getText());
            return medication;
        } else {
            logger.error("Referenced Medication not found in resources: " + medicationReference.getReference() +
                    ". Generating from display: " + medicationReference.getDisplay());
            // The bare minimum Medication will use just the display name of the medication
            Medication medication = new Medication();
            medication.setGenericName(medicationReference.getDisplay());
            medication.setBrandName(medicationReference.getDisplay());
            ClinicalCode nameCode = new ClinicalCode();
            nameCode.setDisplayName(medicationReference.getDisplay());
            medication.setCode(nameCode);
            return medication;
        }
    }

    private void populateClinicalActorsFromReferences(CodedBaseObject codedBaseObject, List<Reference> references, String roleName) {
        for (Reference performer : references) {
            UUID clinicalActorId = getClinicalActorFromReference(performer, roleName);
            if (codedBaseObject.getPrimaryClinicalActorId() == null) {
                codedBaseObject.setPrimaryClinicalActorId(clinicalActorId);
            } else {
                codedBaseObject.getSupplementaryClinicalActorIds().add(clinicalActorId);
            }
        }
    }

    // TODO: Reconcile this with data obtained through the Practitioner resource
    private UUID getClinicalActorFromEncounterParticipantComponent(Encounter.EncounterParticipantComponent participant) {
        String roleName = participant.getTypeFirstRep() == null ? null : participant.getTypeFirstRep().getText();
        return getClinicalActorFromReference(participant.getIndividual(), roleName);
    }

    private UUID getClinicalActorFromProcedurePerformerComponent(Procedure.ProcedurePerformerComponent performer) {
        String roleName = performer.getRole() == null ? "performer" : performer.getRole().getText();
        return getClinicalActorFromReference(performer.getActor(), roleName);
    }

    private UUID getClinicalActorFromImmunizationPractitionerComponent(Immunization.ImmunizationPractitionerComponent practitioner) {
        String roleName = practitioner.getRole() == null ? null : practitioner.getRole().getText();
        return getClinicalActorFromReference(practitioner.getActor(), roleName);
    }

    private UUID getClinicalActorFromReference(Reference practitionerReference, String roleName) {
        String reference = practitionerReference.getReference();
        String displayName = practitionerReference.getDisplay();
        return getClinicalActor(reference, displayName, roleName);
    }

    private UUID getClinicalActor(String reference, String displayName, String roleName) {
        UUID clinicalActorId = null;
        if (clinicalActors.containsKey(reference)) {
            clinicalActorId = clinicalActors.get(reference);
        } else {
            ClinicalActor clinicalActor = new ClinicalActor();
            clinicalActor.setClinicalActorId(clinicalActor.getInternalUUID());
            clinicalActor.setSourceId(primarySource.getSourceId());
            clinicalActor.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
            Practitioner practitioner = (Practitioner) getReferencedResource(reference);
            if (practitioner != null) {
                Set<ExternalID> practitionerIds = FHIRDSTU3Utils.getEidsFromIdentifierList(practitioner.getIdentifier());

                // In Epic, the NPI has a type of "EPIC" (not to be confused with "Epic"). So loop through once and try to get that
                for (ExternalID id : practitionerIds) {
                    if (id.getAssignAuthority().equals("EPIC")) {
                        id.setAssignAuthority("NPI");
                        clinicalActor.setPrimaryId(id);
                        clinicalActor.setOriginalId(id);
                    }
                }
                // Add the rest of the IDs
                for (ExternalID id : practitionerIds) {
                    if (clinicalActor.getPrimaryId() == null) {
                        clinicalActor.setPrimaryId(id);
                        clinicalActor.setOriginalId(id);
                    }
                    else if (id.getAssignAuthority() != clinicalActor.getPrimaryId().getAssignAuthority()) {
                        clinicalActor.getAlternateIds().add(id);
                    }
                }
                List<Name> practitionerNames = FHIRDSTU3Utils.getNames(practitioner.getName());
                List<Name> actorSupplementalNames = new ArrayList<>();
                for (Name practitionerName : practitionerNames) {
                    if (clinicalActor.getActorGivenName() == null) {
                        clinicalActor.setActorGivenName(practitionerName);
                    } else {
                        actorSupplementalNames.add(practitionerName);
                    }
                }
                clinicalActor.setActorSupplementalNames(actorSupplementalNames);

                List<String> titles = new ArrayList<String>();
                for (Practitioner.PractitionerQualificationComponent qualification : practitioner.getQualification()) {
                    titles.add(qualification.getCode().getText());
                }
                clinicalActor.setTitle(String.join(",", titles));
            }
            // Otherwise, build a Practitioner just from the display name
            else {
                logger.warn("Referenced Practitioner not found. Creating from display name.");
                clinicalActor.setActorGivenName(FHIRDSTU3Utils.getNameFromString(displayName));
            }

            if (roleName != null) {
                clinicalActor.setRole(FHIRDSTU3Utils.getHealthCareProviderRole(roleName));
            }
            patient.addClinicalActor(clinicalActor);
            clinicalActorId = clinicalActor.getClinicalActorId();
            clinicalActors.put(reference, clinicalActorId);
        }
        return clinicalActorId;
    }
}
