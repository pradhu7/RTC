package com.apixio.nassembly.apo;

import com.apixio.model.EitherStringOrNumber;
import com.apixio.datacatalog.*;
import com.apixio.model.external.AxmConstants;
import com.apixio.model.patient.*;
import com.apixio.nassembly.apo.converterutils.ArgumentUtil;
import com.apixio.nassembly.apo.converterutils.DocumentConverterUtils;
import com.apixio.nassembly.labresult.LabResultMetadataKeys;
import com.apixio.nassembly.problem.ProblemMetadataKeys;
import com.apixio.nassembly.procedure.ProcedureMetadataKeys;
import com.apixio.util.nassembly.CaretParser;
import com.apixio.util.nassembly.DataCatalogProtoUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.apixio.nassembly.apo.BaseConverter.*;
import static com.apixio.util.nassembly.DataCatalogProtoUtils.*;

public class CodedBaseConverter {

    public static Procedure covertProcedure(CodedBaseObjects.ProcedureCBO proto) {
        ProcedureProto.ProcedureInfo infoProto = proto.getProcedureInfo();
        Procedure procedure = new Procedure();
        procedure.setProcedureName(infoProto.getProcedureName());
        if (infoProto.hasEndDate()) procedure.setEndDate(toDateTime(infoProto.getEndDate()));
        procedure.setInterpretation(infoProto.getInterpretation());
        if (infoProto.hasPerformedOn()) procedure.setPerformedOn(toDateTime(infoProto.getPerformedOn()));
        if (infoProto.hasBodySite()) procedure.setBodySite(convertAnatomy(infoProto.getBodySite()));

        // Coded Base object
        setCode(procedure, infoProto.getCode());
        addCodeTranslations(procedure, infoProto.getCodeTranslationsList());

        // Cpt Modifiers
        CptModifierOuterClass.CptModifier modifier = infoProto.getCptModifier();
        if (!modifier.getModifier1().isEmpty())
            procedure.setMetaTag("MODIFIER1", modifier.getModifier1());
        if (!modifier.getModifier2().isEmpty())
            procedure.setMetaTag("MODIFIER2", modifier.getModifier2());
        if (!modifier.getModifier3().isEmpty())
            procedure.setMetaTag("MODIFIER3", modifier.getModifier3());
        if (!modifier.getModifier4().isEmpty())
            procedure.setMetaTag("MODIFIER4", modifier.getModifier4());

        procedure.setSupportingDiagnosis(
                proto
                        .getSupportingDiagnosisCodesList()
                        .stream()
                        .map(BaseConverter::convertCode)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
        );

        // Normalize data
        normalizeAndSetCodedBaseData(proto.getBase(), procedure);

        // Backwards compatibility by shoving top-level data back into metadata
        if (infoProto.getDeleteIndicator()) procedure.setMetaTag("DELETE_INDICATOR", String.valueOf(true));

        return procedure;
    }

    public static Procedure covertFfsClaim(CodedBaseObjects.FfsClaimCBO proto) {
        CodedBaseObjects.ProcedureCBO procedureCBO = CodedBaseObjects.ProcedureCBO.newBuilder()
                .setBase(proto.getBase())
                .addAllSupportingDiagnosisCodes(proto.getSupportingDiagnosisCodesList())
                .setProcedureInfo(proto.getFfsClaim().getProcedureInfo())
                .build();

        Procedure rawProcedure = covertProcedure(procedureCBO);
        rawProcedure.setMetaTag(AxmConstants.CLAIM_TYPE, AxmConstants.FEE_FOR_SERVICE_CLAIM);
        addBillingInfo(rawProcedure, proto.getFfsClaim().getBillingInfo());
        return rawProcedure;
    }

    private static void addBillingInfo(Procedure procedure, BillingInfoOuterClass.BillingInfo billingInfo) {
        Map<String, String> metadata = procedure.getMetadata();
        if (billingInfo.getSerializedSize() > 0) {


            if (billingInfo.hasBillType() && billingInfo.getBillType().getSerializedSize() > 0)
                wrapMetadataUpdate(metadata, ProcedureMetadataKeys.BILL_TYPE().toString(),
                        CaretParser.toString(billingInfo.getBillType()));

            if (billingInfo.hasProviderType() && billingInfo.getProviderType().getSerializedSize() > 0)
                wrapMetadataUpdate(metadata, ProcedureMetadataKeys.PROVIDER_TYPE().toString(),
                        CaretParser.toString(billingInfo.getProviderType()));

            if (!billingInfo.getPlaceOfService().isEmpty())
                wrapMetadataUpdate(metadata, ProcedureMetadataKeys.PLACE_OF_SERVICE().toString(),
                        billingInfo.getPlaceOfService());

            if (billingInfo.hasProviderId())
                wrapMetadataUpdate(metadata, ProcedureMetadataKeys.BILLING_PROVIDER_ID().toString(),
                        CaretParser.toString(billingInfo.getProviderId()));

            if (!billingInfo.getProviderName().isEmpty())
                wrapMetadataUpdate(metadata, ProcedureMetadataKeys.BILLING_PROVIDER_NAME().toString(),
                        billingInfo.getProviderName());

            if (billingInfo.hasTransactionDate())
                wrapMetadataUpdate(metadata, ProcedureMetadataKeys.TRANSACTION_DATE().toString(),
                        dateToYYYYMMDD(billingInfo.getTransactionDate()));
        }
    }

    public static Problem convertProblem(CodedBaseObjects.ProblemCBO proto) {
        ProblemInfoOuterClass.ProblemInfo infoProto = proto.getProblemInfo();
        Problem problem = new Problem();
        if (infoProto.hasStartDate()) problem.setStartDate(toDateTime(infoProto.getStartDate()));
        if (infoProto.hasEndDate()) problem.setEndDate(toDateTime(infoProto.getEndDate()));
        problem.setProblemName(infoProto.getName());
        problem.setResolutionStatus(convertResolutionStatus(infoProto.getResolutionStatus()));
        problem.setTemporalStatus(infoProto.getTemporalStatus());
        if (infoProto.hasDiagnosisDate()) problem.setDiagnosisDate(toDateTime(infoProto.getDiagnosisDate()));
        problem.setSeverity(infoProto.getSeverity());

        // Coded Base object
        setCode(problem, infoProto.getCode());
        addCodeTranslations(problem, infoProto.getCodeTranslationsList());
        normalizeAndSetCodedBaseData(proto.getBase(), problem);

        // Backwards compatibility by shoving top-level data back into metadata
        if (infoProto.getDeleteIndicator()) problem.setMetaTag("DELETE_INDICATOR", String.valueOf(true).toUpperCase());

        return problem;
    }

    public static Problem convertRaClaim(CodedBaseObjects.RaClaimCBO proto) {
        CodedBaseObjects.ProblemCBO problemCBO = CodedBaseObjects.ProblemCBO.newBuilder()
                .setBase(proto.getBase())
                .setProblemInfo(proto.getRaClaim().getProblemInfo())
                .build();

        Problem problem = convertProblem(problemCBO);
        addRaClaimMeta(problem, proto.getRaClaim());

        return problem;
    }

    public static Problem convertMao004(CodedBaseObjects.Mao004CBO proto) {
        CodedBaseObjects.ProblemCBO problemCBO = CodedBaseObjects.ProblemCBO.newBuilder()
                .setBase(proto.getBase())
                .setProblemInfo(proto.getMao004().getProblemInfo())
                .build();

        Problem problem = convertProblem(problemCBO);
        addMao004Meta(problem, proto.getMao004());

        return problem;
    }

    public static void addRaClaimMeta(Problem problem, RiskAdjustmentClaimOuterClass.RiskAdjustmentClaim proto) {
        Map<String, String> metadata = problem.getMetadata();

        metadata.put(AxmConstants.CLAIM_TYPE, AxmConstants.RISK_ADJUSTMENT_CLAIM);

        // Ra Claim Meta
        wrapMetadataUpdate(metadata, ProblemMetadataKeys.TRANSACTION_DATE().toString(),
                dateToYYYYMMDD(proto.getTransactionDate()));
        wrapMetadataUpdate(metadata, ProblemMetadataKeys.FILE_MODE().toString(), proto.getFileMode());
        wrapMetadataUpdate(metadata, ProblemMetadataKeys.PLAN_NUMBER().toString(), proto.getPlanNumber());
        wrapMetadataUpdate(metadata, ProblemMetadataKeys.OVERPAYMENT_ID().toString(), proto.getOverpaymentId());
        wrapMetadataUpdate(metadata, ProblemMetadataKeys.OVERPAYMENT_ID_ERROR_CODE().toString(),
                proto.getOverpaymentIdErrorCode());
        wrapMetadataUpdate(metadata, ProblemMetadataKeys.PAYMENT_YEAR().toString(), proto.getPaymentYear());
        wrapMetadataUpdate(metadata, ProblemMetadataKeys.PAYMENT_YEAR_ERROR().toString(), proto.getPaymentYearError());
        wrapMetadataUpdate(metadata, ProblemMetadataKeys.DETAIL_NUMBER_ERROR().toString(),
                proto.getDetailErrorNumber());
        wrapMetadataUpdate(metadata, ProblemMetadataKeys.PATIENT_CONTROL_NUMBER().toString(),
                proto.getPatientControlNumber());
        wrapMetadataUpdate(metadata, ProblemMetadataKeys.PATIENT_HIC_NUMBER_ERROR().toString(),
                proto.getPatientHicNumberError());
        wrapMetadataUpdate(metadata, ProblemMetadataKeys.CORRECTED_HIC_NUMBER().toString(),
                proto.getCorrectedHicNumber());
        wrapMetadataUpdate(metadata, ProblemMetadataKeys.PATIENT_DATE_OF_BIRTH().toString(),
                dateToYYYYMMDD(proto.getPatientDob()));
        wrapMetadataUpdate(metadata, ProblemMetadataKeys.PATIENT_DATE_OF_BIRTH_ERROR().toString(),
                proto.getPatientDobError());
        wrapMetadataUpdate(metadata, ProblemMetadataKeys.PROVIDER_TYPE().toString(), proto.getProviderType());
        wrapMetadataUpdate(metadata, ProblemMetadataKeys.DIAGNOSIS_CLUSTER_ERROR1().toString(),
                proto.getDiagnosisClusterError1());
        wrapMetadataUpdate(metadata, ProblemMetadataKeys.DIAGNOSIS_CLUSTER_ERROR2().toString(),
                proto.getDiagnosisClusterError2());
        wrapMetadataUpdate(metadata, ProblemMetadataKeys.RISK_ASSESSMENT_CODE_CLUSTERS().toString(),
                String.join(",", proto.getRiskAssessmentCodeClustersList()));

        cleanseMetadata(metadata);

    }

    public static void addMao004Meta(Problem problem, Mao004OuterClass.Mao004 proto) {
        Map<String, String> metadata = problem.getMetadata();

        metadata.put(AxmConstants.CLAIM_TYPE, AxmConstants.MAO_004_ENCOUNTER_CLAIM);

        wrapMetadataUpdate(metadata, ProblemMetadataKeys.HICN().toString(), proto.getHicn());
        wrapMetadataUpdate(metadata, ProblemMetadataKeys.PLAN_SUBMISSION_DATE().toString(),
                dateToYYYYMMDD(proto.getPlanSubmissionDate()));
        wrapMetadataUpdate(metadata, ProblemMetadataKeys.ENCOUNTER_CLAIM_TYPE().toString(),
                proto.getEncounterClaimType());
        wrapMetadataUpdate(metadata, ProblemMetadataKeys.MA_CONTRACT_ID().toString(), proto.getMaContractId());
        wrapMetadataUpdate(metadata, ProblemMetadataKeys.ORIGINAL_ENCOUNTER_ICN().toString(),
                proto.getOriginalEncounterIcn());
        wrapMetadataUpdate(metadata, ProblemMetadataKeys.ENCOUNTER_TYPE_SWITCH().toString(),
                proto.getEncounterTypeSwitch());
        wrapMetadataUpdate(metadata, ProblemMetadataKeys.REPLACEMENT_ENCOUNTER_SWITCH().toString(),
                proto.getReplacementEncounterSwitch());
        wrapMetadataUpdate(metadata, ProblemMetadataKeys.ENCOUNTER_ICN().toString(), proto.getEncounterIcn());
        wrapMetadataUpdate(metadata, ProblemMetadataKeys.SUBMISSION_FILE_TYPE().toString(),
                proto.getSubmissionFileType());
        wrapMetadataUpdate(metadata, ProblemMetadataKeys.REPORT_DATE().toString(),
                dateToYYYYMMDD(proto.getReportDate()));
        wrapMetadataUpdate(metadata, ProblemMetadataKeys.PROCESSING_DATE().toString(),
                dateToYYYYMMDD(proto.getProcessingDate()));
        wrapMetadataUpdate(metadata, ProblemMetadataKeys.SUBMISSION_INTERCHANGE_NUMBER().toString(),
                proto.getSubmissionInterchangeNumber());

        cleanseMetadata(metadata);
    }

    private static void normalizeAndSetCodedBaseData(CodedBaseObjects.CodedBase codedBase,
                                                     CodedBaseObject codedBaseObject) {
        if (codedBase.getParsingDetailsIdsCount() > 0)
            codedBaseObject.setParsingDetailsId(convertUuid(codedBase.getParsingDetailsIds(0))); // Default to the

        // Default to the head of the list
        if (codedBase.getSourceIdsCount() > 0)
            codedBaseObject.setSourceId(convertUuid(codedBase.getSourceIds(0))); // Default to the head of the list

        // Set originalIds / last editTime
        BaseConverter.applyDataCatalogMeta(codedBaseObject, codedBase.getDataCatalogMeta());

        // head of the list
        if (codedBase.hasPrimaryActorId())
            codedBaseObject.setPrimaryClinicalActorId(convertUuid(codedBase.getPrimaryActorId()));

        setSupplementaryActors(codedBaseObject, codedBase.getSupplementaryActorIdsList());

        if (codedBase.getEncounterIdsCount() > 0)
            codedBaseObject.setSourceEncounter(convertUuid(codedBase.getEncounterIds(0))); // Not used yet


    }

    public static Encounter convertEncounter(CodedBaseObjects.Encounter proto) {
        if (proto == null) return null;

        EncounterInfoOuterClass.EncounterInfo encounterInfo = proto.getEncounterInfo();

        Encounter encounter = new Encounter();
        if (encounterInfo.hasStartDate()) encounter.setEncounterStartDate(toDateTime(encounterInfo.getStartDate()));
        if (encounterInfo.hasEndDate()) encounter.setEncounterEndDate(toDateTime(encounterInfo.getEndDate()));
        if (StringUtils.isNotBlank(encounterInfo.getEncounterType()))
            encounter.setEncType(EncounterType.valueOf(encounterInfo.getEncounterType().toUpperCase()));
        if (encounterInfo.hasCaresite())
            encounter.setSiteOfService(convertCareSite(encounterInfo.getCaresite()));
        encounter.setChiefComplaints(encounterInfo
                .getChiefComplaintsList()
                .stream()
                .map(BaseConverter::convertCode)
                .collect(Collectors.toList())
        );


        // Base Object
        applyDataCatalogMeta(encounter, proto.getDataCatalogMeta());
        setCode(encounter, encounterInfo.getCode());
        addCodeTranslations(encounter, encounterInfo.getCodeTranslationsList());

        // Normalize the data
        if (proto.getParsingDetailsIdsCount() > 0)
            encounter
                    .setParsingDetailsId(convertUuid(proto.getParsingDetailsIds(0))); // Default to the head of the list
        if (proto.getSourceIdsCount() > 0)
            encounter.setSourceId(convertUuid(proto.getSourceIds(0))); // Default to the head of the list

        encounter.setPrimaryClinicalActorId(convertUuid(proto.getPrimaryActorId()));
        setSupplementaryActors(encounter, proto.getSupplementaryActorIdsList());
        encounter.setSourceEncounter(convertUuid(proto.getInternalId())); // Not used yet


        return encounter;
    }

    public static Allergy convertAllergy(CodedBaseObjects.Allergy proto) {
        if (proto == null) return null;

        AllergyInfoOuterClass.AllergyInfo allergyInfo = proto.getAllergyInfo();

        Allergy allergy = new Allergy();
        allergy.setAllergen(allergyInfo.getAllergen());

        if (allergyInfo.hasReaction()) {
            ClinicalCode reaction = BaseConverter.convertCode(allergyInfo.getReaction());
            allergy.setReaction(reaction);
        }

        allergy.setReactionSeverity(allergyInfo.getReactionSeverity());

        if (allergyInfo.hasReactionDate()){
            DateTime reactionDate = toDateTime(allergyInfo.getReactionDate());
            allergy.setReactionDate(reactionDate);
        }

        if (allergyInfo.hasDiagnosisDate()) {
            DateTime diagnosisDate = toDateTime(allergyInfo.getDiagnosisDate());
            allergy.setDiagnosisDate(diagnosisDate);
        }

        if (allergyInfo.hasResolvedDate()){
            DateTime resolvedDate = toDateTime(allergyInfo.getResolvedDate());
            allergy.setResolvedDate(resolvedDate);
        }

        // Coded Object
        setCode(allergy, allergyInfo.getCode());
        addCodeTranslations(allergy, allergyInfo.getCodeTranslationsList());
        normalizeAndSetCodedBaseData(proto.getBase(), allergy);

        return allergy;
    }


    public static SocialHistory convertSocialHistory(CodedBaseObjects.SocialHistory proto) {
        if (proto == null) return null;

        SocialHistoryInfoOuterClass.SocialHistoryInfo historyInfo = proto.getSocialHistoryInfo();

        SocialHistory history = new SocialHistory();

        if (historyInfo.hasDate()){
            DateTime date = toDateTime(historyInfo.getDate());
            history.setDate(date);
        }

        if (historyInfo.hasType()) {
            ClinicalCode type = convertCode(historyInfo.getType());
            history.setType(type);
        }

        history.setFieldName(historyInfo.getFieldName());
        history.setValue(historyInfo.getValue());


        // Coded Base Object
        setCode(history, historyInfo.getCode());
        addCodeTranslations(history, historyInfo.getCodeTranslationsList());
        normalizeAndSetCodedBaseData(proto.getBase(), history);

        return history;
    }

    public static BiometricValue convertBiometricValue(CodedBaseObjects.BiometricValue proto) {
        if (proto == null) return null;

        VitalSignOuterClass.VitalSign vitalSign = proto.getVitalSign();

        BiometricValue biometricValue = new BiometricValue();

        biometricValue.setName(vitalSign.getName());
        biometricValue.setUnitsOfMeasure(vitalSign.getUnitsOfMeasure());

        if (vitalSign.hasResultDate())
            biometricValue.setResultDate(toDateTime(vitalSign.getResultDate()));

        if (vitalSign.hasValue()){
            EitherStringOrNumber either = convertValueAsString(vitalSign.getValue());
            biometricValue.setValue(either);
        }

        // Coded Base Object
        setCode(biometricValue, vitalSign.getCode());
        addCodeTranslations(biometricValue, vitalSign.getCodeTranslationsList());
        normalizeAndSetCodedBaseData(proto.getBase(), biometricValue);

        return biometricValue;
    }

    public static Document convertDocument(CodedBaseObjects.DocumentMetaCBO documentMetaCBO) throws URISyntaxException {
        if (documentMetaCBO == null) return null;

        Document document = new Document();

        DocumentMetaOuterClass.DocumentMeta documentMeta = documentMetaCBO.getDocumentMeta();

        document.setDocumentDate(toDateTime(documentMeta.getDocumentDate()));
        document.setDocumentTitle(documentMeta.getDocumentTitle());
        document.setInternalUUID(convertUuid(documentMeta.getUuid()));

        DocumentConverterUtils.setDocumentContents(document, documentMeta);
        DocumentConverterUtils.setMetadatas(document, documentMeta);

        // Coded Base Object
        setCode(document, documentMeta.getCode());
        addCodeTranslations(document, documentMeta.getCodeTranslationsList());
        normalizeAndSetCodedBaseData(documentMetaCBO.getBase(), document);

        return document;
    }

    public static LabResult convertLabResults(CodedBaseObjects.LabResultCBO labResultCBO) {
        if (labResultCBO == null) return null;

        LabResultProto.LabResultInfo labResultInfo = labResultCBO.getLabResultInfo();
        LabResult labResultApo = new LabResult();

        if (ArgumentUtil.isNotEmpty(labResultInfo.getName()))
            labResultApo.setLabName(labResultInfo.getName());

        if (labResultInfo.hasValue()) {
            EitherStringOrNumber either = convertValueAsString(labResultInfo.getValue());
            if (ArgumentUtil.isNotNull(either)) labResultApo.setValue(either);
        }

        if (ArgumentUtil.isNotEmpty(labResultInfo.getRange()))
            labResultApo.setRange(labResultInfo.getRange());

        labResultApo.setFlag(convertLabFlag(labResultInfo.getFlag()));

        if (ArgumentUtil.isNotEmpty(labResultInfo.getUnits())) labResultApo.setUnits(labResultInfo.getUnits());
        if (ArgumentUtil.isNotEmpty(labResultInfo.getLabNote())) labResultApo.setLabNote(labResultInfo.getLabNote());


        if (labResultInfo.hasSpecimen())
            labResultApo.setPanel(convertCode(labResultInfo.getSpecimen()));
        if (labResultInfo.hasPanel())
            labResultApo.setPanel(convertCode(labResultInfo.getPanel()));
        if (labResultInfo.hasSuperPanel())
            labResultApo.setPanel(convertCode(labResultInfo.getSuperPanel()));


        if (labResultInfo.hasSampleDate())
            labResultApo.setSampleDate(toDateTime(labResultInfo.getSampleDate()));

        if (labResultInfo.hasCareSite())
            labResultApo.setCareSiteId(UUID.fromString(labResultInfo.getCareSite().getInternalId().getUuid()));

        List<TypedDate> typedDates = labResultInfo.getOtherDatesList().stream()
                .map(CodedBaseConverter::convertTypedDate)
                .collect(Collectors.toList());
        if (!typedDates.isEmpty()) labResultApo.setOtherDates(typedDates);

        if (labResultInfo.hasSequenceNumber())
            labResultApo.setSequenceNumber(labResultInfo.getSequenceNumber().getValue());

        if (labResultInfo.hasVendorDetails()) {
            if (ArgumentUtil.isNotEmpty(labResultInfo.getVendorDetails().getVendorCode())) {
                labResultApo.setMetaTag(LabResultMetadataKeys.VENDOR_CODE().toString(), labResultInfo.getVendorDetails().getVendorCode());
            }
            if (ArgumentUtil.isNotEmpty(labResultInfo.getVendorDetails().getVendorId())) {
                labResultApo.setMetaTag(LabResultMetadataKeys.VENDOR_ID().toString(), labResultInfo.getVendorDetails().getVendorId());
            }
        }

        // Coded Base Object
        setCode(labResultApo, labResultInfo.getCode());
        addCodeTranslations(labResultApo, labResultInfo.getCodeTranslationsList());
        normalizeAndSetCodedBaseData(labResultCBO.getBase(), labResultApo);

        return labResultApo;
    }

    public static TypedDate convertTypedDate(LabResultProto.TypedDate typedDate) {
        TypedDate date = new TypedDate();
        date.setType(typedDate.getType());
        date.setDate(new DateTime(typedDate.getEpochMs()));
        return date;
    }

    public static Prescription convertPrescriptions(CodedBaseObjects.PrescriptionCBO prescriptionCBO) {
        if (prescriptionCBO == null) return null;

        PrescriptionProto.PrescriptionInfo prescriptionInfo = prescriptionCBO.getPrescriptionInfo();
        Prescription prescriptionModel = new Prescription();

        if (prescriptionInfo.hasEndDate())
            prescriptionModel.setEndDate(toDateTime(prescriptionInfo.getEndDate()));
        if (prescriptionInfo.hasFillDate())
            prescriptionModel.setFillDate(toDateTime(prescriptionInfo.getFillDate()));
        if (prescriptionInfo.hasPrescriptionDate())
            prescriptionModel.setPrescriptionDate(toDateTime(prescriptionInfo.getPrescriptionDate()));
        if (ArgumentUtil.isNotEmpty(prescriptionInfo.getAmount()))
            prescriptionModel.setAmount(prescriptionInfo.getAmount());
        if (ArgumentUtil.isNotEmpty(prescriptionInfo.getDirections()))
            prescriptionModel.setSig(prescriptionInfo.getDirections());

        if (prescriptionInfo.hasIsActivePrescription())
            prescriptionModel.setActivePrescription(prescriptionInfo.getIsActivePrescription().getValue());

        if (prescriptionInfo.hasRefillsRemaining())
            prescriptionModel.setRefillsRemaining(prescriptionInfo.getRefillsRemaining().getValue());

        prescriptionModel.setDosage(prescriptionInfo.getDosage());

        if (prescriptionInfo.hasQuantity())
            prescriptionModel.setQuantity(prescriptionInfo.getQuantity().getValue());

        prescriptionModel.setFrequency(prescriptionInfo.getFrequency());

        if (prescriptionInfo.hasAssociatedMedication()) {
            prescriptionModel.setAssociatedMedication(convertMedication(prescriptionInfo.getAssociatedMedication()));
        }


        // Coded Base Object
        setCode(prescriptionModel, prescriptionInfo.getCode());
        addCodeTranslations(prescriptionModel, prescriptionInfo.getCodeTranslationsList());
        normalizeAndSetCodedBaseData(prescriptionCBO.getBase(), prescriptionModel);

        return prescriptionModel;
    }

    public static FamilyHistory convertFamilyHistory(
            CodedBaseObjects.FamilyHistoryCBO familyHistoryCBO) {
        if (familyHistoryCBO == null) return null;

        FamilyHistoryProto.FamilyHistoryInfo fhProto = familyHistoryCBO.getFamilyHistoryInfo();
        FamilyHistory fhApo = new FamilyHistory();

        if (StringUtils.isNotBlank(fhProto.getFamilyHistory()))
            fhApo.setFamilyHistory(fhProto.getFamilyHistory());

        // Base Object
        setCode(fhApo, fhProto.getCode());
        addCodeTranslations(fhApo, fhProto.getCodeTranslationsList());
        normalizeAndSetCodedBaseData(familyHistoryCBO.getBase(), fhApo);

        return fhApo;
    }

    public static Administration convertImmunization(
            CodedBaseObjects.ImmunizationCBO immunizationCBO) {
        if (immunizationCBO == null) return null;

        Administration administration = new Administration();
        ImmunizationProto.ImmunizationInfo immunizationProto = immunizationCBO.getImmunizationInfo();


        if (immunizationCBO.getImmunizationInfo().hasMedicationInfo()) {
            administration.setMedication(convertMedication(immunizationProto.getMedicationInfo()));
        }


        if (immunizationProto.hasAdminDate())
            administration.setAdminDate(toDateTime(immunizationProto.getAdminDate()));

        if (immunizationProto.hasQuantity())
            administration.setQuantity(immunizationProto.getQuantity().getValue());
        if (ArgumentUtil.isNotEmpty(immunizationProto.getAmount()))
            administration.setAmount(immunizationProto.getAmount());
        if (ArgumentUtil.isNotEmpty(immunizationProto.getDosage()))
            administration.setDosage(immunizationProto.getDosage());
        if (immunizationProto.hasStartDate())
            administration.setStartDate(toDateTime(immunizationProto.getStartDate()));
        if (immunizationProto.hasEndDate())
            administration.setEndDate(toDateTime(immunizationProto.getEndDate()));

        if (immunizationProto.hasMedicationSeriesNumber())
            administration.setMedicationSeriesNumber(immunizationProto.getMedicationSeriesNumber().getValue());

        // Coded Base Object
        setCode(administration, immunizationProto.getCode());
        addCodeTranslations(administration, immunizationProto.getCodeTranslationsList());
        normalizeAndSetCodedBaseData(immunizationCBO.getBase(), administration);

        return administration;
    }

    public static Medication convertMedication(MedicationProto.MedicationInfo medicationInfo) {
        Medication medicationApo = new Medication();
        if (ArgumentUtil.isNotEmpty(medicationInfo.getGenericName()))
            medicationApo.setGenericName(medicationInfo.getGenericName());
        if (ArgumentUtil.isNotEmpty(medicationInfo.getBrandName()))
            medicationApo.setBrandName(medicationInfo.getBrandName());
        if (ArgumentUtil.isNotEmpty(medicationInfo.getForm())) medicationApo.setForm(medicationInfo.getForm());
        if (ArgumentUtil.isNotEmpty(medicationInfo.getIngredientsList()))
            medicationApo.setIngredients(medicationInfo.getIngredientsList());
        if (ArgumentUtil.isNotEmpty(medicationInfo.getStrength()))
            medicationApo.setStrength(medicationInfo.getStrength());
        if (ArgumentUtil.isNotEmpty(medicationInfo.getRouteOfAdministration()))
            medicationApo.setRouteOfAdministration(medicationInfo.getRouteOfAdministration());
        if (ArgumentUtil.isNotEmpty(medicationInfo.getUnits())) medicationApo.setUnits(medicationInfo.getUnits());
        addCodeTranslations(medicationApo, medicationInfo.getCodeTranslationsList());
        setCode(medicationApo, medicationInfo.getCode());

        return medicationApo;
    }


    public static void addCodeTranslations(CodedBaseObject object,
                                           List<ClinicalCodeOuterClass.ClinicalCode> codeTranslationsList) {
        List<ClinicalCode> convertedClinicalCodeList = convertCodeTranslations(codeTranslationsList);
        if (convertedClinicalCodeList != null)
            object.setCodeTranslations(convertedClinicalCodeList);
    }


    public static List<ClinicalCode> convertCodeTranslations(
            List<ClinicalCodeOuterClass.ClinicalCode> codeTranslationsList) {
        if (codeTranslationsList == null) {
            return null;
        }
        return codeTranslationsList
                .stream()
                .filter(c -> c.getSerializedSize() > 0) // non-empty
                .map(BaseConverter::convertCode)
                .collect(Collectors.toList());
    }

    public static void setCode(CodedBaseObject object, ClinicalCodeOuterClass.ClinicalCode code) {
        ClinicalCode convertedCode = convertCode(code);
        if (convertedCode != null) object.setCode(convertedCode);
    }

    public static void setSupplementaryActors(CodedBaseObject object, List<UUIDOuterClass.UUID> actorIds) {
        object.setSupplementaryClinicalActors(
                actorIds
                        .stream()
                        .filter(id -> id.getSerializedSize() > 0) //valid
                        .map(DataCatalogProtoUtils::convertUuid)
                        .collect(Collectors.toList())
        );
    }
}
