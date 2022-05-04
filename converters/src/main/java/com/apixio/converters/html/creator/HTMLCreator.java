package com.apixio.converters.html.creator;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.apixio.converters.base.Creator;
import com.apixio.model.patient.Address;
import com.apixio.model.patient.Administration;
import com.apixio.model.patient.Allergy;
import com.apixio.model.patient.BiometricValue;
import com.apixio.model.patient.CareSite;
import com.apixio.model.patient.ClinicalActor;
import com.apixio.model.patient.ClinicalCode;
import com.apixio.model.patient.ContactDetails;
import com.apixio.model.patient.Demographics;
import com.apixio.model.patient.Document;
import com.apixio.model.patient.Encounter;
import com.apixio.model.patient.ExternalID;
import com.apixio.model.patient.Gender;
import com.apixio.model.patient.LabResult;
import com.apixio.model.patient.Medication;
import com.apixio.model.patient.Name;
import com.apixio.model.patient.Patient;
import com.apixio.model.patient.Prescription;
import com.apixio.model.patient.Problem;
import com.apixio.model.patient.Procedure;
import com.apixio.model.patient.SocialHistory;
import com.apixio.model.patient.Source;
import com.apixio.model.patient.TelephoneNumber;
import com.apixio.model.patient.TypedDate;

public class HTMLCreator extends Creator {

	private String htmlString = "";
	private DateTimeFormatter dateFormat = DateTimeFormat.forPattern("MM/dd/yyyy");
	private DateTimeFormatter fullDateFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ");
	
	@Override
	public void create(Patient patient){
		try{
			htmlString = getPatientHtmlString(patient);		
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}

	private String getPatientHtmlString(Patient patient) throws IOException {
		String patientHtmlString = startHtmlString("Patient Object");
		patientHtmlString += getDocumentTitle("Patient Object");
		patientHtmlString += getSourceString(patient);
		patientHtmlString += getPatientDemographicsString(patient);
		//patientHtmlString += getPatientContactDetails(patient);
		patientHtmlString += getMedicationsString(patient);
		patientHtmlString += getAllergiesString(patient);
		patientHtmlString += getProblemsString(patient);
		patientHtmlString += getProceduresString(patient);
		patientHtmlString += getImmunizationsString(patient);
		patientHtmlString += getLabsString(patient);
		patientHtmlString += getVitalsString(patient);
		patientHtmlString += getSocialHistoryString(patient);
		patientHtmlString += getEncountersString(patient);
		patientHtmlString += getCareSitesString(patient);
		patientHtmlString += getClinicalActorsString(patient);
		patientHtmlString += getExistingDocuments(patient);
		patientHtmlString += finishHtmlString();
		return patientHtmlString;
	}

	private String getExistingDocuments(Patient patient) throws IOException {
		String existingDocuments = "";
		if (patient.getDocuments() != null) {
			for (Document document : patient.getDocuments()) {
				existingDocuments = startSectionString("DOCUMENT", documentsSections);
				existingDocuments += getPatientDocumentsRow(document);
				existingDocuments += "<div border=1>" + document.getStringContent() + "</div>";
				existingDocuments += endSectionString();
			}
		}
		return existingDocuments;
	}

	private String getPatientDemographicsString(Patient patient) {
		String patientDemographicsString = startSectionString("DEMOGRAPHICS", patientSections);
		patientDemographicsString += getPatientDemographicsRow(patient.getExternalIDs(), patient.getPrimaryDemographics(), patient.getPrimaryContactDetails());
		for (Demographics alternateDemographics : patient.getAlternateDemographics()) {
			patientDemographicsString += getPatientDemographicsRow(null, alternateDemographics, null);
		}
		patientDemographicsString += endSectionString();
		return patientDemographicsString;
	}

	private String getSourceString(Patient patient) {
		String patientSourcesString = startSectionString("SOURCES", sourceSections);
		for (Source source : patient.getSources()) {
			patientSourcesString += getSourceRow(source);
		}
		patientSourcesString += endSectionString();
		return patientSourcesString;
	}

	private String getAllergiesString(Patient patient) {
		String allergiesString = startSectionString("ALLERGIES", allergiesSections);
		for (Allergy allergy : patient.getAllergies()) {
			allergiesString += getAllergyRow(allergy);
		}
		allergiesString += endSectionString();
		return allergiesString;
	}

	private String getImmunizationsString(Patient patient) {
		String immunizationsString = startSectionString("IMMUNIZATIONS", immunizationsSections);
		for (Administration administration : patient.getAdministrations()) {
			immunizationsString += getImmunizationRow(administration);
		}
		immunizationsString += endSectionString();
		return immunizationsString;
	}

	private String getMedicationsString(Patient patient) {
		String medicationsString = startSectionString("MEDICATIONS", medicationsSections);
		for (Prescription prescription : patient.getPrescriptions()) {
			medicationsString += getMedicationRow(prescription);
		}
		medicationsString += endSectionString();
		return medicationsString;
	}

	private String getProblemsString(Patient patient) {
		String problemsString = startSectionString("PROBLEMS", problemsSections);
		for (Problem problem : patient.getProblems()) {
			problemsString += getProblemRow(problem);
		}
		problemsString += endSectionString();
		return problemsString;
	}

	private String getProceduresString(Patient patient) {
		String proceduresString = startSectionString("PROCEDURES", proceduresSections);
		for (Procedure procedure : patient.getProcedures()) {
			proceduresString += getProcedureRow(procedure);
		}
		proceduresString += endSectionString();
		return proceduresString;
	}

	private String getLabsString(Patient patient) {
		String labsString = startSectionString("LAB RESULTS", labsSections);
		for (LabResult lab : patient.getLabs()) {
			labsString += getLabsRow(lab);
		}
		labsString += endSectionString();
		return labsString;
	}

	private String getVitalsString(Patient patient) {
		String vitalsString = startSectionString("VITALS", vitalsSections);
		for (BiometricValue biometricValue : patient.getBiometricValues()) {
			vitalsString += getBiometricValueRow(biometricValue);
		}
		vitalsString += endSectionString();
		return vitalsString;
	}

	private String getSocialHistoryString(Patient patient) {
		String socialHistoryString = startSectionString("SOCIAL HISTORY", socialHistorySections);
		for (SocialHistory socialHistory : patient.getSocialHistories()) {
			socialHistoryString += getSocialHistoryRow(socialHistory);
		}
		socialHistoryString += endSectionString();
		return socialHistoryString;
	}

	private String getEncountersString(Patient patient) {
		String encountersString = startSectionString("Encounters", encountersSections);
		for (Encounter encounter : patient.getEncounters()) {
			encountersString += getEncountersRow(encounter);
		}
		encountersString += endSectionString();
		return encountersString;
	}

	private String getCareSitesString(Patient patient) {
		String careSitesString = startSectionString("Care Sites", careSitesSections);
		for (CareSite careSite : patient.getCareSites()) {
			careSitesString += getCareSitesRow(careSite);
		}
		careSitesString += endSectionString();
		return careSitesString;
	}
	private String getClinicalActorsString(Patient patient) {
		String clinicalActorsString = startSectionString("Clinical Actors", clinicalActorsSections);
		for (ClinicalActor clinicalActor : patient.getClinicalActors()) {
			clinicalActorsString += getClinicalActorsRow(clinicalActor);
		}
		clinicalActorsString += endSectionString();
		return clinicalActorsString;
	}
	String[] allergiesSections = new String[] { "Allergen",
												"Code",
												"Diagnosis Date",
												"Resolved Date",
												"Reaction",
												"Severity",
												"PrimaryClinicalActor",
												"Encounter ID",
												"Metadata",
												"IDs"};
	
	private String getAllergyRow(Allergy allergy) {
		String[] sourceData = new String[allergiesSections.length];
		if (allergy != null) {
			sourceData[0] = allergy.getAllergen();	
			sourceData[1] = getFormattedCode(allergy.getCode());
			sourceData[2] = getDate(allergy.getDiagnosisDate());
			sourceData[3] = getDate(allergy.getResolvedDate());
			sourceData[4] = getFormattedCode(allergy.getReaction());
			sourceData[5] = allergy.getReactionSeverity();
			sourceData[6] = getFormattedUUID(allergy.getPrimaryClinicalActorId());
			sourceData[7] = getFormattedUUID(allergy.getSourceEncounter());
			sourceData[8] = getFormattedMetadata(allergy.getMetadata());
			sourceData[9] = getFormattedIds(allergy.getOriginalId(), allergy.getOtherOriginalIds());
		}
		return getDataRow(sourceData);
	}

	String[] immunizationsSections = new String[] { "Generic Name (Brand Name)",
												 	"Code",
	   											 	"Admin Date",
	   											 	"Start Date",
	   											 	"Route",
													"PrimaryClinicalActor",
													"Encounter ID",
													"Metadata",
													"IDs"};

	private String getImmunizationRow(Administration administration) {
		String[] sourceData = new String[immunizationsSections.length];
		if (administration != null) {
			if (administration.getMedication() != null) {
				Medication medication = administration.getMedication();
				sourceData[0] = medication.getGenericName() + "(" + medication.getBrandName() + ")";				
				sourceData[1] = getFormattedCode(medication.getCode());
				sourceData[2] = getDate(administration.getAdminDate());
				sourceData[3] = getDate(administration.getStartDate());
				sourceData[4] = medication.getRouteOfAdministration();				
				sourceData[5] = getFormattedUUID(administration.getPrimaryClinicalActorId());
				sourceData[6] = getFormattedUUID(administration.getSourceEncounter());
				sourceData[7] = getFormattedMetadata(administration.getMetadata());
				sourceData[8] = getFormattedIds(administration.getOriginalId(), administration.getOtherOriginalIds());
			}			
		}
		return getDataRow(sourceData);
	}

	
	String[] medicationsSections = new String[] { "Brand Name", 
												  "Generic Name", 
												  "Active", 
												  "Directions", 
												  "Date (Start, end)",
												  "Dose",
												  "Form, Strength, Units",
												  "quantity",
												  "frequency",
												  "route",
												  "refills",
												  "code",
												"PrimaryClinicalActor",
												"Encounter ID",
												"Metadata",
												"IDs"};

	private String getMedicationRow(Prescription prescription) {
		String[] sourceData = new String[medicationsSections.length];
		if (prescription != null) {
			if (prescription.getAssociatedMedication() != null) {
				Medication medication = prescription.getAssociatedMedication();
				sourceData[0] = medication.getBrandName();
				sourceData[1] = medication.getGenericName();
				sourceData[2] = getObjectStringValue(prescription.isActivePrescription());
				sourceData[3] = prescription.getSig();
				sourceData[4] = getDate(prescription.getPrescriptionDate()) + "," + getDate(prescription.getEndDate());
				sourceData[5] = prescription.getDosage();
				sourceData[6] = prescription.getAssociatedMedication().getForm() + "," + prescription.getAssociatedMedication().getStrength() + "," + prescription.getAssociatedMedication().getUnits();
				sourceData[7] = getObjectStringValue(prescription.getQuantity());
				sourceData[8] = prescription.getFrequency();
				sourceData[9] = prescription.getAssociatedMedication().getRouteOfAdministration();
				sourceData[10] = getObjectStringValue(prescription.getRefillsRemaining());
				sourceData[11] = getFormattedCode(medication.getCode()) + " (" + getFormattedCodes(medication.getCodeTranslations()) + ")";
				sourceData[12] = getFormattedUUID(prescription.getPrimaryClinicalActorId());
				sourceData[13] = getFormattedUUID(prescription.getSourceEncounter());
				sourceData[14] = getFormattedMetadata(prescription.getMetadata());
				sourceData[15] = getFormattedIds(prescription.getOriginalId(), prescription.getOtherOriginalIds());
			}
			
		}
		return getDataRow(sourceData);
	}
	
	String[] patientSections = new String[] { "Patient Name", 
											  "DOB", 
											  "Gender", 
											  "Languages",
											  "Marital Status",
											  "Race",
											  "Address",
											  "Phone",
											  "IDs"};
	
	private String getPatientDemographicsRow(Iterable<ExternalID> iterable, Demographics demographics, ContactDetails contactDetails) {
		String[] patientDemographics = new String[patientSections.length];
		if (demographics != null) {
			patientDemographics[0] = getFormattedName(demographics.getName());
			patientDemographics[1] = getDate(demographics.getDateOfBirth());
			patientDemographics[2] = (demographics.getGender()==null?Gender.UNKNOWN.toString():demographics.getGender().toString());
			patientDemographics[3] = getListAsString(demographics.getLanguages());
			patientDemographics[4] = String.valueOf(demographics.getMaritalStatus());
			patientDemographics[5] = getFormattedCode(demographics.getRace());			
			if (contactDetails != null) {
				patientDemographics[6] = getFormattedAddress(contactDetails.getPrimaryAddress());
				patientDemographics[7] = getFormattedPhone(contactDetails.getPrimaryPhone());
			}
			patientDemographics[8] = getFormattedIds(iterable);
		}
		return getDataRow(patientDemographics);
	}


	private String getFormattedIds(Iterable<ExternalID> iterable) {
		String formattedIds = "";
		for (ExternalID id : iterable) {
			formattedIds += "ExternalID=" + getFormattedID(id) + ";";
		}
		return formattedIds;
	}
	String[] problemsSections = new String[] { "Name",
											   "Code (translations)",
											   "Dates (Start, end, diagnosis)",
											   "Resolution Status",
												"PrimaryClinicalActor",
												"Encounter ID",
												"Metadata",
												"IDs"};

	private String getProblemRow(Problem problem) {
		String[] sourceData = new String[problemsSections.length];
		if (problem != null) {
			sourceData[0] = problem.getProblemName();
			sourceData[1] = getFormattedCode(problem.getCode()) + " (" + getFormattedCodes(problem.getCodeTranslations()) + ")";
			sourceData[2] = getDate(problem.getStartDate()) + ", " + getDate(problem.getEndDate()) + ", " + getDate(problem.getDiagnosisDate());
			sourceData[3] = getObjectStringValue(problem.getResolutionStatus());
			sourceData[4] = getFormattedUUID(problem.getPrimaryClinicalActorId());
			sourceData[5] = getFormattedUUID(problem.getSourceEncounter());
			sourceData[6] = getFormattedMetadata(problem.getMetadata());
			sourceData[7] = getFormattedIds(problem.getOriginalId(), problem.getOtherOriginalIds());
		}
		return getDataRow(sourceData);
	}
	
	private String getObjectStringValue(Object object) {
		String objectValue = "null";
		if (object != null)
			objectValue = object.toString();
		return objectValue;
	}

	String[] proceduresSections = new String[] { "Name",
												 "Code",
	   											 "Dates (Start and End)",
	   											 "Supporting Diagnosis",
													"PrimaryClinicalActor",
													"Encounter ID",
													"Metadata",
													"IDs"};
	
	private String getProcedureRow(Procedure procedure) {
		String[] sourceData = new String[proceduresSections.length];
		if (procedure != null) {
			sourceData[0] = procedure.getProcedureName();
			sourceData[1] = getFormattedCode(procedure.getCode());
			sourceData[2] = getDate(procedure.getPerformedOn()) + "," + getDate(procedure.getEndDate());
			sourceData[3] = getFormattedCodes(procedure.getSupportingDiagnosis());
			sourceData[4] = getFormattedUUID(procedure.getPrimaryClinicalActorId());
			sourceData[5] = getFormattedUUID(procedure.getSourceEncounter());
			sourceData[6] = getFormattedMetadata(procedure.getMetadata());
			sourceData[7] = getFormattedIds(procedure.getOriginalId(), procedure.getOtherOriginalIds());
		}
		return getDataRow(sourceData);
	}

	String[] labsSections = new String[] { "Lab Panel",
										   "Test Name",
										   "Seq #",
	   									   "Result Value (Units)",
	   									   "Lab Note",
	   									   "Flag",
	   									   "Reference Range",
	   									   "Sample Date",
	   									   "Other Dates",
	   									   "Test Code",
											"PrimaryClinicalActor",
											"Encounter ID",
											"Metadata",
											"IDs"};
	private String getLabsRow(LabResult lab) {
		String[] sourceData = new String[labsSections.length];
		if (lab != null) {
			sourceData[0] = getFormattedCode(lab.getPanel());
			sourceData[1] = lab.getLabName();
			sourceData[2] = getObjectStringValue(lab.getSequenceNumber());
			sourceData[3] = getObjectStringValue(lab.getValue()) + "(" + lab.getUnits() + ")";
			sourceData[4] = lab.getLabNote();
			sourceData[5] = getObjectStringValue(lab.getFlag());
			sourceData[6] = lab.getRange();
			sourceData[7] = getDate(lab.getSampleDate());
			sourceData[8] = getOtherDates(lab.getOtherDates());
			sourceData[9] = getFormattedCode(lab.getCode());
			sourceData[10] = getFormattedUUID(lab.getPrimaryClinicalActorId());
			sourceData[11] = getFormattedUUID(lab.getSourceEncounter());
			sourceData[12] = getFormattedMetadata(lab.getMetadata());
			sourceData[13] = getFormattedIds(lab.getOriginalId(), lab.getOtherOriginalIds());
		}
		return getDataRow(sourceData);
	}
	
	String[] vitalsSections = new String[] {"Date", 
											"Name",
										   	"Value",
										   	"Units",
										   	"Code",
											"PrimaryClinicalActor",
											"Encounter ID",
											"Metadata",
											"IDs"};
	
	private String getBiometricValueRow(BiometricValue biometricValue) {
		String[] sourceData = new String[vitalsSections.length];
		if (biometricValue != null) {
			sourceData[0] = getDate(biometricValue.getResultDate(), fullDateFormat);
			sourceData[1] = biometricValue.getName();
			sourceData[2] = getObjectStringValue(biometricValue.getValue());
			sourceData[3] = biometricValue.getUnitsOfMeasure();
			sourceData[4] = getFormattedCode(biometricValue.getCode());
			sourceData[5] = getFormattedUUID(biometricValue.getPrimaryClinicalActorId());
			sourceData[6] = getFormattedUUID(biometricValue.getSourceEncounter());
			sourceData[7] = getFormattedMetadata(biometricValue.getMetadata());
			sourceData[8] = getFormattedIds(biometricValue.getOriginalId(), biometricValue.getOtherOriginalIds());
		}
		return getDataRow(sourceData);
	}
	String[] socialHistorySections = new String[] { "Date",
											   	 	"Type",
											   	 	"Field Name",
											   	 	"Value",
													"PrimaryClinicalActor",
													"Encounter ID",
													"Metadata",
													"IDs"};
	
	private String getSocialHistoryRow(SocialHistory socialHistory) {
		String[] sourceData = new String[socialHistorySections.length];
		if (socialHistory != null) {
			sourceData[0] = getDate(socialHistory.getDate());
			sourceData[1] = getFormattedCode(socialHistory.getType());
			sourceData[2] = socialHistory.getFieldName();
			sourceData[3] = socialHistory.getValue();
			sourceData[4] = getFormattedUUID(socialHistory.getPrimaryClinicalActorId());
			sourceData[5] = getFormattedUUID(socialHistory.getSourceEncounter());
			sourceData[6] = getFormattedMetadata(socialHistory.getMetadata());
			sourceData[7] = getFormattedIds(socialHistory.getOriginalId(), socialHistory.getOtherOriginalIds());
		}
		return getDataRow(sourceData);
	}
	
	String[] encountersSections = new String[] { "Encounter Date",
								   	 		  	 "Chief Complaints",
								   	 		  	 "Location",
												 "PrimaryClinicalActor",
												 "SupplementaryClinicalActors",
												 "Metadata",
											  	 "Original IDs",
												 "Encounter ID",};
	
	private String getEncountersRow(Encounter encounter) {
		String[] sourceData = new String[encountersSections.length];
		if (encounter != null) {
			sourceData[0] = getDate(encounter.getEncounterDate());
			sourceData[1] = getFormattedCodes(encounter.getChiefComplaints());		
			sourceData[2] = getFormattedCareSite(encounter.getSiteOfService());
			sourceData[3] = getFormattedUUID(encounter.getPrimaryClinicalActorId());
			sourceData[4] = getFormattedUUIDs(encounter.getSupplementaryClinicalActorIds());
			sourceData[5] = getFormattedMetadata(encounter.getMetadata());
			sourceData[6] = getFormattedIds(encounter.getOriginalId(), encounter.getOtherOriginalIds());
			sourceData[7] = getFormattedUUID(encounter.getEncounterId());
		
		}
		return getDataRow(sourceData);
	}
	
	String[] careSitesSections = new String[] { "Name",
							   	 		  	    "Type",
							   	 		  	    "Address",
							   	 		  	    "Care Site Id"};
	
	private String getCareSitesRow(CareSite careSite) {
		String[] sourceData = new String[careSitesSections.length];
		if (careSite != null) {
			sourceData[0] = careSite.getCareSiteName();
			sourceData[1] = getObjectStringValue(careSite.getCareSiteType());
			sourceData[2] = getFormattedAddress(careSite.getAddress());
			sourceData[3] = getFormattedUUID(careSite.getCareSiteId());
		}
		return getDataRow(sourceData);
	}

	String[] clinicalActorsSections = new String[] { "Given Name",
							   	 		  	 		 "IDs",
							   	 		  	 		 "Address",
							   	 		  	 		 "Primary Phone",
							   	 		  	 		 "Email",
							   	 		  	 		 "Clinical Actor ID",
							   	 		  	 		 "Clinical Actor Role"};
	
	private String getClinicalActorsRow(ClinicalActor clinicalActor) {
		String[] sourceData = new String[clinicalActorsSections.length];
		if (clinicalActor != null) {
			sourceData[0] = getFormattedName(clinicalActor.getActorGivenName());
			sourceData[1] = getFormattedIds(clinicalActor.getPrimaryId(), clinicalActor.getAlternateIds());
			if (clinicalActor.getContactDetails() != null) {
				sourceData[2] = getFormattedAddress(clinicalActor.getContactDetails().getPrimaryAddress());
				sourceData[3] = getFormattedPhone(clinicalActor.getContactDetails().getPrimaryPhone());
				sourceData[4] = clinicalActor.getContactDetails().getPrimaryEmail();
			}
			sourceData[5] = getFormattedUUID(clinicalActor.getClinicalActorId());
			sourceData[6] = clinicalActor.getRole().toString();
		}
		return getDataRow(sourceData);
	}
	
	String[] sourceSections = new String[] { "Creation Date",
											 "Source System",
											 "Organization",
											 "Organization ID",
											 "Address",
											 "Phone",
											 "Original ID"};

	private String getSourceRow(Source source) {
		String[] sourceData = new String[sourceSections.length];
		if (source != null) {
			sourceData[0] = getDate(source.getCreationDate());
			sourceData[1] = source.getSourceSystem();
			if (source.getOrganization() != null) {
				sourceData[2] = source.getOrganization().getName();
				sourceData[3] = getFormattedID(source.getOrganization().getPrimaryId());
				if (source.getOrganization().getContactDetails() != null) {
					sourceData[4] = getFormattedAddress(source.getOrganization().getContactDetails().getPrimaryAddress());
					sourceData[5] = getFormattedPhone(source.getOrganization().getContactDetails().getPrimaryPhone());
				}
			}
			sourceData[6] = getFormattedID(source.getOriginalId());
		}
		return getDataRow(sourceData);
	}

	String[] documentsSections = new String[] { "Creation Date",
												"Title",
												"ID"};

	private String getPatientDocumentsRow(Document document) {
		String[] sourceData = new String[documentsSections.length];
		if (document != null) {
			sourceData[0] = getDate(document.getDocumentDate());
			sourceData[1] = document.getDocumentTitle();
			sourceData[2] = getFormattedID(document.getOriginalId());
		}
		return getDataRow(sourceData);
	}

	private String getDataRow(String[] data) {
		String dataRow = "<TR>";
		for (int i = 0; i < data.length; i++) {
			
			dataRow += "<TD>" + data[i] + "</TD>";
		}
		dataRow += "</TR>";
		return dataRow;
	}
	
	private String startHtmlString(String title) {
		String htmlStartString = "<HTML><HEAD><TITLE>" + title + "</TITLE></HEAD><BODY>";
		return htmlStartString;
	}

	private String finishHtmlString() {
		String htmlFinishString = "</BODY></HTML>";
		return htmlFinishString;
	}

	private String getFormattedID(ExternalID primaryId) {
		String formattedId = null;
		if (primaryId != null) {
			formattedId = primaryId.getId() + "^" + primaryId.getSource() + "^" + primaryId.getAssignAuthority();
		}
		return formattedId;
	}

	private String getFormattedCodes(List<ClinicalCode> codes) {
		String formattedCodes = "";
		if (codes != null) {
			for (ClinicalCode code : codes) {
				formattedCodes += getFormattedCode(code) + "|";
			}
			
		}
		return formattedCodes;
	}

	private String getFormattedCode(ClinicalCode code) {
		String formattedCode = null;
		if (code != null) {
			formattedCode = code.getDisplayName() + "^" + code.getCode() + "^" + code.getCodingSystemOID() + "^" + code.getCodingSystemVersions() + " (" + code.getCodingSystem() + ")";
		}
		return formattedCode;
	}

	private String getFormattedUUID(UUID sourceEncounter) {
		String formattedUUID = null;
		if (sourceEncounter != null)
			formattedUUID = sourceEncounter.toString();
		return formattedUUID;
	}

	private String getFormattedUUIDs(List<UUID> sourceEncounters) {
		String formattedUUIDs = "";
		if (sourceEncounters != null)
		{
			for (UUID uuid : sourceEncounters) {
				formattedUUIDs += getFormattedUUID(uuid) + "|";
			}
		}
		return formattedUUIDs;
	}
	
	private String getFormattedCareSite(CareSite siteOfService) {
		String formattedCareSite = "";
		if (siteOfService != null) {
			formattedCareSite += "Name: " + siteOfService.getCareSiteName();
			formattedCareSite += "\nAddress: " + getFormattedAddress(siteOfService.getAddress());
		}
 		return formattedCareSite;
	}

	private String getFormattedMetadata(Map<String, String> metadata) {
		String metadataString = null;
		if (metadata != null)
		{			
			for (String key : metadata.keySet())
			{
				String metadataValue = key + "=" + metadata.get(key) + "|";
				if (metadataString == null)
					metadataString = metadataValue;
				else
					metadataString += metadataValue;
			}
		}
		return metadataString;
	}

	private String getFormattedIds(ExternalID originalId, List<ExternalID> otherOriginalIds) {
		String formattedIds = "";
		if (originalId != null)
		{
			formattedIds = "OriginalID=" + getFormattedID(originalId) + ";";
		}
		if (otherOriginalIds != null) {
			for (ExternalID id : otherOriginalIds)
			{
				formattedIds += "OtherOriginalID=" + getFormattedID(id) + ";";
			}
		}
		return formattedIds;
	}
	
	private String getDate(DateTime dateTime) {
		return getDate(dateTime, dateFormat);
	}

	private String getOtherDates(List<TypedDate> typedDates) {
		String otherDates = "";
		if (typedDates != null) {
			for (TypedDate typedDate : typedDates) {
				otherDates += getObjectStringValue(typedDate.getType()) + "=" + getDate(typedDate.getDate()) + "|";
			}
		}		
		return otherDates;
	}
	private String getDate(DateTime dateTime, DateTimeFormatter dateFormat2) {
		String date = "null";
		if (dateTime != null) {
			date = dateTime.toString(dateFormat2);
		}
		return date;
	}

	private String getFormattedName(Name name) {
		String formattedName = "";
		if (name != null) {
			formattedName += getListAsString(name.getPrefixes());
			for (String givenName : name.getGivenNames()) {
				formattedName += " " + givenName;
			}
			formattedName.trim();
			for (String familyName : name.getFamilyNames()) {
				formattedName += " " + familyName;
			}
			formattedName.trim();
			formattedName += getListAsString(name.getSuffixes());
		}
		return formattedName;
	}

	private String getListAsString(List<String> stringList) {
		String listAsString = "";
		if (stringList != null) {
			for (String string : stringList) {
				if (listAsString.equals(""))
					listAsString = string;
				else
					listAsString += "," + string;
			}
		}
		return listAsString;
	}

	private String startSectionString(String sectionTitle, String[] columns) {
		String startSectionString = getSectionTitle(sectionTitle);
		startSectionString += "<TABLE BORDER=1>";
		startSectionString += getSectionHeaderRow(columns);
		return startSectionString;
	}

	private String getSectionHeaderRow(String[] columns) {
		String sectionHeaderRow = "<TR>";
		for (int i = 0; i < columns.length; i++) {
			sectionHeaderRow += "<TH>" + columns[i] + "</TH>";
		}
		sectionHeaderRow += "</TR>";
		return sectionHeaderRow;
	}
	
	private String getDocumentTitle(String documentTitle) {	
		return "<H1>" + documentTitle + "</H1>";
	}

	private String getSectionTitle(String sectionTitle) {		
		return "<BR/><H2>" + sectionTitle + "</H2>";
	}

	private String endSectionString() {
		String endSectionString = "</TABLE>";
		return endSectionString;
	}

	@Override
	public String getString() {
		// TODO Auto-generated method stub
		return htmlString;
	}
	
	private String getPatientContactDetails(Patient patient) {
		String patientContacts = startSectionString("Primary Contact Details", contactSections);
		patientContacts += getPatientContactRow(patient.getPrimaryContactDetails());
		
		patientContacts += endSectionString();
		return patientContacts;
	}

	private String getPatientContactRow(ContactDetails contacts) {
		String[] patientContacts = new String[contactSections.length];
		if (contacts != null) {
			patientContacts[0] = getFormattedAddress(contacts.getPrimaryAddress());
			
			String alternateAdd = "";
			for (Address add : contacts.getAlternateAddresses()) {				
				alternateAdd += " " +getFormattedAddress(add);
			}
			
			patientContacts[1] = alternateAdd.trim();
			patientContacts[2] = contacts.getPrimaryEmail();
			patientContacts[3] = getListAsString(contacts.getAlternateEmails());
			patientContacts[4] = getFormattedPhone(contacts.getPrimaryPhone());
			
			String alternatePhone = "";
			for (TelephoneNumber ph : contacts.getAlternatePhones()) {				
				alternateAdd += getFormattedPhone(ph);
			}
			
			patientContacts[5] = alternateAdd.trim();
		}
		return getDataRow(patientContacts);
	}
	
	private String getFormattedAddress(Address add) {
		String formattedAdd = "";
		if (add != null) {
			formattedAdd += getListAsString(add.getStreetAddresses());
			formattedAdd += " " + add.getCity();
			formattedAdd += " " + add.getState();
			formattedAdd += " " + add.getZip();
			formattedAdd += " " + add.getCounty();
			formattedAdd += " " + add.getCountry();
			
			formattedAdd.trim();
			
		}
		return formattedAdd;
	}
	
	private String getFormattedPhone(TelephoneNumber phone) {
		String formattedphone = "";
		if (phone != null) {
			formattedphone += phone.getPhoneNumber();
			formattedphone += " " + phone.getPhoneType();
			
			formattedphone.trim();
			
		}
		return formattedphone;
	}
	
	String[] contactSections = new String[] { "Primary Address", 
			  "Alternate Address", 
			  "Primary Email", 
			  "Alternate Email",
			  "Primary Phone",
			  "Alternate Phone"};
	
}
