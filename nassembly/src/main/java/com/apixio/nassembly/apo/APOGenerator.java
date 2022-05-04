package com.apixio.nassembly.apo;

import com.apixio.datacatalog.*;
import com.apixio.datacatalog.CoverageInfoOuterClass;
import com.apixio.model.patient.*;
import com.apixio.model.patient.FamilyHistory;
import com.apixio.nassembly.patient.MergeUtils;
import com.apixio.nassembly.patient.PatientUtils;
import com.apixio.util.nassembly.DataCatalogProtoUtils;
import com.google.protobuf.GeneratedMessageV3;
import scala.collection.JavaConverters;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class APOGenerator {

    public static Patient mergeSkinny(List<GeneratedMessageV3> skinnyProtos) {
        List<PatientProto.Patient> patients = skinnyProtos
                .stream()
                .map(PatientUtils::convertSkinny)
                .collect(Collectors.toList());

        return merge(patients);
    }

    public static Patient merge(List<PatientProto.Patient> patientProtos) {
        if (patientProtos.isEmpty()) return null;

        // Consolidate the wrappers
        // This will take care of all the source / parsing details / clinical actor consolidation
        // TODO: Should when should we use complex merge?
        PatientProto.Patient consolidatedWrapper = MergeUtils.mergePatients(JavaConverters.asScalaBufferConverter(patientProtos).asScala().toList(), false);

        return fromProto(consolidatedWrapper);
    }

    public static List<Source> convertSources(List<SourceOuterClass.Source> sourcesList, List<BaseObjects.Coverage> coverages) {
        return sourcesList.stream()
                .map(s -> {
                    Optional<CoverageInfoOuterClass.CoverageInfo> coverageWithSpan = coverages
                            .stream()
                            .filter(c -> c.getBase().getSourceIdsCount() > 0)
                            .filter(c -> c.getBase().getSourceIds(0).equals(s.getInternalId()))
                            .findFirst()
                            .map(BaseObjects.Coverage::getCoverageInfo);

                    return BaseConverter.convertSource(s, coverageWithSpan);
                })
                .collect(Collectors.toList());
    }

    public static List<ParsingDetail> convertParsingDetails(List<ParsingDetailsOuterClass.ParsingDetails> details) {
        return details.stream()
                .map(BaseConverter::convertParsingDetails)
                .collect(Collectors.toList());
    }

    public static List<ClinicalActor> convertClinicalActors(List<BaseObjects.ClinicalActor> actors) {
        return actors.stream()
                .map(BaseConverter::convertClinicalActor)
                .collect(Collectors.toList());
    }

    public static List<Encounter> convertEncounters(List<CodedBaseObjects.Encounter> encounters) {
        return encounters.stream()
                .map(CodedBaseConverter::convertEncounter)
                .collect(Collectors.toList());
    }

    public static List<CareSite> convertCareSites(PatientProto.Patient patient) {
        return PatientUtils.getCareSitesList(patient)
                .stream()
                .map(BaseConverter::convertCareSite)
                .collect(Collectors.toList());
    }

    public static List<Allergy> convertAllergies(List<CodedBaseObjects.Allergy> allergies) {
        return allergies.stream()
                .map(CodedBaseConverter::convertAllergy)
                .collect(Collectors.toList());
    }

    public static List<Coverage> convertCoverages(List<BaseObjects.Coverage> coverages) {
        return coverages.stream()
                .map(BaseConverter::convertCoverage)
                .collect(Collectors.toList());
    }

    public static List<BiometricValue> convertBiometricValues(List<CodedBaseObjects.BiometricValue> allergies) {
        return allergies.stream()
                .map(CodedBaseConverter::convertBiometricValue)
                .collect(Collectors.toList());
    }

    public static List<Procedure> convertProcedures(List<CodedBaseObjects.ProcedureCBO> procedures) {
        return procedures
                .stream()
                .map(CodedBaseConverter::covertProcedure)
                .collect(Collectors.toList());
    }

    public static List<Procedure> convertFfsClaims(List<CodedBaseObjects.FfsClaimCBO> ffsClaims) {
        return ffsClaims
                .stream()
                .map(CodedBaseConverter::covertFfsClaim)
                .collect(Collectors.toList());
    }

    public static List<Problem> convertProblems(List<CodedBaseObjects.ProblemCBO> problems) {
        return problems
                .stream()
                .map(CodedBaseConverter::convertProblem)
                .collect(Collectors.toList());
    }

    public static List<Problem> convertRaClaims(List<CodedBaseObjects.RaClaimCBO> problems) {
        return problems
                .stream()
                .map(CodedBaseConverter::convertRaClaim)
                .collect(Collectors.toList());
    }

    public static List<Problem> convertMao004s(List<CodedBaseObjects.Mao004CBO> problems) {
        return problems
                .stream()
                .map(CodedBaseConverter::convertMao004)
                .collect(Collectors.toList());
    }


    public static List<Document> convertDocumentMetadata(List<CodedBaseObjects.DocumentMetaCBO> documents) {
        return documents
                .stream()
                .map(doc -> {
                    try {
                        return CodedBaseConverter.convertDocument(doc);
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    public static List<ContactDetails> convertAlternateContactDetails(PatientProto.Patient patient) {
        return patient
                .getAlternateContactDetailsList()
                .stream()
                .map(BaseConverter::convertContactDetails)
                .collect(Collectors.toList());
    }

    public static List<SocialHistory> convertSocialHistories(List<CodedBaseObjects.SocialHistory> histories) {
        return histories
                .stream()
                .map(CodedBaseConverter::convertSocialHistory)
                .collect(Collectors.toList());
    }

    public static List<Demographics> convertDemographics(List<BaseObjects.PatientDemographics> demographics) {
        return demographics
                .stream()
                .map(BaseConverter::convertDemographic)
                .collect(Collectors.toList());
    }

    public static List<LabResult> convertLabResults(List<CodedBaseObjects.LabResultCBO> labResults) {
        return labResults
                .stream()
                .map(CodedBaseConverter::convertLabResults)
                .collect(Collectors.toList());
    }

    public static List<Prescription> convertPrescriptions(
            List<CodedBaseObjects.PrescriptionCBO> prescriptionProtoList) {
        return prescriptionProtoList
                .stream()
                .map(CodedBaseConverter::convertPrescriptions)
                .collect(Collectors.toList());
    }

    public static List<FamilyHistory> convertFamilyHistory(
            List<CodedBaseObjects.FamilyHistoryCBO> fhProtoList) {
        return fhProtoList
                .stream()
                .map(CodedBaseConverter::convertFamilyHistory)
                .collect(Collectors.toList());
    }

    public static List<Administration> convertImmunization(
            List<CodedBaseObjects.ImmunizationCBO> adminImmuProtoList) {
        return adminImmuProtoList
                .stream()
                .map(CodedBaseConverter::convertImmunization)
                .collect(Collectors.toList());
    }

    /**
     * Convert a skinny Patient to a java Patient
     * This particular "skinny patient hydration" is special because clinical actor are a part of coded base objects
     * @param proto Skinny Patient with clinical actors
     * @return null if proto is null
     */
    public static Patient fromSkinnyActor(SkinnyPatientProto.ClinicalActors proto) {
        if (proto == null)
            return null;

        PatientProto.Patient patientProto = PatientUtils.convertSkinnyClinicalActor(proto);
        return fromProto(patientProto);
    }

    public static Patient fromProto(PatientProto.Patient patientProto) {
        if (patientProto == null)
            return null;

        Patient patient = new Patient();

        PatientMetaProto.PatientMeta patientMeta = getCodedBasePatient(patientProto).getPatientMeta();

        // Add patient Id
        UUID patientId = DataCatalogProtoUtils.convertUuid(patientMeta.getPatientId());
        patient.setPatientId(patientId);

        // Add Primary ExternalId
        ExternalID primaryExternalId = BaseConverter.convertExternalId(patientMeta.getPrimaryExternalId());
        patient.setPrimaryExternalID(primaryExternalId);

        // add externalIds
        patientMeta.getExternalIdsList().stream()
                .map(BaseConverter::convertExternalId)
                .forEach(patient::addExternalId);

        PatientProto.CodedBasePatient codedBase = getCodedBasePatient(patientProto);
        // Set Normalized Data
        patient.setSources(convertSources(codedBase.getSourcesList(), patientProto.getCoverageList()));
        patient.setParsingDetails(convertParsingDetails(codedBase.getParsingDetailsList()));
        patient.setEncounters(convertEncounters(codedBase.getEncountersList()));
        patient.setCareSites(convertCareSites(patientProto));
        patient.setClinicalActors(convertClinicalActors(codedBase.getClinicalActorsList()));

        // Set summary data
        patient.setDocuments(convertDocumentMetadata(patientProto.getDocumentsList()));
        patient.setSocialHistories(convertSocialHistories(patientProto.getSocialHistoriesList()));
        patient.setBiometricValues(convertBiometricValues(patientProto.getBiometricValuesList()));
        patient.setAllergies(convertAllergies(patientProto.getAllergiesList()));
        patient.setCoverage(convertCoverages(patientProto.getCoverageList()));

        // Procedures = FFS Claims + Base Procedures
        List<Procedure> procedures = convertProcedures(patientProto.getProceduresList());
        procedures.addAll(convertFfsClaims(patientProto.getFfsClaimsList()));
        patient.setProcedures(procedures);

        // Problems = Ra Claims + Mao004 + Base Problems
        List<Problem> problems = convertProblems(patientProto.getProblemsList());
        problems.addAll(convertRaClaims(patientProto.getRaClaimsList()));
        problems.addAll(convertMao004s(patientProto.getMao004SList()));
        patient.setProblems(problems);

        // Primary Contact Details
        if (patientProto.hasPrimaryContactDetails()) {
            patient.setPrimaryContactDetails(BaseConverter.convertContactDetails(patientProto.getPrimaryContactDetails()));
        }
        // Alternate Contact Details
        patient.setAlternateContactDetails(convertAlternateContactDetails(patientProto));

        //primary demographics
        if (patientProto.hasPrimaryDemographics()) {
            patient.setPrimaryDemographics(BaseConverter.convertDemographic(patientProto.getPrimaryDemographics()));
        }
        //alternate demographics
        patient.setAlternateDemographics(convertDemographics(patientProto.getAlternateDemographicsList()));
        patient.setLabs(convertLabResults(patientProto.getLabResultsList()));
        patient.setPrescriptions(convertPrescriptions(patientProto.getPrescriptionsList()));
        patient.setFamilyHistories(convertFamilyHistory(patientProto.getFamilyHistoriesList()));
        patient.setAdministrations(convertImmunization(patientProto.getImmunizationsList()));

        // TODO!!!! When we add a new datatype, we need to add it to our patient apo conversion AND MERGER

        return patient;
    }

    private static PatientProto.CodedBasePatient getCodedBasePatient(PatientProto.Patient patientProto) {
        return patientProto.getBase();
    }

}