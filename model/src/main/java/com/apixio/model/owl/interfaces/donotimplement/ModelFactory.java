package com.apixio.model.owl.interfaces.donotimplement;


import com.apixio.model.owl.interfaces.Address;
import com.apixio.model.owl.interfaces.Allergy;
import com.apixio.model.owl.interfaces.AmendmentAnnotation;
import com.apixio.model.owl.interfaces.AnatomicalEntity;
import com.apixio.model.owl.interfaces.BiometricValue;
import com.apixio.model.owl.interfaces.CareSite;
import com.apixio.model.owl.interfaces.ClinicalCode;
import com.apixio.model.owl.interfaces.ClinicalCodingSystem;
import com.apixio.model.owl.interfaces.ContactDetails;
import com.apixio.model.owl.interfaces.Date;
import com.apixio.model.owl.interfaces.DateRange;
import com.apixio.model.owl.interfaces.Demographics;
import com.apixio.model.owl.interfaces.ExternalIdentifier;
import com.apixio.model.owl.interfaces.FamilyHistory;
import com.apixio.model.owl.interfaces.File;
import com.apixio.model.owl.interfaces.HICN;
import com.apixio.model.owl.interfaces.ImageRendering;
import com.apixio.model.owl.interfaces.Immunization;
import com.apixio.model.owl.interfaces.InsurancePlan;
import com.apixio.model.owl.interfaces.InsurancePolicy;
import com.apixio.model.owl.interfaces.LabResult;
import com.apixio.model.owl.interfaces.MedicalProfessional;
import com.apixio.model.owl.interfaces.Medication;
import com.apixio.model.owl.interfaces.*;

public interface ModelFactory {



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000000
     */

    /**
     * Creates an instance of type Document.
     */
    public Document createDocument(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000002
     */

    /**
     * Creates an instance of type Label.
     */
    public Label createLabel(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000003
     */

    /**
     * Creates an instance of type Identifier.
     */
    public Identifier createIdentifier(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000009
     */

    /**
     * Creates an instance of type EncounterDocument.
     */
    public EncounterDocument createEncounterDocument(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000017
     */

    /**
     * Creates an instance of type GenderType.
     */
    public GenderType createGenderType(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000020
     */

    /**
     * Creates an instance of type LabFlagType.
     */
    public LabFlagType createLabFlagType(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000024
     */

    /**
     * Creates an instance of type ResolutionStatusType.
     */
    public ResolutionStatusType createResolutionStatusType(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000027
     */

    /**
     * Creates an instance of type File.
     */
    public File createFile(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000028
     */

    /**
     * Creates an instance of type Patient.
     */
    public Patient createPatient(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000033
     */

    /**
     * Creates an instance of type NameType.
     */
    public NameType createNameType(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000037
     */

    /**
     * Creates an instance of type AmendmentAnnotation.
     */
    public AmendmentAnnotation createAmendmentAnnotation(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000040
     */

    /**
     * Creates an instance of type CareSiteType.
     */
    public CareSiteType createCareSiteType(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000042
     */

    /**
     * Creates an instance of type PlanType.
     */
    public PlanType createPlanType(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000044
     */

    /**
     * Creates an instance of type MedicareOrganization.
     */
    public MedicareOrganization createMedicareOrganization(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000046
     */

    /**
     * Creates an instance of type Medication.
     */
    public Medication createMedication(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000050
     */

    /**
     * Creates an instance of type Person.
     */
    public Person createPerson(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000051
     */

    /**
     * Creates an instance of type Demographics.
     */
    public Demographics createDemographics(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000054
     */

    /**
     * Creates an instance of type Page.
     */
    public Page createPage(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000056
     */

    /**
     * Creates an instance of type TimeMeasurment.
     */
    public TimeMeasurment createTimeMeasurment(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000065
     */

    /**
     * Creates an instance of type Provenance.
     */
    public Provenance createProvenance(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000066
     */

    /**
     * Creates an instance of type DataTransformation.
     */
    public DataTransformation createDataTransformation(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000072
     */

    /**
     * Creates an instance of type ClinicalRoleType.
     */
    public ClinicalRoleType createClinicalRoleType(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000075
     */

    /**
     * Creates an instance of type InformationalEntityIdentifier.
     */
    public InformationalEntityIdentifier createInformationalEntityIdentifier(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000088
     */

    /**
     * Creates an instance of type CareSiteOrganization.
     */
    public CareSiteOrganization createCareSiteOrganization(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000089
     */

    /**
     * Creates an instance of type AnatomicalEntity.
     */
    public AnatomicalEntity createAnatomicalEntity(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000095
     */

    /**
     * Creates an instance of type DataProcessingDetail.
     */
    public DataProcessingDetail createDataProcessingDetail(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000099
     */

    /**
     * Creates an instance of type ExternalIdentifier.
     */
    public ExternalIdentifier createExternalIdentifier(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000100
     */

    /**
     * Creates an instance of type SignatureType.
     */
    public SignatureType createSignatureType(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000102
     */

    /**
     * Creates an instance of type ParserType.
     */
    public ParserType createParserType(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000107
     */

    /**
     * Creates an instance of type Name.
     */
    public Name createName(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000108
     */

    /**
     * Creates an instance of type DateRange.
     */
    public DateRange createDateRange(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000112
     */

    /**
     * Creates an instance of type Collective.
     */
    public Collective createCollective(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000113
     */

    /**
     * Creates an instance of type Organization.
     */
    public Organization createOrganization(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000114
     */

    /**
     * Creates an instance of type Corporation.
     */
    public Corporation createCorporation(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000115
     */

    /**
     * Creates an instance of type DocumentType.
     */
    public DocumentType createDocumentType(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000119
     */

    /**
     * Creates an instance of type InsuranceCompany.
     */
    public InsuranceCompany createInsuranceCompany(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000127
     */

    /**
     * Creates an instance of type HICN.
     */
    public HICN createHICN(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000129
     */

    /**
     * Creates an instance of type ClinicalCode.
     */
    public ClinicalCode createClinicalCode(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000132
     */

    /**
     * Creates an instance of type FeeForServiceInsuranceClaim.
     */
    public FeeForServiceInsuranceClaim createFeeForServiceInsuranceClaim(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000133
     */

    /**
     * Creates an instance of type RiskAdjustmentInsuranceClaim.
     */
    public RiskAdjustmentInsuranceClaim createRiskAdjustmentInsuranceClaim(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000134
     */

    /**
     * Creates an instance of type PreferredProviderOrganizationPlan.
     */
    public PreferredProviderOrganizationPlan createPreferredProviderOrganizationPlan(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000135
     */

    /**
     * Creates an instance of type HealthCarePayor.
     */
    public HealthCarePayor createHealthCarePayor(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000139
     */

    /**
     * Creates an instance of type SocialQuality.
     */
    public SocialQuality createSocialQuality(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000145
     */

    /**
     * Creates an instance of type MaritalStatusType.
     */
    public MaritalStatusType createMaritalStatusType(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000147
     */

    /**
     * Creates an instance of type CodedEntity.
     */
    public CodedEntity createCodedEntity(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000157
     */

    /**
     * Creates an instance of type CareSite.
     */
    public CareSite createCareSite(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000158
     */

    /**
     * Creates an instance of type PositionalIdentifer.
     */
    public PositionalIdentifer createPositionalIdentifer(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000159
     */

    /**
     * Creates an instance of type Address.
     */
    public Address createAddress(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000170
     */

    /**
     * Creates an instance of type TelephoneNumber.
     */
    public TelephoneNumber createTelephoneNumber(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000171
     */

    /**
     * Creates an instance of type ApixioDictionary.
     */
    public ApixioDictionary createApixioDictionary(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000174
     */

    /**
     * Creates an instance of type MedicalProfessional.
     */
    public MedicalProfessional createMedicalProfessional(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000175
     */

    /**
     * Creates an instance of type ContactDetails.
     */
    public ContactDetails createContactDetails(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000177
     */

    /**
     * Creates an instance of type Illness.
     */
    public Illness createIllness(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000179
     */

    /**
     * Creates an instance of type Human.
     */
    public Human createHuman(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000181
     */

    /**
     * Creates an instance of type ProvenancedEntity.
     */
    public ProvenancedEntity createProvenancedEntity(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000186
     */

    /**
     * Creates an instance of type Processor.
     */
    public Processor createProcessor(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000191
     */

    /**
     * Creates an instance of type ProcessorType.
     */
    public ProcessorType createProcessorType(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000193
     */

    /**
     * Creates an instance of type SourceType.
     */
    public SourceType createSourceType(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000222
     */

    /**
     * Creates an instance of type MedicalCondition.
     */
    public MedicalCondition createMedicalCondition(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000233
     */

    /**
     * Creates an instance of type InsuranceClaimableEvent.
     */
    public InsuranceClaimableEvent createInsuranceClaimableEvent(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000234
     */

    /**
     * Creates an instance of type CMSClaimValidationResult.
     */
    public CMSClaimValidationResult createCMSClaimValidationResult(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000235
     */

    /**
     * Creates an instance of type RAPSValidationError.
     */
    public RAPSValidationError createRAPSValidationError(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000236
     */

    /**
     * Creates an instance of type EDPSValidationError.
     */
    public EDPSValidationError createEDPSValidationError(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000240
     */

    /**
     * Creates an instance of type CMSValidationError.
     */
    public CMSValidationError createCMSValidationError(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000304
     */

    /**
     * Creates an instance of type Rendering.
     */
    public Rendering createRendering(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000309
     */

    /**
     * Creates an instance of type Date.
     */
    public Date createDate(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000312
     */

    /**
     * Creates an instance of type TextRendering.
     */
    public TextRendering createTextRendering(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000318
     */

    /**
     * Creates an instance of type ApixioDate.
     */
    public ApixioDate createApixioDate(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000321
     */

    /**
     * Creates an instance of type MeasurmentQuality.
     */
    public MeasurmentQuality createMeasurmentQuality(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000324
     */

    /**
     * Creates an instance of type DateType.
     */
    public DateType createDateType(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000354
     */

    /**
     * Creates an instance of type InsurancePlan.
     */
    public InsurancePlan createInsurancePlan(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000381
     */

    /**
     * Creates an instance of type ImageRendering.
     */
    public ImageRendering createImageRendering(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000388
     */

    /**
     * Creates an instance of type PatientHistory.
     */
    public PatientHistory createPatientHistory(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000389
     */

    /**
     * Creates an instance of type FamilyHistory.
     */
    public FamilyHistory createFamilyHistory(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000390
     */

    /**
     * Creates an instance of type SocialHistory.
     */
    public SocialHistory createSocialHistory(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000391
     */

    /**
     * Creates an instance of type ClinicalCodingSystem.
     */
    public ClinicalCodingSystem createClinicalCodingSystem(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000410
     */

    /**
     * Creates an instance of type AddressType.
     */
    public AddressType createAddressType(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000411
     */

    /**
     * Creates an instance of type Prescription.
     */
    public Prescription createPrescription(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000417
     */

    /**
     * Creates an instance of type TelephoneType.
     */
    public TelephoneType createTelephoneType(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000431
     */

    /**
     * Creates an instance of type PrimaryCareProvider.
     */
    public PrimaryCareProvider createPrimaryCareProvider(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000450
     */

    /**
     * Creates an instance of type TimeBoundExternalIdentifier.
     */
    public TimeBoundExternalIdentifier createTimeBoundExternalIdentifier(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000451
     */

    /**
     * Creates an instance of type Medicare.
     */
    public Medicare createMedicare(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000455
     */

    /**
     * Creates an instance of type InsurancePolicy.
     */
    public InsurancePolicy createInsurancePolicy(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000458
     */

    /**
     * Creates an instance of type MedicareInsurancePolicy.
     */
    public MedicareInsurancePolicy createMedicareInsurancePolicy(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000459
     */

    /**
     * Creates an instance of type Problem.
     */
    public Problem createProblem(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000474
     */

    /**
     * Creates an instance of type BiometricValue.
     */
    public BiometricValue createBiometricValue(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000485
     */

    /**
     * Creates an instance of type LabResult.
     */
    public LabResult createLabResult(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#000916
     */

    /**
     * Creates an instance of type Procedure.
     */
    public Procedure createProcedure(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#010145
     */

    /**
     * Creates an instance of type Source.
     */
    public Source createSource(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#070156
     */

    /**
     * Creates an instance of type PatientEncounter.
     */
    public PatientEncounter createPatientEncounter(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#073046
     */

    /**
     * Creates an instance of type StatusAnnotationType.
     */
    public StatusAnnotationType createStatusAnnotationType(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#076616
     */

    /**
     * Creates an instance of type PatientEncounterType.
     */
    public PatientEncounterType createPatientEncounterType(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#077747
     */

    /**
     * Creates an instance of type OperationAnnotationType.
     */
    public OperationAnnotationType createOperationAnnotationType(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#090478
     */

    /**
     * Creates an instance of type Allergy.
     */
    public Allergy createAllergy(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#100001
     */

    /**
     * Creates an instance of type Immunization.
     */
    public Immunization createImmunization(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#330022
     */

    /**
     * Creates an instance of type InsuranceClaim.
     */
    public InsuranceClaim createInsuranceClaim(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#331033
     */

    /**
     * Creates an instance of type Provider.
     */
    public Provider createProvider(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#350035
     */

    /**
     * Creates an instance of type PreferedProviderOrganization.
     */
    public PreferedProviderOrganization createPreferedProviderOrganization(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#455045
     */

    /**
     * Creates an instance of type InformationContentQuality.
     */
    public InformationContentQuality createInformationContentQuality(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#456132
     */

    /**
     * Creates an instance of type HealthMaintenanceOrganizationPlan.
     */
    public HealthMaintenanceOrganizationPlan createHealthMaintenanceOrganizationPlan(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#540026
     */

    /**
     * Creates an instance of type InsuranceClaimStatusType.
     */
    public InsuranceClaimStatusType createInsuranceClaimStatusType(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#555015
     */

    /**
     * Creates an instance of type MedicalQuality.
     */
    public MedicalQuality createMedicalQuality(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#640023
     */

    /**
     * Creates an instance of type Invoice.
     */
    public Invoice createInvoice(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#646036
     */

    /**
     * Creates an instance of type HealthMaintenanceOrganization.
     */
    public HealthMaintenanceOrganization createHealthMaintenanceOrganization(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#777051
     */

    /**
     * Creates an instance of type Contract.
     */
    public Contract createContract(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#9000000
     */

    /**
     * Creates an instance of type ApixioObject.
     */
    public ApixioObject createApixioObject(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#9000001
     */

    /**
     * Creates an instance of type ApixioProcess.
     */
    public ApixioProcess createApixioProcess(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#9000002
     */

    /**
     * Creates an instance of type Attribute.
     */
    public Attribute createAttribute(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#9000003
     */

    /**
     * Creates an instance of type InformationContentEntity.
     */
    public InformationContentEntity createInformationContentEntity(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#9000004
     */

    /**
     * Creates an instance of type ComputationalEntity.
     */
    public ComputationalEntity createComputationalEntity(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#9000005
     */

    /**
     * Creates an instance of type MathematicalEntity.
     */
    public MathematicalEntity createMathematicalEntity(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#9000006
     */

    /**
     * Creates an instance of type MedicalEntity.
     */
    public MedicalEntity createMedicalEntity(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#9000007
     */

    /**
     * Creates an instance of type SocialEntity.
     */
    public SocialEntity createSocialEntity(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#9000008
     */

    /**
     * Creates an instance of type TextualEntity.
     */
    public TextualEntity createTextualEntity(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#9000009
     */

    /**
     * Creates an instance of type SoftwareEntity.
     */
    public SoftwareEntity createSoftwareEntity(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#9000010
     */

    /**
     * Creates an instance of type MaterialEntity.
     */
    public MaterialEntity createMaterialEntity(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#9000011
     */

    /**
     * Creates an instance of type BiologicalEntity.
     */
    public BiologicalEntity createBiologicalEntity(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#9000012
     */

    /**
     * Creates an instance of type ChemicalEntity.
     */
    public ChemicalEntity createChemicalEntity(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#9000013
     */

    /**
     * Creates an instance of type SpatialRegion.
     */
    public SpatialRegion createSpatialRegion(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#9000014
     */

    /**
     * Creates an instance of type Operation.
     */
    public Operation createOperation(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#9000015
     */

    /**
     * Creates an instance of type Event.
     */
    public Event createEvent(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#9000017
     */

    /**
     * Creates an instance of type InformationProcessing.
     */
    public InformationProcessing createInformationProcessing(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#9000018
     */

    /**
     * Creates an instance of type MedicalEvent.
     */
    public MedicalEvent createMedicalEvent(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#9000019
     */

    /**
     * Creates an instance of type Observation.
     */
    public Observation createObservation(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#9000020
     */

    /**
     * Creates an instance of type Quality.
     */
    public Quality createQuality(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#9000021
     */

    /**
     * Creates an instance of type BiologicalQuality.
     */
    public BiologicalQuality createBiologicalQuality(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#9000022
     */

    /**
     * Creates an instance of type InformationalQuality.
     */
    public InformationalQuality createInformationalQuality(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#9000023
     */

    /**
     * Creates an instance of type RealizableEntity.
     */
    public RealizableEntity createRealizableEntity(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#9000024
     */

    /**
     * Creates an instance of type Function.
     */
    public Function createFunction(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#9000025
     */

    /**
     * Creates an instance of type Role.
     */
    public Role createRole(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#9000046
     */

    /**
     * Creates an instance of type MeasurmentValue.
     */
    public MeasurmentValue createMeasurmentValue(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#9000047
     */

    /**
     * Creates an instance of type Quantity.
     */
    public Quantity createQuantity(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#9000048
     */

    /**
     * Creates an instance of type DimensionalQuantity.
     */
    public DimensionalQuantity createDimensionalQuantity(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#9000049
     */

    /**
     * Creates an instance of type DimensionlessQuantity.
     */
    public DimensionlessQuantity createDimensionlessQuantity(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#9000143
     */

    /**
     * Creates an instance of type Entity.
     */
    public Entity createEntity(String name);



    /* ***************************************************
     * Class http://apixio.com/ontology/alo#999913
     */

    /**
     * Creates an instance of type MedicalCodeEntity.
     */
    public MedicalCodeEntity createMedicalCodeEntity(String name);
}
