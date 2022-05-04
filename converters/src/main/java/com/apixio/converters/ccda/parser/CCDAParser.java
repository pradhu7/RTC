package com.apixio.converters.ccda.parser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.*;

import com.apixio.converters.ccda.utils.CCDAConstants;
import com.apixio.converters.ccda.utils.CCDAUtils;
import com.apixio.model.file.catalog.jaxb.generated.ApxCatalog;
import com.apixio.model.patient.*;
import com.apixio.model.patient.Encounter;
import com.apixio.model.patient.Organization;
import com.apixio.model.patient.Patient;
import com.apixio.model.patient.Procedure;
import org.apache.commons.lang.StringUtils;
import org.eclipse.emf.common.util.EList;
import org.eclipse.mdht.uml.hl7.datatypes.*;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.eclipse.mdht.uml.cda.*;
import org.eclipse.mdht.uml.cda.util.CDAUtil;
import org.eclipse.mdht.uml.hl7.datatypes.impl.REALImpl;
import org.eclipse.mdht.uml.hl7.vocab.ParticipationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apixio.converters.base.BaseUtils;
import com.apixio.converters.base.Parser;
import com.apixio.model.EitherStringOrNumber;
import com.apixio.model.file.catalog.jaxb.generated.ApxCatalog.CatalogEntry;
import com.apixio.utility.HashCalculator;
import javax.xml.bind.DatatypeConverter;

// TODO:
// 1) Update library to latest CCDA Support
// 2) Modify parsing code to be more resilient to errors
// 3) Get the patient id from the catalog file
// 4) Move constants for template IDS to a data structure

public class CCDAParser extends Parser {

	private ClinicalDocument ccdaDocument;
	private static final String PARSER_VERSION = "1.0";
	private byte[] docBytes = null;
	private Patient patient  = new Patient();
	private Source primarySource = new Source();
	private ParsingDetail currentParsingDetails;
	private CatalogEntry catalogEntry;
	private Encounter encompassingEncounter;
	// TODO: Why is this even here?
	private URI sourceDocumentUri;
	private HashMap<String, UUID> currentClinicalActors = new HashMap<String, UUID>();
	private static final Logger logger = LoggerFactory.getLogger(CCDAParser.class);
	
	@Override
	public void parse(InputStream ccdDocument, CatalogEntry catalogEntry) throws Exception {
		ApxCatalog.CatalogEntry.Patient catalogPatient = catalogEntry.getPatient();
		patient.setExternalIDs(BaseUtils.getExternalIdsFromCatalog(catalogPatient.getPatientId()));
		patient.setPrimaryExternalID(getPrimaryExternalId(patient.getExternalIDs()));
		docBytes = BaseUtils.getBytesFromInputStream(ccdDocument);
		this.catalogEntry = catalogEntry;
		// should we re-initialize since this method could be called more than once?
//		CDAUtil.ValidationHandler validationHandler = new ApixioValidationHandler();
//		((ApixioValidationHandler) validationHandler).shouldCaptureValidationStatistics(true);
		try{
            // TODO: Custom Load and Validation Handlers will enable us to capture detailed error messages for failures
//			CDAUtil.LoadHandler loadHandler = new ApixioLoadHandler();
			ccdaDocument = CDAUtil.load(new ByteArrayInputStream(docBytes));
			populateParsingDetails();
			populatePrimarySource();
			// TODO: Need to parse the legalAuthenticator to assign provider for the generated document
			populateEncompassingEncounter();
			populatePatient();		
			populateDocument();
//			Iterator<String> it = ((ApixioValidationHandler) validationHandler).getErrors().iterator();
//			while(it.hasNext()){
//				System.out.println(it.next());
//			}
		}catch(Exception e){
//			Iterator<String> it = ((ApixioValidationHandler) validationHandler).getErrors().iterator();
//			while(it.hasNext()){
//				System.out.println(it.next());
//			}
			logger.error(e.getMessage());
			throw e;
		}
	}

	@Override
	public Patient getPatient() throws Exception {
		return patient;
	}

	private void populateParsingDetails() {
		currentParsingDetails = new ParsingDetail();
		currentParsingDetails.setContentSourceDocumentType(DocumentType.STRUCTURED_DOCUMENT);
		currentParsingDetails.setContentSourceDocumentURI(sourceDocumentUri);
		currentParsingDetails.setSourceFileArchiveUUID(UUID.fromString(catalogEntry.getDocumentUUID()));
		currentParsingDetails.setParser(ParserType.CCD);
		currentParsingDetails.setParserVersion(PARSER_VERSION);
		currentParsingDetails.setParsingDateTime(DateTime.now());
        currentParsingDetails.setSourceUploadBatch(catalogEntry.getBatchId());
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

	// TODO: Get more detailed source information from the structured content
	private void populatePrimarySource() {
		primarySource = new Source();
		primarySource.setSourceId(primarySource.getInternalUUID());
		primarySource.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
		primarySource.setClinicalActorId(getAuthorClinicalActorId(ccdaDocument.getAuthors())); // -- "Author" - We'll need to add an actor and reference it
		primarySource.setCreationDate(CCDAUtils.getDateTimeFromTS(ccdaDocument.getEffectiveTime()));
		primarySource.setOrganization(getCustodianOrganization());
		primarySource.setOriginalId(CCDAUtils.getEidFromII(ccdaDocument.getId()));
		primarySource.setSourceSystem(CCDAUtils.getAuthoringDevice(ccdaDocument.getAuthors()));
		primarySource.setSourceType("CCDA");
		this.patient.addSource(primarySource);		
	}

	private void populateEncompassingEncounter() {
        if (ccdaDocument.getComponentOf() != null) {
            EncompassingEncounter encounter = ccdaDocument.getComponentOf().getEncompassingEncounter();
            if (encounter != null) {
                encompassingEncounter = new Encounter();
                encompassingEncounter.setEncounterStartDate(CCDAUtils.getLowTime(encounter.getEffectiveTime()));
                encompassingEncounter.setEncounterEndDate(CCDAUtils.getHighTime(encounter.getEffectiveTime()));
                for (EncounterParticipant encounterParticipant : encounter.getEncounterParticipants()) {
                    AssignedEntity assignedEntity = encounterParticipant.getAssignedEntity();
                    if (assignedEntity != null) {
                        // TODO: move assignedEntity parsing to a common method
                        CareSite careSite = new CareSite();
                        if (assignedEntity.getAddrs().size() > 0) {
                            careSite.setAddress(CCDAUtils.getAddressFromAD(assignedEntity.getAddrs().get(0)));
                        }
                        ClinicalActor encounterClinicalActor = getClinicalActorFromAssignedEntity(assignedEntity);
                        // TODO: Integrate health care provider taxonomy code system
                        encounterClinicalActor.setRole(CCDAUtils.getHealthCareProviderRole(assignedEntity.getCode()));
                        // TODO: Does the provider get the phone number or the care site?
                        encounterClinicalActor.setContactDetails(getContactDetails(assignedEntity.getTelecoms(), assignedEntity.getAddrs()));
                        patient.addCareSite(careSite);
                        encompassingEncounter.setSiteOfService(careSite);
                        patient.addClinicalActor(encounterClinicalActor);
                        encompassingEncounter.setPrimaryClinicalActorId(encounterClinicalActor.getClinicalActorId());
                        patient.addEncounter(encompassingEncounter);
                        // TODO: Should we set the "chief complaint" of this global encounter from the Chief Complaint sectioN?
                        // TODO: Where do we get the Encounter Type and Encounter ID?
                    }
                }
            }
        }
    }

	private UUID getAuthorClinicalActorId(EList<Author> authors) {
		UUID authorClinicalActorId = null;
		for (Author author : authors) {
			ClinicalActor authorActor = getClinicalActorFromAuthor(author.getAssignedAuthor());
			authorClinicalActorId = authorActor.getClinicalActorId();
			patient.addClinicalActor(authorActor);
		}
		return authorClinicalActorId;
	}

	private void populatePatient() throws Exception {
		populateHealthcareProviders();
		populatePatientDetails();
		populateAllergies();
		populateMedications();
		populateProblems();
		populateResolvedProblems();
		populateImmunizations();
		populateLabResults();
		populateVitalSigns();
		populateFamilyHistory();
		populateSocialHistory();
		populateProcedures();
		populateEncounters();
		populateCoverage();

		// TODO: Patient Level Sections
        // MEDICATIONS_ADMINISTERED_SECTION (Current data only indicates "none administered" -- awaiting better data)

        // TODO: Encounter Level Sections
        // Note: Some (all?) of the below are unstructured. (should we semi-structure the textextracted for these sections?)
        // ASSESSMENT_SECTION
        // CHIEF_COMPLAINT_SECTION (put into encompassing encounter?)
        // REASON_FOR_VISIT_SECTION (put into encompassing encounter?)
        // FUNCTIONAL_STATUS_SECTION
        // HISTORY_OF_PRESENT_ILLNESS_SECTION
        // INSTRUCTIONS_SECTION
        // PHYSICAL_EXAM_SECTION
        // PLAN_OF_CARE_SECTION
        // REVIEW_OF_SYSTEMS_SECTION

        // TODO: NOT YET SUPPORTED IN UNDERLYING MODEL
        // ADVANCE_DIRECTIVES_SECTION
		
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

		// TODO: What other things should have their source encounter set?
		// If we have an encounter scoping this, use the encounter date, else use the date document was created
		if (encompassingEncounter != null) {
			document.setSourceEncounter(encompassingEncounter.getEncounterId());
			document.setDocumentDate(encompassingEncounter.getEncounterStartDate());
		} else {
			document.setDocumentDate(CCDAUtils.getDateTimeFromTS(ccdaDocument.getEffectiveTime()));
		}

		//document.setDocumentId(documentId);
		String title = "CDA Document";
		if (ccdaDocument.getTitle() != null)
			title = ccdaDocument.getTitle().getText();
		document.setDocumentTitle(title);
		document.setOriginalId(CCDAUtils.getEidFromII(ccdaDocument.getId()));

		DocumentContent documentContent = new DocumentContent();
		documentContent.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
		documentContent.setSourceId(primarySource.getSourceId());

		// If we have a component/nonXMLBody/text
		// mediaType="application/pdf" representation="B64"
		if (ccdaDocument.getComponent() != null &&
				ccdaDocument.getComponent().getNonXMLBody() != null &&
				ccdaDocument.getComponent().getNonXMLBody().getText() != null) {
			ED nonXmlText = ccdaDocument.getComponent().getNonXMLBody().getText();
			String mimeType = nonXmlText.getMediaType();
			BinaryDataEncoding encoding = nonXmlText.getRepresentation();
			byte[] content = null;
			if (encoding.equals(BinaryDataEncoding.B64)) {
				content = DatatypeConverter.parseBase64Binary(nonXmlText.getText());
			} else {
				content = nonXmlText.getText().getBytes();
			}
			documentContent.setContent(content);
			try{
				String fileContentHash = HashCalculator.getFileHash(content);
				documentContent.setHash(fileContentHash);
				currentParsingDetails.setSourceFileHash(fileContentHash);
			}catch(Exception e){
				logger.warn("Error while calculating Hash of the file:"+e.getMessage());
			}
			documentContent.setLength(content.length);
			documentContent.setMimeType(mimeType);
		} else {
			try{
				String fileContentHash = HashCalculator.getFileHash(docBytes);
				documentContent.setHash(fileContentHash);
				currentParsingDetails.setSourceFileHash(fileContentHash);
			}catch(Exception e){
				logger.warn("Error while calculating Hash of the file:"+e.getMessage());
			}
			documentContent.setLength(docBytes.length);
			documentContent.setMimeType(catalogEntry.getMimeType());
		}
        documentContent.setUri(sourceDocumentUri);
		
		document.getDocumentContents().add(documentContent);
		patient.addDocument(document);
	}
	
	private void populateHealthcareProviders() {
		for (DocumentationOf documentationOf : ccdaDocument.getDocumentationOfs()) {
			addDocumentationOfProviders(documentationOf);
		}
	}

	private void populatePatientDetails() throws Exception {
		if (ccdaDocument.getRecordTargets() != null) {
			for (RecordTarget recordTarget : ccdaDocument.getRecordTargets()) {
				PatientRole patientRole = recordTarget.getPatientRole();
				if (patientRole != null) {
					// get patient IDs
					patient.setExternalIDs(CCDAUtils.getEidsFromIIList(patientRole.getIds()));
					ContactDetails contactDetails = getContactDetailsFromRecordTarget(patientRole);
					if (patient.getPrimaryContactDetails() == null)
						patient.setPrimaryContactDetails(contactDetails);
					else
						patient.addAlternateContactDetails(contactDetails);
					
					if (patientRole.getPatient() != null)
					{
						Set<ExternalID> moreIds = CCDAUtils.getEidsFromIIList(patientRole.getPatient().getIds());
						for (ExternalID id : moreIds) {
							patient.addExternalId(id);						
						}
						Demographics demographics = getDemographicsFromRecordTarget(patientRole.getPatient());
						if (patient.getPrimaryDemographics() == null)
							patient.setPrimaryDemographics(demographics);
						else
							patient.addAlternateDemographics(demographics);
					}
				}				
			}
		}	
	}

	private void populateAllergies() throws Exception {
		Section alertsSection = CCDAUtils.getFirstSectionByTemplateId(ccdaDocument.getSections(), CCDAConstants.ALLERGIES_SECTION);
		if (alertsSection != null)
		{
			for (Act allergyAct : alertsSection.getActs()) {

				for (Observation observation : allergyAct.getObservations()) {

					Allergy allergy = new Allergy();
					allergy.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
					allergy.setSourceId(primarySource.getSourceId());
					CCDAUtils.setBaseObjectIds(allergy, observation.getIds());

					PlayingEntity alertPlayingEntity = CCDAUtils.getObservationPlayingEntity(observation);
					if (alertPlayingEntity != null) {
						CCDAUtils.setBaseObjectCodes(allergy, alertPlayingEntity.getCode());
					}

					allergy.setAllergen(CCDAUtils.getAllergen(observation));
					allergy.setDiagnosisDate(CCDAUtils.getLowTime(allergyAct.getEffectiveTime()));
					//allergy.setMetadata(metadata;)
					//allergy.setMetaTag(tag, value)
					//allergy.setPrimaryClinicalActor(primaryClinicalActor);
					//allergy.setReactionDate(reactionDate)
					allergy.setReaction(CCDAUtils.getFirstAllergyReaction(observation));
					allergy.setReactionSeverity(CCDAUtils.getFirstReactionSeverity(observation));
					//allergy.setSourceEncounter(sourceEncounter)
					allergy.setSourceId(primarySource.getSourceId());
					//allergy.setSupplementaryClinicalActors(supplementaryClinicalActors)

					patient.addAllergy(allergy);
				}
			}
		}
	}
	
	private void populateMedications() {
//		MedicationsSection medicationsSection = CCDAUtils.getccdObject.getMedicationsSection();
		Section medicationsSection = CCDAUtils.getFirstSectionByTemplateId(ccdaDocument.getSections(), CCDAConstants.MEDICATIONS_SECTION);
		if (medicationsSection != null)
		{
			for (SubstanceAdministration medicationActivity : medicationsSection.getSubstanceAdministrations()) {
				Medication medication = new Medication();
				medication.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
				medication.setSourceId(primarySource.getSourceId());

				if (medicationActivity.getConsumable() != null &&
					medicationActivity.getConsumable().getManufacturedProduct() != null &&
					medicationActivity.getConsumable().getManufacturedProduct().getManufacturedMaterial() != null) {
					Material material = medicationActivity.getConsumable().getManufacturedProduct().getManufacturedMaterial();
					medication.setBrandName(CCDAUtils.getNameFromEN(material.getName()));
					if (material.getCode() != null) {
						CCDAUtils.setBaseObjectCodes(medication, material.getCode()); // TODO: Should we move translation up into main code if main code missing?
                        // TODO: For Athena, the sig mysteriously appears here so we will use the code.displayName
						if (material.getCode().getOriginalText() != null) {
							medication.setGenericName(CCDAUtils.getTextOrReference(medicationsSection, material.getCode().getOriginalText()));
						} else if (material.getCode().getDisplayName() != null){
							medication.setGenericName(material.getCode().getDisplayName());
						}
					}
				}

//				medication.setForm(form);
//				medication.setIngredients(ingredients);
//				medication.setPrimaryClinicalActor(primaryClinicalActor);
				medication.setRouteOfAdministration(CCDAUtils.getCodeDisplayName(medicationActivity.getRouteCode()));
//				medication.setSourceEncounter(sourceEncounter);
//				medication.setStrength(strength);
//				medication.setSupplementaryClinicalActors(supplementaryClinicalActors);
//				medication.setUnits(units);

				Prescription prescription = new Prescription();
				prescription.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
				prescription.setSourceId(primarySource.getSourceId());


				CCDAUtils.setBaseObjectIds(prescription, medicationActivity.getIds());

				// TODO: Update medication status parsing to work with generic CDA (is this as simple as statusCode="active" or maybe statusCode="completed"?
//				prescription.setActivePrescription(CCDAUtils.getPrescriptionActive(medicationActivity.getObservations().getMedicationStatusObservation()));
//				prescription.setAmount(amount);
				prescription.setAssociatedMedication(medication);
//				prescription.setCode(code);
//				prescription.setCodeTranslations(codeTranslations);
				prescription.setDosage(CCDAUtils.getDoseQuantityString(medicationActivity.getDoseQuantity()));
//				prescription.setFillDate(fillDate);
				prescription.setPrimaryClinicalActorId(getFirstClinicalActorIdFromPerformer(medicationActivity.getPerformers()));
//				prescription.setQuantity(quantity);

				if (medicationActivity.getSupplies() != null && medicationActivity.getSupplies().size() > 0) {
					Supply medicationActivitySupply = medicationActivity.getSupplies().get(0);
					CCDAUtils.setBaseObjectIds(prescription, medicationActivitySupply.getIds());
					prescription.setRefillsRemaining(CCDAUtils.getRefills(medicationActivitySupply.getRepeatNumber()));
				}

				prescription.setSig(CCDAUtils.getTextOrReference(medicationsSection, medicationActivity.getText()));
//				prescription.setSourceEncounter(sourceEncounter);
//				prescription.setSupplementaryClinicalActors(supplementaryClinicalActors);
				EList<SXCM_TS> medicationTimes = medicationActivity.getEffectiveTimes();

				if (medicationTimes != null) {
					for (int i = 0; i < medicationTimes.size(); i++) {
						if (i == 0 && medicationTimes.get(0) instanceof IVL_TS) {
							IVL_TS medicationTime = (IVL_TS) medicationTimes.get(0);
							prescription.setPrescriptionDate(CCDAUtils.getLowTime(medicationTime));
							prescription.setEndDate(CCDAUtils.getHighTime(medicationTime));
						} else if (i == 1) {
							prescription.setFrequency(CCDAUtils.getFrequencyString(medicationTimes.get(1))); // how do i do this???
						}
					}
				}

				patient.addPrescription(prescription);
			}
		}
	}

	private UUID getFirstClinicalActorIdFromPerformer(EList<Performer2> performers) {
		UUID actorId = null;
		if (performers.size() > 0) {
			actorId = getClinicalActorIdFromPerformer(performers.get(0));
		}
		return actorId;
	}

	private UUID getClinicalActorIdFromPerformer(Performer2 performer2) {
		UUID clinicalActorId = null;
		if (performer2 != null && performer2.getAssignedEntity() != null) {
			String comparableId = UUID.randomUUID().toString(); // start with a random UUID
			if (performer2.getAssignedEntity().getIds().size() > 0) {
				ExternalID firstId = CCDAUtils.getEidFromII(performer2.getAssignedEntity().getIds().get(0));
				comparableId = CCDAUtils.getExternalIDComparable(firstId);
			}
			if (currentClinicalActors.containsKey(comparableId))
				clinicalActorId = currentClinicalActors.get(comparableId);
			else
				clinicalActorId = addClinicalActor(performer2.getAssignedEntity(), comparableId);
		}
		return clinicalActorId;
	}
	

	private UUID addClinicalActor(AssignedEntity assignedEntity, String comparableId) {
		UUID clinicalActorId = null;
		ClinicalActor clinicalActor = getClinicalActorFromAssignedEntity(assignedEntity);
		if (clinicalActor != null) {
			patient.addClinicalActor(clinicalActor);
			clinicalActorId = clinicalActor.getClinicalActorId();				
			currentClinicalActors.put(comparableId, clinicalActorId);
		}
		return clinicalActorId;
	}

	private void populateImmunizations() {
		Section immunizationsSection = CCDAUtils.getFirstSectionByTemplateId(ccdaDocument.getSections(), CCDAConstants.VACCINE_LIST_SECTION);
		if (immunizationsSection != null)
		{
			for (SubstanceAdministration medicationActivity : immunizationsSection.getSubstanceAdministrations()) {
				Medication medication = new Medication();
				medication.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
				medication.setSourceId(primarySource.getSourceId());

				if (medicationActivity.getConsumable() != null &&
					medicationActivity.getConsumable().getManufacturedProduct() != null &&
					medicationActivity.getConsumable().getManufacturedProduct().getManufacturedMaterial() != null) {
					Material material = medicationActivity.getConsumable().getManufacturedProduct().getManufacturedMaterial();
					medication.setBrandName(CCDAUtils.getNameFromEN(material.getName()));
					if (material.getCode() != null) {
						CCDAUtils.setBaseObjectCodes(medication, material.getCode());
						medication.setGenericName(CCDAUtils.getTextOrReference(immunizationsSection, material.getCode().getOriginalText()));
					}

				}
//				medication.setForm(form);
//				medication.setIngredients(ingredients);
//				medication.setPrimaryClinicalActor(primaryClinicalActor);
				medication.setRouteOfAdministration(CCDAUtils.getCodeDisplayName(medicationActivity.getRouteCode()));
//				medication.setSourceEncounter(sourceEncounter);
//				medication.setStrength(strength);
//				medication.setSupplementaryClinicalActors(supplementaryClinicalActors);
//				medication.setUnits(units);

				Administration administration = new Administration();
				administration.setSourceId(primarySource.getSourceId());
				administration.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
				CCDAUtils.setBaseObjectIds(administration, medicationActivity.getIds());
//				administration.setAdminDate(adminDate);
//				administration.setAmount(amount);
//				administration.setCode(code);
//				administration.setCodeTranslations(codeTranslations);
//				administration.setDosage(dosage);
//				administration.setEndDate(endDate);
				administration.setMedication(medication);
//				administration.setMedicationSeriesNumber(medicationSeriesNumber);
//				administration.setPrimaryClinicalActor(primaryClinicalActor);
//				administration.setQuantity(quantity);
//				administration.setSourceEncounter(sourceEncounter);
				administration.setStartDate(CCDAUtils.getFirstDateTimeFromList(medicationActivity.getEffectiveTimes()));
//				administration.setSupplementaryClinicalActors(supplementaryClinicalActors);

				patient.addAdministration(administration);
			}
		}
	}

	private void populateResolvedProblems() {
		Section resolvedProblemSection = CCDAUtils.getFirstSectionByTemplateId(ccdaDocument.getSections(), CCDAConstants.PAST_MEDICAL_HISTORY_SECTION);
		if (resolvedProblemSection != null) {
		    for (Observation problemObservation : resolvedProblemSection.getObservations()) {
                Problem problem = new Problem();
                problem.setSourceId(primarySource.getSourceId());
                problem.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
                CCDAUtils.setBaseObjectIds(problem, problemObservation.getIds());
                problem.setResolutionStatus(ResolutionStatus.RESOLVED);
                problem.setStartDate(CCDAUtils.getLowTime(problemObservation.getEffectiveTime()));
                problem.setEndDate(CCDAUtils.getHighTime(problemObservation.getEffectiveTime()));
                problem.setProblemName(CCDAUtils.getFirstTextFromCDList(problemObservation.getSection(),problemObservation.getValues()));
                if (problemObservation.getValues() != null && problemObservation.getValues().size() > 0) {
                    CD value = (CD) problemObservation.getValues().get(0);
                    List<ClinicalCode> problemClinicalCodes = CCDAUtils.getClinicalCodes(value);
                    for (int i = 0; i < problemClinicalCodes.size(); i++) {
                        if (i == 0) {
                            problem.setCode(problemClinicalCodes.get(i));
                        }
                        else
                            problem.getCodeTranslations().add(problemClinicalCodes.get(i));
                    }
                }

                patient.addProblem(problem);
            }
        }

//          <entry>
//            <observation moodCode="EVN" classCode="OBS">
//              <templateId root="2.16.840.1.113883.10.20.22.4.4"/>
//              <id root="12e71a3d-2020-dd6e-1de6-001A64958C30"/>
//              <code code="282291009" codeSystem="2.16.840.1.113883.6.96" codeSystemName="SNOMED CT" displayName="Diagnosis">
//                <translation code="29308-4" codeSystem="2.16.840.1.113883.6.1" codeSystemName="LOINC" displayName="Diagnosis"/>
//              </code>
//              <statusCode code="completed"/>
//              <effectiveTime>
//                <low nullFlavor="NI"/>
//              </effectiveTime>
//              <value xsi:type="CD" displayName="acid reflux/GERD"/>
//            </observation>
//          </entry>
	}

	private void populateProblems() {
        Section problemSection = CCDAUtils.getFirstSectionByTemplateId(ccdaDocument.getSections(),CCDAConstants.PROBLEMS_SECTION);
        if (problemSection != null) {
			for (Act problemAct : problemSection.getActs()) {
				if (problemAct.getObservations().size() > 0) {
                    // TODO: Additional Acts and Observations may exist. Update code to limit to correct Problem Act and Problem Observation
                    Observation problemObservation = problemAct.getObservations().get(0);

					Problem problem = new Problem();
					problem.setSourceId(primarySource.getSourceId());
					problem.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
					CCDAUtils.setBaseObjectIds(problem, problemObservation.getIds());
					problem.setResolutionStatus(ResolutionStatus.ACTIVE);
					problem.setStartDate(CCDAUtils.getLowTime(problemAct.getEffectiveTime()));
					problem.setEndDate(CCDAUtils.getHighTime(problemAct.getEffectiveTime()));
					problem.setProblemName(CCDAUtils.getTextOrReference(problemSection, problemObservation.getText()));
					if (problemObservation.getValues() != null && problemObservation.getValues().size() > 0) {
						CD value = (CD) problemObservation.getValues().get(0);
						List<ClinicalCode> problemClinicalCodes = CCDAUtils.getClinicalCodes(value);
						for (int i = 0; i < problemClinicalCodes.size(); i++) {
							if (i == 0)
								problem.setCode(problemClinicalCodes.get(i));
							else
								problem.getCodeTranslations().add(problemClinicalCodes.get(i));
						}
					}
					// TODO: Update code to get correct problem status
					// problem.setResolutionStatus(CCDAUtils.getResolutionStatus(problemObservation.getProblemStatus()));
					// TODO: Get comment and add to metadata
					//				problem.setAssociatedEncounter(associatedEncounter);
					//				problem.setPrimaryClinicalActor(primaryClinicalActor);
					problem.setResolutionStatus(CCDAUtils.getProblemStatus(problemAct.getStatusCode()));
					//				problem.setResolved(resolved);
					//				problem.setSeverity(severity);
					//				problem.setSourceEncounter(sourceEncounter);
					//				problem.setSupplementaryClinicalActors(supplementaryClinicalActors);
					//				problem.setTemporalStatus(temporalStatus);

					patient.addProblem(problem);
				}
			}
		}
	}

	private void populateProcedures() {
		Section proceduresSection = CCDAUtils.getFirstSectionByTemplateId(ccdaDocument.getSections(), CCDAConstants.PROCEDURES_SECTION);
		if (proceduresSection != null) {
			// Loop through all "Procedures"
			for (org.eclipse.mdht.uml.cda.Procedure cdaProcedure : proceduresSection.getProcedures()) {
				Procedure procedure = new Procedure();
				procedure.setSourceId(primarySource.getSourceId());
				procedure.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
				CCDAUtils.setBaseObjectCodes(procedure, cdaProcedure.getCode());
				CCDAUtils.setBaseObjectIds(procedure, cdaProcedure.getIds());
				procedure.setPerformedOn(CCDAUtils.getLowTime(cdaProcedure.getEffectiveTime()));
				procedure.setEndDate(CCDAUtils.getHighTime(cdaProcedure.getEffectiveTime()));
				//procedure.setBodySite(bodySite);
				procedure.setPrimaryClinicalActorId(getFirstClinicalActorIdFromPerformer(cdaProcedure.getPerformers()));
				if (cdaProcedure.getCode().getOriginalText() != null) {
					procedure.setProcedureName(CCDAUtils.getTextOrReference(proceduresSection, cdaProcedure.getCode().getOriginalText()));
				} else {
					procedure.setProcedureName(cdaProcedure.getCode().getDisplayName());
				}
				// Get interpretation
				for (Act procedureAct : cdaProcedure.getActs()) {
					procedure.setInterpretation(CCDAUtils.getTextOrReference(proceduresSection, procedureAct.getText()));
                }
				patient.addProcedure(procedure);
			}
			// Loop through all "Observations"
			for (org.eclipse.mdht.uml.cda.Observation cdaObservation : proceduresSection.getObservations()) {
				Procedure procedure = new Procedure();
				procedure.setSourceId(primarySource.getSourceId());
				procedure.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
				CCDAUtils.setBaseObjectCodes(procedure, cdaObservation.getCode());
				CCDAUtils.setBaseObjectIds(procedure, cdaObservation.getIds());
				procedure.setPerformedOn(CCDAUtils.getLowTime(cdaObservation.getEffectiveTime()));
				procedure.setEndDate(CCDAUtils.getHighTime(cdaObservation.getEffectiveTime()));
				//procedure.setBodySite(bodySite);
				procedure.setPrimaryClinicalActorId(getFirstClinicalActorIdFromPerformer(cdaObservation.getPerformers()));

				if (cdaObservation.getCode().getOriginalText() != null) {
					procedure.setProcedureName(CCDAUtils.getTextOrReference(proceduresSection, cdaObservation.getCode().getOriginalText()));
				} else {
					procedure.setProcedureName(cdaObservation.getCode().getDisplayName());
				}
				// Get interpretation
				for (Act procedureAct : cdaObservation.getActs()) {
					procedure.setInterpretation(CCDAUtils.getTextOrReference(proceduresSection, procedureAct.getText()));
				}
				patient.addProcedure(procedure);
			}
		}
	}

	// TODO: Add Provider and Care Site information
	private void populateEncounters() {
		Section encountersSection = CCDAUtils.getFirstSectionByTemplateId(ccdaDocument.getSections(),CCDAConstants.ENCOUNTERS_SECTION);

		if (encountersSection != null) {
            for (org.eclipse.mdht.uml.cda.Encounter encounterActivity : encountersSection.getEncounters()) {
                Encounter encounter = new Encounter();
                encounter.setSourceId(primarySource.getSourceId());
                encounter.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
                CCDAUtils.setBaseObjectCodes(encounter, encounterActivity.getCode());
                CCDAUtils.setBaseObjectIds(encounter, encounterActivity.getIds());
                encounter.setEncounterStartDate(CCDAUtils.getLowTime(encounterActivity.getEffectiveTime()));
                encounter.setEncounterEndDate(CCDAUtils.getHighTime(encounterActivity.getEffectiveTime()));
                // TODO: Get encounter type from code (ie. Office Visit) -- do this for encompassing encounter as well
//                encounter.setEncType(EncounterType.);
                for (Performer2 encounterPerformer : encounterActivity.getPerformers()) {
                    AssignedEntity assignedEntity = encounterPerformer.getAssignedEntity();
                    if (assignedEntity != null) {
                        // TODO: move assignedEntity parsing to a common method
                        CareSite careSite = new CareSite();
                        if (assignedEntity.getAddrs().size() > 0) {
                            careSite.setAddress(CCDAUtils.getAddressFromAD(assignedEntity.getAddrs().get(0)));
                        }
                        ClinicalActor encounterClinicalActor = getClinicalActorFromAssignedEntity(assignedEntity);
                        // TODO: Integrate health care provider taxonomy code system
                        encounterClinicalActor.setRole(CCDAUtils.getHealthCareProviderRole(assignedEntity.getCode()));
                        // TODO: Does the provider get the phone number or the care site?
                        encounterClinicalActor.setContactDetails(getContactDetails(assignedEntity.getTelecoms(), assignedEntity.getAddrs()));
                        patient.addCareSite(careSite);
                        encounter.setSiteOfService(careSite);
                        patient.addClinicalActor(encounterClinicalActor);
                        encounter.setPrimaryClinicalActorId(encounterClinicalActor.getClinicalActorId());

                        // TODO: Should encounter diagnosis be added as problems with link to this encounter?
                        for (Act encounterDiagnosisAct : encounterActivity.getActs()) {
                            for (Observation encounterDiagnosisObservation : encounterDiagnosisAct.getObservations()) {
                                for (ANY value : encounterDiagnosisObservation.getValues()) {
                                    if (value instanceof CD) {
                                        List<ClinicalCode> chiefComplaintCodes = CCDAUtils.getClinicalCodes((CD) value);
                                        for (ClinicalCode chiefComplaintCode : chiefComplaintCodes) {
											if (chiefComplaintCode.getDisplayName() == null && ((CD) value).getOriginalText() != null) {
												chiefComplaintCode.setDisplayName(CCDAUtils.getTextOrReference(encountersSection, ((CD) value).getOriginalText()));
											}
											encounter.getChiefComplaints().add(chiefComplaintCode);
										}
                                    }
                                }
                            }
                        }
                        // TODO: Should we set the "chief complaint" of this global encounter from the Chief Complaint sectioN?
                        // TODO: Where do we get the Encounter Type and Encounter ID?
                    }
                }

                // TODO: Should these be set to Encounter Diagnosis from encounter object?
                // encounter.getChiefComplaints().add();
                patient.addEncounter(encounter);
            }
        }
    }
	
	private void populateSocialHistory() {
		Section socialHistorySection = CCDAUtils.getFirstSectionByTemplateId(ccdaDocument.getSections(), CCDAConstants.SOCIAL_HISTORY_SECTION);
		if (socialHistorySection != null) {
			for (Observation socialHistoryObservation : socialHistorySection.getObservations()) {
                SocialHistory socialHistory = new SocialHistory();
                socialHistory.setSourceId(primarySource.getSourceId());
                socialHistory.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
                CCDAUtils.setBaseObjectIds(socialHistory, socialHistoryObservation.getIds());
				CCDAUtils.setBaseObjectCodes(socialHistory, socialHistoryObservation.getCode());
				DateTime socialHistoryTime = CCDAUtils.getDateTimeFromTS(socialHistoryObservation.getEffectiveTime());
				if (socialHistoryTime == null) {
					socialHistoryTime = CCDAUtils.getLowTime(socialHistoryObservation.getEffectiveTime());
				}
                socialHistory.setDate(socialHistoryTime);
                socialHistory.setType(CCDAUtils.getClinicalCode(socialHistoryObservation.getCode()));
                // Set field name to Display Name of Type
				if (socialHistoryObservation.getCode() != null) {
					if (socialHistoryObservation.getCode().getOriginalText() != null) {
						socialHistory.setFieldName(CCDAUtils.getTextOrReference(socialHistorySection, socialHistoryObservation.getCode().getOriginalText()));
					} else if (socialHistoryObservation.getCode().getDisplayName() != null){
						socialHistory.setFieldName(socialHistoryObservation.getCode().getDisplayName());
					}
				}
                // Set Value
                if (socialHistoryObservation.getValues().size() > 0) {
                    if (socialHistoryObservation.getValues().get(0) instanceof CD) {
                        CD socialHistoryValue = (CD) socialHistoryObservation.getValues().get(0);
                        if (socialHistoryValue.getOriginalText() != null) {
							socialHistory.setValue(CCDAUtils.getTextOrReference(socialHistorySection, socialHistoryValue.getOriginalText()));
						} else {
							socialHistory.setValue(socialHistoryValue.getDisplayName());
						}
                    } else if (socialHistoryObservation.getValues().get(0) instanceof ST) {
						ST socialHistoryValue = (ST) socialHistoryObservation.getValues().get(0);
						socialHistory.setValue(socialHistoryValue.getText());
					} else if (socialHistoryObservation.getValues().get(0) instanceof PQ) {
						PQ socialHistoryValue = (PQ) socialHistoryObservation.getValues().get(0);
						socialHistory.setValue(socialHistoryValue.getValue().toString());
					}
                }
                patient.addSocialHistory(socialHistory);
			}
		}
	}

	private void populateFamilyHistory() {
		// TODO: Our Family History model isn't properly modeled. For now, just capture text.
        Section familyHistorySection = CCDAUtils.getFirstSectionByTemplateId(ccdaDocument.getSections(),CCDAConstants.FAMILY_HISTORY_SECTION);
        if (familyHistorySection != null) {
            for (Organizer familyHistoryOrganizer : familyHistorySection.getOrganizers()){
                String familyHistoryString = "";
                if(familyHistoryOrganizer.getSubject() != null && familyHistoryOrganizer.getSubject().getRelatedSubject() != null) {
                    RelatedSubject relatedSubject = familyHistoryOrganizer.getSubject().getRelatedSubject();
                    familyHistoryString = relatedSubject.getCode().getDisplayName();
                    if (relatedSubject.getSubject() != null && relatedSubject.getSubject().getAdministrativeGenderCode() != null) {
                        familyHistoryString += " (" + CCDAUtils.getGender(relatedSubject.getSubject().getAdministrativeGenderCode()).toString() + ")";
                    }
                }
                for (Observation familyHistoryObservation : familyHistoryOrganizer.getObservations()) {
                    FamilyHistory familyHistory = new FamilyHistory();
                    familyHistory.setSourceId(primarySource.getSourceId());
                    familyHistory.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
                    CCDAUtils.setBaseObjectIds(familyHistory, familyHistoryObservation.getIds());
                    if (familyHistoryObservation.getValues().size() > 0) {
                        familyHistory.setCode(CCDAUtils.getClinicalCode((CD) familyHistoryObservation.getValues().get(0)));
                    }
                    familyHistory.setFamilyHistory(familyHistoryString);
                    patient.addFamilyHistory(familyHistory);
                }
            }
        }
	}

	private void populateVitalSigns() {
		Section vitalSignsSection = CCDAUtils.getFirstSectionByTemplateId(ccdaDocument.getSections(),CCDAConstants.VITALS_SECTION);
		if (vitalSignsSection != null) {
			// An Organizer combines a group of components into a panel. (It is possible to have observations off of the main section)
			for (Organizer vitalSignsOrganizer : vitalSignsSection.getOrganizers()) {
				for (Observation resultObservation : vitalSignsOrganizer.getObservations()) {
					BiometricValue biometricValue = new BiometricValue();
					biometricValue.setSourceId(primarySource.getSourceId());
					biometricValue.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
					CCDAUtils.setBaseObjectIds(biometricValue, resultObservation.getIds());
					CCDAUtils.setBaseObjectCodes(biometricValue ,resultObservation.getCode());
					biometricValue.setResultDate(CCDAUtils.getDateTimeFromTS(resultObservation.getEffectiveTime()));
					if (resultObservation.getCode() != null) {
						String labDisplayName = resultObservation.getCode().getDisplayName();

						// We will likely fallback to the code itself for e-MDs compatibility

						biometricValue.setName(labDisplayName);
					}
					if (resultObservation.getValues().size() > 0)
					{
						ANY value = resultObservation.getValues().get(0);
						if (value instanceof PQ) {
							PQ physicalQuantity = (PQ) value;
							biometricValue.setValue(new EitherStringOrNumber(physicalQuantity.getValue()));
							biometricValue.setUnitsOfMeasure(physicalQuantity.getUnit()); // TODO: If this is "1", should we omit???
						}
						else if (value instanceof ST)
						{
							ST stringContent = (ST) value;
							biometricValue.setValue(new EitherStringOrNumber(stringContent.getText()));
						}
					}
					patient.addBiometricValue(biometricValue);
				}
			}
		}
	}

	private void populateLabResults() {
		Section resultsSection = CCDAUtils.getFirstSectionByTemplateId(ccdaDocument.getSections(),CCDAConstants.RESULTS_SECTION);
		if (resultsSection != null) {
			// An Organizer combines a group of components into a panel. (It is possible to have observations off of the main section)
			for (Organizer resultOrganizer : resultsSection.getOrganizers()) {
				ClinicalCode panel = null;
				if (resultOrganizer.getCode() != null)
					panel = CCDAUtils.getClinicalCode(resultOrganizer.getCode());

				for (Observation resultObservation : resultOrganizer.getObservations()) {
					// get labs information
					LabResult labResult = new LabResult();
					labResult.setSourceId(primarySource.getSourceId());
					labResult.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
					CCDAUtils.setBaseObjectIds(labResult, resultObservation.getIds());
					CCDAUtils.setBaseObjectCodes(labResult ,resultObservation.getCode());
					labResult.setPanel(panel);
					if (resultObservation.getCode() != null) {
						String labDisplayName = resultObservation.getCode().getDisplayName();
						if (labDisplayName == null || labDisplayName.equals("")) {
							// fallback to the code itself for e-MDs compatibility
							labDisplayName = resultObservation.getCode().getCode();
						}

						labResult.setLabName(labDisplayName);
					}

					labResult.setSampleDate(CCDAUtils.getDateTimeFromTS(resultObservation.getEffectiveTime()));

					if (resultObservation.getInterpretationCodes().size() > 0) {
						labResult.setFlag(CCDAUtils.getLabFlag(resultObservation.getInterpretationCodes().get(0).getCode()));
					}

					if (resultObservation.getValues().size() > 0)
					{
						ANY value = resultObservation.getValues().get(0);
						if (value instanceof PQ) {
							PQ physicalQuantity = (PQ) value;
							labResult.setValue(new EitherStringOrNumber(physicalQuantity.getValue()));
							labResult.setUnits(physicalQuantity.getUnit()); // TODO: If this is "1", should we omit???
						}
						else if (value instanceof ST)
						{
							ST stringContent = (ST) value;
							labResult.setValue(new EitherStringOrNumber(stringContent.getText()));
						}
						else if (value instanceof REALImpl){
						    REALImpl stringContent = (REALImpl) value;
                            labResult.setValue(new EitherStringOrNumber(stringContent.getValue()));
                        }
					}

					if (resultObservation.getReferenceRanges().size() > 0) {
						ReferenceRange referenceRange = resultObservation.getReferenceRanges().get(0);
						if (referenceRange.getObservationRange() != null) {
							labResult.setRange(CCDAUtils.getTextOrReference(resultsSection, referenceRange.getObservationRange().getText()));
						}
					}

					patient.addLabResult(labResult);
				}
			}
		}
	}

	// TODO: The coverage data we are getting currently seems insufficient for our typical use cases.
	private void populateCoverage() {
        Section payersSection = CCDAUtils.getFirstSectionByTemplateId(ccdaDocument.getSections(),CCDAConstants.PAYERS_SECTION);
        if (payersSection != null) {
            for (Act payerAct : payersSection.getActs()) {
                for (EntryRelationship entryRelationship : payerAct.getEntryRelationships()) {
                    if (entryRelationship.getAct() != null) {
                        Act payerSequenceAct = entryRelationship.getAct();
                        Coverage coverage = new Coverage();
                        coverage.setSourceId(primarySource.getSourceId());
                        coverage.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
                        CCDAUtils.setBaseObjectIds(coverage, payerSequenceAct.getIds());
                        if (entryRelationship.getSequenceNumber() != null && entryRelationship.getSequenceNumber().getValue() != null) {
							coverage.setSequenceNumber(entryRelationship.getSequenceNumber().getValue().intValue());
						}
						coverage.setGroupNumber(CCDAUtils.getFirstEidFromIIList(payerSequenceAct.getIds()));
						coverage.setType(CCDAUtils.getCoverageType(payerSequenceAct.getCode()));

						// TODO: We are defaulting the start and end date and then trying to find better values.
						coverage.setStartDate(primarySource.getCreationDate().toLocalDate());
						coverage.setEndDate(LocalDate.now());
						for (Performer2 performer : payerSequenceAct.getPerformers()) {
							// TODO: We need to find data with actual dates to determine which fields to use for start and end
							// For now, use the "time" of the performer that is the Payor
							if (performer.getAssignedEntity() != null &&
								performer.getAssignedEntity().getCode() != null &&
								performer.getAssignedEntity().getCode().getCode() != null &&
								performer.getAssignedEntity().getCode().getCode().equals("PAYOR")) {
								DateTime startDateTime = CCDAUtils.getLowTime(performer.getTime());
								if (startDateTime != null) {
									coverage.setStartDate(startDateTime.toLocalDate());
								}
								DateTime endDateTime = CCDAUtils.getHighTime(performer.getTime());
								if (endDateTime != null) {
									coverage.setEndDate(endDateTime.toLocalDate());
								}
								// TODO: Where should we stick the organization name?
								if (performer.getAssignedEntity().getRepresentedOrganizations() != null) {
                                    for (org.eclipse.mdht.uml.cda.Organization organization : performer.getAssignedEntity().getRepresentedOrganizations()) {
                                        coverage.setHealthPlanName(CCDAUtils.getFirstNameFromONList(organization.getNames()));
                                    }
                                }
							}
						}

						for (Participant2 participant : payerSequenceAct.getParticipants()) {
							// Information about the patient with respect to the policy or program
							if (participant.getTypeCode() == ParticipationType.COV) {
								ExternalID id = CCDAUtils.getFirstEidFromIIList(participant.getParticipantRole().getIds());
								// TODO: What is the difference between member number and beneficiary id in this context?
								coverage.setBeneficiaryID(id);
								coverage.setMemberNumber(id);
							}
							// Subscriber
							if (participant.getTypeCode() == ParticipationType.HLD) {
								for (ExternalID id : CCDAUtils.getEidsFromIIList(participant.getParticipantRole().getIds())) {
									coverage.setSubscriberID(id);
								}
							}
						}
						// Note: Some CCDAs contain multiple acts and some will already have plan name from organization name.
                        for (Act planAct : payerSequenceAct.getActs()) {
                            // SHALL contain at least one [1..*] entryRelationship (CONF:8939) such that it
                            // The target of a policy activity with act/entryRelationship/@typeCode="REFR" SHALL be an authorization activity (templateId 2.16.840.1.113883.10.20.1.19)
                            // OR an act, with act[@classCode="ACT"] and act[@moodCode="DEF"], representing a description of the coverage plan
                            // A description of the coverage plan SHALL contain one or more act/id, to represent the plan identifier, and an act/text with the name of the plan
                            String healthPlanName = CCDAUtils.getTextOrReference(planAct.getSection(), planAct.getText());
						    if (StringUtils.isEmpty(coverage.getHealthPlanName()) && !StringUtils.isEmpty(healthPlanName)) {
                                coverage.setHealthPlanName(healthPlanName);
                            }
                        }
                        // TODO: Remove this and update downstream consumers to be more resilient.
                        if (coverage.getHealthPlanName() == null) {
						    coverage.setHealthPlanName("Unknown");
                        }
                        // TODO: in order to support coverage, we need DCI start and end
						// For now, assume if no start, then the date that document was created
                        if (primarySource.getDciStart() == null)
                            primarySource.setDciStart(primarySource.getCreationDate().toLocalDate());
						// For now, assume if no end, then the current date
                        if (primarySource.getDciEnd() == null)
                            primarySource.setDciEnd(LocalDate.now());

                        patient.addCoverage(coverage);
                    }
                }
            }
        }
    }
	
	private ContactDetails getContactDetailsFromRecordTarget(PatientRole patientRole) {
		ContactDetails contactDetails = null;
		
		if (patientRole != null) {
			contactDetails = getContactDetails(patientRole.getTelecoms(), patientRole.getAddrs());
			
		}
		return contactDetails;
	}
	
	private ContactDetails getContactDetails(EList<TEL> telecoms, EList<AD> addrs) {
		ContactDetails contactDetails = new ContactDetails();
		contactDetails.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
		contactDetails.setSourceId(primarySource.getSourceId());	
		if (addrs != null) {
			for (AD address : addrs) {
				if (contactDetails.getPrimaryAddress() == null)
					contactDetails.setPrimaryAddress(CCDAUtils.getAddressFromAD(address));
				else
					contactDetails.getAlternateAddresses().add(CCDAUtils.getAddressFromAD(address));
			}
		}
		if (telecoms != null) {
			for (TEL telecom : telecoms) {					
				if (telecom != null && telecom.getValue() != null) {
					String[] telecomParts = telecom.getValue().split(":", 2);
					if (telecomParts.length == 2) {
						if (telecomParts[0].equalsIgnoreCase("tel")) {
							TelephoneNumber telephoneNumber = new TelephoneNumber();
							telephoneNumber.setPhoneNumber(telecomParts[1]);
							telephoneNumber.setPhoneType(CCDAUtils.getTelephoneType(telecom.getUses()));
							if (contactDetails.getPrimaryPhone() == null)
								contactDetails.setPrimaryPhone(telephoneNumber);	
							else
								contactDetails.getAlternatePhones().add(telephoneNumber);
						}
						else if (telecomParts[1].equalsIgnoreCase("mailto")) {
							if (contactDetails.getPrimaryEmail() == null)
								contactDetails.setPrimaryEmail(telecomParts[1]);	
							else
								contactDetails.getAlternateEmails().add(telecomParts[1]);
						}
					}						
				}					
			}
		}	
		return contactDetails;
	}

	private void addDocumentationOfProviders(DocumentationOf documentationOf) {
		if (documentationOf != null && documentationOf.getServiceEvent() != null) {
			for (Performer1 performer : documentationOf.getServiceEvent().getPerformers()) {
				patient.addClinicalActor(getClinicalActorFromPerformer(performer));
			}
		}		
	}
	
	private Organization getCustodianOrganization() {
		Organization organization = null;
		if (ccdaDocument.getCustodian() != null &&
			ccdaDocument.getCustodian().getAssignedCustodian() != null &&
			ccdaDocument.getCustodian().getAssignedCustodian().getRepresentedCustodianOrganization() != null) {
			CustodianOrganization custodianOrganization = ccdaDocument.getCustodian().getAssignedCustodian().getRepresentedCustodianOrganization();
			organization = new Organization();
			organization.setName(CCDAUtils.getNameFromON(custodianOrganization.getName()));
			Set<ExternalID> custodianOrganizationIds = CCDAUtils.getEidsFromIIList(custodianOrganization.getIds());
			int idNum = 0;
			for (ExternalID id : custodianOrganizationIds) { //.size() > 0) {
				if (idNum == 0) {
					organization.setPrimaryId(id);
					organization.setOriginalId(id);
				}
				else
					organization.getAlternateIds().add(id);
				idNum++;
			}
			
			ContactDetails organizationContact = getContactDetails(custodianOrganization.getTelecoms(),custodianOrganization.getAddrs());
			organization.setContactDetails(organizationContact);
		}
		return organization;
	}

	private Name getNameFromPN(PN firstNameFromList) {
		Name name = new Name();
		if (firstNameFromList != null) {
			name.setFamilyNames(CCDAUtils.getListFromENXP(firstNameFromList.getFamilies()));
			name.setGivenNames(CCDAUtils.getListFromENXP(firstNameFromList.getGivens()));
			name.setNameType(NameType.CURRENT_NAME); // Is this legit?
			name.setPrefixes(CCDAUtils.getListFromENXP(firstNameFromList.getPrefixes()));
			name.setSuffixes(CCDAUtils.getListFromENXP(firstNameFromList.getSuffixes()));
			name.setParsingDetailsId(currentParsingDetails.getParsingDetailsId()); // Does it really make sense to do this? Maybe.
			name.setSourceId(primarySource.getSourceId()); // same argument here.
		}
		return name;
	}

	private Name getFirstNameFromList(EList<PN> names) {
		Name name = new Name();
		if (names != null && names.size() > 0) {
			PN firstNameFromList = names.get(0);
			name = getNameFromPN(firstNameFromList);
		}
		return name;
	}
	
	private Demographics getDemographicsFromRecordTarget(org.eclipse.mdht.uml.cda.Patient ccdPatient) {
		Demographics demographics = new Demographics();
		if (ccdPatient != null)
		{
			demographics.setDateOfBirth(CCDAUtils.getDateTimeFromTS(ccdPatient.getBirthTime()));
			demographics.setEthnicity(CCDAUtils.getClinicalCode(ccdPatient.getEthnicGroupCode()));
			demographics.setGender(CCDAUtils.getGender(ccdPatient.getAdministrativeGenderCode()));
			demographics.getLanguages().addAll(CCDAUtils.getLanguages(ccdPatient.getLanguageCommunications()));
			demographics.setMaritalStatus(CCDAUtils.getMaritalStatus(ccdPatient.getMaritalStatusCode()));
			demographics.setName(getFirstNameFromList(ccdPatient.getNames()));
			demographics.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
			demographics.setRace(CCDAUtils.getClinicalCode(ccdPatient.getRaceCode()));
			demographics.setReligiousAffiliation(CCDAUtils.getCodeValue(ccdPatient.getReligiousAffiliationCode()));
			demographics.setSourceId(primarySource.getSourceId());
			demographics.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
		}
		return demographics;
	}	

	private ClinicalActor getClinicalActorFromPerformer(Performer1 performer) {
		ClinicalActor clinicalActor = new ClinicalActor();
		if (performer != null && performer.getAssignedEntity() != null) {
			clinicalActor = getClinicalActorFromAssignedEntity(performer.getAssignedEntity());
			clinicalActor.setRole(CCDAUtils.getHealthCareProviderRole(performer.getFunctionCode()));
		}
		return clinicalActor;
	}

	private ClinicalActor getClinicalActorFromAssignedEntity(AssignedEntity assignedEntity) {
		ClinicalActor clinicalActor = new ClinicalActor();
		clinicalActor.setClinicalActorId(clinicalActor.getInternalUUID());
		clinicalActor.setSourceId(primarySource.getSourceId());
		clinicalActor.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
		
		if (assignedEntity.getAssignedPerson() != null)
			clinicalActor.setActorGivenName(getPersonName(assignedEntity.getAssignedPerson()));
		else if (assignedEntity.getRepresentedOrganizations() != null && assignedEntity.getRepresentedOrganizations().size() > 0) {
			Name name = new Name();
			name.getGivenNames().add(CCDAUtils.getFirstNameFromONList(assignedEntity.getRepresentedOrganizations().get(0).getNames()));
			clinicalActor.setActorGivenName(name);
		}
			
		Set<ExternalID> performerIds = CCDAUtils.getEidsFromIIList(assignedEntity.getIds());
		int idNum = 0;
		for (ExternalID id : performerIds) {
			if (idNum == 0) {
				clinicalActor.setPrimaryId(id);
				clinicalActor.setOriginalId(id);
			}
			else
				clinicalActor.getAlternateIds().add(id);
			idNum++;				
		}
		clinicalActor.setContactDetails(getContactDetails(assignedEntity.getTelecoms(), assignedEntity.getAddrs()));
		clinicalActor.setAssociatedOrg(getFirstOrganization(assignedEntity.getRepresentedOrganizations()));
		
		return clinicalActor;
	}

	private ClinicalActor getClinicalActorFromAuthor(AssignedAuthor assignedAuthor) {
		ClinicalActor clinicalActor = new ClinicalActor();
		clinicalActor.setClinicalActorId(clinicalActor.getInternalUUID());
		clinicalActor.setSourceId(primarySource.getSourceId());
		clinicalActor.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
		
		if (assignedAuthor.getAssignedPerson() != null)
			clinicalActor.setActorGivenName(getPersonName(assignedAuthor.getAssignedPerson()));
		else if (assignedAuthor.getRepresentedOrganization() != null) {
			Name name = new Name();
			name.getGivenNames().add(CCDAUtils.getFirstNameFromONList(assignedAuthor.getRepresentedOrganization().getNames()));
			clinicalActor.setActorGivenName(name);
		}
			
		Set<ExternalID> performerIds = CCDAUtils.getEidsFromIIList(assignedAuthor.getIds());
		int idNum = 0;
		for (ExternalID id : performerIds) {
			if (idNum == 0) {
				clinicalActor.setPrimaryId(id);
				clinicalActor.setOriginalId(id);
			}
			else
				clinicalActor.getAlternateIds().add(id);
			idNum++;				
		}
		clinicalActor.setContactDetails(getContactDetails(assignedAuthor.getTelecoms(), assignedAuthor.getAddrs()));
		clinicalActor.setAssociatedOrg(getOrganization(assignedAuthor.getRepresentedOrganization()));
		
		return clinicalActor;
	}

	private Name getPersonName(Person assignedPerson) {
		Name name = null;
		if (assignedPerson != null) {
			name = getFirstNameFromList(assignedPerson.getNames());			
		}
		return name;
	}

	private Organization getFirstOrganization(EList<org.eclipse.mdht.uml.cda.Organization> representedOrganizations) {
		Organization firstOrganization = null;
		if (representedOrganizations != null && representedOrganizations.size() > 0) {
			firstOrganization = getOrganization(representedOrganizations.get(0));
		}
		return firstOrganization;
	}

	private Organization getOrganization(org.eclipse.mdht.uml.cda.Organization ccdOrganization) {
		Organization organization = null;
		if (ccdOrganization != null) {
			organization = new Organization();
			organization.setParsingDetailsId(currentParsingDetails.getParsingDetailsId());
			organization.setSourceId(primarySource.getSourceId());
			organization.setName(CCDAUtils.getFirstNameFromONList(ccdOrganization.getNames()));
			Set<ExternalID> custodianOrganizationIds = CCDAUtils.getEidsFromIIList(ccdOrganization.getIds());
			int idNum = 0;
			for (ExternalID id : custodianOrganizationIds) {
				if (idNum == 0) {
					organization.setPrimaryId(id);
					organization.setOriginalId(id);
				}
				else
					organization.getAlternateIds().add(id);
				idNum++;				
			}
		}
		return organization;
	}

}
