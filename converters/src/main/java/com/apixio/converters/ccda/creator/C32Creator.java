package com.apixio.converters.ccda.creator;
// TODO: This is totally out of date
//import java.io.FileOutputStream;
//import java.io.StringWriter;
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Set;
//import java.util.UUID;
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
//import org.eclipse.mdht.uml.cda.EntryRelationship;
//import org.eclipse.mdht.uml.cda.InfrastructureRootTypeId;
//import org.eclipse.mdht.uml.cda.Material;
//import org.eclipse.mdht.uml.cda.PatientRole;
//import org.eclipse.mdht.uml.cda.Person;
//import org.eclipse.mdht.uml.cda.RecordTarget;
//import org.eclipse.mdht.uml.cda.Section;
//import org.eclipse.mdht.uml.cda.ServiceEvent;
//import org.eclipse.mdht.uml.cda.ccd.CCDFactory;
//import org.eclipse.mdht.uml.cda.ccd.ProceduresSection;
//import org.eclipse.mdht.uml.cda.hitsp.Condition;
//import org.eclipse.mdht.uml.cda.hitsp.ConditionEntry;
//import org.eclipse.mdht.uml.cda.hitsp.EncountersSection;
//import org.eclipse.mdht.uml.cda.hitsp.HITSPFactory;
//import org.eclipse.mdht.uml.cda.hitsp.HealthcareProvider;
//import org.eclipse.mdht.uml.cda.hitsp.MedicationInformation;
//import org.eclipse.mdht.uml.cda.hitsp.MedicationsSection;
//import org.eclipse.mdht.uml.cda.hitsp.PatientSummary;
//import org.eclipse.mdht.uml.cda.hitsp.ProblemListSection;
//import org.eclipse.mdht.uml.cda.hitsp.VitalSignsSection;
//import org.eclipse.mdht.uml.cda.util.CDAUtil;
//import org.eclipse.mdht.uml.hl7.datatypes.AD;
//import org.eclipse.mdht.uml.hl7.datatypes.ANY;
//import org.eclipse.mdht.uml.hl7.datatypes.CD;
//import org.eclipse.mdht.uml.hl7.datatypes.CE;
//import org.eclipse.mdht.uml.hl7.datatypes.CS;
//import org.eclipse.mdht.uml.hl7.datatypes.DatatypesFactory;
//import org.eclipse.mdht.uml.hl7.datatypes.ED;
//import org.eclipse.mdht.uml.hl7.datatypes.EN;
//import org.eclipse.mdht.uml.hl7.datatypes.ENXP;
//import org.eclipse.mdht.uml.hl7.datatypes.II;
//import org.eclipse.mdht.uml.hl7.datatypes.IVL_TS;
//import org.eclipse.mdht.uml.hl7.datatypes.IVXB_TS;
//import org.eclipse.mdht.uml.hl7.datatypes.ON;
//import org.eclipse.mdht.uml.hl7.datatypes.PN;
//import org.eclipse.mdht.uml.hl7.datatypes.SC;
//import org.eclipse.mdht.uml.hl7.datatypes.ST;
//import org.eclipse.mdht.uml.hl7.datatypes.TEL;
//import org.eclipse.mdht.uml.hl7.datatypes.TS;
//import org.eclipse.mdht.uml.hl7.vocab.ActClass;
//import org.eclipse.mdht.uml.hl7.vocab.ActClassRoot;
//import org.eclipse.mdht.uml.hl7.vocab.NullFlavor;
//import org.eclipse.mdht.uml.hl7.vocab.x_DocumentEncounterMood;
//import org.eclipse.mdht.uml.hl7.vocab.x_DocumentProcedureMood;
//import org.eclipse.mdht.uml.hl7.vocab.x_DocumentSubstanceMood;
//import org.eclipse.mdht.uml.hl7.vocab.x_ServiceEventPerformer;
//
//import com.apixio.converters.base.Creator;
//import com.apixio.model.patient.ClinicalActor;
//import com.apixio.model.patient.ClinicalCode;
//import com.apixio.model.patient.Demographics;
//import com.apixio.model.patient.Encounter;
//import com.apixio.model.patient.ExternalID;
//import com.apixio.model.patient.Gender;
//import com.apixio.model.patient.Name;
//import com.apixio.model.patient.Organization;
//import com.apixio.model.patient.Patient;
//import com.apixio.model.patient.Prescription;
//import com.apixio.model.patient.Problem;
//import com.apixio.model.patient.Procedure;
//
//public class C32Creator extends Creator{
//
//	enum DataSet {narrative, coded, all};
//	DataSet includeDataSet = DataSet.all;
//	PatientSummary summary;
//	String cdaDateFormat = "yyyyMMddHHmmssZ";
//	DateTime minDate = DateTime.now();
//	DateTime maxDate = DateTime.now();
//	String currentPatientPid = "";
//	ClinicalActor clinicalActor;
//	Organization organization;
//
//	public C32Creator(Patient patient)
//	{
//		// new Provider("Dummy", "Provider", "1234567893"), new Organization("Apixio", "123456")
//		this(patient, new ClinicalActor(), new Organization(), DataSet.all);
//	}
//
//	public C32Creator(Patient patient, ClinicalActor clinicalActor, Organization organization, DataSet includeDataSet)
//	{
//		this.clinicalActor = clinicalActor;
//		this.organization = organization;
//		this.currentPatientPid = String.valueOf(patient.getPatientId());
//		this.includeDataSet = includeDataSet;
//		setupNewDocument();
//
//		getMinAndMaxDates(patient);
//
//		addPatient(patient, true);
//	}
//
//	public C32Creator(Patient patient, ClinicalActor clinicalActor,
//			Organization organization2) {
//		// TODO Auto-generated constructor stub
//	}
//
//	public void exportFile(String fileName) throws Exception {
//		FileOutputStream fileOutput = new FileOutputStream(fileName);
//		CDAUtil.save(summary, fileOutput);
//	}
//
//	public String getString()
//	{
//		StringWriter writer = new StringWriter();
//		try {
//			CDAUtil.save(summary, writer);
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		return writer.toString();
//	}
//
//	private void getMinAndMaxDates(Patient patient) {
//		Iterable<Encounter> encounters = patient.getEncounters();
//
//		if (encounters != null)
//		{
//			for (Encounter encounter : encounters)
//			{
//				DateTime encounterDate = encounter.getEncounterDate();
//				if (encounterDate != null && encounterDate.isBefore(minDate))
//					minDate = encounterDate;
//			}
//		}
//	}
//
//	private void setupNewDocument() {
//
//		summary = HITSPFactory.eINSTANCE.createPatientSummary().init();
//		//summary = CCDFactory.eINSTANCE.createContinuityOfCareDocument().init();
//		summary.setTypeId(getTypeId());
//		summary.setId(getRandomId());
//		summary.setCode(getCE("Summarization of episode note", "34133-9", "2.16.840.1.113883.6.1", "LOINC"));
//		summary.setTitle(getString("Referral Summary"));
//		summary.setEffectiveTime(getCurrentTime());
//		summary.setConfidentialityCode(getCE("Normal", "N", "1.3.6.1.4.1.21367.100.1", "Connectathon Confidentiality Codes"));
//		summary.setLanguageCode(createCS("en-US"));
//		summary.getAuthors().add(getAuthor());
//		summary.setCustodian(createCustodian());
//		summary.getDocumentationOfs().add(getDocumentationOf());
//	}
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
//		serviceEvent.setEffectiveTime(getIVLTS(DateTime.now().minusYears(20), DateTime.now()));
//		serviceEvent.getPerformers().add(getPerformer());
//		return serviceEvent;
//	}
//
//	private HealthcareProvider getPerformer() {
//		HealthcareProvider performer = HITSPFactory.eINSTANCE.createHealthcareProvider().init();
//		performer.setTypeCode(x_ServiceEventPerformer.PRF);
//		performer.setFunctionCode(getCE("Consulting Provider", "CP", "2.16.840.1.113883.12.443", "Provider Role"));
//		performer.setTime(getIVLTS(DateTime.now().minusYears(20), DateTime.now()));
//		performer.setAssignedEntity(getAssignedEntity());
//		return performer;
//	}
//
//	private AssignedEntity getAssignedEntity() {
//		AssignedEntity assignedEntity = CDAFactory.eINSTANCE.createAssignedEntity();
//		assignedEntity.getIds().add(getIdFromExternalID(clinicalActor.getPrimaryId())); //createId("2.16.840.1.113883.4.6", clinicalActor.get(), "NPI")); //1234567893, 1111111112
//		assignedEntity.setCode(getCE("Generic Physicians", "200000000X", "2.16.840.1.113883.6.101", "ProviderCodes"));
//		assignedEntity.setAssignedPerson(getPersonFromName(clinicalActor.getActorGivenName())); //getPerson(clinicalActor.getFirstName(), clinicalActor.getLastName()));
//		assignedEntity.getAddrs().add(createNullAddress());
//		assignedEntity.getTelecoms().add(createNullTelecom());
//		assignedEntity.getRepresentedOrganizations().add(createOrganization(organization.getName()));
//		return assignedEntity;
//	}
//
//
//	private II getIdFromExternalID(ExternalID primaryId) {
//		return getId(primaryId.getId(), primaryId.getSource(), primaryId.getAssignAuthority());
//	}
//
//	private II getId(String root, String extension, String assigningAuthorityName) {
//		II id = DatatypesFactory.eINSTANCE.createII();
//		if (!root.equals(""))
//			id.setRoot(root);
//		if (!extension.equals(""))
//			id.setExtension(extension);
//		if (!assigningAuthorityName.equals(""))
//			id.setAssigningAuthorityName(assigningAuthorityName);
//		return id;
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
//		custodianOrganization.getIds().add(getRandomId());
//		custodianOrganization.setAddr(createNullAddress());
//		custodianOrganization.setTelecom(createNullTelecom());
//		custodianOrganization.setName(createON(organization.getName()));
//		return custodianOrganization;
//	}
//
//	private org.eclipse.mdht.uml.cda.Organization createOrganization(String name) {
//		org.eclipse.mdht.uml.cda.Organization cdaOrganization = CDAFactory.eINSTANCE.createOrganization();
//		cdaOrganization.getIds().add(getRandomId());
//		cdaOrganization.getAddrs().add(createNullAddress());
//		cdaOrganization.getTelecoms().add(createNullTelecom());
//		cdaOrganization.getNames().add(createON(name));
//		return cdaOrganization;
//	}
//
//
//	private ON createON(String string) {
//		ON on = DatatypesFactory.eINSTANCE.createON();
//		on.addText(string);
//		return on;
//	}
//
//	private Author getAuthor() {
//		Author author = CDAFactory.eINSTANCE.createAuthor();
//		author.setTime(getCurrentTime());
//		author.setAssignedAuthor(getAssignedAuthor());
//		return author;
//	}
//
//	private AssignedAuthor getAssignedAuthor() {
//		AssignedAuthor assignedAuthor = CDAFactory.eINSTANCE.createAssignedAuthor();
//		assignedAuthor.getIds().add(getRandomId());
//		assignedAuthor.setAssignedPerson(getPersonFromName(clinicalActor.getActorGivenName())); //getPerson(clinicalActor.getFirstName(), clinicalActor.getLastName()));
//		assignedAuthor.setRepresentedOrganization(createOrganization(organization.getName()));
//		//assignedAuthor.setAssignedAuthoringDevice(getAssignedAuthoringDevice());
//		assignedAuthor.getAddrs().add(createNullAddress());
//		assignedAuthor.getTelecoms().add(createNullTelecom());
//		return assignedAuthor;
//	}
//
//	private Person getPersonFromName(Name actorGivenName) {
//		Person person = CDAFactory.eINSTANCE.createPerson();
//		if (actorGivenName != null) {
//			person.getNames().add(getPNFromName(actorGivenName));
//		}
//		return person;
//	}
//
//	private Person getPerson(String firstName, String lastName) {
//		Person person = CDAFactory.eINSTANCE.createPerson();
//		person.getNames().add(getName(firstName, lastName));
//		return person;
//	}
//	private Person getPerson(String name) {
//		Person person = CDAFactory.eINSTANCE.createPerson();
//		person.getNames().add(getName(name));
//		return person;
//	}
//
//	private TEL createNullTelecom() {
//		TEL nullTelecom = DatatypesFactory.eINSTANCE.createTEL();
//		nullTelecom.setNullFlavor(NullFlavor.NI);
//		return nullTelecom;
//	}
//
//	private AD createNullAddress() {
//		AD nullAddress = DatatypesFactory.eINSTANCE.createAD();
//		nullAddress.setNullFlavor(NullFlavor.NI);
//		return nullAddress;
//	}
//
//	private AuthoringDevice getAssignedAuthoringDevice() {
//		AuthoringDevice authoringDevice = CDAFactory.eINSTANCE.createAuthoringDevice();
//		authoringDevice.setSoftwareName(getSC("Apixio"));
//		authoringDevice.setManufacturerModelName(getSC("Apixio"));
//		return authoringDevice;
//	}
//
//	private SC getSC(String string) {
//		SC sc = DatatypesFactory.eINSTANCE.createSC();
//		sc.addText(string);
//		return sc;
//	}
//
//	private ST getString(String string) {
//		ST st = DatatypesFactory.eINSTANCE.createST();
//		if (!string.equals(""))
//			st.addText(string);
//		return st;
//	}
//
//	private InfrastructureRootTypeId getTypeId() {
//		InfrastructureRootTypeId typeId = CDAFactory.eINSTANCE.createInfrastructureRootTypeId();
//		typeId.setRoot("2.16.840.1.113883.1.3");
//		typeId.setExtension("POCD_HD000040");
//		return typeId;
//	}
//
//	public void addPatient(Patient patient, Boolean replaceDemographics)
//	{
//		if (replaceDemographics)
//		{
//			summary.getRecordTargets().clear();
//			summary.getRecordTargets().add(getRecordTarget(patient.getPatientId(), patient.getPrimaryDemographics()));
//		}
//
//		if (patient.getProblems() != null)
//			summary.addSection(getProblemsSection(patient.getProblems()));
//
//		if (patient.getPrescriptions() != null)
//			summary.addSection(getMedicationsSection(patient.getPrescriptions()));
//
//		if (patient.getProcedures() != null)
//			summary.addSection(getProceduresSection(patient.getProcedures()));
//
//		if (patient.getEncounters() != null) // && patient.getEncounters()size() > 0)
//			summary.addSection(getEncountersSection(patient.getEncounters()));
//
//	}
//
//	private Section getProceduresSection(Iterable<Procedure> iterable) {
//		ProceduresSection proceduresSection = CCDFactory.eINSTANCE.createProceduresSection().init();
//		List<String> procedureList = new ArrayList<String>();
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
//	private org.eclipse.mdht.uml.cda.hitsp.Procedure getProcedure(Procedure procedure) {
//		org.eclipse.mdht.uml.cda.hitsp.Procedure hitspProcedure = HITSPFactory.eINSTANCE.createPastProcedure().init();
//		hitspProcedure.setClassCode(ActClass.PROC);
//		hitspProcedure.setMoodCode(x_DocumentProcedureMood.EVN);
//		hitspProcedure.getTemplateIds().add(createId("2.16.840.1.113883.10.20.1.29"));
//		hitspProcedure.getIds().add(getRandomId());
//		CD procedureCode = getCE(procedure.getCode()); //createCode(procedure.getProcedureName(), procedure.getCode(), procedure.getCodingSystem(), "");
//		procedureCode.setOriginalText(getTextWithReference("#reference"));
//		hitspProcedure.setCode(procedureCode);
//		hitspProcedure.setText(getTextWithReference("#reference")); //getText(procedure.getProcedureName()));
//		hitspProcedure.setEffectiveTime(getIVLTS(procedure.getPerformedOn(), null));
//		hitspProcedure.setStatusCode(createCS("migrationCompleted"));
//		return hitspProcedure;
//	}
//
//	private MedicationsSection getMedicationsSection(Iterable<Prescription> iterable) {
//		MedicationsSection medicationsSection = HITSPFactory.eINSTANCE.createMedicationsSection().init();
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
//	private org.eclipse.mdht.uml.cda.hitsp.Medication getMedication(Prescription prescription) {
//		org.eclipse.mdht.uml.cda.hitsp.Medication hitspMedication = HITSPFactory.eINSTANCE.createMedication().init();
//		hitspMedication.setMoodCode(x_DocumentSubstanceMood.INT);
//		hitspMedication.getIds().add(getRandomId());
//		hitspMedication.setText(getText(prescription.getSig()));
////		if ("2.16.840.1.113883.6.88".equalsIgnoreCase(medication.getCodingSystem()) && medication.getPrescriptionDate() != null &&
////			(this.includeDataSet.equals(DataSet.coded)))
////			System.out.println("MU compliant med found for " + currentPatientPid);
//		hitspMedication.getEffectiveTimes().add(getIVLTS(prescription.getPrescriptionDate(), null));
//		//hitspMedication.setDoseQuantity(getDose(medication.getDosage()));
////		String medName = prescription.getAssociatedMedication().getBrandName();
////		if (StringUtils.isBlank(medName))
////			medName = prescription.getAssociatedMedication().getGenericName();
//////		if (StringUtils.isBlank(medName))
//////			medName = prescription.getAssociatedMedication().getCode();
////
////		if (StringUtils.isBlank(prescription.getBrandName()))
////			prescription.setBrandName(medName);
////		if (StringUtils.isBlank(prescription.getGenericName()))
////			prescription.setGenericName(medName);
//		hitspMedication.setConsumable(getConsumable(prescription.getAssociatedMedication().getBrandName(), prescription.getAssociatedMedication().getGenericName(), prescription.getAssociatedMedication().getCode()));
//
//		//medication.getQuantity();
//		//medication.getStrength();
//		//medication.getUnits();
//		return hitspMedication;
//	}
//
//	private Consumable getConsumable(String brandName, String genericName, ClinicalCode code) {
//		Consumable consumable = CDAFactory.eINSTANCE.createConsumable();
//		MedicationInformation medicationInformation = HITSPFactory.eINSTANCE.createMedicationInformation().init();
//		medicationInformation.setManufacturedMaterial(getManufacturedMaterial(brandName, genericName, code));
//		consumable.setManufacturedProduct(medicationInformation);
//		return consumable;
//	}
//
//	private Material getManufacturedMaterial(String brandName, String genericName, ClinicalCode code) {
//		Material material = CDAFactory.eINSTANCE.createMaterial();
//		CE medicationCode = getCE(code);
//		medicationCode.setOriginalText(getText(genericName));
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
//	private VitalSignsSection getVitalsSection() {
//		VitalSignsSection vitalsSection = HITSPFactory.eINSTANCE.createVitalSignsSection();
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
//		EncountersSection encountersSection = HITSPFactory.eINSTANCE.createEncountersSection().init();
//		for (Encounter encounter : iterable)
//		{
//			encountersSection.addEncounter(getEncounter(encounter));
//		}
//		return encountersSection;
//	}
//
//	private org.eclipse.mdht.uml.cda.hitsp.Encounter getEncounter(Encounter encounter) {
//		org.eclipse.mdht.uml.cda.hitsp.Encounter hitspEncounter = HITSPFactory.eINSTANCE.createEncounter().init();
//		hitspEncounter.setMoodCode(x_DocumentEncounterMood.EVN);
//		hitspEncounter.getTemplateIds().add(createId("2.16.840.1.113883.10.20.1.21"));
//		hitspEncounter.getIds().add(createId(encounter.getEncounterId().toString()));
//		CD encounterCode = getCE(encounter.getCode()); //createCode("", encounter.getCode(), encounter.getCodingSystem(), "");
//		encounterCode.setOriginalText(getTextWithReference("#reference"));
//		hitspEncounter.setCode(encounterCode);
//		hitspEncounter.setEffectiveTime(getIVLTS(encounter.getEncounterDate()));
//		hitspEncounter.setText(getTextWithReference("#reference"));
//		return hitspEncounter;
//	}
//
//	private ProblemListSection getProblemsSection(Iterable<Problem> iterable) {
//
//		ProblemListSection problemSection = HITSPFactory.eINSTANCE.createProblemListSection().init();
//		Set<String> problemList = new HashSet<String>();
//		for (Problem problem : iterable)
//		{
//			problemSection.addAct(getProblemAct(problem));
////			if (useData(problem.getSourceType()))
////			{
////				String nameAndDate = problem.getCode();
////				if (problem.getStartDate() != null)
////					nameAndDate = nameAndDate + "|" + problem.getStartDate().toString("yyyyMMdd");
////				if (!problemList.contains(nameAndDate))
////				{
////					problemList.add(nameAndDate);
////					problemSection.addAct(getProblemAct(problem));
////				}
//////				else
//////					System.out.println("skipping duplicate problem");
////			}
//		}
//		return problemSection;
//	}
//
//	private boolean useData(String sourceType) {
//		return (this.includeDataSet.equals(DataSet.all) ||
//			(this.includeDataSet.equals(DataSet.narrative) && "narrative".equalsIgnoreCase(sourceType)) ||
//			(this.includeDataSet.equals(DataSet.coded) && !"narrative".equalsIgnoreCase(sourceType)));
//
////		if (sourceType == null)
////			return !useNarrative;
////		else
////			return useNarrative;
////		boolean useData = (sourceType.equalsIgnoreCase("narrative") && useNarrative) || (!sourceType.equalsIgnoreCase("narrative") && !useNarrative);
////		return useData;
//	}
//
//	private Condition getProblemAct(Problem problem) {
//
//		Condition condition = HITSPFactory.eINSTANCE.createCondition().init();
//		//ProblemAct condition = CCDFactory.eINSTANCE.createProblemAct().init();
//		condition.getIds().add(getRandomId());
//		condition.setStatusCode(createCS("active"));
//		ConditionEntry conditionEntry = condition.createConditionEntry().init();
//		EntryRelationship entryRelationship = (EntryRelationship) conditionEntry.eContainer();
//		entryRelationship.setInversionInd(false);
//		//condition.
//		//ProblemObservation conditionEntry = CCDFactory.eINSTANCE.createProblemObservation().init();
//		DateTime problemStart = problem.getStartDate();
//		if (problemStart == null)
//			problemStart = minDate;
//		condition.setEffectiveTime(getIVLTS(problemStart, problem.getEndDate()));
//		//ProblemObservation problemObservation = CCDFactory.eINSTANCE.createProblemObservation().init();
//
//		conditionEntry.getIds().add(getRandomId());
//		conditionEntry.setCode(createCode("Problem", "55607006", "2.16.840.1.113883.6.96", "SNOMED CT"));
//
//		//conditionEntry.setText(getText(problem.getProblemName()));
//		conditionEntry.setText(getTextWithReference("#reference"));
//		//conditionEntry.setStatusCode(createCS("migrationCompleted"));
//		//conditionEntry
//		conditionEntry.setEffectiveTime(getIVLTS(problemStart, problem.getEndDate()));
//		conditionEntry.getValues().add(getCE(problem.getCode())); //createCD(problem.getProblemName(),problem.getCode(), "2.16.840.1.113883.6.103", "ICD9CM")); //"2.16.840.1.113883.6.96", "SNOMED CT"));
//		return condition;
//	}
//
//	private ED getTextWithReference(String string) {
//		ED ed = DatatypesFactory.eINSTANCE.createED();
//		ed.setReference(createReference(string));
//		return ed;
//	}
//
//	private TEL createReference(String string) {
//		TEL reference = DatatypesFactory.eINSTANCE.createTEL();
//		reference.setValue(string);
//		return reference;
//	}
//
//	private ANY createCD(String displayName, String codeValue, String codeSystem, String codeSystemName)
//	{
//		CD code = DatatypesFactory.eINSTANCE.createCD();
//		code.setDisplayName(displayName);
//		code.setCode(codeValue);
//		code.setCodeSystem(codeSystem);
//		code.setCodeSystemName(codeSystemName);
//		return code;
//	}
//
//	private ED getText(String text) {
//		ED ed = DatatypesFactory.eINSTANCE.createED();
//		if (text != null)
//			ed.addText(text);
//		return ed;
//	}
//
//	private IVL_TS getIVLTS(DateTime exactDate) {
//		IVL_TS exactTime = DatatypesFactory.eINSTANCE.createIVL_TS();
//		if (exactDate != null)
//			exactTime.setValue(exactDate.toString(cdaDateFormat));
//		else
//			exactTime.setNullFlavor(NullFlavor.UNK);
//
//		return exactTime;
//	}
//	private IVL_TS getIVLTS(DateTime startDate, DateTime endDate) {
//		IVL_TS interval = DatatypesFactory.eINSTANCE.createIVL_TS();
//		interval.setLow(getIVXBTS(startDate));
//		if (endDate != null)
//			interval.setHigh(getIVXBTS(endDate));
//
//		return interval;
//	}
//
//	private IVXB_TS getIVXBTS(DateTime date) {
//		IVXB_TS datePart = DatatypesFactory.eINSTANCE.createIVXB_TS();
//		if (date != null)
//			datePart.setValue(date.toString(cdaDateFormat));
//		else
//			datePart.setNullFlavor(NullFlavor.UNK);
//			// datePart.setValue(DateTime.now().toString(cdaDateFormat));
//		return datePart;
//	}
//
//	private RecordTarget getRecordTarget(UUID uuid, Demographics demoData) {
//		RecordTarget recordTarget = CDAFactory.eINSTANCE.createRecordTarget();
//		PatientRole patientRole = CDAFactory.eINSTANCE.createPatientRole();
//		patientRole.getIds().add(getIdFromExternalID(organization.getPrimaryId())); //createId(organization.getId(), organization.getId() + "." + String.valueOf(uuid)));
//		patientRole.setPatient(getPatient(demoData));
//		patientRole.getAddrs().add(createNullAddress());
//		patientRole.getTelecoms().add(createNullTelecom());
//		recordTarget.setPatientRole(patientRole);
//		return recordTarget;
//	}
//
//
//
//	private org.eclipse.mdht.uml.cda.Patient getPatient(Demographics demoData) {
//		org.eclipse.mdht.uml.cda.Patient patient = CDAFactory.eINSTANCE.createPatient();
//		patient.getNames().add(getName("Anonymous", "Patient"));
//		patient.setAdministrativeGenderCode(getGender(demoData.getGender()));
//		patient.setBirthTime(getTS(demoData.getDateOfBirth()));
//		return patient;
//	}
//
//	private CE getGender(Gender gender) {
//		CE genderCode = getCE("Unknown", "U", "2.16.840.1.113883.5.1", "AdministrativeGender");
//		if (gender.equals(Gender.MALE))
//			genderCode = getCE("Male", "M", "2.16.840.1.113883.5.1", "AdministrativeGender");
//		if (gender.equals(Gender.FEMALE))
//			genderCode = getCE("Female", "F", "2.16.840.1.113883.5.1", "AdministrativeGender");
//		if (gender.equals(Gender.TRANSGENDER))
//			genderCode = getCE("Undifferentiated", "UN", "2.16.840.1.113883.5.1", "AdministrativeGender");
//		return genderCode;
//	}
//
//	private CE getGender(String sex) {
//		CE genderCode = getCE("Unknown", "U", "2.16.840.1.113883.5.1", "AdministrativeGender");
//		if (sex.equalsIgnoreCase("M"))
//			genderCode = getCE("Male", "M", "2.16.840.1.113883.5.1", "AdministrativeGender");
//		if (sex.equalsIgnoreCase("F"))
//			genderCode = getCE("Female", "F", "2.16.840.1.113883.5.1", "AdministrativeGender");
//		return genderCode;
//	}
//
//	private CE getCE(String displayName, String code, String codeSystem, String codeSystemName) {
//		CE ce = DatatypesFactory.eINSTANCE.createCE();
//		if (displayName != null && !displayName.equals(""))
//			ce.setDisplayName(displayName);
//		if (code != null && !code.equals(""))
//			ce.setCode(code);
//		if (codeSystem != null && !codeSystem.equals(""))
//			ce.setCodeSystem(codeSystem);
//		if (codeSystemName != null && !codeSystemName.equals(""))
//			ce.setCodeSystemName(codeSystemName);
//		return ce;
//	}
//
//	private PN getName(String firstName, String lastName) {
//		PN name = DatatypesFactory.eINSTANCE.createPN();
//		name.getGivens().add(getENXP(firstName));
//		name.getFamilies().add(getENXP(lastName));
//		return name;
//	}
//
//	private PN getPNFromName(Name name) {
//		PN pn = DatatypesFactory.eINSTANCE.createPN();
//		pn.getGivens().addAll(getENXPList(name.getGivenNames()));
//		pn.getFamilies().addAll(getENXPList(name.getFamilyNames()));
//		pn.getPrefixes().addAll(getENXPList(name.getPrefixes()));
//		pn.getSuffixes().addAll(getENXPList(name.getSuffixes()));
//		return pn;
//	}
//
//	private PN getName(String fullName) {
//		PN name = DatatypesFactory.eINSTANCE.createPN();
//		name.addText(fullName);
//		return name;
//	}
//
//	private List<ENXP> getENXPList(List<String> nameList) {
//		List<ENXP> enxpList = new LinkedList<ENXP>();
//		for (String name : nameList) {
//			ENXP namePart = DatatypesFactory.eINSTANCE.createENXP();
//			namePart.addText(name);
//			enxpList.add(namePart);
//		}
//		return enxpList;
//	}
//
//	private ENXP getENXP(String nameText) {
//		ENXP namePart = DatatypesFactory.eINSTANCE.createENXP();
//		namePart.addText(nameText);
//		return namePart;
//	}
//
//	private TS getTS(DateTime dateTime) {
//		TS ts = DatatypesFactory.eINSTANCE.createTS();
//		if (dateTime != null)
//			ts.setValue(dateTime.toString(cdaDateFormat));
//		return ts;
//	}
//
//	private II createId(String root) {
//		return createId(root, "", "");
//	}
//
//	private II createId(String root, String extension) {
//		return createId(root, extension, "");
//	}
//
//	private II createId(String root, String extension, String assigningAuthorityName) {
//		II id = DatatypesFactory.eINSTANCE.createII();
//		if (!root.equals(""))
//			id.setRoot(root);
//		if (!extension.equals(""))
//			id.setExtension(extension);
//		if (!assigningAuthorityName.equals(""))
//			id.setAssigningAuthorityName(assigningAuthorityName);
//		return id;
//	}
//
//	private CS createCS(String code) {
//		CS cs = DatatypesFactory.eINSTANCE.createCS();
//		cs.setCode(code);
//		return cs;
//	}
//
//	private CD createCode(String displayName, String codeValue, String codeSystem, String codeSystemName) {
//		CD code = DatatypesFactory.eINSTANCE.createCD();
//		if (displayName != null && !displayName.equals(""))
//			code.setDisplayName(displayName);
//		if (codeValue != null && !codeValue.equals(""))
//			code.setCode(codeValue);
//		if (codeSystem != null && !codeSystem.equals(""))
//			code.setCodeSystem(codeSystem);
//		if (codeSystemName != null && !codeSystemName.equals(""))
//			code.setCodeSystemName(codeSystemName);
//		return code;
//	}
//
//	private CE createCode(NullFlavor nullFlavor)
//	{
//		CE code = DatatypesFactory.eINSTANCE.createCE();
//		code.setNullFlavor(nullFlavor);
//		return code;
//	}
//
//	private TS getCurrentTime() {
//		TS currentTime = DatatypesFactory.eINSTANCE.createTS();
//		String currentTimeString = DateTime.now().toString(cdaDateFormat);
//		currentTime.setValue(currentTimeString);
//		return currentTime;
//	}
//
//	private II getRandomId() {
//		String randomUuid = UUID.randomUUID().toString();
//		II id = DatatypesFactory.eINSTANCE.createII(randomUuid);
//		return id;
//	}
//
//	@Override
//	public void create(Patient patient) {
//		// TODO Auto-generated method stub
//
//	}
//
//}
