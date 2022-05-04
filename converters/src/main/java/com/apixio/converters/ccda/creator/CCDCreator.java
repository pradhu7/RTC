package com.apixio.converters.ccda.creator;
// TODO: This is totally out of date
//import java.io.StringWriter;
//import java.util.ArrayList;
//import java.util.List;
//
//import org.joda.time.DateTime;
//import org.eclipse.mdht.uml.cda.AssignedAuthor;
//import org.eclipse.mdht.uml.cda.AssignedCustodian;
//import org.eclipse.mdht.uml.cda.AssignedEntity;
//import org.eclipse.mdht.uml.cda.Author;
//import org.eclipse.mdht.uml.cda.AuthoringDevice;
//import org.eclipse.mdht.uml.cda.CDAFactory;
//import org.eclipse.mdht.uml.cda.Consumable;
//import org.eclipse.mdht.uml.cda.Custodian;
//import org.eclipse.mdht.uml.cda.CustodianOrganization;
//import org.eclipse.mdht.uml.cda.DocumentationOf;
//import org.eclipse.mdht.uml.cda.InfrastructureRootTypeId;
//import org.eclipse.mdht.uml.cda.ManufacturedProduct;
//import org.eclipse.mdht.uml.cda.Material;
//import org.eclipse.mdht.uml.cda.PatientRole;
//import org.eclipse.mdht.uml.cda.Performer1;
//import org.eclipse.mdht.uml.cda.Person;
//import org.eclipse.mdht.uml.cda.RecordTarget;
//import org.eclipse.mdht.uml.cda.Section;
//import org.eclipse.mdht.uml.cda.ServiceEvent;
//import org.eclipse.mdht.uml.cda.ccd.CCDFactory;
//import org.eclipse.mdht.uml.cda.ccd.ContinuityOfCareDocument;
//import org.eclipse.mdht.uml.cda.ccd.EncountersActivity;
//import org.eclipse.mdht.uml.cda.ccd.EncountersSection;
//import org.eclipse.mdht.uml.cda.ccd.MedicationActivity;
//import org.eclipse.mdht.uml.cda.ccd.MedicationsSection;
//import org.eclipse.mdht.uml.cda.ccd.ProblemAct;
//import org.eclipse.mdht.uml.cda.ccd.ProblemSection;
//import org.eclipse.mdht.uml.cda.ccd.ProcedureActivityProcedure;
//import org.eclipse.mdht.uml.cda.ccd.ProceduresSection;
//import org.eclipse.mdht.uml.cda.ccd.VitalSignsSection;
//import org.eclipse.mdht.uml.cda.util.CDAUtil;
//import org.eclipse.mdht.uml.hl7.datatypes.CE;
//import org.eclipse.mdht.uml.hl7.datatypes.DatatypesFactory;
//import org.eclipse.mdht.uml.hl7.datatypes.EN;
//import org.eclipse.mdht.uml.hl7.vocab.ActClassRoot;
//import org.eclipse.mdht.uml.hl7.vocab.x_ServiceEventPerformer;
//
//import com.apixio.converters.base.Creator;
//import com.apixio.model.patient.BiometricValue;
//import com.apixio.model.patient.ClinicalActor;
//import com.apixio.model.patient.ClinicalCode;
//import com.apixio.model.patient.Demographics;
//import com.apixio.model.patient.Encounter;
//import com.apixio.model.patient.Name;
//import com.apixio.model.patient.Organization;
//import com.apixio.model.patient.Patient;
//import com.apixio.model.patient.Prescription;
//import com.apixio.model.patient.Problem;
//import com.apixio.model.patient.Procedure;
//import com.apixio.model.patient.Source;
//
//public class CCDCreator extends Creator{
//
//	private ContinuityOfCareDocument ccd;
//	private Patient patient;
//	ClinicalActor clinicalActor;
//	Organization organization;
//
//	public CCDCreator(Patient patient)
//	{
//		this.patient = patient;
//
//		// get the organization and actor
//		for (Source source : patient.getSources()) {
//			this.organization = source.getOrganization();
//			this.clinicalActor = patient.getClinicalActorById(source.getClinicalActorId());
//		}
//		addDocumentHeader();
//		addPatientSections();
//	}
//
//	@Override
//	public void create(Patient patient) {
//		// TODO Auto-generated method stub
//
//	}
//
//	public String getString()
//	{
//		StringWriter writer = new StringWriter();
//		try {
//			CDAUtil.save(ccd, writer);
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		return writer.toString();
//	}
//
//	private void addDocumentHeader() {
//
//		ccd = CCDFactory.eINSTANCE.createContinuityOfCareDocument().init();
//		//ccd.setTypeId(getTypeId());
//		ccd.setId(CCDCreatorUtils.getRandomId());
//		ccd.setTitle(CCDCreatorUtils.getString("Continuity of Care Document"));
//		ccd.setEffectiveTime(CCDCreatorUtils.getCurrentTime());
//		ccd.setConfidentialityCode(CCDCreatorUtils.getCE("Normal", "N", "2.16.840.1.113883.5.25", "HL7 Confidentiality Code"));
//		ccd.setLanguageCode(CCDCreatorUtils.createCS("en-US"));
//		ccd.getRecordTargets().add(getRecordTarget());
//		//summary.getAuthors().add(getAuthor());
//		//summary.setCustodian(createCustodian());
//		//summary.getDocumentationOfs().add(getDocumentationOf());
//	}
//
//	public void addPatientSections()
//	{
//		if (patient.getProblems() != null)
//			ccd.addSection(getProblemsSection(patient.getProblems()));
//
//		if (patient.getPrescriptions() != null)
//			ccd.addSection(getMedicationsSection(patient.getPrescriptions()));
//
////		if (patient.getProcedures() != null)
////			ccd.addSection(getProceduresSection(patient.getProcedures()));
//
////		if (patient.getEncounters() != null) // && patient.getEncounters()size() > 0)
////			ccd.addSection(getEncountersSection(patient.getEncounters()));
//
//	}
//
//
//	private RecordTarget getRecordTarget() {
//		RecordTarget recordTarget = CDAFactory.eINSTANCE.createRecordTarget();
//		PatientRole patientRole = CDAFactory.eINSTANCE.createPatientRole();
//		patientRole.getIds().addAll(CCDCreatorUtils.getIdsFromExternalIDs(patient.getExternalIDs()));
//		patientRole.setPatient(getPatient());
//		if (patient.getPrimaryContactDetails() != null) {
//			patientRole.getAddrs().add(CCDCreatorUtils.createAddress(patient.getPrimaryContactDetails().getPrimaryAddress()));
//			patientRole.getTelecoms().add(CCDCreatorUtils.createTelecom(patient.getPrimaryContactDetails().getPrimaryPhone()));
//		}
//		patientRole.setProviderOrganization(CCDCreatorUtils.getOrganization(organization));
//		recordTarget.setPatientRole(patientRole);
//		return recordTarget;
//	}
//
//
//	private DocumentationOf getDocumentationOf() {
//		DocumentationOf documentationOf = CDAFactory.eINSTANCE.createDocumentationOf();
//		documentationOf.setServiceEvent(getServiceEvent());
//		return documentationOf;
//	}
//
//	private ServiceEvent getServiceEvent() {
//		ServiceEvent serviceEvent = CDAFactory.eINSTANCE.createServiceEvent();
//		serviceEvent.setClassCode(ActClassRoot.PCPR);
//		serviceEvent.setEffectiveTime(CCDCreatorUtils.getIVLTS(DateTime.now().minusYears(20), DateTime.now()));
//		serviceEvent.getPerformers().add(getPerformer());
//		return serviceEvent;
//	}
//
//	private Performer1 getPerformer() {
//		Performer1 performer = CDAFactory.eINSTANCE.createPerformer1();
//		performer.setTypeCode(x_ServiceEventPerformer.PRF);
//		performer.setFunctionCode(CCDCreatorUtils.getCE("Consulting Provider", "CP", "2.16.840.1.113883.12.443", "Provider Role"));
//		performer.setTime(CCDCreatorUtils.getIVLTS(DateTime.now().minusYears(20), DateTime.now()));
//		performer.setAssignedEntity(getAssignedEntity());
//		return performer;
//	}
//
//	private AssignedEntity getAssignedEntity() {
//		AssignedEntity assignedEntity = CDAFactory.eINSTANCE.createAssignedEntity();
//		assignedEntity.getIds().add(CCDCreatorUtils.getIdFromExternalID(clinicalActor.getOriginalId())); //createId("2.16.840.1.113883.4.6", clinicalActor.get(), "NPI")); //1234567893, 1111111112
//		assignedEntity.setCode(CCDCreatorUtils.getCE("Generic Physicians", "200000000X", "2.16.840.1.113883.6.101", "ProviderCodes"));
//		assignedEntity.setAssignedPerson(getPersonFromName(clinicalActor.getActorGivenName())); //getPerson(clinicalActor.getFirstName(), clinicalActor.getLastName()));
//		assignedEntity.getAddrs().add(CCDCreatorUtils.createNullAddress());
//		assignedEntity.getTelecoms().add(CCDCreatorUtils.createNullTelecom());
//		assignedEntity.getRepresentedOrganizations().add(createOrganization(organization.getName()));
//		return assignedEntity;
//	}
//
//	private Custodian createCustodian() {
//		Custodian custodian = CDAFactory.eINSTANCE.createCustodian();
//		custodian.setAssignedCustodian(createAssignedCustodian());
//		return custodian;
//	}
//
//	private AssignedCustodian createAssignedCustodian() {
//		AssignedCustodian assignedCustodian = CDAFactory.eINSTANCE.createAssignedCustodian();
//		assignedCustodian.setRepresentedCustodianOrganization(createCustodianOrganization());
//		return assignedCustodian;
//	}
//
//	private CustodianOrganization createCustodianOrganization() {
//		CustodianOrganization custodianOrganization = CDAFactory.eINSTANCE.createCustodianOrganization();
//		custodianOrganization.getIds().add(CCDCreatorUtils.getRandomId());
//		custodianOrganization.setAddr(CCDCreatorUtils.createNullAddress());
//		custodianOrganization.setTelecom(CCDCreatorUtils.createNullTelecom());
//		custodianOrganization.setName(CCDCreatorUtils.createON(organization.getName()));
//		return custodianOrganization;
//	}
//
//	private org.eclipse.mdht.uml.cda.Organization createOrganization(String name) {
//		org.eclipse.mdht.uml.cda.Organization cdaOrganization = CDAFactory.eINSTANCE.createOrganization();
//		cdaOrganization.getIds().add(CCDCreatorUtils.getRandomId());
//		cdaOrganization.getAddrs().add(CCDCreatorUtils.createNullAddress());
//		cdaOrganization.getTelecoms().add(CCDCreatorUtils.createNullTelecom());
//		cdaOrganization.getNames().add(CCDCreatorUtils.createON(name));
//		return cdaOrganization;
//	}
//
//	private Author getAuthor() {
//		Author author = CDAFactory.eINSTANCE.createAuthor();
//		author.setTime(CCDCreatorUtils.getCurrentTime());
//		author.setAssignedAuthor(getAssignedAuthor());
//		return author;
//	}
//
//	private AssignedAuthor getAssignedAuthor() {
//		AssignedAuthor assignedAuthor = CDAFactory.eINSTANCE.createAssignedAuthor();
//		assignedAuthor.getIds().add(CCDCreatorUtils.getRandomId());
//		assignedAuthor.setAssignedPerson(getPersonFromName(clinicalActor.getActorGivenName())); //getPerson(clinicalActor.getFirstName(), clinicalActor.getLastName()));
//		assignedAuthor.setRepresentedOrganization(createOrganization(organization.getName()));
//		//assignedAuthor.setAssignedAuthoringDevice(getAssignedAuthoringDevice());
//		assignedAuthor.getAddrs().add(CCDCreatorUtils.createNullAddress());
//		assignedAuthor.getTelecoms().add(CCDCreatorUtils.createNullTelecom());
//		return assignedAuthor;
//	}
//
//	private Person getPersonFromName(Name actorGivenName) {
//		Person person = CDAFactory.eINSTANCE.createPerson();
//		if (actorGivenName != null) {
//			person.getNames().add(CCDCreatorUtils.getPNFromName(actorGivenName));
//		}
//		return person;
//	}
//
//	private Person getPerson(String firstName, String lastName) {
//		Person person = CDAFactory.eINSTANCE.createPerson();
//		person.getNames().add(CCDCreatorUtils.getName(firstName, lastName));
//		return person;
//	}
//	private Person getPerson(String name) {
//		Person person = CDAFactory.eINSTANCE.createPerson();
//		person.getNames().add(CCDCreatorUtils.getName(name));
//		return person;
//	}
//
//	private AuthoringDevice getAssignedAuthoringDevice() {
//		AuthoringDevice authoringDevice = CDAFactory.eINSTANCE.createAuthoringDevice();
//		authoringDevice.setSoftwareName(CCDCreatorUtils.getSC("Apixio"));
//		authoringDevice.setManufacturerModelName(CCDCreatorUtils.getSC("Apixio"));
//		return authoringDevice;
//	}
//
//	private InfrastructureRootTypeId getTypeId() {
//		InfrastructureRootTypeId typeId = CDAFactory.eINSTANCE.createInfrastructureRootTypeId();
//		typeId.setRoot("2.16.840.1.113883.1.3");
//		typeId.setExtension("POCD_HD000040");
//		return typeId;
//	}
//
//	private Section getProceduresSection(Iterable<Procedure> iterable) {
//		ProceduresSection proceduresSection = CCDFactory.eINSTANCE.createProceduresSection().init();
//		for (Procedure procedure : iterable)
//		{
//			proceduresSection.addProcedure(getProcedure(procedure));
////			if (useData(procedure.getSourceType()))
////			{
////				String nameAndDate = procedure.getCode();
////				if (procedure.getPerformedOn() != null)
////					nameAndDate = nameAndDate + "|" + procedure.getPerformedOn().toString("yyyyMMdd");
////				if (!procedureList.contains(nameAndDate))
////				{
////					procedureList.add(nameAndDate);
////					proceduresSection.addProcedure(getProcedure(procedure));
////				}
////			}
//		}
//		return proceduresSection;
//	}
//
//	private ProcedureActivityProcedure getProcedure(Procedure patientProcedure) {
//		ProcedureActivityProcedure procedure = CCDFactory.eINSTANCE.createProcedureActivityProcedure().init();
////		hitspProcedure.setClassCode(ActClass.PROC);
////		hitspProcedure.setMoodCode(x_DocumentProcedureMood.EVN);
////		hitspProcedure.getTemplateIds().add(createId("2.16.840.1.113883.10.20.1.29"));
////		hitspProcedure.getIds().add(getRandomId());
////		CD procedureCode = getCE(procedure.getCode()); //createCode(procedure.getProcedureName(), procedure.getCode(), procedure.getCodingSystem(), "");
////		procedureCode.setOriginalText(getTextWithReference("#reference"));
////		hitspProcedure.setCode(procedureCode);
////		hitspProcedure.setText(getTextWithReference("#reference")); //getText(procedure.getProcedureName()));
////		hitspProcedure.setEffectiveTime(getIVLTS(procedure.getPerformedOn(), null));
////		hitspProcedure.setStatusCode(createCS("migrationCompleted"));
//		return procedure;
//	}
//
//	private MedicationsSection getMedicationsSection(Iterable<Prescription> iterable) {
//		MedicationsSection medicationsSection = CCDFactory.eINSTANCE.createMedicationsSection().init();
//		List<String> medicationList = new ArrayList<String>();
//		for (Prescription prescription : iterable)
//		{
//			medicationsSection.addSubstanceAdministration(getMedication(prescription));
////			if (useData(medication.getSourceType()))
////			{
////
////				String nameAndDate = medication.getCode();
////				if (medication.getPrescriptionDate() != null)
////					nameAndDate = nameAndDate + "|" + medication.getPrescriptionDate().toString("yyyyMMdd");
////				if (!medicationList.contains(nameAndDate))
////				{
////					medicationList.add(nameAndDate);
////					medicationsSection.addSubstanceAdministration(getMedication(medication));
////				}
////			}
//		}
//		return medicationsSection;
//	}
//
//	private MedicationActivity getMedication(Prescription prescription) {
//		MedicationActivity medication = CCDFactory.eINSTANCE.createMedicationActivity().init();
////		hitspMedication.setMoodCode(x_DocumentSubstanceMood.INT);
////		hitspMedication.getIds().add(getRandomId());
////		hitspMedication.setText(getText(prescription.getSig()));
//////		if ("2.16.840.1.113883.6.88".equalsIgnoreCase(medication.getCodingSystem()) && medication.getPrescriptionDate() != null &&
//////			(this.includeDataSet.equals(DataSet.coded)))
//////			System.out.println("MU compliant med found for " + currentPatientPid);
////		hitspMedication.getEffectiveTimes().add(getIVLTS(prescription.getPrescriptionDate(), null));
////		//hitspMedication.setDoseQuantity(getDose(medication.getDosage()));
//////		String medName = prescription.getAssociatedMedication().getBrandName();
//////		if (StringUtils.isBlank(medName))
//////			medName = prescription.getAssociatedMedication().getGenericName();
////////		if (StringUtils.isBlank(medName))
////////			medName = prescription.getAssociatedMedication().getCode();
//////
//////		if (StringUtils.isBlank(prescription.getBrandName()))
//////			prescription.setBrandName(medName);
//////		if (StringUtils.isBlank(prescription.getGenericName()))
//////			prescription.setGenericName(medName);
////		hitspMedication.setConsumable(getConsumable(prescription.getAssociatedMedication().getBrandName(), prescription.getAssociatedMedication().getGenericName(), prescription.getAssociatedMedication().getCode()));
////
////		//medication.getQuantity();
////		//medication.getStrength();
////		//medication.getUnits();
//		return medication;
//	}
//
//	private Consumable getConsumable(String brandName, String genericName, ClinicalCode code) {
//		Consumable consumable = CDAFactory.eINSTANCE.createConsumable();
//		ManufacturedProduct manufacturedProduct = CDAFactory.eINSTANCE.createManufacturedProduct();
//		manufacturedProduct.setManufacturedMaterial(getManufacturedMaterial(brandName, genericName, code));
//		consumable.setManufacturedProduct(manufacturedProduct);
//		return consumable;
//	}
//
//	private Material getManufacturedMaterial(String brandName, String genericName, ClinicalCode code) {
//		Material material = CDAFactory.eINSTANCE.createMaterial();
//		CE medicationCode = getCE(code);
//		medicationCode.setOriginalText(CCDCreatorUtils.getText(genericName));
//		material.setCode(medicationCode);
//		material.setName(createEN(brandName));
//		return material;
//	}
//
//	private CE getCE(ClinicalCode code) {
//		// Note: add support for NullFlavor
//		CE ce = DatatypesFactory.eINSTANCE.createCE();
//		ce.setCode(code.getCode());
//		ce.setCodeSystem(code.getCodingSystemOID());
//		ce.setCodeSystemName(code.getCodingSystem());
//		ce.setCodeSystemVersion(code.getCodingSystemVersions());
//		ce.setDisplayName(code.getDisplayName());
//		return ce;
//	}
//
//	private EN createEN(String brandName) {
//		EN name = DatatypesFactory.eINSTANCE.createEN();
//		if (brandName != null)
//			name.addText(brandName);
//		return name;
//	}
//
//	private VitalSignsSection getVitalsSection(Iterable<BiometricValue> iterable) {
//		VitalSignsSection vitalsSection = CCDFactory.eINSTANCE.createVitalSignsSection().init();
//		//HITSPFactory.eINSTANCE.createVitalSign();
//		return vitalsSection;
//	}
//
////	private Encounter addApixioEncounter() {
////		Encounter apixioEncounter = new Encounter();
////		apixioEncounter.setEncounterDate(DateTime.now());
////		apixioEncounter.setCode("99201");
////		apixioEncounter.setCodingSystem("2.16.840.1.113883.6.12");
////		apixioEncounter.setEncounterId(UUID.randomUUID());
////		return apixioEncounter;
////	}
//
//	private EncountersSection getEncountersSection(Iterable<Encounter> iterable) {
//		EncountersSection encountersSection = CCDFactory.eINSTANCE.createEncountersSection().init();
//		for (Encounter encounter : iterable)
//		{
//			encountersSection.addEncounter(getEncounter(encounter));
//		}
//		return encountersSection;
//	}
//
//	private EncountersActivity getEncounter(Encounter patientEncounter) {
//		EncountersActivity encounter = CCDFactory.eINSTANCE.createEncountersActivity().init();
////		hitspEncounter.setMoodCode(x_DocumentEncounterMood.EVN);
////		hitspEncounter.getTemplateIds().add(createId("2.16.840.1.113883.10.20.1.21"));
////		hitspEncounter.getIds().add(createId(patientEncounter.getEncounterId().toString()));
////		CD encounterCode = getCE(patientEncounter.getCode()); //createCode("", encounter.getCode(), encounter.getCodingSystem(), "");
////		encounterCode.setOriginalText(getTextWithReference("#reference"));
////		hitspEncounter.setCode(encounterCode);
////		hitspEncounter.setEffectiveTime(getIVLTS(patientEncounter.getEncounterDate()));
////		hitspEncounter.setText(getTextWithReference("#reference"));
//		return encounter;
//	}
//
//	private ProblemSection getProblemsSection(Iterable<Problem> iterable) {
//
//		ProblemSection problemSection = CCDFactory.eINSTANCE.createProblemSection().init();
//		String problemNarrative = "<list>";
//		for (Problem problem : iterable)
//		{
//			problemSection.addAct(getProblemAct(problem, problemNarrative));
//		}
//		problemNarrative += "</list>";
//		problemSection.createStrucDocText(problemNarrative);
//		return problemSection;
//	}
//
//	private ProblemAct getProblemAct(Problem problem, String problemNarrative) {
//
//		ProblemAct problemAct = CCDFactory.eINSTANCE.createProblemAct().init();
//		String problemInstanceNarrative = "<item>" + problem.getProblemName() + "</item>";
//		//ProblemAct condition = CCDFactory.eINSTANCE.createProblemAct().init();
//		problemAct.getIds().add(CCDCreatorUtils.getRandomId());
//		problemAct.setStatusCode(CCDCreatorUtils.createCS("active"));
////		ConditionEntry conditionEntry = condition.createConditionEntry().init();
////		EntryRelationship entryRelationship = (EntryRelationship) conditionEntry.eContainer();
////		entryRelationship.setInversionInd(false);
////		//condition.
////		//ProblemObservation conditionEntry = CCDFactory.eINSTANCE.createProblemObservation().init();
////		DateTime problemStart = problem.getStartDate();
////		if (problemStart == null)
////			problemStart = minDate;
////		condition.setEffectiveTime(getIVLTS(problemStart, problem.getEndDate()));
////		//ProblemObservation problemObservation = CCDFactory.eINSTANCE.createProblemObservation().init();
////
////		conditionEntry.getIds().add(getRandomId());
////		conditionEntry.setCode(createCode("Problem", "55607006", "2.16.840.1.113883.6.96", "SNOMED CT"));
////
////		//conditionEntry.setText(getText(problem.getProblemName()));
////		conditionEntry.setText(getTextWithReference("#reference"));
////		//conditionEntry.setStatusCode(createCS("migrationCompleted"));
////		//conditionEntry
////		conditionEntry.setEffectiveTime(getIVLTS(problemStart, problem.getEndDate()));
////		conditionEntry.getValues().add(getCE(problem.getCode())); //createCD(problem.getProblemName(),problem.getCode(), "2.16.840.1.113883.6.103", "ICD9CM")); //"2.16.840.1.113883.6.96", "SNOMED CT"));
//		problemNarrative += problemInstanceNarrative;
//		return problemAct;
//	}
//
//	private org.eclipse.mdht.uml.cda.Patient getPatient() {
//		org.eclipse.mdht.uml.cda.Patient ccdPatient = CDAFactory.eINSTANCE.createPatient();
//		Demographics patientDemographics = patient.getPrimaryDemographics();
//		if (patientDemographics != null) {
//			ccdPatient.getNames().add(CCDCreatorUtils.getPNFromName(patientDemographics.getName()));
//			ccdPatient.setAdministrativeGenderCode(CCDCreatorUtils.getGender(patientDemographics.getGender()));
//			ccdPatient.setBirthTime(CCDCreatorUtils.getTS(patientDemographics.getDateOfBirth()));
//			ccdPatient.setMaritalStatusCode(CCDCreatorUtils.getMaritalStatus(patientDemographics.getMaritalStatus()));
//		}
//		return ccdPatient;
//	}
//}
