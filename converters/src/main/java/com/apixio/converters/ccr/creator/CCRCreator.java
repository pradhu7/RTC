package com.apixio.converters.ccr.creator;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.joda.time.DateTime;

import com.apixio.converters.ccr.jaxb.ActorReferenceType;
import com.apixio.converters.ccr.jaxb.ActorType;
import com.apixio.converters.ccr.jaxb.CodeType;
import com.apixio.converters.ccr.jaxb.CodedDescriptionType;
import com.apixio.converters.ccr.jaxb.ContinuityOfCareRecord;
import com.apixio.converters.ccr.jaxb.DateTimeType;
import com.apixio.converters.ccr.jaxb.IDType;
import com.apixio.converters.ccr.jaxb.PersonNameType;
import com.apixio.converters.ccr.jaxb.ProblemType;
import com.apixio.converters.ccr.jaxb.SourceType;
import com.apixio.converters.ccr.jaxb.StructuredProductType;
import com.apixio.converters.ccr.jaxb.ActorType.Organization;
import com.apixio.converters.ccr.jaxb.ActorType.Person;
import com.apixio.converters.ccr.jaxb.ActorType.Person.Name;
import com.apixio.converters.ccr.jaxb.ContinuityOfCareRecord.Actors;
import com.apixio.converters.ccr.jaxb.ContinuityOfCareRecord.Body;
import com.apixio.converters.ccr.jaxb.ContinuityOfCareRecord.From;
import com.apixio.converters.ccr.jaxb.ContinuityOfCareRecord.To;
import com.apixio.converters.ccr.jaxb.ContinuityOfCareRecord.Body.Medications;
import com.apixio.converters.ccr.jaxb.ContinuityOfCareRecord.Body.Problems;
import com.apixio.model.patient.ClinicalCode;
import com.apixio.model.patient.Gender;
import com.apixio.model.patient.Medication;
import com.apixio.model.patient.Patient;
import com.apixio.model.patient.Problem;
import com.apixio.model.patient.Procedure;

public class CCRCreator {

	private ContinuityOfCareRecord ccr;
	private Actors actors;
	private Body body;
	private int currentActorId = 0;
	private String sourceActorId; 
	//private String dateFormat = "yyyy-MM-ddZHH:mm:ss";
	
	public CCRCreator(Patient patient)
	{
		ccr = new ContinuityOfCareRecord();
		ccr.setCCRDocumentObjectID(UUID.randomUUID().toString());
		CodedDescriptionType language = new CodedDescriptionType();
		language.setText("English");
		ccr.setLanguage(language);
		ccr.setVersion("V1.0");
		DateTimeType documentDate = new DateTimeType();
		documentDate.setExactDateTime(DateTime.now().toString());
		ccr.setDateTime(documentDate);
		actors = new Actors();
		ccr.setActors(actors);
		body = new Body();
		ccr.setBody(body);
		addSource();
		addPatientInformation(patient);		
	}
	
	public void exportFile(String fileName) throws JAXBException, FileNotFoundException
	{
        JAXBContext jaxbContext = JAXBContext.newInstance("com.apixio.engine.parser.ccr.jaxb");
		Marshaller marshaller = jaxbContext.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
		marshaller.marshal(ccr, new FileOutputStream(fileName));		
	}
	
	private void addPatientInformation(Patient patient) {
		String patientActorId = getNextActorID();		
		com.apixio.converters.ccr.jaxb.ContinuityOfCareRecord.Patient ccrPatient = new com.apixio.converters.ccr.jaxb.ContinuityOfCareRecord.Patient();
		ccrPatient.setActorID(patientActorId);
		ccr.getPatient().add(ccrPatient);

		ActorReferenceType actorReference = new ActorReferenceType();
		actorReference.setActorID(patientActorId);
		ActorType patientActor = new ActorType();
		IDType patientId = new IDType();
		patientId.setID(String.valueOf(patient.getPatientId()));
		patientId.setIssuedBy(actorReference);
		patientId.getSource().add(getSourceActor());
		patientActor.getIDs().add(patientId);
		patientActor.setActorObjectID(patientActorId);
		Person person = new Person();
		Name patientName = new Name();
		PersonNameType personName = new PersonNameType();
		personName.getGiven().add("Anonymous");
		personName.getFamily().add("Patient");
		patientName.setCurrentName(personName);
		person.setName(patientName);
		if (patient.getPrimaryDemographics() != null) {	
			DateTimeType birthDate = new DateTimeType();		
			birthDate.setExactDateTime(patient.getPrimaryDemographics().getDateOfBirth().toString("yyyy-MM-dd"));
			person.setDateOfBirth(birthDate);
			person.setGender(getCodedGender(patient.getPrimaryDemographics().getGender())); //CodedDescriptionType(gender,"",""));			
		}
		patientActor.setPerson(person);
		patientActor.getSource().add(getSourceActor());
		actors.getActor().add(patientActor);
		To to = new To();
		to.getActorLink().add(actorReference);
		ccr.setTo(to);
		
		addProblems(patient.getProblems());
		//addProcedures(patient.getProcedures());
		//addMedications(patient.getMedications());
	}
	
	private CodedDescriptionType getCodedGender(Gender gender) {
		String genderString = "Unknown";
		if (gender.equals(Gender.MALE))
			genderString = "Male";
		else if (gender.equals(Gender.FEMALE))
			genderString = "Female";
		return getCodedDescriptionType(genderString, null);
	}

	private void addMedications(List<Medication> ccrmedications) {
		Medications medications = new Medications();
		body.setMedications(medications);
		for (Medication currentMedication : ccrmedications)
		{
			StructuredProductType medication = new StructuredProductType();
			//medication.
			//medications.getMedication().add(medication);
		}
	}

	private void addProcedures(List<Procedure> procedures) {
		// TODO Auto-generated method stub
		
	}

	private void addProblems(Iterable<Problem> iterable) {
		Problems problems = new Problems();
		body.setProblems(problems);
		for (Problem currentProblem : iterable)
		{
			ProblemType currentProblemType = new ProblemType();
			currentProblemType.setCCRDataObjectID(UUID.randomUUID().toString());
			DateTimeType problemDate = new DateTimeType();
			if (currentProblem.getStartDate() != null)
				problemDate.setExactDateTime(currentProblem.getStartDate().toString());
			currentProblemType.getDateTime().add(problemDate);
			currentProblemType.setDescription(getCodedDescriptionType(currentProblem.getProblemName(), currentProblem.getCode()));
			currentProblemType.getSource().add(getSourceActor());
			problems.getProblem().add(currentProblemType);
		}
		
	}

	private CodedDescriptionType getCodedDescriptionType(String text, ClinicalCode code) {
		CodedDescriptionType codedDescriptionType = new CodedDescriptionType();
		codedDescriptionType.setText(text);
		if (code != null)
		{
			CodeType codeType = new CodeType();
			codeType.setCodingSystem(code.getCodingSystemOID());
			codeType.setValue(code.getCode());
			codedDescriptionType.getCode().add(codeType);
		}
		return codedDescriptionType;
	}

	private void addSource()
	{
        sourceActorId = getNextActorID();
        ActorType sourceActor = new ActorType();
        sourceActor.setActorObjectID(sourceActorId);
        Organization organization = new Organization();
        organization.setName("Apixio");
        sourceActor.setOrganization(organization);
        sourceActor.getSource().add(getSourceActor());
        actors.getActor().add(sourceActor);

		From author = new From();
		ActorReferenceType actorReference = new ActorReferenceType();
		actorReference.setActorID(sourceActorId);
		author.getActorLink().add(actorReference);
        // add this as the "From" for now (it can be overwritten)
        ccr.setFrom(author);
	}
	private SourceType getSourceActor() {
		SourceType source = new SourceType();
		ActorReferenceType actorReference = new ActorReferenceType();
		actorReference.setActorID(sourceActorId);
		source.getActor().add(actorReference);
		return source;
	}
	private String getNextActorID() {
        currentActorId++;
        return String.valueOf(currentActorId);
	}
	
	
}
