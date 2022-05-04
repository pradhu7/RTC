package com.apixio.model.converter.vocabulary;

import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import java.util.HashMap;
import java.util.Map;
public class OWLVocabulary {
    public static final String BASE_NAMESPACE = "http://apixio.com/rdf/";
    public static final String ONTOLOGY_NAMESPACE = "http://apixio.com/ontology/";
    private static final OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
    static {
        model.setNsPrefix("apixioRDF", BASE_NAMESPACE);
        model.setNsPrefix("apixioOntology", ONTOLOGY_NAMESPACE);
    }

    public static enum OWLClass{
        ExternalIdentifier(ONTOLOGY_NAMESPACE + "alo#000099"),
        Source(ONTOLOGY_NAMESPACE + "alo#010145"),
        InsurancePlan(ONTOLOGY_NAMESPACE + "alo#000354"),
        DimensionalQuantity(ONTOLOGY_NAMESPACE + "alo#9000048"),
        SocialEntity(ONTOLOGY_NAMESPACE + "alo#9000007"),
        MeasurmentValue(ONTOLOGY_NAMESPACE + "alo#9000046"),
        Quality(ONTOLOGY_NAMESPACE + "alo#9000020"),
        Provider(ONTOLOGY_NAMESPACE + "alo#331033"),
        SocialQuality(ONTOLOGY_NAMESPACE + "alo#000139"),
        Collective(ONTOLOGY_NAMESPACE + "alo#000112"),
        CMSValidationError(ONTOLOGY_NAMESPACE + "alo#000240"),
        Corporation(ONTOLOGY_NAMESPACE + "alo#000114"),
        HealthCarePayor(ONTOLOGY_NAMESPACE + "alo#000135"),
        PreferredProviderOrganizationPlan(ONTOLOGY_NAMESPACE + "alo#000134"),
        RiskAdjustmentInsuranceClaim(ONTOLOGY_NAMESPACE + "alo#000133"),
        FeeForServiceInsuranceClaim(ONTOLOGY_NAMESPACE + "alo#000132"),
        Patient(ONTOLOGY_NAMESPACE + "alo#000028"),
        File(ONTOLOGY_NAMESPACE + "alo#000027"),
        Problem(ONTOLOGY_NAMESPACE + "alo#000459"),
        MedicareInsurancePolicy(ONTOLOGY_NAMESPACE + "alo#000458"),
        ResolutionStatusType(ONTOLOGY_NAMESPACE + "alo#000024"),
        InsurancePolicy(ONTOLOGY_NAMESPACE + "alo#000455"),
        LabFlagType(ONTOLOGY_NAMESPACE + "alo#000020"),
        SourceType(ONTOLOGY_NAMESPACE + "alo#000193"),
        AnatomicalEntity(ONTOLOGY_NAMESPACE + "alo#000089"),
        CareSiteOrganization(ONTOLOGY_NAMESPACE + "alo#000088"),
        Entity(ONTOLOGY_NAMESPACE + "alo#9000143"),
        FamilyHistory(ONTOLOGY_NAMESPACE + "alo#000389"),
        Document(ONTOLOGY_NAMESPACE + "alo#000000"),
        PatientEncounterType(ONTOLOGY_NAMESPACE + "alo#076616"),
        Provenance(ONTOLOGY_NAMESPACE + "alo#000065"),
        RAPSValidationError(ONTOLOGY_NAMESPACE + "alo#000235"),
        DataProcessingDetail(ONTOLOGY_NAMESPACE + "alo#000095"),
        InsuranceClaimableEvent(ONTOLOGY_NAMESPACE + "alo#000233"),
        InsuranceClaimStatusType(ONTOLOGY_NAMESPACE + "alo#540026"),
        HICN(ONTOLOGY_NAMESPACE + "alo#000127"),
        InsuranceCompany(ONTOLOGY_NAMESPACE + "alo#000119"),
        PositionalIdentifer(ONTOLOGY_NAMESPACE + "alo#000158"),
        ClinicalCode(ONTOLOGY_NAMESPACE + "alo#000129"),
        Observation(ONTOLOGY_NAMESPACE + "alo#9000019"),
        InputDataType(ONTOLOGY_NAMESPACE + "alo#000191"),
        GenderType(ONTOLOGY_NAMESPACE + "alo#000017"),
        Date(ONTOLOGY_NAMESPACE + "alo#000309"),
        Processor(ONTOLOGY_NAMESPACE + "alo#000186"),
        PatientEncounter(ONTOLOGY_NAMESPACE + "alo#070156"),
        MedicalCodeEntity(ONTOLOGY_NAMESPACE + "alo#999913"),
        ProvenancedEntity(ONTOLOGY_NAMESPACE + "alo#000181"),
        InformationalEntityIdentifier(ONTOLOGY_NAMESPACE + "alo#000075"),
        ChemicalEntity(ONTOLOGY_NAMESPACE + "alo#9000012"),
        MedicalQuality(ONTOLOGY_NAMESPACE + "alo#555015"),
        ClinicalRoleType(ONTOLOGY_NAMESPACE + "alo#000072"),
        Role(ONTOLOGY_NAMESPACE + "alo#9000025"),
        Function(ONTOLOGY_NAMESPACE + "alo#9000024"),
        RealizableEntity(ONTOLOGY_NAMESPACE + "alo#9000023"),
        FeeForServiceInsuranceClaimType(ONTOLOGY_NAMESPACE + "alo#000224"),
        BiologicalQuality(ONTOLOGY_NAMESPACE + "alo#9000021"),
        MedicalCondition(ONTOLOGY_NAMESPACE + "alo#000222"),
        CMSClaimValidationResult(ONTOLOGY_NAMESPACE + "alo#000234"),
        DocumentType(ONTOLOGY_NAMESPACE + "alo#000115"),
        ClinicalCodingSystem(ONTOLOGY_NAMESPACE + "alo#000391"),
        SocialHistory(ONTOLOGY_NAMESPACE + "alo#000390"),
        EncounterDocument(ONTOLOGY_NAMESPACE + "alo#000009"),
        PreferedProviderOrganization(ONTOLOGY_NAMESPACE + "alo#350035"),
        Identifier(ONTOLOGY_NAMESPACE + "alo#000003"),
        Human(ONTOLOGY_NAMESPACE + "alo#000179"),
        Illness(ONTOLOGY_NAMESPACE + "alo#000177"),
        Label(ONTOLOGY_NAMESPACE + "alo#000002"),
        ContactDetails(ONTOLOGY_NAMESPACE + "alo#000175"),
        MedicalProfessional(ONTOLOGY_NAMESPACE + "alo#000174"),
        PrimaryCareProvider(ONTOLOGY_NAMESPACE + "alo#000431"),
        ApixioDictionary(ONTOLOGY_NAMESPACE + "alo#000171"),
        TelephoneNumber(ONTOLOGY_NAMESPACE + "alo#000170"),
        DataTransformation(ONTOLOGY_NAMESPACE + "alo#000066"),
        DateType(ONTOLOGY_NAMESPACE + "alo#000324"),
        MedicalEvent(ONTOLOGY_NAMESPACE + "alo#9000018"),
        InformationProcessing(ONTOLOGY_NAMESPACE + "alo#9000017"),
        MeasurmentQuality(ONTOLOGY_NAMESPACE + "alo#000321"),
        InformationContentQuality(ONTOLOGY_NAMESPACE + "alo#455045"),
        Operation(ONTOLOGY_NAMESPACE + "alo#9000014"),
        SpatialRegion(ONTOLOGY_NAMESPACE + "alo#9000013"),
        PatientHistory(ONTOLOGY_NAMESPACE + "alo#000388"),
        BiologicalEntity(ONTOLOGY_NAMESPACE + "alo#9000011"),
        MaterialEntity(ONTOLOGY_NAMESPACE + "alo#9000010"),
        DateRange(ONTOLOGY_NAMESPACE + "alo#000108"),
        RiskAdjustmentInsuranceClaimType(ONTOLOGY_NAMESPACE + "alo#000210"),
        InsuranceClaim(ONTOLOGY_NAMESPACE + "alo#330022"),
        ImageRendering(ONTOLOGY_NAMESPACE + "alo#000381"),
        ProcessorType(ONTOLOGY_NAMESPACE + "alo#000102"),
        MaritalStatusType(ONTOLOGY_NAMESPACE + "alo#000145"),
        HealthMaintenanceOrganizationPlan(ONTOLOGY_NAMESPACE + "alo#456132"),
        Contract(ONTOLOGY_NAMESPACE + "alo#777051"),
        Invoice(ONTOLOGY_NAMESPACE + "alo#640023"),
        EDPSValidationError(ONTOLOGY_NAMESPACE + "alo#000236"),
        OperationAnnotationType(ONTOLOGY_NAMESPACE + "alo#077747"),
        ApixioDate(ONTOLOGY_NAMESPACE + "alo#000318"),
        SoftwareEntity(ONTOLOGY_NAMESPACE + "alo#9000009"),
        TimeMeasurment(ONTOLOGY_NAMESPACE + "alo#000056"),
        TimeBoundExternalIdentifier(ONTOLOGY_NAMESPACE + "alo#000450"),
        Page(ONTOLOGY_NAMESPACE + "alo#000054"),
        TextRendering(ONTOLOGY_NAMESPACE + "alo#000312"),
        StatusAnnotationType(ONTOLOGY_NAMESPACE + "alo#073046"),
        Demographics(ONTOLOGY_NAMESPACE + "alo#000051"),
        Person(ONTOLOGY_NAMESPACE + "alo#000050"),
        InformationContentEntity(ONTOLOGY_NAMESPACE + "alo#9000003"),
        Attribute(ONTOLOGY_NAMESPACE + "alo#9000002"),
        ApixioProcess(ONTOLOGY_NAMESPACE + "alo#9000001"),
        ApixioObject(ONTOLOGY_NAMESPACE + "alo#9000000"),
        InsuranceClaimErrorCodeFilter(ONTOLOGY_NAMESPACE + "alo#000253"),
        ComputationalEntity(ONTOLOGY_NAMESPACE + "alo#9000004"),
        MathematicalEntity(ONTOLOGY_NAMESPACE + "alo#9000005"),
        MedicalEntity(ONTOLOGY_NAMESPACE + "alo#9000006"),
        Address(ONTOLOGY_NAMESPACE + "alo#000159"),
        TelephoneType(ONTOLOGY_NAMESPACE + "alo#000417"),
        CareSite(ONTOLOGY_NAMESPACE + "alo#000157"),
        TextualEntity(ONTOLOGY_NAMESPACE + "alo#9000008"),
        PatientEncounterSwitchType(ONTOLOGY_NAMESPACE + "alo#000154"),
        Allergy(ONTOLOGY_NAMESPACE + "alo#090478"),
        Prescription(ONTOLOGY_NAMESPACE + "alo#000411"),
        AddressType(ONTOLOGY_NAMESPACE + "alo#000410"),
        Medication(ONTOLOGY_NAMESPACE + "alo#000046"),
        Rendering(ONTOLOGY_NAMESPACE + "alo#000304"),
        MedicareOrganization(ONTOLOGY_NAMESPACE + "alo#000044"),
        Immunization(ONTOLOGY_NAMESPACE + "alo#100001"),
        PlanType(ONTOLOGY_NAMESPACE + "alo#000042"),
        BiometricValue(ONTOLOGY_NAMESPACE + "alo#000474"),
        CareSiteType(ONTOLOGY_NAMESPACE + "alo#000040"),
        Quantity(ONTOLOGY_NAMESPACE + "alo#9000047"),
        DimensionlessQuantity(ONTOLOGY_NAMESPACE + "alo#9000049"),
        Event(ONTOLOGY_NAMESPACE + "alo#9000015"),
        LabResult(ONTOLOGY_NAMESPACE + "alo#000485"),
        Medicare(ONTOLOGY_NAMESPACE + "alo#000451"),
        SignatureType(ONTOLOGY_NAMESPACE + "alo#000100"),
        Organization(ONTOLOGY_NAMESPACE + "alo#000113"),
        CodedEntity(ONTOLOGY_NAMESPACE + "alo#000147"),
        Procedure(ONTOLOGY_NAMESPACE + "alo#000916"),
        InformationalQuality(ONTOLOGY_NAMESPACE + "alo#9000022"),
        Name(ONTOLOGY_NAMESPACE + "alo#000107"),
        HealthMaintenanceOrganization(ONTOLOGY_NAMESPACE + "alo#646036"),
        AmendmentAnnotation(ONTOLOGY_NAMESPACE + "alo#000037"),
        NameType(ONTOLOGY_NAMESPACE + "alo#000033")
        ;

        private final String uri;
        private OWLClass(String uri){ this.uri = uri;}
        public String toString() { return uri; }
        public String uri() { return uri; }
        public static OWLClass getByClass(java.lang.Class<?> k){
            if (k == ExternalIdentifier.getClass()){
                return ExternalIdentifier;
            }
            if (k == Source.getClass()){
                return Source;
            }
            if (k == InsurancePlan.getClass()){
                return InsurancePlan;
            }
            if (k == DimensionalQuantity.getClass()){
                return DimensionalQuantity;
            }
            if (k == SocialEntity.getClass()){
                return SocialEntity;
            }
            if (k == MeasurmentValue.getClass()){
                return MeasurmentValue;
            }
            if (k == Quality.getClass()){
                return Quality;
            }
            if (k == Provider.getClass()){
                return Provider;
            }
            if (k == SocialQuality.getClass()){
                return SocialQuality;
            }
            if (k == Collective.getClass()){
                return Collective;
            }
            if (k == CMSValidationError.getClass()){
                return CMSValidationError;
            }
            if (k == Corporation.getClass()){
                return Corporation;
            }
            if (k == HealthCarePayor.getClass()){
                return HealthCarePayor;
            }
            if (k == PreferredProviderOrganizationPlan.getClass()){
                return PreferredProviderOrganizationPlan;
            }
            if (k == RiskAdjustmentInsuranceClaim.getClass()){
                return RiskAdjustmentInsuranceClaim;
            }
            if (k == FeeForServiceInsuranceClaim.getClass()){
                return FeeForServiceInsuranceClaim;
            }
            if (k == Patient.getClass()){
                return Patient;
            }
            if (k == File.getClass()){
                return File;
            }
            if (k == Problem.getClass()){
                return Problem;
            }
            if (k == MedicareInsurancePolicy.getClass()){
                return MedicareInsurancePolicy;
            }
            if (k == ResolutionStatusType.getClass()){
                return ResolutionStatusType;
            }
            if (k == InsurancePolicy.getClass()){
                return InsurancePolicy;
            }
            if (k == LabFlagType.getClass()){
                return LabFlagType;
            }
            if (k == SourceType.getClass()){
                return SourceType;
            }
            if (k == AnatomicalEntity.getClass()){
                return AnatomicalEntity;
            }
            if (k == CareSiteOrganization.getClass()){
                return CareSiteOrganization;
            }
            if (k == Entity.getClass()){
                return Entity;
            }
            if (k == FamilyHistory.getClass()){
                return FamilyHistory;
            }
            if (k == Document.getClass()){
                return Document;
            }
            if (k == PatientEncounterType.getClass()){
                return PatientEncounterType;
            }
            if (k == Provenance.getClass()){
                return Provenance;
            }
            if (k == RAPSValidationError.getClass()){
                return RAPSValidationError;
            }
            if (k == DataProcessingDetail.getClass()){
                return DataProcessingDetail;
            }
            if (k == InsuranceClaimableEvent.getClass()){
                return InsuranceClaimableEvent;
            }
            if (k == InsuranceClaimStatusType.getClass()){
                return InsuranceClaimStatusType;
            }
            if (k == HICN.getClass()){
                return HICN;
            }
            if (k == InsuranceCompany.getClass()){
                return InsuranceCompany;
            }
            if (k == PositionalIdentifer.getClass()){
                return PositionalIdentifer;
            }
            if (k == ClinicalCode.getClass()){
                return ClinicalCode;
            }
            if (k == Observation.getClass()){
                return Observation;
            }
            if (k == InputDataType.getClass()){
                return InputDataType;
            }
            if (k == GenderType.getClass()){
                return GenderType;
            }
            if (k == Date.getClass()){
                return Date;
            }
            if (k == Processor.getClass()){
                return Processor;
            }
            if (k == PatientEncounter.getClass()){
                return PatientEncounter;
            }
            if (k == MedicalCodeEntity.getClass()){
                return MedicalCodeEntity;
            }
            if (k == ProvenancedEntity.getClass()){
                return ProvenancedEntity;
            }
            if (k == InformationalEntityIdentifier.getClass()){
                return InformationalEntityIdentifier;
            }
            if (k == ChemicalEntity.getClass()){
                return ChemicalEntity;
            }
            if (k == MedicalQuality.getClass()){
                return MedicalQuality;
            }
            if (k == ClinicalRoleType.getClass()){
                return ClinicalRoleType;
            }
            if (k == Role.getClass()){
                return Role;
            }
            if (k == Function.getClass()){
                return Function;
            }
            if (k == RealizableEntity.getClass()){
                return RealizableEntity;
            }
            if (k == FeeForServiceInsuranceClaimType.getClass()){
                return FeeForServiceInsuranceClaimType;
            }
            if (k == BiologicalQuality.getClass()){
                return BiologicalQuality;
            }
            if (k == MedicalCondition.getClass()){
                return MedicalCondition;
            }
            if (k == CMSClaimValidationResult.getClass()){
                return CMSClaimValidationResult;
            }
            if (k == DocumentType.getClass()){
                return DocumentType;
            }
            if (k == ClinicalCodingSystem.getClass()){
                return ClinicalCodingSystem;
            }
            if (k == SocialHistory.getClass()){
                return SocialHistory;
            }
            if (k == EncounterDocument.getClass()){
                return EncounterDocument;
            }
            if (k == PreferedProviderOrganization.getClass()){
                return PreferedProviderOrganization;
            }
            if (k == Identifier.getClass()){
                return Identifier;
            }
            if (k == Human.getClass()){
                return Human;
            }
            if (k == Illness.getClass()){
                return Illness;
            }
            if (k == Label.getClass()){
                return Label;
            }
            if (k == ContactDetails.getClass()){
                return ContactDetails;
            }
            if (k == MedicalProfessional.getClass()){
                return MedicalProfessional;
            }
            if (k == PrimaryCareProvider.getClass()){
                return PrimaryCareProvider;
            }
            if (k == ApixioDictionary.getClass()){
                return ApixioDictionary;
            }
            if (k == TelephoneNumber.getClass()){
                return TelephoneNumber;
            }
            if (k == DataTransformation.getClass()){
                return DataTransformation;
            }
            if (k == DateType.getClass()){
                return DateType;
            }
            if (k == MedicalEvent.getClass()){
                return MedicalEvent;
            }
            if (k == InformationProcessing.getClass()){
                return InformationProcessing;
            }
            if (k == MeasurmentQuality.getClass()){
                return MeasurmentQuality;
            }
            if (k == InformationContentQuality.getClass()){
                return InformationContentQuality;
            }
            if (k == Operation.getClass()){
                return Operation;
            }
            if (k == SpatialRegion.getClass()){
                return SpatialRegion;
            }
            if (k == PatientHistory.getClass()){
                return PatientHistory;
            }
            if (k == BiologicalEntity.getClass()){
                return BiologicalEntity;
            }
            if (k == MaterialEntity.getClass()){
                return MaterialEntity;
            }
            if (k == DateRange.getClass()){
                return DateRange;
            }
            if (k == RiskAdjustmentInsuranceClaimType.getClass()){
                return RiskAdjustmentInsuranceClaimType;
            }
            if (k == InsuranceClaim.getClass()){
                return InsuranceClaim;
            }
            if (k == ImageRendering.getClass()){
                return ImageRendering;
            }
            if (k == ProcessorType.getClass()){
                return ProcessorType;
            }
            if (k == MaritalStatusType.getClass()){
                return MaritalStatusType;
            }
            if (k == HealthMaintenanceOrganizationPlan.getClass()){
                return HealthMaintenanceOrganizationPlan;
            }
            if (k == Contract.getClass()){
                return Contract;
            }
            if (k == Invoice.getClass()){
                return Invoice;
            }
            if (k == EDPSValidationError.getClass()){
                return EDPSValidationError;
            }
            if (k == OperationAnnotationType.getClass()){
                return OperationAnnotationType;
            }
            if (k == ApixioDate.getClass()){
                return ApixioDate;
            }
            if (k == SoftwareEntity.getClass()){
                return SoftwareEntity;
            }
            if (k == TimeMeasurment.getClass()){
                return TimeMeasurment;
            }
            if (k == TimeBoundExternalIdentifier.getClass()){
                return TimeBoundExternalIdentifier;
            }
            if (k == Page.getClass()){
                return Page;
            }
            if (k == TextRendering.getClass()){
                return TextRendering;
            }
            if (k == StatusAnnotationType.getClass()){
                return StatusAnnotationType;
            }
            if (k == Demographics.getClass()){
                return Demographics;
            }
            if (k == Person.getClass()){
                return Person;
            }
            if (k == InformationContentEntity.getClass()){
                return InformationContentEntity;
            }
            if (k == Attribute.getClass()){
                return Attribute;
            }
            if (k == ApixioProcess.getClass()){
                return ApixioProcess;
            }
            if (k == ApixioObject.getClass()){
                return ApixioObject;
            }
            if (k == InsuranceClaimErrorCodeFilter.getClass()){
                return InsuranceClaimErrorCodeFilter;
            }
            if (k == ComputationalEntity.getClass()){
                return ComputationalEntity;
            }
            if (k == MathematicalEntity.getClass()){
                return MathematicalEntity;
            }
            if (k == MedicalEntity.getClass()){
                return MedicalEntity;
            }
            if (k == Address.getClass()){
                return Address;
            }
            if (k == TelephoneType.getClass()){
                return TelephoneType;
            }
            if (k == CareSite.getClass()){
                return CareSite;
            }
            if (k == TextualEntity.getClass()){
                return TextualEntity;
            }
            if (k == PatientEncounterSwitchType.getClass()){
                return PatientEncounterSwitchType;
            }
            if (k == Allergy.getClass()){
                return Allergy;
            }
            if (k == Prescription.getClass()){
                return Prescription;
            }
            if (k == AddressType.getClass()){
                return AddressType;
            }
            if (k == Medication.getClass()){
                return Medication;
            }
            if (k == Rendering.getClass()){
                return Rendering;
            }
            if (k == MedicareOrganization.getClass()){
                return MedicareOrganization;
            }
            if (k == Immunization.getClass()){
                return Immunization;
            }
            if (k == PlanType.getClass()){
                return PlanType;
            }
            if (k == BiometricValue.getClass()){
                return BiometricValue;
            }
            if (k == CareSiteType.getClass()){
                return CareSiteType;
            }
            if (k == Quantity.getClass()){
                return Quantity;
            }
            if (k == DimensionlessQuantity.getClass()){
                return DimensionlessQuantity;
            }
            if (k == Event.getClass()){
                return Event;
            }
            if (k == LabResult.getClass()){
                return LabResult;
            }
            if (k == Medicare.getClass()){
                return Medicare;
            }
            if (k == SignatureType.getClass()){
                return SignatureType;
            }
            if (k == Organization.getClass()){
                return Organization;
            }
            if (k == CodedEntity.getClass()){
                return CodedEntity;
            }
            if (k == Procedure.getClass()){
                return Procedure;
            }
            if (k == InformationalQuality.getClass()){
                return InformationalQuality;
            }
            if (k == Name.getClass()){
                return Name;
            }
            if (k == HealthMaintenanceOrganization.getClass()){
                return HealthMaintenanceOrganization;
            }
            if (k == AmendmentAnnotation.getClass()){
                return AmendmentAnnotation;
            }
            if (k == NameType.getClass()){
                return NameType;
            }
            return null;
        }
        public com.hp.hpl.jena.ontology.OntClass resource() {
            synchronized(model) {
                return model.createClass(uri);
            }
        }
    };
    public static enum OWLObjectProperty{
        HasGroupNumber(ONTOLOGY_NAMESPACE + "alo#000374"),
        HasMemberNumber(ONTOLOGY_NAMESPACE + "alo#000373"),
        HasMedicalProcedure(ONTOLOGY_NAMESPACE + "alo#000032"),
        HasCareSiteType(ONTOLOGY_NAMESPACE + "alo#810018"),
        HasNameType(ONTOLOGY_NAMESPACE + "alo#000637"),
        HasFeeForServiceClaimTypeValue(ONTOLOGY_NAMESPACE + "alo#000223"),
        HasRenderingProvider(ONTOLOGY_NAMESPACE + "alo#000221"),
        HasSourceAuthor(ONTOLOGY_NAMESPACE + "alo#000166"),
        HasAuthor(ONTOLOGY_NAMESPACE + "alo#000165"),
        HasDateOfServiceRange(ONTOLOGY_NAMESPACE + "alo#000162"),
        HasServicesRendered(ONTOLOGY_NAMESPACE + "alo#000161"),
        HasAddress(ONTOLOGY_NAMESPACE + "alo#000160"),
        DerivesInto(ONTOLOGY_NAMESPACE + "alo#9000027"),
        HasRace(ONTOLOGY_NAMESPACE + "alo#000429"),
        HasAlternateDemographics(ONTOLOGY_NAMESPACE + "alo#000428"),
        HasAlternateTelephoneNumber(ONTOLOGY_NAMESPACE + "alo#000426"),
        HasAlternateContactDetails(ONTOLOGY_NAMESPACE + "alo#000425"),
        HasContactDetails(ONTOLOGY_NAMESPACE + "alo#000424"),
        HasTelephoneNumberType(ONTOLOGY_NAMESPACE + "alo#000423"),
        HasHICNNumber(ONTOLOGY_NAMESPACE + "alo#000453"),
        HasDocumentDate(ONTOLOGY_NAMESPACE + "alo#000367"),
        HasTelephoneNumber(ONTOLOGY_NAMESPACE + "alo#000420"),
        HasDemographics(ONTOLOGY_NAMESPACE + "alo#000365"),
        HasPatientControlNumber(ONTOLOGY_NAMESPACE + "alo#000217"),
        HasBillType(ONTOLOGY_NAMESPACE + "alo#000216"),
        HasSupportingDiagnosis(ONTOLOGY_NAMESPACE + "alo#000030"),
        HasProviderType(ONTOLOGY_NAMESPACE + "alo#000214"),
        HasDiagnosisCode(ONTOLOGY_NAMESPACE + "alo#000213"),
        HasValidatedClaimTransactionDate(ONTOLOGY_NAMESPACE + "alo#000212"),
        HasRiskAdjustmentInsuranceClaimType(ONTOLOGY_NAMESPACE + "alo#000211"),
        HasNationalProviderIdentifier(ONTOLOGY_NAMESPACE + "alo#000155"),
        HasDataProcessingDetail(ONTOLOGY_NAMESPACE + "alo#000098"),
        HasPage(ONTOLOGY_NAMESPACE + "alo#000008"),
        HasAllergyResolvedDate(ONTOLOGY_NAMESPACE + "alo#000006"),
        HasCodedEntityClinicalCode(ONTOLOGY_NAMESPACE + "alo#000093"),
        HasAllergyReactionDate(ONTOLOGY_NAMESPACE + "alo#000004"),
        HasAlternateAddress(ONTOLOGY_NAMESPACE + "alo#000416"),
        HasPrimaryAddress(ONTOLOGY_NAMESPACE + "alo#000415"),
        HasProblemResolutionDate(ONTOLOGY_NAMESPACE + "alo#000001"),
        HasSourceType(ONTOLOGY_NAMESPACE + "alo#000194"),
        HasRendering(ONTOLOGY_NAMESPACE + "alo#000355"),
        RefersToInternalObject(ONTOLOGY_NAMESPACE + "alo#000197"),
        HasSourceCreationDate(ONTOLOGY_NAMESPACE + "alo#151151"),
        HasClaimedEvent(ONTOLOGY_NAMESPACE + "alo#250025"),
        HasSourceOrganization(ONTOLOGY_NAMESPACE + "alo#000204"),
        HasPatientEncounterSwitchType(ONTOLOGY_NAMESPACE + "alo#000202"),
        HasClaimOverpaymentIdentifier(ONTOLOGY_NAMESPACE + "alo#000200"),
        HasEncounter(ONTOLOGY_NAMESPACE + "alo#000144"),
        HasDocumentPage(ONTOLOGY_NAMESPACE + "alo#000143"),
        HasDateRangeForCareOfPatient(ONTOLOGY_NAMESPACE + "alo#000142"),
        HasStatusAnnotationType(ONTOLOGY_NAMESPACE + "alo#490049"),
        HasInsuranceClaimStatus(ONTOLOGY_NAMESPACE + "alo#000140"),
        HasAlternateName(ONTOLOGY_NAMESPACE + "alo#000085"),
        HasAmendment(ONTOLOGY_NAMESPACE + "alo#048048"),
        HasDocumentCareSite(ONTOLOGY_NAMESPACE + "alo#000083"),
        HasSampleDate(ONTOLOGY_NAMESPACE + "alo#000494"),
        HasChiefComplaint(ONTOLOGY_NAMESPACE + "alo#200020"),
        HasLabSuperPanel(ONTOLOGY_NAMESPACE + "alo#000492"),
        HasLabResultPart(ONTOLOGY_NAMESPACE + "alo#000491"),
        HasLabPanel(ONTOLOGY_NAMESPACE + "alo#000490"),
        HasClinicalCodingSystem(ONTOLOGY_NAMESPACE + "alo#000401"),
        HasMaritalStatusType(ONTOLOGY_NAMESPACE + "alo#000346"),
        HasFeeForServiceClaim(ONTOLOGY_NAMESPACE + "alo#000150"),
        IsDerivedFrom(ONTOLOGY_NAMESPACE + "alo#9000031"),
        HasDrug(ONTOLOGY_NAMESPACE + "alo#110011"),
        HasPatientEncounterCareSite(ONTOLOGY_NAMESPACE + "alo#000067"),
        HasFileDate(ONTOLOGY_NAMESPACE + "alo#000068"),
        HasLabResultCareSite(ONTOLOGY_NAMESPACE + "alo#000069"),
        HasInsuranceClaim(ONTOLOGY_NAMESPACE + "alo#000137"),
        HasPatientCareSite(ONTOLOGY_NAMESPACE + "alo#000078"),
        HasDocumentAuthor(ONTOLOGY_NAMESPACE + "alo#000077"),
        HasSocialHistory(ONTOLOGY_NAMESPACE + "alo#000076"),
        HasFamilyHistory(ONTOLOGY_NAMESPACE + "alo#000073"),
        HasCareSite(ONTOLOGY_NAMESPACE + "alo#000071"),
        HasAllergyDiagnosisDate(ONTOLOGY_NAMESPACE + "alo#000480"),
        HasGenderType(ONTOLOGY_NAMESPACE + "alo#000015"),
        HasSource(ONTOLOGY_NAMESPACE + "alo#000094"),
        HasProblemDateRange(ONTOLOGY_NAMESPACE + "alo#000468"),
        HasEncounterAgent(ONTOLOGY_NAMESPACE + "alo#000332"),
        HasImmunizationMedication(ONTOLOGY_NAMESPACE + "alo#120012"),
        HasSourceFile(ONTOLOGY_NAMESPACE + "alo#000167"),
        IsRefferedToBy(ONTOLOGY_NAMESPACE + "alo#9000035"),
        HasFile(ONTOLOGY_NAMESPACE + "alo#000128"),
        HasClinicalCode(ONTOLOGY_NAMESPACE + "alo#000208"),
        RefersTo(ONTOLOGY_NAMESPACE + "alo#9000036"),
        HasImageRendering(ONTOLOGY_NAMESPACE + "alo#000123"),
        HasContractIdentifier(ONTOLOGY_NAMESPACE + "alo#000122"),
        RefersToPatient(ONTOLOGY_NAMESPACE + "alo#000121"),
        HasDataProcessingDetailDate(ONTOLOGY_NAMESPACE + "alo#000120"),
        HasEndDate(ONTOLOGY_NAMESPACE + "alo#000378"),
        HasSocialHistoryType(ONTOLOGY_NAMESPACE + "alo#000064"),
        HasBiometricValue(ONTOLOGY_NAMESPACE + "alo#000476"),
        HasName(ONTOLOGY_NAMESPACE + "alo#000062"),
        HasPrescription(ONTOLOGY_NAMESPACE + "alo#000473"),
        HasAddressType(ONTOLOGY_NAMESPACE + "alo#000414"),
        HasSpecimen(ONTOLOGY_NAMESPACE + "alo#000489"),
        HasProblem(ONTOLOGY_NAMESPACE + "alo#000472"),
        HasImmunizationAdminDate(ONTOLOGY_NAMESPACE + "alo#110010"),
        HasAmendmentAnnotation(ONTOLOGY_NAMESPACE + "alo#044044"),
        HasPatientEncounterType(ONTOLOGY_NAMESPACE + "alo#000019"),
        HasDate(ONTOLOGY_NAMESPACE + "alo#000049"),
        HasAllergy(ONTOLOGY_NAMESPACE + "alo#000479"),
        HasCMSValidationError(ONTOLOGY_NAMESPACE + "alo#000249"),
        HasClinicalRole(ONTOLOGY_NAMESPACE + "alo#000084"),
        HasTextRendering(ONTOLOGY_NAMESPACE + "alo#000118"),
        HasMedicalProfessional(ONTOLOGY_NAMESPACE + "alo#000086"),
        HasInsuranceClaimCareSite(ONTOLOGY_NAMESPACE + "alo#000116"),
        HasDocument(ONTOLOGY_NAMESPACE + "alo#000231"),
        IsRelatedTo(ONTOLOGY_NAMESPACE + "alo#9000026"),
        HasSocialHistoryDate(ONTOLOGY_NAMESPACE + "alo#000058"),
        HasInsuranceCompany(ONTOLOGY_NAMESPACE + "alo#000057"),
        HasParticipant(ONTOLOGY_NAMESPACE + "alo#9000029"),
        HasIdentifier(ONTOLOGY_NAMESPACE + "alo#000055"),
        HasRiskAdjustmentClaim(ONTOLOGY_NAMESPACE + "alo#000141"),
        HasLastEditDate(ONTOLOGY_NAMESPACE + "alo#000146"),
        HasInsuranceClaimProcessingDate(ONTOLOGY_NAMESPACE + "alo#000148"),
        HasInputDataType(ONTOLOGY_NAMESPACE + "alo#000190"),
        HasOperationAnnotationType(ONTOLOGY_NAMESPACE + "alo#050050"),
        HasMedicalProcedureDateRange(ONTOLOGY_NAMESPACE + "alo#000502"),
        HasImmunization(ONTOLOGY_NAMESPACE + "alo#610006"),
        IsStoredAs(ONTOLOGY_NAMESPACE + "alo#000091"),
        HasOtherOriginalIDs(ONTOLOGY_NAMESPACE + "alo#000092"),
        IsAttributeOf(ONTOLOGY_NAMESPACE + "alo#9000030"),
        HasCMSClaimValidationResult(ONTOLOGY_NAMESPACE + "alo#000250"),
        HasInsurancePlanIdentifier(ONTOLOGY_NAMESPACE + "alo#000106"),
        HasMeasurmentResultDate(ONTOLOGY_NAMESPACE + "alo#000477"),
        HasProcessorType(ONTOLOGY_NAMESPACE + "alo#000103"),
        HasInsuranceType(ONTOLOGY_NAMESPACE + "alo#000048"),
        HasAttribute(ONTOLOGY_NAMESPACE + "alo#9000028"),
        HasInsurancePolicy(ONTOLOGY_NAMESPACE + "alo#000457"),
        HasInsurance(ONTOLOGY_NAMESPACE + "alo#000456"),
        HasImmunizationDateRange(ONTOLOGY_NAMESPACE + "alo#770007"),
        HasAgent(ONTOLOGY_NAMESPACE + "alo#9000050"),
        HasInsurancePolicyDateRange(ONTOLOGY_NAMESPACE + "alo#000452"),
        HasDateType(ONTOLOGY_NAMESPACE + "alo#000303"),
        HasTimeBoundIdentifierDateRange(ONTOLOGY_NAMESPACE + "alo#000016"),
        HasLabResult(ONTOLOGY_NAMESPACE + "alo#000486"),
        HasProcessor(ONTOLOGY_NAMESPACE + "alo#000185"),
        HasClaimSubmissionDate(ONTOLOGY_NAMESPACE + "alo#032024"),
        HasInsurancePlan(ONTOLOGY_NAMESPACE + "alo#000036"),
        HasAllergenCode(ONTOLOGY_NAMESPACE + "alo#000035"),
        IsPartOf(ONTOLOGY_NAMESPACE + "alo#9000044"),
        HasPart(ONTOLOGY_NAMESPACE + "alo#9000043"),
        IsParameterOf(ONTOLOGY_NAMESPACE + "alo#9000042"),
        HasParameter(ONTOLOGY_NAMESPACE + "alo#9000041"),
        IsOutputOf(ONTOLOGY_NAMESPACE + "alo#9000040"),
        HasPrescriptionDate(ONTOLOGY_NAMESPACE + "alo#000441"),
        HasPrescriptionEndDate(ONTOLOGY_NAMESPACE + "alo#000440"),
        HasDateOfDeath(ONTOLOGY_NAMESPACE + "alo#000384"),
        HasDateOfBirth(ONTOLOGY_NAMESPACE + "alo#000383"),
        HasLabFlagType(ONTOLOGY_NAMESPACE + "alo#000021"),
        HasLabResultSampleDate(ONTOLOGY_NAMESPACE + "alo#000022"),
        HasBodySite(ONTOLOGY_NAMESPACE + "alo#000029"),
        HasEncounterDateRange(ONTOLOGY_NAMESPACE + "alo#000366"),
        HasBillingProvider(ONTOLOGY_NAMESPACE + "alo#000220"),
        HasOriginalID(ONTOLOGY_NAMESPACE + "alo#000090"),
        HasOrganization(ONTOLOGY_NAMESPACE + "alo#149149"),
        HasSignatureDate(ONTOLOGY_NAMESPACE + "alo#000172"),
        IsInputOf(ONTOLOGY_NAMESPACE + "alo#9000039"),
        HasOutput(ONTOLOGY_NAMESPACE + "alo#9000038"),
        HasInput(ONTOLOGY_NAMESPACE + "alo#9000037"),
        HasPrescriptionFillDate(ONTOLOGY_NAMESPACE + "alo#000439"),
        HasResolutionStatusType(ONTOLOGY_NAMESPACE + "alo#000025"),
        IsParticipantIn(ONTOLOGY_NAMESPACE + "alo#9000034"),
        IsLocationOf(ONTOLOGY_NAMESPACE + "alo#9000033"),
        IsLocatedIn(ONTOLOGY_NAMESPACE + "alo#9000032"),
        HasPrescribedDrug(ONTOLOGY_NAMESPACE + "alo#000434"),
        HasDateRange(ONTOLOGY_NAMESPACE + "alo#000379"),
        HasPrimaryCareProvider(ONTOLOGY_NAMESPACE + "alo#000432"),
        HasStartDate(ONTOLOGY_NAMESPACE + "alo#000377"),
        HasEthnicity(ONTOLOGY_NAMESPACE + "alo#000430"),
        HasSubscriberID(ONTOLOGY_NAMESPACE + "alo#000375")
        ;

        private final String uri;
        private OWLObjectProperty(String uri){ this.uri = uri;}
        public String toString() { return uri; }
        public String uri() { return uri; }
        public static OWLObjectProperty getByClass(java.lang.Class<?> k){
            if (k == HasGroupNumber.getClass()){
                return HasGroupNumber;
            }
            if (k == HasMemberNumber.getClass()){
                return HasMemberNumber;
            }
            if (k == HasMedicalProcedure.getClass()){
                return HasMedicalProcedure;
            }
            if (k == HasCareSiteType.getClass()){
                return HasCareSiteType;
            }
            if (k == HasNameType.getClass()){
                return HasNameType;
            }
            if (k == HasFeeForServiceClaimTypeValue.getClass()){
                return HasFeeForServiceClaimTypeValue;
            }
            if (k == HasRenderingProvider.getClass()){
                return HasRenderingProvider;
            }
            if (k == HasSourceAuthor.getClass()){
                return HasSourceAuthor;
            }
            if (k == HasAuthor.getClass()){
                return HasAuthor;
            }
            if (k == HasDateOfServiceRange.getClass()){
                return HasDateOfServiceRange;
            }
            if (k == HasServicesRendered.getClass()){
                return HasServicesRendered;
            }
            if (k == HasAddress.getClass()){
                return HasAddress;
            }
            if (k == DerivesInto.getClass()){
                return DerivesInto;
            }
            if (k == HasRace.getClass()){
                return HasRace;
            }
            if (k == HasAlternateDemographics.getClass()){
                return HasAlternateDemographics;
            }
            if (k == HasAlternateTelephoneNumber.getClass()){
                return HasAlternateTelephoneNumber;
            }
            if (k == HasAlternateContactDetails.getClass()){
                return HasAlternateContactDetails;
            }
            if (k == HasContactDetails.getClass()){
                return HasContactDetails;
            }
            if (k == HasTelephoneNumberType.getClass()){
                return HasTelephoneNumberType;
            }
            if (k == HasHICNNumber.getClass()){
                return HasHICNNumber;
            }
            if (k == HasDocumentDate.getClass()){
                return HasDocumentDate;
            }
            if (k == HasTelephoneNumber.getClass()){
                return HasTelephoneNumber;
            }
            if (k == HasDemographics.getClass()){
                return HasDemographics;
            }
            if (k == HasPatientControlNumber.getClass()){
                return HasPatientControlNumber;
            }
            if (k == HasBillType.getClass()){
                return HasBillType;
            }
            if (k == HasSupportingDiagnosis.getClass()){
                return HasSupportingDiagnosis;
            }
            if (k == HasProviderType.getClass()){
                return HasProviderType;
            }
            if (k == HasDiagnosisCode.getClass()){
                return HasDiagnosisCode;
            }
            if (k == HasValidatedClaimTransactionDate.getClass()){
                return HasValidatedClaimTransactionDate;
            }
            if (k == HasRiskAdjustmentInsuranceClaimType.getClass()){
                return HasRiskAdjustmentInsuranceClaimType;
            }
            if (k == HasNationalProviderIdentifier.getClass()){
                return HasNationalProviderIdentifier;
            }
            if (k == HasDataProcessingDetail.getClass()){
                return HasDataProcessingDetail;
            }
            if (k == HasPage.getClass()){
                return HasPage;
            }
            if (k == HasAllergyResolvedDate.getClass()){
                return HasAllergyResolvedDate;
            }
            if (k == HasCodedEntityClinicalCode.getClass()){
                return HasCodedEntityClinicalCode;
            }
            if (k == HasAllergyReactionDate.getClass()){
                return HasAllergyReactionDate;
            }
            if (k == HasAlternateAddress.getClass()){
                return HasAlternateAddress;
            }
            if (k == HasPrimaryAddress.getClass()){
                return HasPrimaryAddress;
            }
            if (k == HasProblemResolutionDate.getClass()){
                return HasProblemResolutionDate;
            }
            if (k == HasSourceType.getClass()){
                return HasSourceType;
            }
            if (k == HasRendering.getClass()){
                return HasRendering;
            }
            if (k == RefersToInternalObject.getClass()){
                return RefersToInternalObject;
            }
            if (k == HasSourceCreationDate.getClass()){
                return HasSourceCreationDate;
            }
            if (k == HasClaimedEvent.getClass()){
                return HasClaimedEvent;
            }
            if (k == HasSourceOrganization.getClass()){
                return HasSourceOrganization;
            }
            if (k == HasPatientEncounterSwitchType.getClass()){
                return HasPatientEncounterSwitchType;
            }
            if (k == HasClaimOverpaymentIdentifier.getClass()){
                return HasClaimOverpaymentIdentifier;
            }
            if (k == HasEncounter.getClass()){
                return HasEncounter;
            }
            if (k == HasDocumentPage.getClass()){
                return HasDocumentPage;
            }
            if (k == HasDateRangeForCareOfPatient.getClass()){
                return HasDateRangeForCareOfPatient;
            }
            if (k == HasStatusAnnotationType.getClass()){
                return HasStatusAnnotationType;
            }
            if (k == HasInsuranceClaimStatus.getClass()){
                return HasInsuranceClaimStatus;
            }
            if (k == HasAlternateName.getClass()){
                return HasAlternateName;
            }
            if (k == HasAmendment.getClass()){
                return HasAmendment;
            }
            if (k == HasDocumentCareSite.getClass()){
                return HasDocumentCareSite;
            }
            if (k == HasSampleDate.getClass()){
                return HasSampleDate;
            }
            if (k == HasChiefComplaint.getClass()){
                return HasChiefComplaint;
            }
            if (k == HasLabSuperPanel.getClass()){
                return HasLabSuperPanel;
            }
            if (k == HasLabResultPart.getClass()){
                return HasLabResultPart;
            }
            if (k == HasLabPanel.getClass()){
                return HasLabPanel;
            }
            if (k == HasClinicalCodingSystem.getClass()){
                return HasClinicalCodingSystem;
            }
            if (k == HasMaritalStatusType.getClass()){
                return HasMaritalStatusType;
            }
            if (k == HasFeeForServiceClaim.getClass()){
                return HasFeeForServiceClaim;
            }
            if (k == IsDerivedFrom.getClass()){
                return IsDerivedFrom;
            }
            if (k == HasDrug.getClass()){
                return HasDrug;
            }
            if (k == HasPatientEncounterCareSite.getClass()){
                return HasPatientEncounterCareSite;
            }
            if (k == HasFileDate.getClass()){
                return HasFileDate;
            }
            if (k == HasLabResultCareSite.getClass()){
                return HasLabResultCareSite;
            }
            if (k == HasInsuranceClaim.getClass()){
                return HasInsuranceClaim;
            }
            if (k == HasPatientCareSite.getClass()){
                return HasPatientCareSite;
            }
            if (k == HasDocumentAuthor.getClass()){
                return HasDocumentAuthor;
            }
            if (k == HasSocialHistory.getClass()){
                return HasSocialHistory;
            }
            if (k == HasFamilyHistory.getClass()){
                return HasFamilyHistory;
            }
            if (k == HasCareSite.getClass()){
                return HasCareSite;
            }
            if (k == HasAllergyDiagnosisDate.getClass()){
                return HasAllergyDiagnosisDate;
            }
            if (k == HasGenderType.getClass()){
                return HasGenderType;
            }
            if (k == HasSource.getClass()){
                return HasSource;
            }
            if (k == HasProblemDateRange.getClass()){
                return HasProblemDateRange;
            }
            if (k == HasEncounterAgent.getClass()){
                return HasEncounterAgent;
            }
            if (k == HasImmunizationMedication.getClass()){
                return HasImmunizationMedication;
            }
            if (k == HasSourceFile.getClass()){
                return HasSourceFile;
            }
            if (k == IsRefferedToBy.getClass()){
                return IsRefferedToBy;
            }
            if (k == HasFile.getClass()){
                return HasFile;
            }
            if (k == HasClinicalCode.getClass()){
                return HasClinicalCode;
            }
            if (k == RefersTo.getClass()){
                return RefersTo;
            }
            if (k == HasImageRendering.getClass()){
                return HasImageRendering;
            }
            if (k == HasContractIdentifier.getClass()){
                return HasContractIdentifier;
            }
            if (k == RefersToPatient.getClass()){
                return RefersToPatient;
            }
            if (k == HasDataProcessingDetailDate.getClass()){
                return HasDataProcessingDetailDate;
            }
            if (k == HasEndDate.getClass()){
                return HasEndDate;
            }
            if (k == HasSocialHistoryType.getClass()){
                return HasSocialHistoryType;
            }
            if (k == HasBiometricValue.getClass()){
                return HasBiometricValue;
            }
            if (k == HasName.getClass()){
                return HasName;
            }
            if (k == HasPrescription.getClass()){
                return HasPrescription;
            }
            if (k == HasAddressType.getClass()){
                return HasAddressType;
            }
            if (k == HasSpecimen.getClass()){
                return HasSpecimen;
            }
            if (k == HasProblem.getClass()){
                return HasProblem;
            }
            if (k == HasImmunizationAdminDate.getClass()){
                return HasImmunizationAdminDate;
            }
            if (k == HasAmendmentAnnotation.getClass()){
                return HasAmendmentAnnotation;
            }
            if (k == HasPatientEncounterType.getClass()){
                return HasPatientEncounterType;
            }
            if (k == HasDate.getClass()){
                return HasDate;
            }
            if (k == HasAllergy.getClass()){
                return HasAllergy;
            }
            if (k == HasCMSValidationError.getClass()){
                return HasCMSValidationError;
            }
            if (k == HasClinicalRole.getClass()){
                return HasClinicalRole;
            }
            if (k == HasTextRendering.getClass()){
                return HasTextRendering;
            }
            if (k == HasMedicalProfessional.getClass()){
                return HasMedicalProfessional;
            }
            if (k == HasInsuranceClaimCareSite.getClass()){
                return HasInsuranceClaimCareSite;
            }
            if (k == HasDocument.getClass()){
                return HasDocument;
            }
            if (k == IsRelatedTo.getClass()){
                return IsRelatedTo;
            }
            if (k == HasSocialHistoryDate.getClass()){
                return HasSocialHistoryDate;
            }
            if (k == HasInsuranceCompany.getClass()){
                return HasInsuranceCompany;
            }
            if (k == HasParticipant.getClass()){
                return HasParticipant;
            }
            if (k == HasIdentifier.getClass()){
                return HasIdentifier;
            }
            if (k == HasRiskAdjustmentClaim.getClass()){
                return HasRiskAdjustmentClaim;
            }
            if (k == HasLastEditDate.getClass()){
                return HasLastEditDate;
            }
            if (k == HasInsuranceClaimProcessingDate.getClass()){
                return HasInsuranceClaimProcessingDate;
            }
            if (k == HasInputDataType.getClass()){
                return HasInputDataType;
            }
            if (k == HasOperationAnnotationType.getClass()){
                return HasOperationAnnotationType;
            }
            if (k == HasMedicalProcedureDateRange.getClass()){
                return HasMedicalProcedureDateRange;
            }
            if (k == HasImmunization.getClass()){
                return HasImmunization;
            }
            if (k == IsStoredAs.getClass()){
                return IsStoredAs;
            }
            if (k == HasOtherOriginalIDs.getClass()){
                return HasOtherOriginalIDs;
            }
            if (k == IsAttributeOf.getClass()){
                return IsAttributeOf;
            }
            if (k == HasCMSClaimValidationResult.getClass()){
                return HasCMSClaimValidationResult;
            }
            if (k == HasInsurancePlanIdentifier.getClass()){
                return HasInsurancePlanIdentifier;
            }
            if (k == HasMeasurmentResultDate.getClass()){
                return HasMeasurmentResultDate;
            }
            if (k == HasProcessorType.getClass()){
                return HasProcessorType;
            }
            if (k == HasInsuranceType.getClass()){
                return HasInsuranceType;
            }
            if (k == HasAttribute.getClass()){
                return HasAttribute;
            }
            if (k == HasInsurancePolicy.getClass()){
                return HasInsurancePolicy;
            }
            if (k == HasInsurance.getClass()){
                return HasInsurance;
            }
            if (k == HasImmunizationDateRange.getClass()){
                return HasImmunizationDateRange;
            }
            if (k == HasAgent.getClass()){
                return HasAgent;
            }
            if (k == HasInsurancePolicyDateRange.getClass()){
                return HasInsurancePolicyDateRange;
            }
            if (k == HasDateType.getClass()){
                return HasDateType;
            }
            if (k == HasTimeBoundIdentifierDateRange.getClass()){
                return HasTimeBoundIdentifierDateRange;
            }
            if (k == HasLabResult.getClass()){
                return HasLabResult;
            }
            if (k == HasProcessor.getClass()){
                return HasProcessor;
            }
            if (k == HasClaimSubmissionDate.getClass()){
                return HasClaimSubmissionDate;
            }
            if (k == HasInsurancePlan.getClass()){
                return HasInsurancePlan;
            }
            if (k == HasAllergenCode.getClass()){
                return HasAllergenCode;
            }
            if (k == IsPartOf.getClass()){
                return IsPartOf;
            }
            if (k == HasPart.getClass()){
                return HasPart;
            }
            if (k == IsParameterOf.getClass()){
                return IsParameterOf;
            }
            if (k == HasParameter.getClass()){
                return HasParameter;
            }
            if (k == IsOutputOf.getClass()){
                return IsOutputOf;
            }
            if (k == HasPrescriptionDate.getClass()){
                return HasPrescriptionDate;
            }
            if (k == HasPrescriptionEndDate.getClass()){
                return HasPrescriptionEndDate;
            }
            if (k == HasDateOfDeath.getClass()){
                return HasDateOfDeath;
            }
            if (k == HasDateOfBirth.getClass()){
                return HasDateOfBirth;
            }
            if (k == HasLabFlagType.getClass()){
                return HasLabFlagType;
            }
            if (k == HasLabResultSampleDate.getClass()){
                return HasLabResultSampleDate;
            }
            if (k == HasBodySite.getClass()){
                return HasBodySite;
            }
            if (k == HasEncounterDateRange.getClass()){
                return HasEncounterDateRange;
            }
            if (k == HasBillingProvider.getClass()){
                return HasBillingProvider;
            }
            if (k == HasOriginalID.getClass()){
                return HasOriginalID;
            }
            if (k == HasOrganization.getClass()){
                return HasOrganization;
            }
            if (k == HasSignatureDate.getClass()){
                return HasSignatureDate;
            }
            if (k == IsInputOf.getClass()){
                return IsInputOf;
            }
            if (k == HasOutput.getClass()){
                return HasOutput;
            }
            if (k == HasInput.getClass()){
                return HasInput;
            }
            if (k == HasPrescriptionFillDate.getClass()){
                return HasPrescriptionFillDate;
            }
            if (k == HasResolutionStatusType.getClass()){
                return HasResolutionStatusType;
            }
            if (k == IsParticipantIn.getClass()){
                return IsParticipantIn;
            }
            if (k == IsLocationOf.getClass()){
                return IsLocationOf;
            }
            if (k == IsLocatedIn.getClass()){
                return IsLocatedIn;
            }
            if (k == HasPrescribedDrug.getClass()){
                return HasPrescribedDrug;
            }
            if (k == HasDateRange.getClass()){
                return HasDateRange;
            }
            if (k == HasPrimaryCareProvider.getClass()){
                return HasPrimaryCareProvider;
            }
            if (k == HasStartDate.getClass()){
                return HasStartDate;
            }
            if (k == HasEthnicity.getClass()){
                return HasEthnicity;
            }
            if (k == HasSubscriberID.getClass()){
                return HasSubscriberID;
            }
            return null;
        }
        public com.hp.hpl.jena.ontology.ObjectProperty property() {
            synchronized(model) {
                return model.createObjectProperty(uri);
            }
        }
    };
    public static enum OWLDataProperty{
        HasStatusAnnotationValue(ONTOLOGY_NAMESPACE + "alo#343043"),
        HasLabResultSequenceNumber(ONTOLOGY_NAMESPACE + "alo#000163"),
        HasLabFlagValues(ONTOLOGY_NAMESPACE + "alo#000018"),
        HasMedicationDosage(ONTOLOGY_NAMESPACE + "alo#000436"),
        HasNumberOfRefillsRemaining(ONTOLOGY_NAMESPACE + "alo#000427"),
        HasFamilyHistoryValue(ONTOLOGY_NAMESPACE + "alo#000013"),
        HasClinicalCodeDisplayName(ONTOLOGY_NAMESPACE + "alo#000012"),
        HasClinicalCodeValues(ONTOLOGY_NAMESPACE + "alo#000011"),
        HasMeasurmentUnit(ONTOLOGY_NAMESPACE + "alo#000010"),
        HasAlternateEmailAddress(ONTOLOGY_NAMESPACE + "alo#000422"),
        HasPrimaryEmailAddress(ONTOLOGY_NAMESPACE + "alo#000421"),
        HasTextRenderingValue(ONTOLOGY_NAMESPACE + "alo#000364"),
        HasResolutionStatusValue(ONTOLOGY_NAMESPACE + "alo#000363"),
        HasMedicareAdvantagePolicyURI(ONTOLOGY_NAMESPACE + "alo#000219"),
        HasSourceTypeStringValue(ONTOLOGY_NAMESPACE + "alo#000218"),
        HasDeleteIndicator(ONTOLOGY_NAMESPACE + "alo#000215"),
        HasValidatedClaimPatientDateOfBirth(ONTOLOGY_NAMESPACE + "alo#000246"),
        HasSourceSystem(ONTOLOGY_NAMESPACE + "alo#000156"),
        HasDateQualityValue(ONTOLOGY_NAMESPACE + "alo#000104"),
        HasSuffixName(ONTOLOGY_NAMESPACE + "alo#009834"),
        HasAssignAuthority(ONTOLOGY_NAMESPACE + "alo#000153"),
        HasIDType(ONTOLOGY_NAMESPACE + "alo#000152"),
        HasDrugValues(ONTOLOGY_NAMESPACE + "alo#000444"),
        HasPageNumber(ONTOLOGY_NAMESPACE + "alo#000096"),
        HasUnit(ONTOLOGY_NAMESPACE + "alo#000007"),
        HasTelephoneTypeValue(ONTOLOGY_NAMESPACE + "alo#000418"),
        HasFamilyName(ONTOLOGY_NAMESPACE + "alo#000005"),
        HasDrugIngredient(ONTOLOGY_NAMESPACE + "alo#000449"),
        HasInsurnacePolicyName(ONTOLOGY_NAMESPACE + "alo#000052"),
        HasInsurancePlanName(ONTOLOGY_NAMESPACE + "alo#000053"),
        HasAddressTypeQualityValue(ONTOLOGY_NAMESPACE + "alo#000412"),
        HasRiskAdjustmentInsuranceClaimTypeValue(ONTOLOGY_NAMESPACE + "alo#000209"),
        HasValidatedClaimPlanNumber(ONTOLOGY_NAMESPACE + "alo#000252"),
        HasPrimaryMedicalProfessional(ONTOLOGY_NAMESPACE + "alo#000207"),
        HasCodedEntityName(ONTOLOGY_NAMESPACE + "alo#000206"),
        HasSourceEncounterId(ONTOLOGY_NAMESPACE + "alo#000205"),
        HasPageClassification(ONTOLOGY_NAMESPACE + "alo#000149"),
        HasPatientEncounterSwitchTypeValue(ONTOLOGY_NAMESPACE + "alo#000201"),
        HasIDValue(ONTOLOGY_NAMESPACE + "alo#000151"),
        HasURI(ONTOLOGY_NAMESPACE + "alo#000087"),
        HasOrganizationNameValue(ONTOLOGY_NAMESPACE + "alo#000498"),
        HasStreetAddress(ONTOLOGY_NAMESPACE + "alo#000409"),
        HasCity(ONTOLOGY_NAMESPACE + "alo#000408"),
        HasState(ONTOLOGY_NAMESPACE + "alo#000407"),
        HasZipCode(ONTOLOGY_NAMESPACE + "alo#000406"),
        HasCounty(ONTOLOGY_NAMESPACE + "alo#000405"),
        HasCountry(ONTOLOGY_NAMESPACE + "alo#000404"),
        HasAddressValue(ONTOLOGY_NAMESPACE + "alo#000403"),
        HasLabResultUnit(ONTOLOGY_NAMESPACE + "alo#000402"),
        HasValue(ONTOLOGY_NAMESPACE + "alo#9000045"),
        HasClinicalCodeValue(ONTOLOGY_NAMESPACE + "alo#000400"),
        HasFamilyNameValue(ONTOLOGY_NAMESPACE + "alo#000340"),
        HasAddIndicator(ONTOLOGY_NAMESPACE + "alo#000138"),
        HasTitle(ONTOLOGY_NAMESPACE + "alo#000136"),
        HasXUUID(ONTOLOGY_NAMESPACE + "alo#000124"),
        HasErrorCodeValue(ONTOLOGY_NAMESPACE + "alo#000079"),
        HasGivenName(ONTOLOGY_NAMESPACE + "alo#888880"),
        HasTextRenderingType(ONTOLOGY_NAMESPACE + "alo#000131"),
        HasRenderingValue(ONTOLOGY_NAMESPACE + "alo#000130"),
        HasBitMaskValue(ONTOLOGY_NAMESPACE + "alo#980040"),
        HasAllergyAllergenString(ONTOLOGY_NAMESPACE + "alo#000041"),
        HasImmunizationMedicationSeriesNumber(ONTOLOGY_NAMESPACE + "alo#870008"),
        HasReligiousAffiliation(ONTOLOGY_NAMESPACE + "alo#000014"),
        HasAllergyReactionSeverity(ONTOLOGY_NAMESPACE + "alo#000483"),
        HasFileDescription(ONTOLOGY_NAMESPACE + "alo#000097"),
        HasLabResultNumberOfSamples(ONTOLOGY_NAMESPACE + "alo#000070"),
        HasPrefixNameValue(ONTOLOGY_NAMESPACE + "alo#000338"),
        HasFileURL(ONTOLOGY_NAMESPACE + "alo#000337"),
        HasAmendmentAnnotationValue(ONTOLOGY_NAMESPACE + "alo#023041"),
        HasClinicalCodingSystemName(ONTOLOGY_NAMESPACE + "alo#000164"),
        HasSourceValue(ONTOLOGY_NAMESPACE + "alo#443146"),
        HasPrefixName(ONTOLOGY_NAMESPACE + "alo#012321"),
        HasAlternateMedicalProfessional(ONTOLOGY_NAMESPACE + "alo#000232"),
        HasLaboratoryResultValue(ONTOLOGY_NAMESPACE + "alo#000125"),
        HasAnatomicalEntityName(ONTOLOGY_NAMESPACE + "alo#000350"),
        HasLanguage(ONTOLOGY_NAMESPACE + "alo#000368"),
        HasImmunizationValue(ONTOLOGY_NAMESPACE + "alo#220002"),
        HasInsuranceClaimStatusTypeValue(ONTOLOGY_NAMESPACE + "alo#828028"),
        HasDrugUnits(ONTOLOGY_NAMESPACE + "alo#000447"),
        HasLabResultValue(ONTOLOGY_NAMESPACE + "alo#000063"),
        HasBiometricValueName(ONTOLOGY_NAMESPACE + "alo#000475"),
        HasSocialHistoryValue(ONTOLOGY_NAMESPACE + "alo#000061"),
        HasSocialHistoryFieldName(ONTOLOGY_NAMESPACE + "alo#000060"),
        HasProblemSeverity(ONTOLOGY_NAMESPACE + "alo#000471"),
        HasMIMEType(ONTOLOGY_NAMESPACE + "alo#000034"),
        HasClinicalRoleType(ONTOLOGY_NAMESPACE + "alo#000082"),
        HasSuffixNameValue(ONTOLOGY_NAMESPACE + "alo#000336"),
        HasGivenNameValue(ONTOLOGY_NAMESPACE + "alo#000339"),
        HasMaritalStatusQualityValue(ONTOLOGY_NAMESPACE + "alo#000320"),
        HasDocumentTypeValue(ONTOLOGY_NAMESPACE + "alo#000117"),
        HasSocialHistory(ONTOLOGY_NAMESPACE + "alo#000059"),
        HasProcessorTypeValue(ONTOLOGY_NAMESPACE + "alo#000110"),
        HasAllergyValue(ONTOLOGY_NAMESPACE + "alo#000482"),
        HasLabNote(ONTOLOGY_NAMESPACE + "alo#000467"),
        HasProblemName(ONTOLOGY_NAMESPACE + "alo#000466"),
        HasProblemValue(ONTOLOGY_NAMESPACE + "alo#000465"),
        HasCareSiteName(ONTOLOGY_NAMESPACE + "alo#000463"),
        HasCareSiteValue(ONTOLOGY_NAMESPACE + "alo#000462"),
        HasCareSiteTypeVal(ONTOLOGY_NAMESPACE + "alo#000461"),
        HasClinicalRole(ONTOLOGY_NAMESPACE + "alo#000080"),
        HasRange(ONTOLOGY_NAMESPACE + "alo#000317"),
        HasTimeZone(ONTOLOGY_NAMESPACE + "alo#000311"),
        InOutputReport(ONTOLOGY_NAMESPACE + "alo#000256"),
        HasInsuranceClaimErrorCodeFilterValue(ONTOLOGY_NAMESPACE + "alo#000255"),
        RouteToCoder(ONTOLOGY_NAMESPACE + "alo#000254"),
        HasRAValue(ONTOLOGY_NAMESPACE + "alo#000198"),
        HasValidatedClaimFileMode(ONTOLOGY_NAMESPACE + "alo#000251"),
        HasSourceTypeValue(ONTOLOGY_NAMESPACE + "alo#000195"),
        HasHash(ONTOLOGY_NAMESPACE + "alo#000105"),
        HasInputDataTypeValue(ONTOLOGY_NAMESPACE + "alo#000192"),
        HasInsurancePlanType(ONTOLOGY_NAMESPACE + "alo#000047"),
        HasInsuranceValue(ONTOLOGY_NAMESPACE + "alo#000043"),
        HasNameValue(ONTOLOGY_NAMESPACE + "alo#000334"),
        HasMedicarePlanURI(ONTOLOGY_NAMESPACE + "alo#000454"),
        HasClinicalCodeLabel(ONTOLOGY_NAMESPACE + "alo#000399"),
        HasClinicalCodingSystemVersion(ONTOLOGY_NAMESPACE + "alo#000398"),
        HasClinicalCodingSystemOID(ONTOLOGY_NAMESPACE + "alo#000397"),
        HasClinicalCodingSystemValue(ONTOLOGY_NAMESPACE + "alo#000396"),
        HasRiskAssessmentCodeClusters(ONTOLOGY_NAMESPACE + "alo#000248"),
        HasValidatedClaimProviderType(ONTOLOGY_NAMESPACE + "alo#000247"),
        HasPatientEncounterQualityValue(ONTOLOGY_NAMESPACE + "alo#077717"),
        HasValidatedClaimControlNumber(ONTOLOGY_NAMESPACE + "alo#000245"),
        HasValidatedClaimPaymentYear(ONTOLOGY_NAMESPACE + "alo#000244"),
        HasProcessorName(ONTOLOGY_NAMESPACE + "alo#000189"),
        HasProcessorVersion(ONTOLOGY_NAMESPACE + "alo#000188"),
        HasCMSValidatedClaimValue(ONTOLOGY_NAMESPACE + "alo#000241"),
        HasLineNumber(ONTOLOGY_NAMESPACE + "alo#000184"),
        HasPatientEncounterURI(ONTOLOGY_NAMESPACE + "alo#000183"),
        HasVersion(ONTOLOGY_NAMESPACE + "alo#000182"),
        HasPhoneNumber(ONTOLOGY_NAMESPACE + "alo#000039"),
        HasOtherId(ONTOLOGY_NAMESPACE + "alo#000180"),
        HasDescriptionValue(ONTOLOGY_NAMESPACE + "alo#000503"),
        HasRouteOfAdministration(ONTOLOGY_NAMESPACE + "alo#000448"),
        HasMedicalProcedureNameValue(ONTOLOGY_NAMESPACE + "alo#000501"),
        HasDrugForm(ONTOLOGY_NAMESPACE + "alo#000446"),
        HasDrugStrength(ONTOLOGY_NAMESPACE + "alo#000445"),
        HasMedicalProcedureInterpretation(ONTOLOGY_NAMESPACE + "alo#000031"),
        HasPrescriptionFrequency(ONTOLOGY_NAMESPACE + "alo#000443"),
        HasPrescriptionQuantity(ONTOLOGY_NAMESPACE + "alo#000442"),
        HasLabResultNameValue(ONTOLOGY_NAMESPACE + "alo#000487"),
        HasCMSValidationErrorCodeValue(ONTOLOGY_NAMESPACE + "alo#000239"),
        HasGenderValue(ONTOLOGY_NAMESPACE + "alo#000380"),
        HasDateValue(ONTOLOGY_NAMESPACE + "alo#000109"),
        HasProcessorValue(ONTOLOGY_NAMESPACE + "alo#000178"),
        HasGenericName(ONTOLOGY_NAMESPACE + "alo#000176"),
        HasImageResolution(ONTOLOGY_NAMESPACE + "alo#000173"),
        HasOperationAnnotationValue(ONTOLOGY_NAMESPACE + "alo#345042"),
        HasProblemTemporalStatus(ONTOLOGY_NAMESPACE + "alo#000026"),
        HasSig(ONTOLOGY_NAMESPACE + "alo#000438"),
        HasMedicationAmount(ONTOLOGY_NAMESPACE + "alo#000437"),
        HasBiometricValueString(ONTOLOGY_NAMESPACE + "alo#000023"),
        HasPrescriptionValues(ONTOLOGY_NAMESPACE + "alo#000435"),
        IsActivePrescription(ONTOLOGY_NAMESPACE + "alo#000433"),
        HasNameTypeValue(ONTOLOGY_NAMESPACE + "alo#000497"),
        HasInsuranceSequnceNumberValue(ONTOLOGY_NAMESPACE + "alo#000376"),
        HasTelephoneNumberValue(ONTOLOGY_NAMESPACE + "alo#000038"),
        HasImmunizationQuantity(ONTOLOGY_NAMESPACE + "alo#343003"),
        HasFeeForServiceInsuranceClaimTypeValue(ONTOLOGY_NAMESPACE + "alo#000225"),
        HasSignedStatus(ONTOLOGY_NAMESPACE + "alo#000169"),
        HasBrandName(ONTOLOGY_NAMESPACE + "alo#000168"),
        FeeForServiceInsuranceClaimTypeValue(ONTOLOGY_NAMESPACE + "alo#000226"),
        MaritalStatusValue(ONTOLOGY_NAMESPACE + "alo#000074"),
        InsuranceClaimStatusValue(ONTOLOGY_NAMESPACE + "alo#054327"),
        PatientEncounterSwitchTypeValue(ONTOLOGY_NAMESPACE + "alo#000199"),
        NameTypeValue(ONTOLOGY_NAMESPACE + "alo#000496"),
        PayorTypeValue(ONTOLOGY_NAMESPACE + "alo#000045"),
        CareSiteTypeValue(ONTOLOGY_NAMESPACE + "alo#000460"),
        DateQualityValue(ONTOLOGY_NAMESPACE + "alo#000126"),
        PatientEncounterTypeValue(ONTOLOGY_NAMESPACE + "alo#944014"),
        ResolutionStatusTypeValue(ONTOLOGY_NAMESPACE + "alo#000464"),
        ClinicalRoleTypeValue(ONTOLOGY_NAMESPACE + "alo#000081"),
        RiskAdjustmentInsuranceClaimTypeValue(ONTOLOGY_NAMESPACE + "alo#000203"),
        OperationAnnotationValue(ONTOLOGY_NAMESPACE + "alo#087839"),
        DocumentTypeValue(ONTOLOGY_NAMESPACE + "alo#000111"),
        AddressTypeValue(ONTOLOGY_NAMESPACE + "alo#000413"),
        SourceTypeValue(ONTOLOGY_NAMESPACE + "alo#000196"),
        ProcessorTypeValue(ONTOLOGY_NAMESPACE + "alo#000101"),
        StatusAnnotationValue(ONTOLOGY_NAMESPACE + "alo#399038"),
        LabFlagValue(ONTOLOGY_NAMESPACE + "alo#000719"),
        TelephoneTypeValue(ONTOLOGY_NAMESPACE + "alo#000419"),
        InputDataTypeValue(ONTOLOGY_NAMESPACE + "alo#000187"),
        GenderValues(ONTOLOGY_NAMESPACE + "alo#000308")
        ;

        private final String uri;
        private OWLDataProperty (String uri){ this.uri = uri;}
        public String toString() { return uri; }
        public String uri() { return uri; }
        public static OWLDataProperty getByClass(java.lang.Class<?> k){
            if (k == FeeForServiceInsuranceClaimTypeValue.getClass()){
                return FeeForServiceInsuranceClaimTypeValue;
            }
            if (k == MaritalStatusValue.getClass()){
                return MaritalStatusValue;
            }
            if (k == InsuranceClaimStatusValue.getClass()){
                return InsuranceClaimStatusValue;
            }
            if (k == PatientEncounterSwitchTypeValue.getClass()){
                return PatientEncounterSwitchTypeValue;
            }
            if (k == NameTypeValue.getClass()){
                return NameTypeValue;
            }
            if (k == PayorTypeValue.getClass()){
                return PayorTypeValue;
            }
            if (k == CareSiteTypeValue.getClass()){
                return CareSiteTypeValue;
            }
            if (k == DateQualityValue.getClass()){
                return DateQualityValue;
            }
            if (k == PatientEncounterTypeValue.getClass()){
                return PatientEncounterTypeValue;
            }
            if (k == ResolutionStatusTypeValue.getClass()){
                return ResolutionStatusTypeValue;
            }
            if (k == ClinicalRoleTypeValue.getClass()){
                return ClinicalRoleTypeValue;
            }
            if (k == RiskAdjustmentInsuranceClaimTypeValue.getClass()){
                return RiskAdjustmentInsuranceClaimTypeValue;
            }
            if (k == OperationAnnotationValue.getClass()){
                return OperationAnnotationValue;
            }
            if (k == DocumentTypeValue.getClass()){
                return DocumentTypeValue;
            }
            if (k == AddressTypeValue.getClass()){
                return AddressTypeValue;
            }
            if (k == SourceTypeValue.getClass()){
                return SourceTypeValue;
            }
            if (k == ProcessorTypeValue.getClass()){
                return ProcessorTypeValue;
            }
            if (k == StatusAnnotationValue.getClass()){
                return StatusAnnotationValue;
            }
            if (k == LabFlagValue.getClass()){
                return LabFlagValue;
            }
            if (k == TelephoneTypeValue.getClass()){
                return TelephoneTypeValue;
            }
            if (k == InputDataTypeValue.getClass()){
                return InputDataTypeValue;
            }
            if (k == GenderValues.getClass()){
                return GenderValues;
            }
            return null;
        }
        public DatatypeProperty property() {
            synchronized(model) {
                return model.createDatatypeProperty(uri);
            }
        }
    };
    private static Map<String, String> m = new HashMap<>();
    static {
        m.put(ONTOLOGY_NAMESPACE + "alo#000099", "ExternalIdentifier");
        m.put(ONTOLOGY_NAMESPACE + "alo#010145", "Source");
        m.put(ONTOLOGY_NAMESPACE + "alo#000354", "InsurancePlan");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000048", "DimensionalQuantity");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000007", "SocialEntity");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000046", "MeasurmentValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000020", "Quality");
        m.put(ONTOLOGY_NAMESPACE + "alo#331033", "Provider");
        m.put(ONTOLOGY_NAMESPACE + "alo#000139", "SocialQuality");
        m.put(ONTOLOGY_NAMESPACE + "alo#000112", "Collective");
        m.put(ONTOLOGY_NAMESPACE + "alo#000240", "CMSValidationError");
        m.put(ONTOLOGY_NAMESPACE + "alo#000114", "Corporation");
        m.put(ONTOLOGY_NAMESPACE + "alo#000135", "HealthCarePayor");
        m.put(ONTOLOGY_NAMESPACE + "alo#000134", "PreferredProviderOrganizationPlan");
        m.put(ONTOLOGY_NAMESPACE + "alo#000133", "RiskAdjustmentInsuranceClaim");
        m.put(ONTOLOGY_NAMESPACE + "alo#000132", "FeeForServiceInsuranceClaim");
        m.put(ONTOLOGY_NAMESPACE + "alo#000028", "Patient");
        m.put(ONTOLOGY_NAMESPACE + "alo#000027", "File");
        m.put(ONTOLOGY_NAMESPACE + "alo#000459", "Problem");
        m.put(ONTOLOGY_NAMESPACE + "alo#000458", "MedicareInsurancePolicy");
        m.put(ONTOLOGY_NAMESPACE + "alo#000024", "ResolutionStatusType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000455", "InsurancePolicy");
        m.put(ONTOLOGY_NAMESPACE + "alo#000020", "LabFlagType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000193", "SourceType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000089", "AnatomicalEntity");
        m.put(ONTOLOGY_NAMESPACE + "alo#000088", "CareSiteOrganization");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000143", "Entity");
        m.put(ONTOLOGY_NAMESPACE + "alo#000389", "FamilyHistory");
        m.put(ONTOLOGY_NAMESPACE + "alo#000000", "Document");
        m.put(ONTOLOGY_NAMESPACE + "alo#076616", "PatientEncounterType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000065", "Provenance");
        m.put(ONTOLOGY_NAMESPACE + "alo#000235", "RAPSValidationError");
        m.put(ONTOLOGY_NAMESPACE + "alo#000095", "DataProcessingDetail");
        m.put(ONTOLOGY_NAMESPACE + "alo#000233", "InsuranceClaimableEvent");
        m.put(ONTOLOGY_NAMESPACE + "alo#540026", "InsuranceClaimStatusType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000127", "HICN");
        m.put(ONTOLOGY_NAMESPACE + "alo#000119", "InsuranceCompany");
        m.put(ONTOLOGY_NAMESPACE + "alo#000158", "PositionalIdentifer");
        m.put(ONTOLOGY_NAMESPACE + "alo#000129", "ClinicalCode");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000019", "Observation");
        m.put(ONTOLOGY_NAMESPACE + "alo#000191", "InputDataType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000017", "GenderType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000309", "Date");
        m.put(ONTOLOGY_NAMESPACE + "alo#000186", "Processor");
        m.put(ONTOLOGY_NAMESPACE + "alo#070156", "PatientEncounter");
        m.put(ONTOLOGY_NAMESPACE + "alo#999913", "MedicalCodeEntity");
        m.put(ONTOLOGY_NAMESPACE + "alo#000181", "ProvenancedEntity");
        m.put(ONTOLOGY_NAMESPACE + "alo#000075", "InformationalEntityIdentifier");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000012", "ChemicalEntity");
        m.put(ONTOLOGY_NAMESPACE + "alo#555015", "MedicalQuality");
        m.put(ONTOLOGY_NAMESPACE + "alo#000072", "ClinicalRoleType");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000025", "Role");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000024", "Function");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000023", "RealizableEntity");
        m.put(ONTOLOGY_NAMESPACE + "alo#000224", "FeeForServiceInsuranceClaimType");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000021", "BiologicalQuality");
        m.put(ONTOLOGY_NAMESPACE + "alo#000222", "MedicalCondition");
        m.put(ONTOLOGY_NAMESPACE + "alo#000234", "CMSClaimValidationResult");
        m.put(ONTOLOGY_NAMESPACE + "alo#000115", "DocumentType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000391", "ClinicalCodingSystem");
        m.put(ONTOLOGY_NAMESPACE + "alo#000390", "SocialHistory");
        m.put(ONTOLOGY_NAMESPACE + "alo#000009", "EncounterDocument");
        m.put(ONTOLOGY_NAMESPACE + "alo#350035", "PreferedProviderOrganization");
        m.put(ONTOLOGY_NAMESPACE + "alo#000003", "Identifier");
        m.put(ONTOLOGY_NAMESPACE + "alo#000179", "Human");
        m.put(ONTOLOGY_NAMESPACE + "alo#000177", "Illness");
        m.put(ONTOLOGY_NAMESPACE + "alo#000002", "Label");
        m.put(ONTOLOGY_NAMESPACE + "alo#000175", "ContactDetails");
        m.put(ONTOLOGY_NAMESPACE + "alo#000174", "MedicalProfessional");
        m.put(ONTOLOGY_NAMESPACE + "alo#000431", "PrimaryCareProvider");
        m.put(ONTOLOGY_NAMESPACE + "alo#000171", "ApixioDictionary");
        m.put(ONTOLOGY_NAMESPACE + "alo#000170", "TelephoneNumber");
        m.put(ONTOLOGY_NAMESPACE + "alo#000066", "DataTransformation");
        m.put(ONTOLOGY_NAMESPACE + "alo#000324", "DateType");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000018", "MedicalEvent");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000017", "InformationProcessing");
        m.put(ONTOLOGY_NAMESPACE + "alo#000321", "MeasurmentQuality");
        m.put(ONTOLOGY_NAMESPACE + "alo#455045", "InformationContentQuality");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000014", "Operation");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000013", "SpatialRegion");
        m.put(ONTOLOGY_NAMESPACE + "alo#000388", "PatientHistory");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000011", "BiologicalEntity");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000010", "MaterialEntity");
        m.put(ONTOLOGY_NAMESPACE + "alo#000108", "DateRange");
        m.put(ONTOLOGY_NAMESPACE + "alo#000210", "RiskAdjustmentInsuranceClaimType");
        m.put(ONTOLOGY_NAMESPACE + "alo#330022", "InsuranceClaim");
        m.put(ONTOLOGY_NAMESPACE + "alo#000381", "ImageRendering");
        m.put(ONTOLOGY_NAMESPACE + "alo#000102", "ProcessorType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000145", "MaritalStatusType");
        m.put(ONTOLOGY_NAMESPACE + "alo#456132", "HealthMaintenanceOrganizationPlan");
        m.put(ONTOLOGY_NAMESPACE + "alo#777051", "Contract");
        m.put(ONTOLOGY_NAMESPACE + "alo#640023", "Invoice");
        m.put(ONTOLOGY_NAMESPACE + "alo#000236", "EDPSValidationError");
        m.put(ONTOLOGY_NAMESPACE + "alo#077747", "OperationAnnotationType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000318", "ApixioDate");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000009", "SoftwareEntity");
        m.put(ONTOLOGY_NAMESPACE + "alo#000056", "TimeMeasurment");
        m.put(ONTOLOGY_NAMESPACE + "alo#000450", "TimeBoundExternalIdentifier");
        m.put(ONTOLOGY_NAMESPACE + "alo#000054", "Page");
        m.put(ONTOLOGY_NAMESPACE + "alo#000312", "TextRendering");
        m.put(ONTOLOGY_NAMESPACE + "alo#073046", "StatusAnnotationType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000051", "Demographics");
        m.put(ONTOLOGY_NAMESPACE + "alo#000050", "Person");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000003", "InformationContentEntity");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000002", "Attribute");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000001", "ApixioProcess");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000000", "ApixioObject");
        m.put(ONTOLOGY_NAMESPACE + "alo#000253", "InsuranceClaimErrorCodeFilter");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000004", "ComputationalEntity");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000005", "MathematicalEntity");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000006", "MedicalEntity");
        m.put(ONTOLOGY_NAMESPACE + "alo#000159", "Address");
        m.put(ONTOLOGY_NAMESPACE + "alo#000417", "TelephoneType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000157", "CareSite");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000008", "TextualEntity");
        m.put(ONTOLOGY_NAMESPACE + "alo#000154", "PatientEncounterSwitchType");
        m.put(ONTOLOGY_NAMESPACE + "alo#090478", "Allergy");
        m.put(ONTOLOGY_NAMESPACE + "alo#000411", "Prescription");
        m.put(ONTOLOGY_NAMESPACE + "alo#000410", "AddressType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000046", "Medication");
        m.put(ONTOLOGY_NAMESPACE + "alo#000304", "Rendering");
        m.put(ONTOLOGY_NAMESPACE + "alo#000044", "MedicareOrganization");
        m.put(ONTOLOGY_NAMESPACE + "alo#100001", "Immunization");
        m.put(ONTOLOGY_NAMESPACE + "alo#000042", "PlanType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000474", "BiometricValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000040", "CareSiteType");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000047", "Quantity");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000049", "DimensionlessQuantity");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000015", "Event");
        m.put(ONTOLOGY_NAMESPACE + "alo#000485", "LabResult");
        m.put(ONTOLOGY_NAMESPACE + "alo#000451", "Medicare");
        m.put(ONTOLOGY_NAMESPACE + "alo#000100", "SignatureType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000113", "Organization");
        m.put(ONTOLOGY_NAMESPACE + "alo#000147", "CodedEntity");
        m.put(ONTOLOGY_NAMESPACE + "alo#000916", "Procedure");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000022", "InformationalQuality");
        m.put(ONTOLOGY_NAMESPACE + "alo#000107", "Name");
        m.put(ONTOLOGY_NAMESPACE + "alo#646036", "HealthMaintenanceOrganization");
        m.put(ONTOLOGY_NAMESPACE + "alo#000037", "AmendmentAnnotation");
        m.put(ONTOLOGY_NAMESPACE + "alo#000033", "NameType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000374", "HasGroupNumber");
        m.put(ONTOLOGY_NAMESPACE + "alo#000373", "HasMemberNumber");
        m.put(ONTOLOGY_NAMESPACE + "alo#000032", "HasMedicalProcedure");
        m.put(ONTOLOGY_NAMESPACE + "alo#810018", "HasCareSiteType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000637", "HasNameType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000223", "HasFeeForServiceClaimTypeValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000221", "HasRenderingProvider");
        m.put(ONTOLOGY_NAMESPACE + "alo#000166", "HasSourceAuthor");
        m.put(ONTOLOGY_NAMESPACE + "alo#000165", "HasAuthor");
        m.put(ONTOLOGY_NAMESPACE + "alo#000162", "HasDateOfServiceRange");
        m.put(ONTOLOGY_NAMESPACE + "alo#000161", "HasServicesRendered");
        m.put(ONTOLOGY_NAMESPACE + "alo#000160", "HasAddress");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000027", "DerivesInto");
        m.put(ONTOLOGY_NAMESPACE + "alo#000429", "HasRace");
        m.put(ONTOLOGY_NAMESPACE + "alo#000428", "HasAlternateDemographics");
        m.put(ONTOLOGY_NAMESPACE + "alo#000426", "HasAlternateTelephoneNumber");
        m.put(ONTOLOGY_NAMESPACE + "alo#000425", "HasAlternateContactDetails");
        m.put(ONTOLOGY_NAMESPACE + "alo#000424", "HasContactDetails");
        m.put(ONTOLOGY_NAMESPACE + "alo#000423", "HasTelephoneNumberType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000453", "HasHICNNumber");
        m.put(ONTOLOGY_NAMESPACE + "alo#000367", "HasDocumentDate");
        m.put(ONTOLOGY_NAMESPACE + "alo#000420", "HasTelephoneNumber");
        m.put(ONTOLOGY_NAMESPACE + "alo#000365", "HasDemographics");
        m.put(ONTOLOGY_NAMESPACE + "alo#000217", "HasPatientControlNumber");
        m.put(ONTOLOGY_NAMESPACE + "alo#000216", "HasBillType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000030", "HasSupportingDiagnosis");
        m.put(ONTOLOGY_NAMESPACE + "alo#000214", "HasProviderType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000213", "HasDiagnosisCode");
        m.put(ONTOLOGY_NAMESPACE + "alo#000212", "HasValidatedClaimTransactionDate");
        m.put(ONTOLOGY_NAMESPACE + "alo#000211", "HasRiskAdjustmentInsuranceClaimType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000155", "HasNationalProviderIdentifier");
        m.put(ONTOLOGY_NAMESPACE + "alo#000098", "HasDataProcessingDetail");
        m.put(ONTOLOGY_NAMESPACE + "alo#000008", "HasPage");
        m.put(ONTOLOGY_NAMESPACE + "alo#000006", "HasAllergyResolvedDate");
        m.put(ONTOLOGY_NAMESPACE + "alo#000093", "HasCodedEntityClinicalCode");
        m.put(ONTOLOGY_NAMESPACE + "alo#000004", "HasAllergyReactionDate");
        m.put(ONTOLOGY_NAMESPACE + "alo#000416", "HasAlternateAddress");
        m.put(ONTOLOGY_NAMESPACE + "alo#000415", "HasPrimaryAddress");
        m.put(ONTOLOGY_NAMESPACE + "alo#000001", "HasProblemResolutionDate");
        m.put(ONTOLOGY_NAMESPACE + "alo#000194", "HasSourceType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000355", "HasRendering");
        m.put(ONTOLOGY_NAMESPACE + "alo#000197", "RefersToInternalObject");
        m.put(ONTOLOGY_NAMESPACE + "alo#151151", "HasSourceCreationDate");
        m.put(ONTOLOGY_NAMESPACE + "alo#250025", "HasClaimedEvent");
        m.put(ONTOLOGY_NAMESPACE + "alo#000204", "HasSourceOrganization");
        m.put(ONTOLOGY_NAMESPACE + "alo#000202", "HasPatientEncounterSwitchType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000200", "HasClaimOverpaymentIdentifier");
        m.put(ONTOLOGY_NAMESPACE + "alo#000144", "HasEncounter");
        m.put(ONTOLOGY_NAMESPACE + "alo#000143", "HasDocumentPage");
        m.put(ONTOLOGY_NAMESPACE + "alo#000142", "HasDateRangeForCareOfPatient");
        m.put(ONTOLOGY_NAMESPACE + "alo#490049", "HasStatusAnnotationType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000140", "HasInsuranceClaimStatus");
        m.put(ONTOLOGY_NAMESPACE + "alo#000085", "HasAlternateName");
        m.put(ONTOLOGY_NAMESPACE + "alo#048048", "HasAmendment");
        m.put(ONTOLOGY_NAMESPACE + "alo#000083", "HasDocumentCareSite");
        m.put(ONTOLOGY_NAMESPACE + "alo#000494", "HasSampleDate");
        m.put(ONTOLOGY_NAMESPACE + "alo#200020", "HasChiefComplaint");
        m.put(ONTOLOGY_NAMESPACE + "alo#000492", "HasLabSuperPanel");
        m.put(ONTOLOGY_NAMESPACE + "alo#000491", "HasLabResultPart");
        m.put(ONTOLOGY_NAMESPACE + "alo#000490", "HasLabPanel");
        m.put(ONTOLOGY_NAMESPACE + "alo#000401", "HasClinicalCodingSystem");
        m.put(ONTOLOGY_NAMESPACE + "alo#000346", "HasMaritalStatusType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000150", "HasFeeForServiceClaim");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000031", "IsDerivedFrom");
        m.put(ONTOLOGY_NAMESPACE + "alo#110011", "HasDrug");
        m.put(ONTOLOGY_NAMESPACE + "alo#000067", "HasPatientEncounterCareSite");
        m.put(ONTOLOGY_NAMESPACE + "alo#000068", "HasFileDate");
        m.put(ONTOLOGY_NAMESPACE + "alo#000069", "HasLabResultCareSite");
        m.put(ONTOLOGY_NAMESPACE + "alo#000137", "HasInsuranceClaim");
        m.put(ONTOLOGY_NAMESPACE + "alo#000078", "HasPatientCareSite");
        m.put(ONTOLOGY_NAMESPACE + "alo#000077", "HasDocumentAuthor");
        m.put(ONTOLOGY_NAMESPACE + "alo#000076", "HasSocialHistory");
        m.put(ONTOLOGY_NAMESPACE + "alo#000073", "HasFamilyHistory");
        m.put(ONTOLOGY_NAMESPACE + "alo#000071", "HasCareSite");
        m.put(ONTOLOGY_NAMESPACE + "alo#000480", "HasAllergyDiagnosisDate");
        m.put(ONTOLOGY_NAMESPACE + "alo#000015", "HasGenderType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000094", "HasSource");
        m.put(ONTOLOGY_NAMESPACE + "alo#000468", "HasProblemDateRange");
        m.put(ONTOLOGY_NAMESPACE + "alo#000332", "HasEncounterAgent");
        m.put(ONTOLOGY_NAMESPACE + "alo#120012", "HasImmunizationMedication");
        m.put(ONTOLOGY_NAMESPACE + "alo#000167", "HasSourceFile");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000035", "IsRefferedToBy");
        m.put(ONTOLOGY_NAMESPACE + "alo#000128", "HasFile");
        m.put(ONTOLOGY_NAMESPACE + "alo#000208", "HasClinicalCode");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000036", "RefersTo");
        m.put(ONTOLOGY_NAMESPACE + "alo#000123", "HasImageRendering");
        m.put(ONTOLOGY_NAMESPACE + "alo#000122", "HasContractIdentifier");
        m.put(ONTOLOGY_NAMESPACE + "alo#000121", "RefersToPatient");
        m.put(ONTOLOGY_NAMESPACE + "alo#000120", "HasDataProcessingDetailDate");
        m.put(ONTOLOGY_NAMESPACE + "alo#000378", "HasEndDate");
        m.put(ONTOLOGY_NAMESPACE + "alo#000064", "HasSocialHistoryType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000476", "HasBiometricValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000062", "HasName");
        m.put(ONTOLOGY_NAMESPACE + "alo#000473", "HasPrescription");
        m.put(ONTOLOGY_NAMESPACE + "alo#000414", "HasAddressType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000489", "HasSpecimen");
        m.put(ONTOLOGY_NAMESPACE + "alo#000472", "HasProblem");
        m.put(ONTOLOGY_NAMESPACE + "alo#110010", "HasImmunizationAdminDate");
        m.put(ONTOLOGY_NAMESPACE + "alo#044044", "HasAmendmentAnnotation");
        m.put(ONTOLOGY_NAMESPACE + "alo#000019", "HasPatientEncounterType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000049", "HasDate");
        m.put(ONTOLOGY_NAMESPACE + "alo#000479", "HasAllergy");
        m.put(ONTOLOGY_NAMESPACE + "alo#000249", "HasCMSValidationError");
        m.put(ONTOLOGY_NAMESPACE + "alo#000084", "HasClinicalRole");
        m.put(ONTOLOGY_NAMESPACE + "alo#000118", "HasTextRendering");
        m.put(ONTOLOGY_NAMESPACE + "alo#000086", "HasMedicalProfessional");
        m.put(ONTOLOGY_NAMESPACE + "alo#000116", "HasInsuranceClaimCareSite");
        m.put(ONTOLOGY_NAMESPACE + "alo#000231", "HasDocument");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000026", "IsRelatedTo");
        m.put(ONTOLOGY_NAMESPACE + "alo#000058", "HasSocialHistoryDate");
        m.put(ONTOLOGY_NAMESPACE + "alo#000057", "HasInsuranceCompany");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000029", "HasParticipant");
        m.put(ONTOLOGY_NAMESPACE + "alo#000055", "HasIdentifier");
        m.put(ONTOLOGY_NAMESPACE + "alo#000141", "HasRiskAdjustmentClaim");
        m.put(ONTOLOGY_NAMESPACE + "alo#000146", "HasLastEditDate");
        m.put(ONTOLOGY_NAMESPACE + "alo#000148", "HasInsuranceClaimProcessingDate");
        m.put(ONTOLOGY_NAMESPACE + "alo#000190", "HasInputDataType");
        m.put(ONTOLOGY_NAMESPACE + "alo#050050", "HasOperationAnnotationType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000502", "HasMedicalProcedureDateRange");
        m.put(ONTOLOGY_NAMESPACE + "alo#610006", "HasImmunization");
        m.put(ONTOLOGY_NAMESPACE + "alo#000091", "IsStoredAs");
        m.put(ONTOLOGY_NAMESPACE + "alo#000092", "HasOtherOriginalIDs");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000030", "IsAttributeOf");
        m.put(ONTOLOGY_NAMESPACE + "alo#000250", "HasCMSClaimValidationResult");
        m.put(ONTOLOGY_NAMESPACE + "alo#000106", "HasInsurancePlanIdentifier");
        m.put(ONTOLOGY_NAMESPACE + "alo#000477", "HasMeasurmentResultDate");
        m.put(ONTOLOGY_NAMESPACE + "alo#000103", "HasProcessorType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000048", "HasInsuranceType");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000028", "HasAttribute");
        m.put(ONTOLOGY_NAMESPACE + "alo#000457", "HasInsurancePolicy");
        m.put(ONTOLOGY_NAMESPACE + "alo#000456", "HasInsurance");
        m.put(ONTOLOGY_NAMESPACE + "alo#770007", "HasImmunizationDateRange");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000050", "HasAgent");
        m.put(ONTOLOGY_NAMESPACE + "alo#000452", "HasInsurancePolicyDateRange");
        m.put(ONTOLOGY_NAMESPACE + "alo#000303", "HasDateType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000016", "HasTimeBoundIdentifierDateRange");
        m.put(ONTOLOGY_NAMESPACE + "alo#000486", "HasLabResult");
        m.put(ONTOLOGY_NAMESPACE + "alo#000185", "HasProcessor");
        m.put(ONTOLOGY_NAMESPACE + "alo#032024", "HasClaimSubmissionDate");
        m.put(ONTOLOGY_NAMESPACE + "alo#000036", "HasInsurancePlan");
        m.put(ONTOLOGY_NAMESPACE + "alo#000035", "HasAllergenCode");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000044", "IsPartOf");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000043", "HasPart");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000042", "IsParameterOf");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000041", "HasParameter");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000040", "IsOutputOf");
        m.put(ONTOLOGY_NAMESPACE + "alo#000441", "HasPrescriptionDate");
        m.put(ONTOLOGY_NAMESPACE + "alo#000440", "HasPrescriptionEndDate");
        m.put(ONTOLOGY_NAMESPACE + "alo#000384", "HasDateOfDeath");
        m.put(ONTOLOGY_NAMESPACE + "alo#000383", "HasDateOfBirth");
        m.put(ONTOLOGY_NAMESPACE + "alo#000021", "HasLabFlagType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000022", "HasLabResultSampleDate");
        m.put(ONTOLOGY_NAMESPACE + "alo#000029", "HasBodySite");
        m.put(ONTOLOGY_NAMESPACE + "alo#000366", "HasEncounterDateRange");
        m.put(ONTOLOGY_NAMESPACE + "alo#000220", "HasBillingProvider");
        m.put(ONTOLOGY_NAMESPACE + "alo#000090", "HasOriginalID");
        m.put(ONTOLOGY_NAMESPACE + "alo#149149", "HasOrganization");
        m.put(ONTOLOGY_NAMESPACE + "alo#000172", "HasSignatureDate");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000039", "IsInputOf");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000038", "HasOutput");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000037", "HasInput");
        m.put(ONTOLOGY_NAMESPACE + "alo#000439", "HasPrescriptionFillDate");
        m.put(ONTOLOGY_NAMESPACE + "alo#000025", "HasResolutionStatusType");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000034", "IsParticipantIn");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000033", "IsLocationOf");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000032", "IsLocatedIn");
        m.put(ONTOLOGY_NAMESPACE + "alo#000434", "HasPrescribedDrug");
        m.put(ONTOLOGY_NAMESPACE + "alo#000379", "HasDateRange");
        m.put(ONTOLOGY_NAMESPACE + "alo#000432", "HasPrimaryCareProvider");
        m.put(ONTOLOGY_NAMESPACE + "alo#000377", "HasStartDate");
        m.put(ONTOLOGY_NAMESPACE + "alo#000430", "HasEthnicity");
        m.put(ONTOLOGY_NAMESPACE + "alo#000375", "HasSubscriberID");
        m.put(ONTOLOGY_NAMESPACE + "alo#343043", "HasStatusAnnotationValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000163", "HasLabResultSequenceNumber");
        m.put(ONTOLOGY_NAMESPACE + "alo#000018", "HasLabFlagValues");
        m.put(ONTOLOGY_NAMESPACE + "alo#000436", "HasMedicationDosage");
        m.put(ONTOLOGY_NAMESPACE + "alo#000427", "HasNumberOfRefillsRemaining");
        m.put(ONTOLOGY_NAMESPACE + "alo#000013", "HasFamilyHistoryValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000012", "HasClinicalCodeDisplayName");
        m.put(ONTOLOGY_NAMESPACE + "alo#000011", "HasClinicalCodeValues");
        m.put(ONTOLOGY_NAMESPACE + "alo#000010", "HasMeasurmentUnit");
        m.put(ONTOLOGY_NAMESPACE + "alo#000422", "HasAlternateEmailAddress");
        m.put(ONTOLOGY_NAMESPACE + "alo#000421", "HasPrimaryEmailAddress");
        m.put(ONTOLOGY_NAMESPACE + "alo#000364", "HasTextRenderingValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000363", "HasResolutionStatusValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000219", "HasMedicareAdvantagePolicyURI");
        m.put(ONTOLOGY_NAMESPACE + "alo#000218", "HasSourceTypeStringValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000215", "HasDeleteIndicator");
        m.put(ONTOLOGY_NAMESPACE + "alo#000246", "HasValidatedClaimPatientDateOfBirth");
        m.put(ONTOLOGY_NAMESPACE + "alo#000156", "HasSourceSystem");
        m.put(ONTOLOGY_NAMESPACE + "alo#000104", "HasDateQualityValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#009834", "HasSuffixName");
        m.put(ONTOLOGY_NAMESPACE + "alo#000153", "HasAssignAuthority");
        m.put(ONTOLOGY_NAMESPACE + "alo#000152", "HasIDType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000444", "HasDrugValues");
        m.put(ONTOLOGY_NAMESPACE + "alo#000096", "HasPageNumber");
        m.put(ONTOLOGY_NAMESPACE + "alo#000007", "HasUnit");
        m.put(ONTOLOGY_NAMESPACE + "alo#000418", "HasTelephoneTypeValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000005", "HasFamilyName");
        m.put(ONTOLOGY_NAMESPACE + "alo#000449", "HasDrugIngredient");
        m.put(ONTOLOGY_NAMESPACE + "alo#000052", "HasInsurnacePolicyName");
        m.put(ONTOLOGY_NAMESPACE + "alo#000053", "HasInsurancePlanName");
        m.put(ONTOLOGY_NAMESPACE + "alo#000412", "HasAddressTypeQualityValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000209", "HasRiskAdjustmentInsuranceClaimTypeValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000252", "HasValidatedClaimPlanNumber");
        m.put(ONTOLOGY_NAMESPACE + "alo#000207", "HasPrimaryMedicalProfessional");
        m.put(ONTOLOGY_NAMESPACE + "alo#000206", "HasCodedEntityName");
        m.put(ONTOLOGY_NAMESPACE + "alo#000205", "HasSourceEncounterId");
        m.put(ONTOLOGY_NAMESPACE + "alo#000149", "HasPageClassification");
        m.put(ONTOLOGY_NAMESPACE + "alo#000201", "HasPatientEncounterSwitchTypeValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000151", "HasIDValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000087", "HasURI");
        m.put(ONTOLOGY_NAMESPACE + "alo#000498", "HasOrganizationNameValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000409", "HasStreetAddress");
        m.put(ONTOLOGY_NAMESPACE + "alo#000408", "HasCity");
        m.put(ONTOLOGY_NAMESPACE + "alo#000407", "HasState");
        m.put(ONTOLOGY_NAMESPACE + "alo#000406", "HasZipCode");
        m.put(ONTOLOGY_NAMESPACE + "alo#000405", "HasCounty");
        m.put(ONTOLOGY_NAMESPACE + "alo#000404", "HasCountry");
        m.put(ONTOLOGY_NAMESPACE + "alo#000403", "HasAddressValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000402", "HasLabResultUnit");
        m.put(ONTOLOGY_NAMESPACE + "alo#9000045", "HasValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000400", "HasClinicalCodeValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000340", "HasFamilyNameValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000138", "HasAddIndicator");
        m.put(ONTOLOGY_NAMESPACE + "alo#000136", "HasTitle");
        m.put(ONTOLOGY_NAMESPACE + "alo#000124", "HasXUUID");
        m.put(ONTOLOGY_NAMESPACE + "alo#000079", "HasErrorCodeValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#888880", "HasGivenName");
        m.put(ONTOLOGY_NAMESPACE + "alo#000131", "HasTextRenderingType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000130", "HasRenderingValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#980040", "HasBitMaskValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000041", "HasAllergyAllergenString");
        m.put(ONTOLOGY_NAMESPACE + "alo#870008", "HasImmunizationMedicationSeriesNumber");
        m.put(ONTOLOGY_NAMESPACE + "alo#000014", "HasReligiousAffiliation");
        m.put(ONTOLOGY_NAMESPACE + "alo#000483", "HasAllergyReactionSeverity");
        m.put(ONTOLOGY_NAMESPACE + "alo#000097", "HasFileDescription");
        m.put(ONTOLOGY_NAMESPACE + "alo#000070", "HasLabResultNumberOfSamples");
        m.put(ONTOLOGY_NAMESPACE + "alo#000338", "HasPrefixNameValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000337", "HasFileURL");
        m.put(ONTOLOGY_NAMESPACE + "alo#023041", "HasAmendmentAnnotationValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000164", "HasClinicalCodingSystemName");
        m.put(ONTOLOGY_NAMESPACE + "alo#443146", "HasSourceValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#012321", "HasPrefixName");
        m.put(ONTOLOGY_NAMESPACE + "alo#000232", "HasAlternateMedicalProfessional");
        m.put(ONTOLOGY_NAMESPACE + "alo#000125", "HasLaboratoryResultValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000350", "HasAnatomicalEntityName");
        m.put(ONTOLOGY_NAMESPACE + "alo#000368", "HasLanguage");
        m.put(ONTOLOGY_NAMESPACE + "alo#220002", "HasImmunizationValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#828028", "HasInsuranceClaimStatusTypeValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000447", "HasDrugUnits");
        m.put(ONTOLOGY_NAMESPACE + "alo#000063", "HasLabResultValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000475", "HasBiometricValueName");
        m.put(ONTOLOGY_NAMESPACE + "alo#000061", "HasSocialHistoryValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000060", "HasSocialHistoryFieldName");
        m.put(ONTOLOGY_NAMESPACE + "alo#000471", "HasProblemSeverity");
        m.put(ONTOLOGY_NAMESPACE + "alo#000034", "HasMIMEType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000082", "HasClinicalRoleType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000336", "HasSuffixNameValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000339", "HasGivenNameValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000320", "HasMaritalStatusQualityValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000117", "HasDocumentTypeValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000059", "HasSocialHistory");
        m.put(ONTOLOGY_NAMESPACE + "alo#000110", "HasProcessorTypeValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000482", "HasAllergyValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000467", "HasLabNote");
        m.put(ONTOLOGY_NAMESPACE + "alo#000466", "HasProblemName");
        m.put(ONTOLOGY_NAMESPACE + "alo#000465", "HasProblemValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000463", "HasCareSiteName");
        m.put(ONTOLOGY_NAMESPACE + "alo#000462", "HasCareSiteValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000461", "HasCareSiteTypeVal");
        m.put(ONTOLOGY_NAMESPACE + "alo#000080", "HasClinicalRole");
        m.put(ONTOLOGY_NAMESPACE + "alo#000317", "HasRange");
        m.put(ONTOLOGY_NAMESPACE + "alo#000311", "HasTimeZone");
        m.put(ONTOLOGY_NAMESPACE + "alo#000256", "InOutputReport");
        m.put(ONTOLOGY_NAMESPACE + "alo#000255", "HasInsuranceClaimErrorCodeFilterValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000254", "RouteToCoder");
        m.put(ONTOLOGY_NAMESPACE + "alo#000198", "HasRAValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000251", "HasValidatedClaimFileMode");
        m.put(ONTOLOGY_NAMESPACE + "alo#000195", "HasSourceTypeValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000105", "HasHash");
        m.put(ONTOLOGY_NAMESPACE + "alo#000192", "HasInputDataTypeValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000047", "HasInsurancePlanType");
        m.put(ONTOLOGY_NAMESPACE + "alo#000043", "HasInsuranceValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000334", "HasNameValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000454", "HasMedicarePlanURI");
        m.put(ONTOLOGY_NAMESPACE + "alo#000399", "HasClinicalCodeLabel");
        m.put(ONTOLOGY_NAMESPACE + "alo#000398", "HasClinicalCodingSystemVersion");
        m.put(ONTOLOGY_NAMESPACE + "alo#000397", "HasClinicalCodingSystemOID");
        m.put(ONTOLOGY_NAMESPACE + "alo#000396", "HasClinicalCodingSystemValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000248", "HasRiskAssessmentCodeClusters");
        m.put(ONTOLOGY_NAMESPACE + "alo#000247", "HasValidatedClaimProviderType");
        m.put(ONTOLOGY_NAMESPACE + "alo#077717", "HasPatientEncounterQualityValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000245", "HasValidatedClaimControlNumber");
        m.put(ONTOLOGY_NAMESPACE + "alo#000244", "HasValidatedClaimPaymentYear");
        m.put(ONTOLOGY_NAMESPACE + "alo#000189", "HasProcessorName");
        m.put(ONTOLOGY_NAMESPACE + "alo#000188", "HasProcessorVersion");
        m.put(ONTOLOGY_NAMESPACE + "alo#000241", "HasCMSValidatedClaimValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000184", "HasLineNumber");
        m.put(ONTOLOGY_NAMESPACE + "alo#000183", "HasPatientEncounterURI");
        m.put(ONTOLOGY_NAMESPACE + "alo#000182", "HasVersion");
        m.put(ONTOLOGY_NAMESPACE + "alo#000039", "HasPhoneNumber");
        m.put(ONTOLOGY_NAMESPACE + "alo#000180", "HasOtherId");
        m.put(ONTOLOGY_NAMESPACE + "alo#000503", "HasDescriptionValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000448", "HasRouteOfAdministration");
        m.put(ONTOLOGY_NAMESPACE + "alo#000501", "HasMedicalProcedureNameValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000446", "HasDrugForm");
        m.put(ONTOLOGY_NAMESPACE + "alo#000445", "HasDrugStrength");
        m.put(ONTOLOGY_NAMESPACE + "alo#000031", "HasMedicalProcedureInterpretation");
        m.put(ONTOLOGY_NAMESPACE + "alo#000443", "HasPrescriptionFrequency");
        m.put(ONTOLOGY_NAMESPACE + "alo#000442", "HasPrescriptionQuantity");
        m.put(ONTOLOGY_NAMESPACE + "alo#000487", "HasLabResultNameValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000239", "HasCMSValidationErrorCodeValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000380", "HasGenderValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000109", "HasDateValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000178", "HasProcessorValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000176", "HasGenericName");
        m.put(ONTOLOGY_NAMESPACE + "alo#000173", "HasImageResolution");
        m.put(ONTOLOGY_NAMESPACE + "alo#345042", "HasOperationAnnotationValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000026", "HasProblemTemporalStatus");
        m.put(ONTOLOGY_NAMESPACE + "alo#000438", "HasSig");
        m.put(ONTOLOGY_NAMESPACE + "alo#000437", "HasMedicationAmount");
        m.put(ONTOLOGY_NAMESPACE + "alo#000023", "HasBiometricValueString");
        m.put(ONTOLOGY_NAMESPACE + "alo#000435", "HasPrescriptionValues");
        m.put(ONTOLOGY_NAMESPACE + "alo#000433", "IsActivePrescription");
        m.put(ONTOLOGY_NAMESPACE + "alo#000497", "HasNameTypeValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000376", "HasInsuranceSequnceNumberValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000038", "HasTelephoneNumberValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#343003", "HasImmunizationQuantity");
        m.put(ONTOLOGY_NAMESPACE + "alo#000225", "HasFeeForServiceInsuranceClaimTypeValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000169", "HasSignedStatus");
        m.put(ONTOLOGY_NAMESPACE + "alo#000168", "HasBrandName");
        m.put(ONTOLOGY_NAMESPACE + "alo#000226", "FeeForServiceInsuranceClaimTypeValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000074", "MaritalStatusValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#054327", "InsuranceClaimStatusValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000199", "PatientEncounterSwitchTypeValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000496", "NameTypeValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000045", "PayorTypeValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000460", "CareSiteTypeValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000126", "DateQualityValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#944014", "PatientEncounterTypeValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000464", "ResolutionStatusTypeValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000081", "ClinicalRoleTypeValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000203", "RiskAdjustmentInsuranceClaimTypeValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#087839", "OperationAnnotationValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000111", "DocumentTypeValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000413", "AddressTypeValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000196", "SourceTypeValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000101", "ProcessorTypeValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#399038", "StatusAnnotationValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000719", "LabFlagValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000419", "TelephoneTypeValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000187", "InputDataTypeValue");
        m.put(ONTOLOGY_NAMESPACE + "alo#000308", "GenderValues");
    }
    public static String getLabelFromURI(String aURI){
        String rm = m.get(aURI);
        return rm;
    }
    public static enum OWLDataType{
    };
}