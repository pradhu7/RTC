//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.09.05 at 03:21:47 PM EDT 
//


package com.apixio.model.xsd.patient;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.apixio.kx.base.xsdmodel.patient package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _ProcedureBodySite_QNAME = new QName("", "bodySite");
    private final static QName _Patient_QNAME = new QName("", "Patient");
    private final static QName _DemographicsLanguages_QNAME = new QName("", "languages");
    private final static QName _DemographicsReligiousAffiliation_QNAME = new QName("", "religiousAffiliation");
    private final static QName _DemographicsRace_QNAME = new QName("", "race");
    private final static QName _AnatomyAnatomicalStructureName_QNAME = new QName("", "anatomicalStructureName");
    private final static QName _CodedBaseObjectSupplementaryClinicalActorId_QNAME = new QName("", "supplementaryClinicalActorId");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.apixio.kx.base.xsdmodel.patient
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link PatientType.BiometricValues }
     * 
     */
    public PatientType.BiometricValues createPatientTypeBiometricValues() {
        return new PatientType.BiometricValues();
    }

    /**
     * Create an instance of {@link PatientType.Prescriptions }
     * 
     */
    public PatientType.Prescriptions createPatientTypePrescriptions() {
        return new PatientType.Prescriptions();
    }

    /**
     * Create an instance of {@link Administration }
     * 
     */
    public Administration createAdministration() {
        return new Administration();
    }

    /**
     * Create an instance of {@link Organization }
     * 
     */
    public Organization createOrganization() {
        return new Organization();
    }

    /**
     * Create an instance of {@link PatientType.ParsingDetails }
     * 
     */
    public PatientType.ParsingDetails createPatientTypeParsingDetails() {
        return new PatientType.ParsingDetails();
    }

    /**
     * Create an instance of {@link PatientType.Sources }
     * 
     */
    public PatientType.Sources createPatientTypeSources() {
        return new PatientType.Sources();
    }

    /**
     * Create an instance of {@link ExternalID }
     * 
     */
    public ExternalID createExternalID() {
        return new ExternalID();
    }

    /**
     * Create an instance of {@link CareSite }
     * 
     */
    public CareSite createCareSite() {
        return new CareSite();
    }

    /**
     * Create an instance of {@link PatientType.ClinicalActors }
     * 
     */
    public PatientType.ClinicalActors createPatientTypeClinicalActors() {
        return new PatientType.ClinicalActors();
    }

    /**
     * Create an instance of {@link ContactDetails }
     * 
     */
    public ContactDetails createContactDetails() {
        return new ContactDetails();
    }

    /**
     * Create an instance of {@link Medication }
     * 
     */
    public Medication createMedication() {
        return new Medication();
    }

    /**
     * Create an instance of {@link Encounter }
     * 
     */
    public Encounter createEncounter() {
        return new Encounter();
    }

    /**
     * Create an instance of {@link Allergy }
     * 
     */
    public Allergy createAllergy() {
        return new Allergy();
    }

    /**
     * Create an instance of {@link MetadataMap }
     * 
     */
    public MetadataMap createMetadataMap() {
        return new MetadataMap();
    }

    /**
     * Create an instance of {@link PatientType.Encounters }
     * 
     */
    public PatientType.Encounters createPatientTypeEncounters() {
        return new PatientType.Encounters();
    }

    /**
     * Create an instance of {@link PatientType.LabResults }
     * 
     */
    public PatientType.LabResults createPatientTypeLabResults() {
        return new PatientType.LabResults();
    }

    /**
     * Create an instance of {@link PatientType.Administrations }
     * 
     */
    public PatientType.Administrations createPatientTypeAdministrations() {
        return new PatientType.Administrations();
    }

    /**
     * Create an instance of {@link SocialHistory }
     * 
     */
    public SocialHistory createSocialHistory() {
        return new SocialHistory();
    }

    /**
     * Create an instance of {@link ClinicalBaseObject }
     * 
     */
    public ClinicalBaseObject createClinicalBaseObject() {
        return new ClinicalBaseObject();
    }

    /**
     * Create an instance of {@link Demographics }
     * 
     */
    public Demographics createDemographics() {
        return new Demographics();
    }

    /**
     * Create an instance of {@link PatientType.FamilyHistories }
     * 
     */
    public PatientType.FamilyHistories createPatientTypeFamilyHistories() {
        return new PatientType.FamilyHistories();
    }

    /**
     * Create an instance of {@link ClinicalActor }
     * 
     */
    public ClinicalActor createClinicalActor() {
        return new ClinicalActor();
    }

    /**
     * Create an instance of {@link PatientType.Problems }
     * 
     */
    public PatientType.Problems createPatientTypeProblems() {
        return new PatientType.Problems();
    }

    /**
     * Create an instance of {@link Name }
     * 
     */
    public Name createName() {
        return new Name();
    }

    /**
     * Create an instance of {@link LabResult }
     * 
     */
    public LabResult createLabResult() {
        return new LabResult();
    }

    /**
     * Create an instance of {@link Source }
     * 
     */
    public Source createSource() {
        return new Source();
    }

    /**
     * Create an instance of {@link Anatomy }
     * 
     */
    public Anatomy createAnatomy() {
        return new Anatomy();
    }

    /**
     * Create an instance of {@link CodedBaseObject }
     * 
     */
    public CodedBaseObject createCodedBaseObject() {
        return new CodedBaseObject();
    }

    /**
     * Create an instance of {@link Prescription }
     * 
     */
    public Prescription createPrescription() {
        return new Prescription();
    }

    /**
     * Create an instance of {@link FamilyHistory }
     * 
     */
    public FamilyHistory createFamilyHistory() {
        return new FamilyHistory();
    }

    /**
     * Create an instance of {@link PatientType.SocialHistories }
     * 
     */
    public PatientType.SocialHistories createPatientTypeSocialHistories() {
        return new PatientType.SocialHistories();
    }

    /**
     * Create an instance of {@link PatientType.Allergies }
     * 
     */
    public PatientType.Allergies createPatientTypeAllergies() {
        return new PatientType.Allergies();
    }

    /**
     * Create an instance of {@link PatientType }
     * 
     */
    public PatientType createPatientType() {
        return new PatientType();
    }

    /**
     * Create an instance of {@link Language }
     * 
     */
    public Language createLanguage() {
        return new Language();
    }

    /**
     * Create an instance of {@link PatientType.Procedures }
     * 
     */
    public PatientType.Procedures createPatientTypeProcedures() {
        return new PatientType.Procedures();
    }

    /**
     * Create an instance of {@link Address }
     * 
     */
    public Address createAddress() {
        return new Address();
    }

    /**
     * Create an instance of {@link PatientType.CareSites }
     * 
     */
    public PatientType.CareSites createPatientTypeCareSites() {
        return new PatientType.CareSites();
    }

    /**
     * Create an instance of {@link Demographics.Languages }
     * 
     */
    public Demographics.Languages createDemographicsLanguages() {
        return new Demographics.Languages();
    }

    /**
     * Create an instance of {@link Code }
     * 
     */
    public Code createCode() {
        return new Code();
    }

    /**
     * Create an instance of {@link ParsingDetail }
     * 
     */
    public ParsingDetail createParsingDetail() {
        return new ParsingDetail();
    }

    /**
     * Create an instance of {@link Procedure }
     * 
     */
    public Procedure createProcedure() {
        return new Procedure();
    }

    /**
     * Create an instance of {@link Actor }
     * 
     */
    public Actor createActor() {
        return new Actor();
    }

    /**
     * Create an instance of {@link Problem }
     * 
     */
    public Problem createProblem() {
        return new Problem();
    }

    /**
     * Create an instance of {@link BiometricValue }
     * 
     */
    public BiometricValue createBiometricValue() {
        return new BiometricValue();
    }

    /**
     * Create an instance of {@link TelephoneNumber }
     * 
     */
    public TelephoneNumber createTelephoneNumber() {
        return new TelephoneNumber();
    }

    /**
     * Create an instance of {@link PatientType.ExternalIds }
     * 
     */
    public PatientType.ExternalIds createPatientTypeExternalIds() {
        return new PatientType.ExternalIds();
    }

    /**
     * Create an instance of {@link NamedPair }
     * 
     */
    public NamedPair createNamedPair() {
        return new NamedPair();
    }

    /**
     * Create an instance of {@link TypedDate }
     * 
     */
    public TypedDate createTypedDate() {
        return new TypedDate();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Anatomy }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "bodySite", scope = Procedure.class)
    public JAXBElement<Anatomy> createProcedureBodySite(Anatomy value) {
        return new JAXBElement<Anatomy>(_ProcedureBodySite_QNAME, Anatomy.class, Procedure.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PatientType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "Patient")
    public JAXBElement<PatientType> createPatient(PatientType value) {
        return new JAXBElement<PatientType>(_Patient_QNAME, PatientType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Demographics.Languages }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "languages", scope = Demographics.class)
    public JAXBElement<Demographics.Languages> createDemographicsLanguages(Demographics.Languages value) {
        return new JAXBElement<Demographics.Languages>(_DemographicsLanguages_QNAME, Demographics.Languages.class, Demographics.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "religiousAffiliation", scope = Demographics.class)
    public JAXBElement<String> createDemographicsReligiousAffiliation(String value) {
        return new JAXBElement<String>(_DemographicsReligiousAffiliation_QNAME, String.class, Demographics.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Code }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "race", scope = Demographics.class)
    public JAXBElement<Code> createDemographicsRace(Code value) {
        return new JAXBElement<Code>(_DemographicsRace_QNAME, Code.class, Demographics.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "anatomicalStructureName", scope = Anatomy.class)
    public JAXBElement<String> createAnatomyAnatomicalStructureName(String value) {
        return new JAXBElement<String>(_AnatomyAnatomicalStructureName_QNAME, String.class, Anatomy.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "supplementaryClinicalActorId", scope = CodedBaseObject.class)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    public JAXBElement<String> createCodedBaseObjectSupplementaryClinicalActorId(String value) {
        return new JAXBElement<String>(_CodedBaseObjectSupplementaryClinicalActorId_QNAME, String.class, CodedBaseObject.class, value);
    }

}
