package com.apixio.converters.ccr.parser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apixio.converters.base.BaseUtils;
import com.apixio.converters.base.Parser;
import com.apixio.converters.ccr.jaxb.ActorReferenceType;
import com.apixio.converters.ccr.jaxb.ActorType;
import com.apixio.converters.ccr.jaxb.ActorType.Person;
import com.apixio.converters.ccr.jaxb.AlertType;
import com.apixio.converters.ccr.jaxb.CodedDescriptionType.ObjectAttribute;
import com.apixio.converters.ccr.jaxb.CodedDescriptionType.ObjectAttribute.AttributeValue;
import com.apixio.converters.ccr.jaxb.CommentType;
import com.apixio.converters.ccr.jaxb.ContinuityOfCareRecord;
import com.apixio.converters.ccr.jaxb.EncounterType;
import com.apixio.converters.ccr.jaxb.IDType;
import com.apixio.converters.ccr.jaxb.InternalCCRLink;
import com.apixio.converters.ccr.jaxb.Location;
import com.apixio.converters.ccr.jaxb.Locations;
import com.apixio.converters.ccr.jaxb.PersonNameType;
import com.apixio.converters.ccr.jaxb.ProblemType;
import com.apixio.converters.ccr.jaxb.ProcedureType;
import com.apixio.converters.ccr.jaxb.ResultType;
import com.apixio.converters.ccr.jaxb.SocialHistoryType;
import com.apixio.converters.ccr.jaxb.SourceType;
import com.apixio.converters.ccr.jaxb.StructuredProductType;
import com.apixio.converters.ccr.jaxb.StructuredProductType.Product;
import com.apixio.converters.ccr.jaxb.TestType;
import com.apixio.converters.ccr.utils.CCRUtils;
import com.apixio.converters.ccr.utils.CCRUtils.DateTypes;
import com.apixio.converters.html.creator.HTMLUtils;
import com.apixio.model.file.catalog.jaxb.generated.ApxCatalog.CatalogEntry;
import com.apixio.model.patient.Administration;
import com.apixio.model.patient.Allergy;
import com.apixio.model.patient.BiometricValue;
import com.apixio.model.patient.CareSite;
import com.apixio.model.patient.ClinicalActor;
import com.apixio.model.patient.ContactDetails;
import com.apixio.model.patient.Demographics;
import com.apixio.model.patient.Document;
import com.apixio.model.patient.DocumentContent;
import com.apixio.model.patient.DocumentType;
import com.apixio.model.patient.Encounter;
import com.apixio.model.patient.ExternalID;
import com.apixio.model.patient.LabResult;
import com.apixio.model.patient.Medication;
import com.apixio.model.patient.Name;
import com.apixio.model.patient.Organization;
import com.apixio.model.patient.ParserType;
import com.apixio.model.patient.ParsingDetail;
import com.apixio.model.patient.Patient;
import com.apixio.model.patient.Prescription;
import com.apixio.model.patient.Problem;
import com.apixio.model.patient.Procedure;
import com.apixio.model.patient.SocialHistory;
import com.apixio.model.patient.Source;
import com.apixio.model.patient.TelephoneNumber;
import com.apixio.utility.HashCalculator;

public class CCRParser extends Parser {

	private static final String PARSER_VERSION = "1.0";
	private byte[] docBytes = null;
	private ContinuityOfCareRecord ccr = null;
	private Patient patient  = new Patient();
	private Source primarySource = new Source();
	private ParsingDetail currentParsingDetails;
	private URI sourceDocumentUri;
	private CatalogEntry catalogEntry;
	private HashMap<String, UUID> currentClinicalActors = new HashMap<String, UUID>();
	private HashMap<String, UUID> currentEncounters = new HashMap<String, UUID>();
	private static final Logger logger = LoggerFactory.getLogger(CCRParser.class);

	@Override
	public Patient getPatient() throws Exception {
		return patient;
	}
	
	@Override
	public void parse(InputStream ccrDocument, CatalogEntry catalogEntry) throws Exception {
		docBytes = BaseUtils.getBytesFromInputStream(ccrDocument);
		this.catalogEntry = catalogEntry;
		JAXBContext jaxbContext = JAXBContext.newInstance("com.apixio.converters.ccr.jaxb");
		Unmarshaller unMarshaller = jaxbContext.createUnmarshaller();
		SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
		Schema schema = schemaFactory.newSchema(new StreamSource(ClassLoader.getSystemResourceAsStream("ccr.xsd")));
		unMarshaller.setSchema(schema);
		try{
			ccr = (ContinuityOfCareRecord) unMarshaller.unmarshal(new ByteArrayInputStream(docBytes));			
			populateParsingDetails();
			populatePrimarySource();
			populatePatient();			
			populateDocument();
		}catch(Exception e){
			//log.error("SEVERE ERROR: Cannot parse the document as it is not complying to the XSD.");
			e.printStackTrace();
			throw new Exception("Parser Error.");
		}
	}	
	
	private void populateParsingDetails() {
		currentParsingDetails = new ParsingDetail();
		currentParsingDetails.setContentSourceDocumentType(DocumentType.STRUCTURED_DOCUMENT);
		currentParsingDetails.setContentSourceDocumentURI(sourceDocumentUri);
		currentParsingDetails.setParser(ParserType.CCR);
		currentParsingDetails.setParserVersion(PARSER_VERSION);
		currentParsingDetails.setParsingDateTime(DateTime.now());
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

	private void populatePrimarySource() {
		primarySource = new Source();
		primarySource.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
		//primarySource.setClinicalActorId(clinicalActorId) -- We'll need to add an actor and reference it
		primarySource.setCreationDate(CCRUtils.getDate(ccr.getDateTime()));
		primarySource.setOrganization(getOrganization());
		primarySource.setOriginalId(BaseUtils.getExternalID(ccr.getCCRDocumentObjectID()));
		//primarySource.setSourceSystem(sourceSystem)
		//primarySource.setSourceType(sourceType)
		this.patient.addSource(primarySource);		
	}

	private void populatePatient() throws Exception {
//		populateHealthcareProviders();
//		populatePatientDetails();
		populatePatientDemographics();
		populateEncounters();
		populateAllergies();
		populateMedications();
		populateProblems();
		populateImmunizations();
		populateLabResults();
		populateProcedures();
		populateVitalSigns();
//		populateFamilyHistory();
		populateSocialHistory();
		
	}

	private void populateDocument() throws Exception {
		// Here we get the HTML for the document, create a document object, and hang it on the patient
		Document document = new Document();
		if (catalogEntry !=null && catalogEntry.getDocumentUUID() !=null)
			document.setInternalUUID(UUID.fromString(catalogEntry.getDocumentUUID()));
		else
			throw new Exception("No Document UUID found in Catalog file.");
		
		document.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
		document.setSourceId(primarySource.getSourceId());
		
		//document.setAssociatedEncounter(associatedEncounter);
		document.setDocumentDate(CCRUtils.getDate(ccr.getDateTime()));
		//document.setDocumentId(documentId);
		document.setDocumentTitle("Continuity of Care Record");
		//document.setMimeType("text/xml");
		document.setOriginalId(BaseUtils.getExternalID(ccr.getCCRDocumentObjectID()));
		document.setStringContent(getHtmlRender());
		
		DocumentContent documentContent = new DocumentContent();
		documentContent.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
		documentContent.setSourceId(primarySource.getSourceId());
//		documentContent.setContent(content);
		try{
			documentContent.setHash(HashCalculator.getFileHash(docBytes));
		}catch(Exception e){
			logger.warn("Error while calculating Hash of the file:"+e.getMessage());
		}
//		documentContent.setLength(length);
		documentContent.setMimeType("text/xml");
//		documentContent.setOriginalId(originalId);
//		documentContent.setUri(uri);
//		document.setDocumentContents(documentContents);
		
		document.getDocumentContents().add(documentContent);
		patient.addDocument(document);
	}

	private String getHtmlRender() {
		String htmlRender = "";
		String resourcePath = this.getClass().getClassLoader().getResource("ccr.xsl").toString();
		htmlRender = HTMLUtils.convertXmlToHtml(ClassLoader.getSystemResourceAsStream("ccr.xsl"), new ByteArrayInputStream(docBytes), resourcePath);//, ClassLoader.getSystemClassLoader().getResource("ccr.xsl").toString());
		return htmlRender;
	}

	private void populatePatientDemographics() throws Exception {

		if(ccr.getPatient() != null && ccr.getPatient().size() > 0)
		{
			String patientActorId = ccr.getPatient().get(0).getActorID();
			ActorType patientActor = getActorFromId(patientActorId);
			patient.setExternalIDs(getIDsForActor(patientActor));
			patient.setPrimaryContactDetails(getContactDetailsForActor(patientActor));
			patient.setPrimaryDemographics(getDemographicsForActor(patientActorId));
			patient.setPrimaryExternalID(getPrimaryExternalId(patient.getExternalIDs()));
			//patient.setAlternateContactDetails(supplementaryContactDetails);
			//patient.setAlternateDemographics(supplementaryDemographics);	
		}
	}

	private void populateEncounters() {
		if (ccr.getBody() != null && 
			ccr.getBody().getEncounters() != null &&
			ccr.getBody().getEncounters().getEncounter() != null) {
			for (EncounterType encounterType : ccr.getBody().getEncounters().getEncounter()) {
				addEncounterToPatient(encounterType);				
			}
		}
	}

	private void populateAllergies() {
		if (ccr.getBody() != null && 
			ccr.getBody().getAlerts() != null &&
			ccr.getBody().getAlerts().getAlert() != null) {
			for (AlertType alertType : ccr.getBody().getAlerts().getAlert()) {
				addAllergyToPatient(alertType);				
			}
		}
	}

	private void populateMedications() {
		if (ccr.getBody() != null && 
			ccr.getBody().getMedications() != null &&
			ccr.getBody().getMedications().getMedication() != null) {
			for (StructuredProductType structuredProductType : ccr.getBody().getMedications().getMedication()) {
				addMedicationToPatient(structuredProductType);				
			}
		}
	}

	private void populateProblems() {
		if (ccr.getBody() != null && 
			ccr.getBody().getProblems() != null &&
			ccr.getBody().getProblems().getProblem() != null) {
			for (ProblemType problemType : ccr.getBody().getProblems().getProblem()) {
				addProblemToPatient(problemType);
			}
		}
	}

	private void populateProcedures() {
		if (ccr.getBody() != null &&
			ccr.getBody().getProcedures() != null &&
			ccr.getBody().getProcedures().getProcedure() != null) {
			for (ProcedureType procedureType : ccr.getBody().getProcedures().getProcedure()) {
				addProcedureToPatient(procedureType);
			}
		}
			
	}

	private void populateLabResults() {
		if (ccr.getBody() != null &&
			ccr.getBody().getResults() != null &&
			ccr.getBody().getResults().getResult() != null) {
			for (ResultType resultType : ccr.getBody().getResults().getResult()) {
				addResultToPatient(resultType);
			}
		}
	}

	private void populateImmunizations() {
		if (ccr.getBody() != null &&
			ccr.getBody().getImmunizations() != null &&
			ccr.getBody().getImmunizations().getImmunization() != null) {
			for (StructuredProductType structuredProductType : ccr.getBody().getImmunizations().getImmunization()) {
				addImmunizationToPatient(structuredProductType);
			}
		}
	}

	private void populateVitalSigns() {
		if (ccr.getBody() != null &&
			ccr.getBody().getVitalSigns() != null &&
			ccr.getBody().getVitalSigns().getResult() != null) {
			for (ResultType resultType : ccr.getBody().getVitalSigns().getResult()) {
				addVitalSignToPatient(resultType);
			}
		}
	}

	private void populateSocialHistory() {
		if (ccr.getBody() != null &&
			ccr.getBody().getSocialHistory() != null &&
			ccr.getBody().getSocialHistory().getSocialHistoryElement() != null) {
			for (SocialHistoryType socialHistoryType : ccr.getBody().getSocialHistory().getSocialHistoryElement()) {
				addSocialHistoryToPatient(socialHistoryType);
			}
		}
	}

	private void addEncounterToPatient(EncounterType encounterType) {
		Encounter encounter = new Encounter();
		String encounterObjectId = encounterType.getCCRDataObjectID();
		encounter.setOriginalId(getFirstExternalID(encounterType.getIDs()));
		encounter.getOtherOriginalIds().addAll((getOtherExternalIDs(encounterType.getIDs())));
		encounter.setEncounterDate(CCRUtils.getFirstDate(encounterType.getDateTime()));
		encounter.getChiefComplaints().add(CCRUtils.getClinicalCode(encounterType.getDescription()));
		encounter.setSiteOfService(getFirstCareSite(encounterType.getLocations()));

		// get this right (multiple actors.)
		encounter.setPrimaryClinicalActorId(getFirstSourceClinicalActorId(encounterType.getSource()));
		encounter.getSupplementaryClinicalActorIds().addAll(getSupplementarayClinicalActorIds(encounterType.getSource()));

		encounter.setMetadata(getMetadata(encounterType.getCommentID()));
		currentEncounters.put(encounterObjectId, encounter.getEncounterId());
		patient.addEncounter(encounter);
	}

	private void addAllergyToPatient(AlertType alertType) {
		Allergy allergy = new Allergy();
		allergy.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
		allergy.setSourceId(primarySource.getSourceId());
		
		allergy.setAllergen(CCRUtils.getDescriptionText(alertType.getDescription()));
		allergy.setCode(CCRUtils.getClinicalCode(alertType.getDescription()));
		//allergy.setCodeTranslations(codeTranslations);
		DateTime diagnosisDate = CCRUtils.getDate(alertType.getDateTime(), DateTypes.DIAGNOSIS);
		if (diagnosisDate == null)
			diagnosisDate = CCRUtils.getDate(alertType.getDateTime(), DateTypes.START);
		allergy.setDiagnosisDate(diagnosisDate);
		allergy.setResolvedDate(CCRUtils.getDate(alertType.getDateTime(), DateTypes.END));
		allergy.setOriginalId(getFirstExternalID(alertType.getIDs()));
		allergy.setPrimaryClinicalActorId(getFirstSourceClinicalActorId(alertType.getSource()));
		allergy.setReaction(CCRUtils.getReactionDescription(alertType.getReaction()));
		//allergy.setReactionDate(reactionDate);
		allergy.setReactionSeverity(CCRUtils.getReactionSeverity(alertType.getReaction()));
		allergy.setSourceEncounter(getEncounterId(alertType.getInternalCCRLink()));
		//allergy.setSupplementaryClinicalActors(supplementaryClinicalActorIds);
		
		allergy.setMetadata(getMetadata(alertType.getCommentID()));
		patient.addAllergy(allergy);
	}

	private void addProblemToPatient(ProblemType problemType) {
		Problem problem = new Problem();
		problem.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
		problem.setSourceId(primarySource.getSourceId());
		
		problem.setCode(CCRUtils.getClinicalCode(problemType.getDescription()));
		//problem.setCodeTranslations(codeTranslations);
		problem.setEndDate(CCRUtils.getDate(problemType.getDateTime(), DateTypes.END));
		problem.setDiagnosisDate(CCRUtils.getDate(problemType.getDateTime(), DateTypes.DIAGNOSIS));
		problem.setOriginalId(getFirstExternalID(problemType.getIDs()));
		problem.setPrimaryClinicalActorId(getFirstSourceClinicalActorId(problemType.getSource()));
		problem.setProblemName(CCRUtils.getDescriptionText(problemType.getDescription()));
		problem.setResolutionStatus(CCRUtils.getResolutionStatus(problemType.getStatus()));
		//problem.setSeverity(severity);
		problem.setSourceEncounter(getEncounterId(problemType.getInternalCCRLink())); 
		problem.setStartDate(CCRUtils.getDate(problemType.getDateTime(), DateTypes.START));
		//problem.setSupplementaryClinicalActors(supplementaryClinicalActorIds);
		//problem.setTemporalStatus(temporalStatus);

		problem.setMetadata(getMetadata(problemType.getCommentID()));
		
		patient.addProblem(problem);
	}

	private void addProcedureToPatient(ProcedureType procedureType) {
		Procedure procedure = new Procedure();
		procedure.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
		procedure.setSourceId(primarySource.getSourceId());
		
		//procedure.setBodySite(bodySite);
		procedure.setCode(CCRUtils.getClinicalCode(procedureType.getDescription()));
		//procedure.setCodeTranslations(codeTranslations);
		procedure.setEndDate(CCRUtils.getDate(procedureType.getDateTime(), DateTypes.END));
		//procedure.setInterpretation(interpretation);
		procedure.setOriginalId(getExternalID(procedureType.getIDs()));
		procedure.setPerformedOn(CCRUtils.getDate(procedureType.getDateTime(), DateTypes.START));
		procedure.setPrimaryClinicalActorId(getFirstSourceClinicalActorId(procedureType.getSource()));
		procedure.setProcedureName(CCRUtils.getDescriptionText(procedureType.getDescription()));
		procedure.setSupportingDiagnosis(CCRUtils.getIndicationsAsCodeList(procedureType.getIndications()));
		procedure.setSourceEncounter(getEncounterId(procedureType.getInternalCCRLink())); 
		//procedure.setSupplementaryClinicalActors(supplementaryClinicalActorIds);

		procedure.setMetadata(getMetadata(procedureType.getCommentID()));
		patient.addProcedure(procedure);		
	}	

	private void addImmunizationToPatient(StructuredProductType structuredProductType) {
		Administration administration = new Administration();
		administration.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
		administration.setSourceId(primarySource.getSourceId());
		
		Medication medication = getFirstMedication(structuredProductType.getProduct());
		medication.setRouteOfAdministration(CCRUtils.getRouteText(structuredProductType.getDirections()));
		
		administration.setAdminDate(CCRUtils.getFirstDate(structuredProductType.getDateTime()));
		//administration.setAmount(amount);
		administration.setCode(CCRUtils.getClinicalCode(structuredProductType.getDescription()));
		//administration.setCodeTranslations(codeTranslations);
		//administration.setDosage(dosage);
		//administration.setEndDate(endDate);
		administration.setMedication(medication);
		administration.setOriginalId(getFirstExternalID(structuredProductType.getIDs()));
		administration.setPrimaryClinicalActorId(getFirstSourceClinicalActorId(structuredProductType.getSource()));
		//administration.setQuantity(quantity);
		administration.setSourceEncounter(getEncounterId(structuredProductType.getInternalCCRLink())); 
		administration.setStartDate(CCRUtils.getDate(structuredProductType.getDateTime(), DateTypes.START));
		//administration.setSupplementaryClinicalActors(supplementaryClinicalActorIds);

		administration.setMetadata(getMetadata(structuredProductType.getCommentID()));
		patient.addAdministration(administration);			
	}

	private void addMedicationToPatient(StructuredProductType structuredProductType) {
		Prescription prescription = new Prescription();
		Medication associatedMedication = getFirstMedication(structuredProductType.getProduct());
		// we modeled route in medication but CCR has it outside of the product
		associatedMedication.setRouteOfAdministration(CCRUtils.getRouteText(structuredProductType.getDirections()));
		associatedMedication.setUnits(CCRUtils.getDosageUnits(structuredProductType.getDirections()));
		prescription.setAssociatedMedication(associatedMedication);
		
		prescription.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
		prescription.setSourceId(primarySource.getSourceId());
		
		prescription.setActivePrescription(CCRUtils.isActiveStatus(structuredProductType.getStatus()));
		//prescription.setAmount(amount);
		prescription.setCode(CCRUtils.getClinicalCode(structuredProductType.getDescription()));
		//prescription.setCodeTranslations(codeTranslations);
		prescription.setDosage(CCRUtils.getDosageText(structuredProductType.getDirections()));
		prescription.setEndDate(CCRUtils.getDate(structuredProductType.getDateTime(), DateTypes.END));
		//prescription.setFillDate(getFillDate(structuredProductType.getFulfillmentHistory())); //how can we actually know this?
		prescription.setFrequency(CCRUtils.getFrequencyText(structuredProductType.getDirections()));
		//prescription.setOriginalId(getPrescriptionId(structuredProductType.getFulfillmentHistory()));
		prescription.setOriginalId(getFirstExternalID(structuredProductType.getIDs()));
		prescription.setPrescriptionDate(CCRUtils.getDate(structuredProductType.getDateTime(), DateTypes.START));
		prescription.setPrimaryClinicalActorId(getFirstSourceClinicalActorId(structuredProductType.getSource()));
		prescription.setQuantity(CCRUtils.getQuantity(structuredProductType.getQuantity()));
		prescription.setRefillsRemaining(CCRUtils.getRefills(structuredProductType.getRefills()));
		prescription.setSig(CCRUtils.getDirectionsText(structuredProductType.getDirections()));
		prescription.setSourceEncounter(getEncounterId(structuredProductType.getInternalCCRLink())); 
		//prescription.setSupplementaryClinicalActors(supplementaryClinicalActorIds);

		prescription.setMetadata(getMetadata(structuredProductType.getCommentID()));
		patient.addPrescription(prescription);
	}

	private void addResultToPatient(ResultType resultType) {
		if (resultType.getTest() != null && resultType.getTest().size() > 0) {
			for (TestType test : resultType.getTest()) {

				LabResult labResult = new LabResult();
				labResult.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
				labResult.setSourceId(primarySource.getSourceId());
				
				//labResult.setCareSiteId(careSiteId);
				labResult.setCode(CCRUtils.getClinicalCode(test.getDescription()));
				//labResult.setCodeTranslations(codeTranslations);
				labResult.setFlag(CCRUtils.getLabFlag(test.getFlag()));
				labResult.setLabName(CCRUtils.getDescriptionText(test.getDescription()));
				labResult.setLabNote(getComments(test.getCommentID()));
				labResult.setOriginalId(getFirstExternalID(resultType.getIDs()));
				labResult.setPanel(CCRUtils.getClinicalCode(resultType.getDescription()));
				labResult.setPrimaryClinicalActorId(getFirstSourceClinicalActorId(resultType.getSource())); // do i use source from lab result or lab test?
				labResult.setRange(CCRUtils.getNormalResult(test.getNormalResult()));
				labResult.setSampleDate(CCRUtils.getDate(resultType.getDateTime(), DateTypes.SAMPLE));
				labResult.setOtherDates(CCRUtils.getOtherDates(resultType.getDateTime(), DateTypes.SAMPLE));
				labResult.setSequenceNumber(CCRUtils.getSequenceNumber(test.getNormalResult()));
				labResult.setSourceEncounter(getEncounterId(resultType.getInternalCCRLink())); 
				labResult.setSpecimen(CCRUtils.getClinicalCode(resultType.getSubstance()));
				//labResult.setSuperPanel(superPanel);
				//labResult.setSupplementaryClinicalActors(supplementaryClinicalActorIds);
				labResult.setUnits(CCRUtils.getTestResultUnits(test.getTestResult()));
				labResult.setValue(CCRUtils.getTestResultValue(test.getTestResult()));

				// TODO: do we get metadata from the result or from the test?
				labResult.setMetadata(getMetadata(resultType.getCommentID()));
				patient.addLabResult(labResult);	
			}
		}
	}

	private void addVitalSignToPatient(ResultType resultType) {
		if (resultType.getTest() != null && resultType.getTest().size() > 0) {
			for (TestType test : resultType.getTest()) {

				BiometricValue biometricValue = new BiometricValue();
				biometricValue.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
				biometricValue.setSourceId(primarySource.getSourceId());
				
				//labResult.setCareSiteId(careSiteId);
				biometricValue.setCode(CCRUtils.getClinicalCode(resultType.getType()));
				biometricValue.setOriginalId(getFirstExternalID(resultType.getIDs()));
				biometricValue.setName(CCRUtils.getDescriptionText(test.getDescription()));
				biometricValue.setResultDate(CCRUtils.getFirstDate(resultType.getDateTime()));
				biometricValue.setValue(CCRUtils.getTestResultValue(test.getTestResult()));
				biometricValue.setSourceEncounter(getEncounterId(resultType.getInternalCCRLink())); 
				biometricValue.setPrimaryClinicalActorId(getFirstSourceClinicalActorId(resultType.getSource())); // do i use source from lab result or lab test?

				// do we get metadata from the result or from the test?
				biometricValue.setMetadata(getMetadata(resultType.getCommentID()));
				patient.addBiometricValue(biometricValue);
			}
		}		
	}

	private void addSocialHistoryToPatient(SocialHistoryType socialHistoryType) {
		if (socialHistoryType.getDescription() != null && 
			socialHistoryType.getDescription().getObjectAttribute() != null)
		{
			for (ObjectAttribute objectAttribute : socialHistoryType.getDescription().getObjectAttribute())
			{
				SocialHistory socialHistory = new SocialHistory();
				socialHistory.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
				socialHistory.setSourceId(primarySource.getSourceId());
				socialHistory.setDate(CCRUtils.getFirstDate(socialHistoryType.getDateTime()));
				socialHistory.setType(CCRUtils.getClinicalCode(socialHistoryType.getType()));
				socialHistory.setOriginalId(getFirstExternalID(socialHistoryType.getIDs()));
				socialHistory.setPrimaryClinicalActorId(getFirstSourceClinicalActorId(socialHistoryType.getSource()));
				socialHistory.setSourceEncounter(getEncounterId(socialHistoryType.getInternalCCRLink())); 
				socialHistory.setFieldName(objectAttribute.getAttribute());
				socialHistory.setValue(getAttributeValueFromList(objectAttribute.getAttributeValue()));

				socialHistory.setMetadata(getMetadata(socialHistoryType.getCommentID()));
				patient.addSocialHistory(socialHistory);
			}
		}
	}
	
	private String getAttributeValueFromList(List<AttributeValue> attributeValues) {
		String attributeValueString = null;
		if (attributeValues != null) {
			for (AttributeValue attributeValue : attributeValues) {
				if (attributeValueString == null)
					attributeValueString = attributeValue.getValue().toString();
				else
					attributeValueString += " " + attributeValue.getValue();
			}
		}
		return attributeValueString;
	}

	private Medication getFirstMedication(List<Product> product) {
		Medication firstMedication = null;
		if (product != null && product.size() > 0)
			firstMedication = getMedication(product.get(0));
		return firstMedication;
	}

	private Medication getMedication(Product product) {
		Medication medication = new Medication();
		medication.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
		medication.setSourceId(primarySource.getSourceId());
		
		medication.setBrandName(CCRUtils.getDescriptionText(product.getBrandName()));
		medication.setCode(CCRUtils.getClinicalCode(product.getProductName()));
		medication.getCodeTranslations().add(CCRUtils.getClinicalCode(product.getBrandName()));
		medication.setForm(CCRUtils.getFormText(product.getForm()));
		medication.setGenericName(CCRUtils.getDescriptionText(product.getProductName()));
		//medication.setIngredients(ingredients);
		//medication.setOriginalId(originalId);
		//medication.setPrimaryClinicalActorId(getFirstSourceActorId(product.getSource())); // What is this meant to be for a product?
		//medication.setRouteOfAdministration(routeOfAdministration); // not sure how this is conveyed in CCR
		//medication.setSourceEncounter(sourceEncounter);
		medication.setStrength(CCRUtils.getStrengthValue(product.getStrength()));
		//medication.setSupplementaryClinicalActors(supplementaryClinicalActorIds);
		//medication.setUnits(CCRUtils.getStrengthUnits(product.getStrength()));
		
		return medication;
	}

	private String getComments(List<String> commentID) {
		String comments = null;
		if (commentID != null) {
			String text = "";
			for (String commentId : commentID)
			{
				text = text + "\n" + getCommentFromCommentID(commentId);
				text.trim();
			}
			if (!text.equals(""))
				comments = text;
		}
		return comments;
	}

	private String getCommentFromCommentID(String commentId) {
		String commentString = "";
		if (ccr.getComments() != null)
		{
			for (CommentType comment : ccr.getComments().getComment()) {
              if (comment.getCommentObjectID().equalsIgnoreCase(commentId)) {
                 if (comment.getDescription() != null)
                 {
                	 commentString = comment.getDescription().getText();
                	 break;
                 }
              }
			}
		}
		return commentString;
	}

	private UUID getEncounterId(List<InternalCCRLink> internalCCRLinks) {
		UUID encounterId = null;
		if (internalCCRLinks != null)
		{
			for (InternalCCRLink internalCCRLink : internalCCRLinks)
			{		
				if (CCRUtils.hasListItemContaining(internalCCRLink.getLinkRelationship(), "encounter"))
				{
					String encounterObjectId = internalCCRLink.getLinkID();
					encounterId = currentEncounters.get(encounterObjectId);
					break;
				}				
			}
		}
		
		return encounterId;
	}
	
	private Organization getOrganization() {
		Organization organization = null;
		if(ccr.getFrom() !=null && ccr.getFrom().getActorLink() !=null){
			//there could be only one source for the document, so just picking the first one.
			ActorType sourceOrganization = getActorFromReference(ccr.getFrom().getActorLink().get(0));
			organization = getOrganizationFromActor(sourceOrganization);
    	}
		return organization;
	}

	private Organization getOrganizationFromActor(ActorType sourceOrganization) {
		Organization organization = null;
		if (sourceOrganization != null) {
			organization = new Organization();
			organization.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
			organization.setSourceId(primarySource.getSourceId());
			
			//organization.setAlternateIds(alternateIds);
			organization.setName(getOrganizationName(sourceOrganization.getOrganization()));
			organization.setContactDetails(getContactDetailsForActor(sourceOrganization));
			
			//organization.setOriginalId(originalId);
			organization.setPrimaryId(getFirstExternalID(sourceOrganization.getIDs()));
		}
		return organization;
	}

	private String getOrganizationName(com.apixio.converters.ccr.jaxb.ActorType.Organization organization) {
		String organizationName = null;
		if (organization != null)
			organizationName = organization.getName();
		return organizationName;
	}

	private ActorType getActorFromReference(ActorReferenceType actorReferenceType) {
		ActorType actorType = null;
		if (actorReferenceType != null) {
			actorType = getActorFromId(actorReferenceType.getActorID());
		}
		return actorType;
	}

	private CareSite getFirstCareSite(Locations locations) {
		CareSite careSite = null;
		if (locations != null)
		{			
			if (locations.getLocation() != null && locations.getLocation().size() > 0)
			{
				careSite = getCareSite(locations.getLocation().get(0));
			}
		}
		return careSite;
	}

	private CareSite getCareSite(Location location) {
		CareSite careSite = new CareSite();
		careSite.setCareSiteName(CCRUtils.getDescriptionText(location.getDescription()));
		careSite.setAddress(getLocationAddress(location));
		return careSite;
	}

	private com.apixio.model.patient.Address getLocationAddress(Location location) {
		com.apixio.model.patient.Address locationAddress = null;
		ActorType locationActor = getActorFromReference(location.getActor());
		locationAddress = CCRUtils.getFirstAddressFromAddressTypes(locationActor.getAddress());
		return locationAddress;
	}
	
	private ClinicalActor getClinicalActorFromActor(ActorType actor) {
		ClinicalActor clinicalActor = new ClinicalActor();
		clinicalActor.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
		clinicalActor.setSourceId(primarySource.getClinicalActorId());
		
		clinicalActor.setActorGivenName(getPersonName(actor.getPerson()));
		//clinicalActor.setActorSupplementalNames(actorSupplementalNames);
		//clinicalActor.setAssociatedOrg(associatedOrg);
		//clinicalActor.setOriginalId(originalId);
		clinicalActor.setPrimaryId(getFirstExternalID(actor.getIDs()));
		clinicalActor.setAlternateIds(getOtherExternalIDs(actor.getIDs()));
		//clinicalActor.setRole(role); // this probably needs to come from the actual actor reference?
		//clinicalActor.setTitle(title);
		clinicalActor.setContactDetails(getContactDetailsForActor(actor));
		return clinicalActor;
	}

	private Name getPersonName(Person person) {
		Name name = null;
		if (person != null) {
			name = getName(person.getName());
		}
		return name;
	}

	private UUID getFirstSourceClinicalActorId(List<SourceType> source) {
		UUID firstSourceActorId = null;
		if (source != null && source.size() > 0 && source.get(0) != null) {
			firstSourceActorId = getFirstClinicalActorId(source.get(0).getActor());
		}		
		return firstSourceActorId;
	}

	private List<UUID> getSupplementarayClinicalActorIds(List<SourceType> sources) {
		List<UUID> clinicalActorIds = new LinkedList<UUID>();
		if (sources != null && sources.size() > 1) {
			for (int i = 1; i < sources.size(); i++)
			{				
				clinicalActorIds.add(getFirstClinicalActorId(sources.get(i).getActor()));
			}
		}		
		return clinicalActorIds;
	}

	private UUID getFirstClinicalActorId(List<ActorReferenceType> actor) {
		UUID firstClinicalActorId = null;
		if (actor != null && actor.size() > 0) {
			firstClinicalActorId = getClinicalActorId(actor.get(0));
		}
		return firstClinicalActorId;
	}

	private UUID addClinicalActor(ActorType actor) {
		UUID clinicalActorId = null;
		ClinicalActor clinicalActor = getClinicalActorFromActor(actor);
		if (clinicalActor != null) {
			patient.addClinicalActor(clinicalActor);
			clinicalActorId = clinicalActor.getClinicalActorId();				
			currentClinicalActors.put(actor.getActorObjectID(), clinicalActorId);
		}
		return clinicalActorId;
	}

	private UUID getClinicalActorId(ActorReferenceType actorReferenceType) {
		UUID clinicalActorId = null;
		if (actorReferenceType != null) {
			ActorType actor = getActorFromReference(actorReferenceType);
			if (actor != null) {
				String actorObjectId = actor.getActorObjectID();
				if (currentClinicalActors.containsKey(actorObjectId))
					clinicalActorId = currentClinicalActors.get(actorObjectId);
				else
					clinicalActorId = addClinicalActor(actor);
			}
		}
		return clinicalActorId;
	}

	private Demographics getDemographicsForActor(String patientActorId) {
		Demographics actorDemographics = null;
		Person person = getPersonFromActorId(patientActorId);
		if (person != null) {
			actorDemographics = new Demographics();
			actorDemographics.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
			actorDemographics.setSourceId(primarySource.getSourceId());			

			actorDemographics.setDateOfBirth(CCRUtils.getDate(person.getDateOfBirth()));
			actorDemographics.setGender(CCRUtils.getGender(person.getGender()));
			actorDemographics.setName(getName(person.getName()));
			//actorDemographics.setOriginalId(originalId);
			
			// NOTE: In the CCR, the below are communicated in Social History
			//actorDemographics.setEthnicity(ethnicity);
			//actorDemographics.setLanguages(languages);
			//actorDemographics.setMaritalStatus(maritalStatus);
			//actorDemographics.setRace(race);
			//actorDemographics.setReligiousAffiliation(religiousAffiliation);
		}
		return actorDemographics;
	}

	private Person getPersonFromActorId(String patientActorId) {
		Person person = null;
		ActorType actorType = getActorFromId(patientActorId);
		if (actorType != null && actorType.getPerson() != null) {
			person = actorType.getPerson();
		}
		return person;
	}

	private ContactDetails getContactDetailsForActor(ActorType actorType) {
		ContactDetails contactDetails = null;
		if (actorType != null) {
			contactDetails = new ContactDetails();
			contactDetails.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
			contactDetails.setSourceId(primarySource.getSourceId());
			
//			contactDetails.setOriginalId(originalId);
			
			List<com.apixio.model.patient.Address> addList = CCRUtils.getAddressListFromAddressTypes(actorType.getAddress());
			
			//for now we are setting the 1st address as the primary 
			//as most CCR's do not contain primary indicator
			if(addList.size()>0){
				contactDetails.setPrimaryAddress(addList.get(0));
			}
			if(addList.size()>1){
				contactDetails.setAlternateAddress(addList.subList(1, addList.size()-1));
			}

			List<String> emailList = CCRUtils.getEmailsFromCommunicationType(actorType.getEMail());
			if(emailList.size()>0){
				contactDetails.setPrimaryEmail(emailList.get(0));
			}
			if(emailList.size()>1){
				contactDetails.setAlternateEmails(emailList.subList(1, emailList.size()-1));
			}
			
			List<TelephoneNumber> phoneList = CCRUtils.getTelephoneFromCommunicationType(actorType.getTelephone());
			if(phoneList.size()>0){
				contactDetails.setPrimaryPhone(phoneList.get(0));
			}
			if(phoneList.size()>1){
				contactDetails.setAlternatePhone(phoneList.subList(1, phoneList.size()-1));
			}		
		}
		return contactDetails;
	}

	private Set<ExternalID> getIDsForActor(ActorType actorType) {
		Set<ExternalID> actorIds = new HashSet<ExternalID>();
		if (actorType != null && actorType.getIDs() != null) {
			actorIds = getExternalIDsFromIDs(actorType.getIDs());
		}
		return actorIds;
	}
	
	private ActorType getActorFromId(String actorId) {
		ActorType actorType = null;
		for (ActorType currentActorType : ccr.getActors().getActor()) {
			if (currentActorType.getActorObjectID().equalsIgnoreCase(actorId)) {
				actorType = currentActorType;
			}
		}
		return actorType;
	}	

	private String getIdSource(IDType id) {
		String idSource = null;
		if (id.getIssuedBy() != null) {
			idSource = getActorId(id.getIssuedBy());
		} else if (id.getSource() != null) {
			idSource = getSourceId(id.getSource());
		}
		return idSource;
	}

	private String getSourceId(List<SourceType> sources) {
		String sourceId = null;
		if (sources != null && sources.size() > 0) {
			SourceType firstSource = sources.get(0);
			if (firstSource.getActor() != null && firstSource.getActor().size() > 0) {
				sourceId = getActorId(firstSource.getActor().get(0));
			}
		}
		return sourceId;
	}

	private String getActorId(ActorReferenceType actorReferenceType) {
		String actorId = null;
		ActorType actor = getActorFromId(actorReferenceType.getActorID());
		if (actor != null) {
			actorId = CCRUtils.getFirstIdString(actor.getIDs());
		}		
		return actorId;
	}

	private Map<String, String> getMetadata(List<String> commentIds) {
		Map<String, String> metadata = new HashMap<String, String>();
		if (commentIds != null) {
			for (String commentId : commentIds)
			{
				CommentType comment = getCommentTypeFromId(commentId);
				if (comment != null)
				{
					String key = CCRUtils.getDescriptionText(comment.getType());
					String value = CCRUtils.getDescriptionText(comment.getDescription());
					metadata.put(key, value);
				}
			}
		}
		return metadata;
	}

	private CommentType getCommentTypeFromId(String commentId) {
		CommentType commentType = null;
		for (CommentType currentCommentType : ccr.getComments().getComment()) {
			if (currentCommentType.getCommentObjectID().equalsIgnoreCase(commentId)) {
				commentType = currentCommentType;
			}
		}
		return commentType;
	}

	private Set<ExternalID> getExternalIDsFromIDs(List<IDType> ids) {
		Set<ExternalID> externalIds = new HashSet<ExternalID>();
		if (ids != null) {
			for (IDType id : ids) {
				externalIds.add(getExternalID(id));
			}
		}
		return externalIds;
	}

	public ExternalID getFirstExternalID(List<IDType> ids) {
		ExternalID externalId = null;
		if (ids != null && ids.size() > 0) {
			externalId = getExternalID(ids.get(0));
		}
		return externalId;
	}

	private List<ExternalID> getOtherExternalIDs(List<IDType> ids) {
		List<ExternalID> externalIds = new LinkedList<ExternalID>();
		if (ids != null && ids.size() > 1) {
			for (int i = 1; i < ids.size(); i++)
			{
				externalIds.add(getExternalID(ids.get(i)));
			}
		}
		return externalIds;
	}	
	
	private ExternalID getExternalID(IDType idType) {
		ExternalID externalId = null;
		if (idType != null) {
			externalId = new ExternalID();
			externalId.setId(idType.getID());
			externalId.setAssignAuthority(CCRUtils.getDescriptionText(idType.getType()));
			externalId.setSource(getIdSource(idType));
			externalId.setType(CCRUtils.getDescriptionText(idType.getType()));
		}
		return externalId;
	}
 
	private Name getName(com.apixio.converters.ccr.jaxb.ActorType.Person.Name personName) {
		Name name = null;
		if (personName != null) {
			if (personName.getCurrentName() != null)
				name = getNameFromPersonNameType(personName.getCurrentName());
		}
		return name;
	}

	private Name getNameFromPersonNameType(PersonNameType personNameType) {
		Name name = new Name();
		name.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
		name.setSourceId(primarySource.getSourceId());
		
		name.setFamilyNames(personNameType.getFamily());
		name.getGivenNames().addAll(personNameType.getGiven());
		name.getGivenNames().addAll(personNameType.getMiddle());
		//name.setNameType(nameType);
		//name.setOriginalId(originalId);
		name.setPrefixes(personNameType.getTitle());
		name.setSuffixes(personNameType.getSuffix());
		return name;
	}
}
