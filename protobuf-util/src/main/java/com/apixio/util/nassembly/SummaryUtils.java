package com.apixio.util.nassembly;

import com.apixio.datacatalog.*;
import com.apixio.datacatalog.ParsingDetailsOuterClass.ParsingDetails;
import com.apixio.datacatalog.PatientMetaProto.PatientMeta;
import com.apixio.datacatalog.SourceOuterClass.Source;
import com.apixio.datacatalog.UUIDOuterClass.UUID;
import org.apache.commons.collections.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

public class SummaryUtils {

    /**
     * Supplement a clinical actor by setting dataCatalogMeta, sources and parsing details
     *
     * @param actor          Clinical Actor
     * @param parsingDetails List of Parsing Details
     * @param sources        List of Sources
     */
    public static SummaryObjects.ClinicalActorSummary createClinicalActorSummary(BaseObjects.ClinicalActor actor,
                                                                                 List<ParsingDetails> parsingDetails,
                                                                                 List<Source> sources) {
        if (actor == null) return null;

        BaseObjects.Base base = actor.getBase();

        List<ParsingDetails> filteredParsingDetails =
                filterParsingDetailsByInternalId(parsingDetails, base.getParsingDetailsIdsList());

        List<Source> filteredSources =
                filterSourcesByInternalId(sources, base.getSourceIdsList());

        SummaryObjects.ClinicalActorSummary.Builder builder = SummaryObjects.ClinicalActorSummary.newBuilder();


        builder.setDataCatalogMeta(base.getDataCatalogMeta());
        builder.setClinicalActorInfo(actor.getClinicalActorInfo());
        builder.addAllParsingDetails(filteredParsingDetails);
        builder.addAllSources(filteredSources);
        builder.setInternalId(actor.getInternalId());

        return builder.build();
    }

    /**
     * Supplement a clinical actor by setting dataCatalogMeta, sources and parsing details
     *
     * @param actor          Clinical Actor
     * @param codedBase      Patient CodedBase Object
     */
    public static SummaryObjects.PatientClinicalActorSummary createPatientClinicalActorSummary(BaseObjects.ClinicalActor actor,
                                                                                               PatientProto.CodedBasePatient codedBase) {
        if (actor == null) return null;

        BaseObjects.Base base = actor.getBase();

        List<ParsingDetails> filteredParsingDetails =
                filterParsingDetailsByInternalId(codedBase.getParsingDetailsList(), base.getParsingDetailsIdsList());

        List<Source> filteredSources =
                filterSourcesByInternalId(codedBase.getSourcesList(), base.getSourceIdsList());

        SummaryObjects.PatientClinicalActorSummary.Builder builder = SummaryObjects.PatientClinicalActorSummary.newBuilder();

        SummaryObjects.BaseSummary baseSummary = buildBaseSummary(codedBase.getDataCatalogMeta(), codedBase.getPatientMeta(), filteredParsingDetails, filteredSources);

        builder.setBase(baseSummary);
        builder.setClinicalActorInfo(actor.getClinicalActorInfo());
        builder.setInternalId(actor.getInternalId());

        return builder.build();
    }

    public static BaseObjects.ClinicalActor normalizePatientClinicalActor(SummaryObjects.PatientClinicalActorSummary actor) {
        if (actor == null) return null;

        Set<UUID> parsingDetailIds = actor.getBase().getParsingDetailsList().stream().map(ParsingDetails::getInternalId)
                .collect(Collectors.toSet());
        Set<UUID> sourceIds = getSourceIdSet(actor.getBase().getSourcesList());

        BaseObjects.ClinicalActor.Builder caBuilder = BaseObjects.ClinicalActor.newBuilder();
        BaseObjects.Base baseCa = buildBaseObject(actor.getBase().getDataCatalogMeta(), parsingDetailIds, sourceIds);

        caBuilder.setInternalId(actor.getInternalId());
        caBuilder.setClinicalActorInfo(actor.getClinicalActorInfo());

        caBuilder.setBase(baseCa);
        return caBuilder.build();
    }

    private static BaseObjects.Base buildBaseObject(DataCatalogMetaOuterClass.DataCatalogMeta dataCatalogMeta,
                                                    Set<UUID> parsingDetailIdSet,
                                                    Set<UUID> sourceIdSet) {
        BaseObjects.Base.Builder baseBuilder = BaseObjects.Base.newBuilder();
        if (dataCatalogMeta != null) baseBuilder.setDataCatalogMeta(dataCatalogMeta);
        if (parsingDetailIdSet != null) baseBuilder.addAllParsingDetailsIds(parsingDetailIdSet);
        if (sourceIdSet != null) baseBuilder.addAllSourceIds(sourceIdSet);
        return baseBuilder.build();
    }

    /**
     * Supplement an encounter by setting dataCatalogMeta, sources and parsing details
     *
     * @param encounter      Encounter
     * @param patientMeta    Patient Meta
     * @param parsingDetails List of Parsing Details
     * @param sources        List of Sources
     * @param actors         List of Actors
     */
    public static SummaryObjects.EncounterSummary createEncounterSummary(CodedBaseObjects.Encounter encounter,
                                                                         PatientMeta patientMeta,
                                                                         List<ParsingDetails> parsingDetails,
                                                                         List<Source> sources,
                                                                         List<BaseObjects.ClinicalActor> actors) {
        if (encounter.getSerializedSize() == 0) return null;

        SummaryObjects.BaseSummary baseSummary =
                buildBaseSummaryFilteredByInternalId(patientMeta, parsingDetails, sources,
                        encounter.getParsingDetailsIdsList(),
                        encounter.getSourceIdsList(),
                        encounter.getDataCatalogMeta());

        Optional<BaseObjects.ClinicalActor> primaryActor =
                actors.stream().filter(actor -> actor.getInternalId().equals(encounter.getPrimaryActorId()))
                        .findAny();

        List<BaseObjects.ClinicalActor> supplementaryActors = actors.stream()
                .filter(actor -> encounter.getSupplementaryActorIdsList().contains(actor.getInternalId()))
                .collect(Collectors.toList());

        SummaryObjects.EncounterSummary.Builder builder = SummaryObjects.EncounterSummary.newBuilder();

        primaryActor.ifPresent(builder::setPrimaryActor);
        builder.addAllSupplementaryActors(supplementaryActors);
        builder.setEncounterInfo(encounter.getEncounterInfo());
        builder.setBase(baseSummary);
        return builder.build();
    }

    private static SummaryObjects.BaseSummary buildBaseSummaryFilteredByInternalId(
            PatientMeta patientMeta,
            List<ParsingDetails> parsingDetails,
            List<Source> sources,
            List<UUID> parsingDetailsIdsList,
            List<UUID> sourceIdsList,
            DataCatalogMetaOuterClass.DataCatalogMeta dataCatalogMeta) {
        List<ParsingDetails> filteredParsingDetails =
                filterParsingDetailsByInternalId(parsingDetails, parsingDetailsIdsList);

        List<Source> filteredSources =
                filterSourcesByInternalId(sources, sourceIdsList);

        return buildBaseSummary(dataCatalogMeta, patientMeta, filteredParsingDetails, filteredSources);
    }

    private static SummaryObjects.BaseSummary buildBaseSummary(
            DataCatalogMetaOuterClass.DataCatalogMeta dataCatalogMeta,
            PatientMeta patientMeta,
            List<ParsingDetails> filteredParsingDetails,
            List<Source> filteredSources) {
        SummaryObjects.BaseSummary.Builder baseSummaryBuilder = SummaryObjects.BaseSummary.newBuilder();
        if (dataCatalogMeta != null) baseSummaryBuilder.setDataCatalogMeta(dataCatalogMeta);
        if (patientMeta != null) baseSummaryBuilder.setPatientMeta(patientMeta);
        baseSummaryBuilder.addAllParsingDetails(filteredParsingDetails);
        baseSummaryBuilder.addAllSources(filteredSources);
        return baseSummaryBuilder.build();
    }

    public static CodedBaseObjects.Encounter normalizeEncounter(SummaryObjects.EncounterSummary encounterSummary) {
        if (encounterSummary == null) return null;

        SummaryObjects.BaseSummary baseSummary = encounterSummary.getBase();

        Set<UUID> parsingDetailIds = getParsingDetailsIdSet(baseSummary.getParsingDetailsList());
        Set<UUID> sourceIds = getSourceIdSet(baseSummary.getSourcesList());

        Set<UUID> supplementaryActors =
                encounterSummary.getSupplementaryActorsList().stream().map(BaseObjects.ClinicalActor::getInternalId)
                        .collect(Collectors.toSet());


        CodedBaseObjects.Encounter.Builder builder = CodedBaseObjects.Encounter.newBuilder();


        builder.setDataCatalogMeta(baseSummary.getDataCatalogMeta());
        builder.addAllParsingDetailsIds(parsingDetailIds);
        builder.addAllSourceIds(sourceIds);
        builder.setPrimaryActorId(encounterSummary.getPrimaryActor().getInternalId());
        builder.addAllSupplementaryActorIds(supplementaryActors);
        builder.setInternalId(encounterSummary.getInternalId());
        builder.setEncounterInfo(encounterSummary.getEncounterInfo());

        return builder.build();
    }


    /**
     * Supplement a procedure by setting dataCatalogMeta, sources and parsing details
     *
     * @param procedure      Procedure
     * @param patientMeta    Patient Meta
     * @param parsingDetails List of Parsing Details
     * @param sources        List of Sources
     * @param actors         List of Actors
     * @param encounters     List of encounters
     */
    public static List<SummaryObjects.ProcedureSummary> createProcedureSummaries(CodedBaseObjects.ProcedureCBO procedure,
                                                                                 PatientMeta patientMeta,
                                                                                 List<ParsingDetails> parsingDetails,
                                                                                 List<Source> sources,
                                                                                 List<BaseObjects.ClinicalActor> actors,
                                                                                 List<CodedBaseObjects.Encounter> encounters) {
        if (procedure == null) return null;

        CodedBaseObjects.CodedBase codedBase = procedure.getBase();
        List<ParsingDetails> filteredParsingDetails =
                filterParsingDetailsByInternalId(parsingDetails, codedBase.getParsingDetailsIdsList());
        List<Source> sourceList = filterSourcesByInternalId(sources, codedBase.getSourceIdsList());
        List<CodedBaseObjects.Encounter> encounterList =
                filterEncountersByInternalId(encounters, codedBase.getEncounterIdsList());


        Optional<BaseObjects.ClinicalActor> primaryActor =
                actors.stream().filter(actor -> actor.getInternalId().equals(codedBase.getPrimaryActorId()))
                        .findAny();
        List<BaseObjects.ClinicalActor> supplementaryActors = actors.stream()
                .filter(actor -> codedBase.getSupplementaryActorIdsList().contains(actor.getInternalId()))
                .collect(Collectors.toList());
        List<ClinicalCodeOuterClass.ClinicalCode> supportingCodes = procedure.getSupportingDiagnosisCodesList();

        if (supportingCodes.isEmpty()) {
            // Build it!
            SummaryObjects.ProcedureSummary.Builder builder = SummaryObjects.ProcedureSummary.newBuilder();

            SummaryObjects.CodedBaseSummary codedBaseSummBuilder =
                    buildCodedBaseSummary(patientMeta, codedBase, filteredParsingDetails, sourceList, encounterList,
                            primaryActor,
                            supplementaryActors);

            builder.setProcedureInfo(procedure.getProcedureInfo());
            builder.setBase(codedBaseSummBuilder);
            return Collections.singletonList(builder.build());
        }
        else {


            return supportingCodes.stream().map(code -> {

                // Build it!
                SummaryObjects.ProcedureSummary.Builder builder = SummaryObjects.ProcedureSummary.newBuilder();

                SummaryObjects.CodedBaseSummary codedBaseSummBuilder =
                        buildCodedBaseSummary(patientMeta, codedBase, filteredParsingDetails, sourceList, encounterList,
                                primaryActor,
                                supplementaryActors);

                builder.setProcedureInfo(procedure.getProcedureInfo());
                builder.setSupportingDiagnosisCode(code);
                builder.setBase(codedBaseSummBuilder);
                return builder.build();
            }).collect(Collectors.toList());
        }
    }


    public static CodedBaseObjects.ProcedureCBO normalizeProcedure(SummaryObjects.ProcedureSummary procedureSummary) {
        if (procedureSummary == null) return null;

        // Build it!
        CodedBaseObjects.ProcedureCBO.Builder procedureCboBuilder = CodedBaseObjects.ProcedureCBO.newBuilder();
        CodedBaseObjects.CodedBase codedBase = buildCodedBase(procedureSummary.getBase());

        procedureCboBuilder.addSupportingDiagnosisCodes(procedureSummary.getSupportingDiagnosisCode());
        procedureCboBuilder.setProcedureInfo(procedureSummary.getProcedureInfo());
        procedureCboBuilder.setBase(codedBase);
        return procedureCboBuilder.build();
    }

    /**
     * Supplement a procedure by setting dataCatalogMeta, sources and parsing details
     *
     * @param ffsClaim      Procedure
     * @param patientMeta    Patient Meta
     * @param parsingDetails List of Parsing Details
     * @param sources        List of Sources
     * @param actors         List of Actors
     * @param encounters     List of encounters
     */
    public static List<SummaryObjects.FfsClaimSummary> createFfsClaimSummaries(CodedBaseObjects.FfsClaimCBO ffsClaim,
                                                                               PatientMeta patientMeta,
                                                                               List<ParsingDetails> parsingDetails,
                                                                               List<Source> sources,
                                                                               List<BaseObjects.ClinicalActor> actors,
                                                                               List<CodedBaseObjects.Encounter> encounters) {
        if (ffsClaim == null) return null;

        CodedBaseObjects.CodedBase codedBase = ffsClaim.getBase();
        List<ParsingDetails> filteredParsingDetails =
                filterParsingDetailsByInternalId(parsingDetails, codedBase.getParsingDetailsIdsList());
        List<Source> sourceList = filterSourcesByInternalId(sources, codedBase.getSourceIdsList());
        List<CodedBaseObjects.Encounter> encounterList =
                filterEncountersByInternalId(encounters, codedBase.getEncounterIdsList());


        Optional<BaseObjects.ClinicalActor> primaryActor =
                actors.stream().filter(actor -> actor.getInternalId().equals(codedBase.getPrimaryActorId()))
                        .findAny();
        List<BaseObjects.ClinicalActor> supplementaryActors = actors.stream()
                .filter(actor -> codedBase.getSupplementaryActorIdsList().contains(actor.getInternalId()))
                .collect(Collectors.toList());
        List<ClinicalCodeOuterClass.ClinicalCode> supportingCodes = ffsClaim.getSupportingDiagnosisCodesList();

        if (supportingCodes.isEmpty()) {
            // Build it!
            SummaryObjects.FfsClaimSummary.Builder builder = SummaryObjects.FfsClaimSummary.newBuilder();

            SummaryObjects.CodedBaseSummary codedBaseSummBuilder =
                    buildCodedBaseSummary(patientMeta, codedBase, filteredParsingDetails, sourceList, encounterList,
                            primaryActor,
                            supplementaryActors);

            builder.setFfsClaim(ffsClaim.getFfsClaim());
            builder.setBase(codedBaseSummBuilder);
            return Collections.singletonList(builder.build());
        }
        else {
            return supportingCodes.stream().map(code -> {

                // Build it!
                SummaryObjects.FfsClaimSummary.Builder builder = SummaryObjects.FfsClaimSummary.newBuilder();

                SummaryObjects.CodedBaseSummary codedBaseSummBuilder =
                        buildCodedBaseSummary(patientMeta, codedBase, filteredParsingDetails, sourceList, encounterList,
                                primaryActor,
                                supplementaryActors);

                builder.setFfsClaim(ffsClaim.getFfsClaim());
                builder.setSupportingDiagnosisCode(code);
                builder.setBase(codedBaseSummBuilder);
                return builder.build();
            }).collect(Collectors.toList());
        }

    }

    public static CodedBaseObjects.FfsClaimCBO normalizeFfsClaim(SummaryObjects.FfsClaimSummary ffsClaimSummary) {
        if (ffsClaimSummary == null) return null;

        // Build it!
        CodedBaseObjects.FfsClaimCBO.Builder procedureCboBuilder = CodedBaseObjects.FfsClaimCBO.newBuilder();
        CodedBaseObjects.CodedBase codedBase = buildCodedBase(ffsClaimSummary.getBase());
        procedureCboBuilder.addSupportingDiagnosisCodes(ffsClaimSummary.getSupportingDiagnosisCode());
        procedureCboBuilder.setFfsClaim(ffsClaimSummary.getFfsClaim());
        procedureCboBuilder.setBase(codedBase);
        return procedureCboBuilder.build();
    }

    /**
     * Supplement a ra claim by setting dataCatalogMeta, sources and parsing details
     *
     * @param raClaim        Ra Claim
     * @param patientMeta    Patient Meta
     * @param parsingDetails List of Parsing Details
     * @param sources        List of Sources
     * @param actors         List of Actors
     * @param encounters     List of encounters
     */
    public static SummaryObjects.RaClaimSummary createRaClaimSummary(CodedBaseObjects.RaClaimCBO raClaim,
                                                                     PatientMeta patientMeta,
                                                                     List<ParsingDetails> parsingDetails,
                                                                     List<Source> sources,
                                                                     List<BaseObjects.ClinicalActor> actors,
                                                                     List<CodedBaseObjects.Encounter> encounters) {
        if (raClaim == null) return null;

        CodedBaseObjects.CodedBase codedBase = raClaim.getBase();
        List<ParsingDetails> filteredParsingDetails =
                filterParsingDetailsByInternalId(parsingDetails, codedBase.getParsingDetailsIdsList());
        List<Source> sourceList = filterSourcesByInternalId(sources, codedBase.getSourceIdsList());
        List<CodedBaseObjects.Encounter> encounterList =
                filterEncountersByInternalId(encounters, codedBase.getEncounterIdsList());


        Optional<BaseObjects.ClinicalActor> primaryActor =
                actors.stream().filter(actor -> actor.getInternalId().equals(codedBase.getPrimaryActorId()))
                        .findAny();
        List<BaseObjects.ClinicalActor> supplementaryActors = actors.stream()
                .filter(actor -> codedBase.getSupplementaryActorIdsList().contains(actor.getInternalId()))
                .collect(Collectors.toList());



        // Build it!
        SummaryObjects.RaClaimSummary.Builder builder = SummaryObjects.RaClaimSummary.newBuilder();

        SummaryObjects.CodedBaseSummary codedBaseSummBuilder =
                buildCodedBaseSummary(patientMeta, codedBase, filteredParsingDetails, sourceList, encounterList,
                        primaryActor,
                        supplementaryActors);

        builder.setRaClaim(raClaim.getRaClaim());
        builder.setBase(codedBaseSummBuilder);
        return builder.build();
    }

    public static CodedBaseObjects.RaClaimCBO normalizeRaClaim(SummaryObjects.RaClaimSummary raClaim) {
        if (raClaim == null || raClaim.getSerializedSize() == 0) return null;
        CodedBaseObjects.RaClaimCBO.Builder builder = CodedBaseObjects.RaClaimCBO.newBuilder();

        SummaryObjects.CodedBaseSummary codedBaseSummary = raClaim.getBase();
        CodedBaseObjects.CodedBase codedBase = buildCodedBase(codedBaseSummary);

        builder.setRaClaim(raClaim.getRaClaim());
        builder.setBase(codedBase);

        return builder.build();
    }

    /**
     * Supplement a mao 004 claim by setting dataCatalogMeta, sources and parsing details
     *
     * @param mao004         Mao004 Claim
     * @param patientMeta    Patient Meta
     * @param parsingDetails List of Parsing Details
     * @param sources        List of Sources
     * @param actors         List of Actors
     * @param encounters     List of encounters
     */
    public static SummaryObjects.Mao004Summary createMao004Summary(CodedBaseObjects.Mao004CBO mao004,
                                                                     PatientMeta patientMeta,
                                                                     List<ParsingDetails> parsingDetails,
                                                                     List<Source> sources,
                                                                     List<BaseObjects.ClinicalActor> actors,
                                                                     List<CodedBaseObjects.Encounter> encounters) {
        if (mao004 == null) return null;

        CodedBaseObjects.CodedBase codedBase = mao004.getBase();
        List<ParsingDetails> filteredParsingDetails =
                filterParsingDetailsByInternalId(parsingDetails, codedBase.getParsingDetailsIdsList());
        List<Source> sourceList = filterSourcesByInternalId(sources, codedBase.getSourceIdsList());
        List<CodedBaseObjects.Encounter> encounterList =
                filterEncountersByInternalId(encounters, codedBase.getEncounterIdsList());


        Optional<BaseObjects.ClinicalActor> primaryActor =
                actors.stream().filter(actor -> actor.getInternalId().equals(codedBase.getPrimaryActorId()))
                        .findAny();
        List<BaseObjects.ClinicalActor> supplementaryActors = actors.stream()
                .filter(actor -> codedBase.getSupplementaryActorIdsList().contains(actor.getInternalId()))
                .collect(Collectors.toList());



        // Build it!
        SummaryObjects.Mao004Summary.Builder builder = SummaryObjects.Mao004Summary.newBuilder();

        SummaryObjects.CodedBaseSummary codedBaseSummBuilder =
                buildCodedBaseSummary(patientMeta, codedBase, filteredParsingDetails, sourceList, encounterList,
                        primaryActor,
                        supplementaryActors);

        builder.setMao004(mao004.getMao004());
        builder.setBase(codedBaseSummBuilder);
        return builder.build();
    }

    public static CodedBaseObjects.Mao004CBO normalizeMao004(SummaryObjects.Mao004Summary problem) {
        if (problem == null || problem.getSerializedSize() == 0) return null;
        CodedBaseObjects.Mao004CBO.Builder builder = CodedBaseObjects.Mao004CBO.newBuilder();

        SummaryObjects.CodedBaseSummary codedBaseSummary = problem.getBase();
        CodedBaseObjects.CodedBase codedBase = buildCodedBase(codedBaseSummary);

        builder.setMao004(problem.getMao004());
        builder.setBase(codedBase);

        return builder.build();
    }

    /**
     * Supplement a ra claim by setting dataCatalogMeta, sources and parsing details
     *
     * @param problem        Problem
     * @param patientMeta    Patient Meta
     * @param parsingDetails List of Parsing Details
     * @param sources        List of Sources
     * @param actors         List of Actors
     * @param encounters     List of encounters
     */
    public static SummaryObjects.ProblemSummary createProblemSummary(CodedBaseObjects.ProblemCBO problem,
                                                                     PatientMeta patientMeta,
                                                                     List<ParsingDetails> parsingDetails,
                                                                     List<Source> sources,
                                                                     List<BaseObjects.ClinicalActor> actors,
                                                                     List<CodedBaseObjects.Encounter> encounters) {
        if (problem == null) return null;

        CodedBaseObjects.CodedBase codedBase = problem.getBase();
        List<ParsingDetails> filteredParsingDetails =
                filterParsingDetailsByInternalId(parsingDetails, codedBase.getParsingDetailsIdsList());
        List<Source> sourceList = filterSourcesByInternalId(sources, codedBase.getSourceIdsList());
        List<CodedBaseObjects.Encounter> encounterList =
                filterEncountersByInternalId(encounters, codedBase.getEncounterIdsList());


        Optional<BaseObjects.ClinicalActor> primaryActor =
                actors.stream().filter(actor -> actor.getInternalId().equals(codedBase.getPrimaryActorId()))
                        .findAny();
        List<BaseObjects.ClinicalActor> supplementaryActors = actors.stream()
                .filter(actor -> codedBase.getSupplementaryActorIdsList().contains(actor.getInternalId()))
                .collect(Collectors.toList());



        // Build it!
        SummaryObjects.ProblemSummary.Builder builder = SummaryObjects.ProblemSummary.newBuilder();

        SummaryObjects.CodedBaseSummary codedBaseSummBuilder =
                buildCodedBaseSummary(patientMeta, codedBase, filteredParsingDetails, sourceList, encounterList,
                        primaryActor,
                        supplementaryActors);

        builder.setProblemInfo(problem.getProblemInfo());
        builder.setBase(codedBaseSummBuilder);
        return builder.build();
    }

    public static CodedBaseObjects.ProblemCBO normalizeProblem(SummaryObjects.ProblemSummary problem) {
        if (problem == null || problem.getSerializedSize() == 0) return null;
        CodedBaseObjects.ProblemCBO.Builder builder = CodedBaseObjects.ProblemCBO.newBuilder();

        SummaryObjects.CodedBaseSummary codedBaseSummary = problem.getBase();
        CodedBaseObjects.CodedBase codedBase = buildCodedBase(codedBaseSummary);

        builder.setProblemInfo(problem.getProblemInfo());
        builder.setBase(codedBase);

        return builder.build();
    }

    /**
     * Supplement an allergy value by setting dataCatalogMeta, sources, parsing details, actors, and encounters
     *
     * @param allergy        Allergy
     * @param patientMeta    Patient Meta
     * @param parsingDetails List of Parsing Details
     * @param sources        List of Sources
     * @param actors         List of Actors
     * @param encounters     List of encounters
     */
    public static SummaryObjects.AllergySummary createAllergySummary(CodedBaseObjects.Allergy allergy,
                                                                     PatientMeta patientMeta,
                                                                     List<ParsingDetails> parsingDetails,
                                                                     List<Source> sources,
                                                                     List<BaseObjects.ClinicalActor> actors,
                                                                     List<CodedBaseObjects.Encounter> encounters) {
        if (allergy.getSerializedSize() == 0) return null;

        CodedBaseObjects.CodedBase codedBase = allergy.getBase();
        SummaryObjects.CodedBaseSummary codedBaseSummary =
                buildCodedBaseSummaryFilteringByInternalId(patientMeta, parsingDetails, sources, actors,
                        encounters,
                        codedBase);
        SummaryObjects.AllergySummary.Builder builder = SummaryObjects.AllergySummary.newBuilder();

        builder.setAllergyInfo(allergy.getAllergyInfo());
        builder.setBase(codedBaseSummary);
        return builder.build();

    }

    public static CodedBaseObjects.Allergy normalizeAllergy(SummaryObjects.AllergySummary allergy) {
        if (allergy == null || allergy.getSerializedSize() == 0) return null;
        CodedBaseObjects.Allergy.Builder builder = CodedBaseObjects.Allergy.newBuilder();

        SummaryObjects.CodedBaseSummary codedBaseSummary = allergy.getBase();
        CodedBaseObjects.CodedBase codedBase = buildCodedBase(codedBaseSummary);

        builder.setAllergyInfo(allergy.getAllergyInfo());
        builder.setBase(codedBase);

        return builder.build();
    }

    /**
     * Supplement coverage by setting dataCatalogMeta, sources, parsing details, actors, and encounters
     *
     * @param coverage       Coverage
     * @param patientMeta    Patient Meta
     * @param parsingDetails List of Parsing Details
     * @param sources        List of Sources
     */
    public static SummaryObjects.CoverageSummary createCoverageSummary(BaseObjects.Coverage coverage,
                                                                                   PatientMeta patientMeta,
                                                                                   List<ParsingDetails> parsingDetails,
                                                                                   List<Source> sources) {
        if (coverage.getSerializedSize() == 0) return null;

        SummaryObjects.CoverageSummary.Builder builder = SummaryObjects.CoverageSummary.newBuilder();

        BaseObjects.Base base = coverage.getBase();
        SummaryObjects.BaseSummary baseSummary =
                buildBaseSummaryFilteredByInternalId(patientMeta, parsingDetails, sources,
                        base.getParsingDetailsIdsList(),
                        base.getSourceIdsList(),
                        base.getDataCatalogMeta());

        builder.setCoverageInfo(coverage.getCoverageInfo());
        builder.setBase(baseSummary);

        return builder.build();
    }

    public static BaseObjects.Coverage normalizeCoverage(SummaryObjects.CoverageSummary summary) {
        if (summary == null || summary.getSerializedSize() == 0) return null;

        BaseObjects.Coverage.Builder builder = BaseObjects.Coverage.newBuilder();

        SummaryObjects.BaseSummary baseSummary = summary.getBase();

        BaseObjects.Base cdBaseObject = buildBaseObject(baseSummary.getDataCatalogMeta(),
                getParsingDetailsIdSet(baseSummary.getParsingDetailsList()),
                getSourceIdSet(baseSummary.getSourcesList()));

        builder.setBase(cdBaseObject);
        builder.setCoverageInfo(summary.getCoverageInfo());

        return builder.build();
    }


    /**
     * Supplement a contact details by setting dataCatalogMeta, sources, parsing details, actors, and encounters
     *
     * @param contact        Contact details
     * @param patientMeta    Patient Meta
     * @param parsingDetails List of Parsing Details
     * @param sources        List of Sources
     */
    public static SummaryObjects.ContactDetailsSummary createContactDetailsSummary(BaseObjects.ContactDetails contact,
                                                                                   PatientMeta patientMeta,
                                                                                   List<ParsingDetails> parsingDetails,
                                                                                   List<Source> sources) {
        if (contact.getSerializedSize() == 0) return null;

        SummaryObjects.ContactDetailsSummary.Builder builder = SummaryObjects.ContactDetailsSummary.newBuilder();

        BaseObjects.Base base = contact.getBase();
        SummaryObjects.BaseSummary baseSummary =
                buildBaseSummaryFilteredByInternalId(patientMeta, parsingDetails, sources,
                        base.getParsingDetailsIdsList(),
                        base.getSourceIdsList(),
                        base.getDataCatalogMeta());

        builder.setContactInfo(contact.getContactInfo());
        builder.setBase(baseSummary);

        return builder.build();
    }

    public static BaseObjects.ContactDetails normalizeContactDetails(SummaryObjects.ContactDetailsSummary cdSummary) {
        if (cdSummary == null || cdSummary.getSerializedSize() == 0) return null;

        BaseObjects.ContactDetails.Builder cdBuilder = BaseObjects.ContactDetails.newBuilder();

        SummaryObjects.BaseSummary baseSummary = cdSummary.getBase();

        BaseObjects.Base cdBaseObject = buildBaseObject(baseSummary.getDataCatalogMeta(),
                getParsingDetailsIdSet(baseSummary.getParsingDetailsList()),
                getSourceIdSet(baseSummary.getSourcesList()));

        cdBuilder.setBase(cdBaseObject);
        cdBuilder.setContactInfo(cdSummary.getContactInfo());

        return cdBuilder.build();
    }

    /**
     * Supplement a biometric value by setting dataCatalogMeta, sources, parsing details, actors, and encounters
     *
     * @param bioValue       biometric value
     * @param patientMeta    Patient Meta
     * @param parsingDetails List of Parsing Details
     * @param sources        List of Sources
     * @param actors         List of Actors
     * @param encounters     List of encounters
     */
    public static SummaryObjects.BiometricValueSummary createBiometricSummary(CodedBaseObjects.BiometricValue bioValue,
                                                                              PatientMeta patientMeta,
                                                                              List<ParsingDetails> parsingDetails,
                                                                              List<Source> sources,
                                                                              List<BaseObjects.ClinicalActor> actors,
                                                                              List<CodedBaseObjects.Encounter> encounters) {
        if (bioValue.getSerializedSize() == 0) return null;

        CodedBaseObjects.CodedBase codedBase = bioValue.getBase();
        SummaryObjects.CodedBaseSummary codedBaseSummary =
                buildCodedBaseSummaryFilteringByInternalId(patientMeta, parsingDetails, sources, actors,
                        encounters,
                        codedBase);
        SummaryObjects.BiometricValueSummary.Builder builder = SummaryObjects.BiometricValueSummary.newBuilder();
        builder.setVitalSign(bioValue.getVitalSign());
        builder.setBase(codedBaseSummary);
        return builder.build();

    }

    /**
     * Supplement a biometric value by setting dataCatalogMeta, sources, parsing details, actors, and encounters
     *
     * @param socialHistory  biometric value
     * @param patientMeta    Patient Meta
     * @param parsingDetails List of Parsing Details
     * @param sources        List of Sources
     * @param actors         List of Actors
     * @param encounters     List of encounters
     */
    public static SummaryObjects.SocialHistorySummary createSocialHistorySummary(
            CodedBaseObjects.SocialHistory socialHistory,
            PatientMeta patientMeta,
            List<ParsingDetails> parsingDetails,
            List<Source> sources,
            List<BaseObjects.ClinicalActor> actors,
            List<CodedBaseObjects.Encounter> encounters) {
        if (socialHistory.getSerializedSize() == 0) return null;

        CodedBaseObjects.CodedBase codedBase = socialHistory.getBase();
        SummaryObjects.CodedBaseSummary codedBaseSummary =
                buildCodedBaseSummaryFilteringByInternalId(patientMeta, parsingDetails, sources, actors,
                        encounters,
                        codedBase);

        SummaryObjects.SocialHistorySummary.Builder builder = SummaryObjects.SocialHistorySummary.newBuilder();

        builder.setSocialHistoryInfo(socialHistory.getSocialHistoryInfo());
        builder.setBase(codedBaseSummary);
        return builder.build();

    }

    public static CodedBaseObjects.BiometricValue normalizeBiometricValue(
            SummaryObjects.BiometricValueSummary bioValue) {
        if (bioValue == null || bioValue.getSerializedSize() == 0) return null;

        CodedBaseObjects.BiometricValue.Builder builder = CodedBaseObjects.BiometricValue.newBuilder();

        SummaryObjects.CodedBaseSummary codedBaseSummary = bioValue.getBase();
        CodedBaseObjects.CodedBase codedBase = buildCodedBase(codedBaseSummary);

        builder.setVitalSign(bioValue.getVitalSign());
        builder.setBase(codedBase);
        return builder.build();
    }

    public static CodedBaseObjects.SocialHistory normalizeSocialHistory(
            SummaryObjects.SocialHistorySummary socialHistory) {
        if (socialHistory == null || socialHistory.getSerializedSize() == 0) return null;

        CodedBaseObjects.SocialHistory.Builder builder = CodedBaseObjects.SocialHistory.newBuilder();

        SummaryObjects.CodedBaseSummary codedBaseSummary = socialHistory.getBase();
        CodedBaseObjects.CodedBase codedBase = buildCodedBase(codedBaseSummary);

        builder.setSocialHistoryInfo(socialHistory.getSocialHistoryInfo());
        builder.setBase(codedBase);
        return builder.build();
    }

    public static SummaryObjects.DemographicsSummary createDemographicsSummary(
            BaseObjects.PatientDemographics demographic,
            PatientMeta patientMeta,
            List<ParsingDetails> parsingDetails,
            List<Source> sources) {

        SummaryObjects.DemographicsSummary.Builder demographicsBuilder =
                SummaryObjects.DemographicsSummary.newBuilder();

        if (demographic.getSerializedSize() == 0) return null;

        BaseObjects.Base base = demographic.getBase();

        SummaryObjects.BaseSummary baseSummary =
                buildBaseSummaryFilteredByInternalId(patientMeta, parsingDetails, sources,
                        base.getParsingDetailsIdsList(),
                        base.getSourceIdsList(),
                        base.getDataCatalogMeta());

        demographicsBuilder.setDemographicsInfo(demographic.getDemographicsInfo());
        demographicsBuilder.setBase(baseSummary);

        return demographicsBuilder.build();
    }

    public static BaseObjects.PatientDemographics normalizeDemographic(SummaryObjects.DemographicsSummary demographic) {
        if (demographic == null || demographic.getSerializedSize() == 0) return null;

        BaseObjects.PatientDemographics.Builder builder = BaseObjects.PatientDemographics.newBuilder();

        SummaryObjects.BaseSummary baseSummary = demographic.getBase();

        DataCatalogMetaOuterClass.DataCatalogMeta dataCatalogMeta = baseSummary.getDataCatalogMeta();

        Set<UUID> parsingDetailsIdSet = getParsingDetailsIdSet(baseSummary.getParsingDetailsList());

        builder.setDemographicsInfo(demographic.getDemographicsInfo());

        Set<UUID> sourceIdSet = getSourceIdSet(baseSummary.getSourcesList());

        BaseObjects.Base baseObject = buildBaseObject(dataCatalogMeta, parsingDetailsIdSet, sourceIdSet);

        builder.setBase(baseObject);
        return builder.build();
    }

    private static Set<UUID> getSourceIdSet(List<Source> sourcesList) {
        return sourcesList.stream().map(Source::getInternalId).collect(Collectors.toSet());
    }

    private static Set<UUID> getParsingDetailsIdSet(List<ParsingDetails> parsingDetailsList) {
        return parsingDetailsList.stream().map(ParsingDetails::getInternalId)
                .collect(Collectors.toSet());
    }

    /**
     * Supplement a LabResult value by setting dataCatalogMeta, sources, parsing details, actors, and encounters
     *
     * @param labResultCbo   LabResult value
     * @param patientMeta    Patient Meta
     * @param parsingDetails List of Parsing Details
     * @param sources        List of Sources
     * @param actors         List of Actors
     * @param encounters     List of encounters
     */
    public static SummaryObjects.LabResultSummary createLabResultSummary(CodedBaseObjects.LabResultCBO labResultCbo,
                                                                         PatientMeta patientMeta,
                                                                         List<ParsingDetails> parsingDetails,
                                                                         List<Source> sources,
                                                                         List<BaseObjects.ClinicalActor> actors,
                                                                         List<CodedBaseObjects.Encounter> encounters) {
        if (labResultCbo.getSerializedSize() == 0) return null;

        CodedBaseObjects.CodedBase codedBase = labResultCbo.getBase();

        SummaryObjects.CodedBaseSummary codedBaseSummary =
                buildCodedBaseSummaryFilteringByInternalId(patientMeta, parsingDetails, sources, actors,
                        encounters,
                        codedBase);

        SummaryObjects.LabResultSummary.Builder lrSummaryBuilder = SummaryObjects.LabResultSummary.newBuilder();


        lrSummaryBuilder.setBase(codedBaseSummary);
        lrSummaryBuilder.setLabResultInfo(labResultCbo.getLabResultInfo());
        return lrSummaryBuilder.build();
    }

    public static SummaryObjects.PrescriptionSummary createPrescriptionSummary(
            CodedBaseObjects.PrescriptionCBO prescriptionCBO,
            PatientMeta patientMeta,
            List<ParsingDetails> parsingDetails,
            List<Source> sources,
            List<BaseObjects.ClinicalActor> actors,
            List<CodedBaseObjects.Encounter> encounters) {

        if (prescriptionCBO.getSerializedSize() == 0) return null;

        CodedBaseObjects.CodedBase codedBase = prescriptionCBO.getBase();

        SummaryObjects.CodedBaseSummary codedBaseSummary =
                buildCodedBaseSummaryFilteringByInternalId(patientMeta, parsingDetails, sources, actors,
                        encounters,
                        codedBase);

        SummaryObjects.PrescriptionSummary.Builder prescriptionSummaryBuilder =
                SummaryObjects.PrescriptionSummary.newBuilder();

        prescriptionSummaryBuilder.setBase(codedBaseSummary);
        prescriptionSummaryBuilder.setPrescriptionInfo(prescriptionCBO.getPrescriptionInfo());
        return prescriptionSummaryBuilder.build();
    }

    public static SummaryObjects.FamilyHistorySummary createFamilyHistorySummary(
            CodedBaseObjects.FamilyHistoryCBO familyHistoryCBO,
            PatientProto.CodedBasePatient codedBasePatient) {

        if (familyHistoryCBO.getSerializedSize() == 0) return null;

        CodedBaseObjects.CodedBase codedBase = familyHistoryCBO.getBase();

        SummaryObjects.CodedBaseSummary codedBaseSummary =
                buildCodedBaseSummaryFilteringByInternalId(codedBase, codedBasePatient);

        SummaryObjects.FamilyHistorySummary.Builder fhSummBuilder = SummaryObjects.FamilyHistorySummary.newBuilder();

        fhSummBuilder.setBase(codedBaseSummary);
        if (familyHistoryCBO.hasFamilyHistoryInfo())
            fhSummBuilder.setFamilyHistoryInfo(familyHistoryCBO.getFamilyHistoryInfo());
        return fhSummBuilder.build();
    }

    public static SummaryObjects.ImmunizationSummary createImmunizationSummary(
            CodedBaseObjects.ImmunizationCBO immunizationCBO,
            PatientProto.CodedBasePatient codedBasePatient) {

        if (immunizationCBO.getSerializedSize() == 0) return null;

        CodedBaseObjects.CodedBase codedBase = immunizationCBO.getBase();

        SummaryObjects.CodedBaseSummary codedBaseSummary =
                buildCodedBaseSummaryFilteringByInternalId(codedBase, codedBasePatient);

        SummaryObjects.ImmunizationSummary.Builder immuSummBuilder = SummaryObjects.ImmunizationSummary.newBuilder();

        immuSummBuilder.setBase(codedBaseSummary);
        if (immunizationCBO.hasImmunizationInfo())
            immuSummBuilder.setImmunizationInfo(immunizationCBO.getImmunizationInfo());
        return immuSummBuilder.build();
    }

    private static SummaryObjects.CodedBaseSummary buildCodedBaseSummaryFilteringByInternalId(
            CodedBaseObjects.CodedBase codedBase,
            PatientProto.CodedBasePatient cbp) {
        return buildCodedBaseSummaryFilteringByInternalId(cbp.getPatientMeta(), cbp.getParsingDetailsList(),
                cbp.getSourcesList(), cbp.getClinicalActorsList(), cbp.getEncountersList(), codedBase);
    }


    private static SummaryObjects.CodedBaseSummary buildCodedBaseSummaryFilteringByInternalId(
            PatientMeta patientMeta,
            List<ParsingDetails> parsingDetails,
            List<Source> sources,
            List<BaseObjects.ClinicalActor> actors,
            List<CodedBaseObjects.Encounter> encounters,
            CodedBaseObjects.CodedBase codedBase) {

        List<ParsingDetails> parsingDetailsList =
                filterParsingDetailsByInternalId(parsingDetails, codedBase.getParsingDetailsIdsList());

        List<Source> sourceList = filterSourcesByInternalId(sources, codedBase.getSourceIdsList());

        List<CodedBaseObjects.Encounter> encounterList =
                filterEncountersByInternalId(encounters, codedBase.getEncounterIdsList());

        Optional<BaseObjects.ClinicalActor> primaryActor =
                actors.stream().filter(actor -> actor.getInternalId().equals(codedBase.getPrimaryActorId()))
                        .findAny();

        List<BaseObjects.ClinicalActor> supplementaryActors = actors.stream()
                .filter(actor -> codedBase.getSupplementaryActorIdsList().contains(actor.getInternalId()))
                .collect(Collectors.toList());

        return buildCodedBaseSummary(patientMeta, codedBase, parsingDetailsList, sourceList, encounterList,
                primaryActor,
                supplementaryActors);
    }

    private static List<ParsingDetails> filterParsingDetailsByInternalId(
            List<ParsingDetails> parsingDetails,
            List<UUID> parsingDetailsIdsList) {
        return parsingDetails.stream().filter(pd -> parsingDetailsIdsList.contains(pd.getInternalId()))
                .collect(Collectors.toList());
    }

    private static SummaryObjects.CodedBaseSummary buildCodedBaseSummary(PatientMeta patientMeta,
                                                                         CodedBaseObjects.CodedBase codedBase,
                                                                         List<ParsingDetails> parsingDetailsList,
                                                                         List<Source> sourceList,
                                                                         List<CodedBaseObjects.Encounter> encounterList,
                                                                         Optional<BaseObjects.ClinicalActor> primaryActor,
                                                                         List<BaseObjects.ClinicalActor> supplementaryActors) {
        SummaryObjects.CodedBaseSummary.Builder codedBaseSummBuilder = SummaryObjects.CodedBaseSummary.newBuilder();

        codedBaseSummBuilder.setDataCatalogMeta(codedBase.getDataCatalogMeta());
        if (patientMeta != null) codedBaseSummBuilder.setPatientMeta(patientMeta);
        codedBaseSummBuilder.addAllParsingDetails(parsingDetailsList);
        if (CollectionUtils.isNotEmpty(sourceList)) codedBaseSummBuilder.addAllSources(sourceList);
        primaryActor.ifPresent(codedBaseSummBuilder::setPrimaryActor);
        codedBaseSummBuilder.addAllSupplementaryActors(supplementaryActors);
        codedBaseSummBuilder.addAllEncounters(encounterList);
        return codedBaseSummBuilder.build();
    }

    /**
     * Supplement a DocMeta value by setting dataCatalogMeta, sources, parsing details, actors, and encounters
     *
     * @param docMeta        DocMeta value
     * @param patientMeta    Patient Meta
     * @param parsingDetails List of Parsing Details
     * @param sources        List of Sources
     * @param actors         List of Actors
     * @param encounters     List of encounters
     */
    public static SummaryObjects.DocumentMetaSummary createDocumentMetaSummary(CodedBaseObjects.DocumentMetaCBO docMeta,
                                                                               PatientMeta patientMeta,
                                                                               List<ParsingDetails> parsingDetails,
                                                                               List<Source> sources,
                                                                               List<BaseObjects.ClinicalActor> actors,
                                                                               List<CodedBaseObjects.Encounter> encounters) {
        if (docMeta.getSerializedSize() == 0) return null;

        CodedBaseObjects.CodedBase codedBase = docMeta.getBase();

        List<ParsingDetails> parsingDetailsList =
                parsingDetails.stream().filter(pd -> codedBase.getParsingDetailsIdsList().contains(pd.getInternalId()))
                        .collect(Collectors.toList());

        final List<UUID> sourceIds = codedBase.getSourceIdsList();

        List<Source> sourceList =
                filterSourcesByInternalId(sources, sourceIds);

        List<CodedBaseObjects.Encounter> encounterList =
                filterEncountersByInternalId(encounters, codedBase.getEncounterIdsList());

        Optional<BaseObjects.ClinicalActor> primaryActor =
                actors.stream().filter(actor -> actor.getInternalId().equals(codedBase.getPrimaryActorId()))
                        .findAny();

        List<BaseObjects.ClinicalActor> supplementaryActors = actors.stream()
                .filter(actor -> codedBase.getSupplementaryActorIdsList().contains(actor.getInternalId()))
                .collect(Collectors.toList());

        SummaryObjects.DocumentMetaSummary.Builder docMetaBuilder = SummaryObjects.DocumentMetaSummary.newBuilder();
        SummaryObjects.CodedBaseSummary.Builder codedBaseBuilder = SummaryObjects.CodedBaseSummary.newBuilder();

        codedBaseBuilder.setDataCatalogMeta(codedBase.getDataCatalogMeta());
        if (patientMeta != null) codedBaseBuilder.setPatientMeta(patientMeta);
        codedBaseBuilder.addAllParsingDetails(parsingDetailsList);
        if (CollectionUtils.isNotEmpty(sourceList)) codedBaseBuilder.addAllSources(sourceList);
        primaryActor.ifPresent(codedBaseBuilder::setPrimaryActor);
        codedBaseBuilder.addAllSupplementaryActors(supplementaryActors);
        codedBaseBuilder.addAllEncounters(encounterList);

        docMetaBuilder.setBase(codedBaseBuilder.build());
        docMetaBuilder.setDocumentMeta(docMeta.getDocumentMeta());
        return docMetaBuilder.build();
    }

    public static CodedBaseObjects.DocumentMetaCBO normalizeDocumentMeta(SummaryObjects.DocumentMetaSummary summary) {
        if (summary == null || summary.getSerializedSize() == 0) return null;

        SummaryObjects.CodedBaseSummary codedBaseSummary = summary.getBase();
        CodedBaseObjects.DocumentMetaCBO.Builder documentMetaBuilder = CodedBaseObjects.DocumentMetaCBO.newBuilder();

        documentMetaBuilder.setDocumentMeta(summary.getDocumentMeta());

        CodedBaseObjects.CodedBase codedBase = buildCodedBase(codedBaseSummary);
        documentMetaBuilder.setBase(codedBase);
        return documentMetaBuilder.build();
    }

    private static List<CodedBaseObjects.Encounter> filterEncountersByInternalId(
            List<CodedBaseObjects.Encounter> encounters,
            List<UUID> uuidList) {

        return encounters.stream().filter(e -> uuidList.contains(e.getInternalId()))
                .collect(Collectors.toList());
    }

    private static List<Source> filterSourcesByInternalId(List<Source> sources,
                                                          List<UUID> uuidList) {
        return sources.stream().filter(s -> uuidList.contains(s.getInternalId())).collect(Collectors.toList());
    }

    public static CodedBaseObjects.LabResultCBO normalizeLabResults(SummaryObjects.LabResultSummary lrSummary) {
        if (lrSummary == null || lrSummary.getSerializedSize() == 0) return null;

        SummaryObjects.CodedBaseSummary codedBaseSummary = lrSummary.getBase();
        CodedBaseObjects.CodedBase codedBase = buildCodedBase(codedBaseSummary);
        CodedBaseObjects.LabResultCBO.Builder lrCboBuilder = CodedBaseObjects.LabResultCBO.newBuilder();

        lrCboBuilder.setLabResultInfo(lrSummary.getLabResultInfo());
        lrCboBuilder.setBase(codedBase);
        return lrCboBuilder.build();
    }

    public static CodedBaseObjects.PrescriptionCBO normalizePrescription(
            SummaryObjects.PrescriptionSummary prescriptionSummary) {
        if (prescriptionSummary == null || prescriptionSummary.getSerializedSize() == 0) return null;

        SummaryObjects.CodedBaseSummary codedBaseSummary = prescriptionSummary.getBase();
        CodedBaseObjects.CodedBase codedBase = buildCodedBase(codedBaseSummary);
        CodedBaseObjects.PrescriptionCBO.Builder prescriptionCboBuilder = CodedBaseObjects.PrescriptionCBO.newBuilder();

        prescriptionCboBuilder.setPrescriptionInfo(prescriptionSummary.getPrescriptionInfo());
        prescriptionCboBuilder.setBase(codedBase);
        return prescriptionCboBuilder.build();
    }

    public static CodedBaseObjects.FamilyHistoryCBO normalizeFamilyHistories(SummaryObjects.FamilyHistorySummary summary) {
        if (summary == null || summary.getSerializedSize() == 0) return null;

        SummaryObjects.CodedBaseSummary codedBaseSummary = summary.getBase();
        CodedBaseObjects.CodedBase codedBase = buildCodedBase(codedBaseSummary);
        CodedBaseObjects.FamilyHistoryCBO.Builder fhCboBuilder = CodedBaseObjects.FamilyHistoryCBO.newBuilder();

        if (summary.hasFamilyHistoryInfo())
            fhCboBuilder.setFamilyHistoryInfo(summary.getFamilyHistoryInfo());

        fhCboBuilder.setBase(codedBase);

        return fhCboBuilder.build();
    }

    public static CodedBaseObjects.ImmunizationCBO normalizeImmunization(SummaryObjects.ImmunizationSummary summary) {
        if (summary == null || summary.getSerializedSize() == 0) return null;

        SummaryObjects.CodedBaseSummary codedBaseSummary = summary.getBase();
        CodedBaseObjects.CodedBase codedBase = buildCodedBase(codedBaseSummary);
        CodedBaseObjects.ImmunizationCBO.Builder immuCboBuilder = CodedBaseObjects.ImmunizationCBO.newBuilder();

        if (summary.hasImmunizationInfo())
            immuCboBuilder.setImmunizationInfo(summary.getImmunizationInfo());

        immuCboBuilder.setBase(codedBase);
        return immuCboBuilder.build();
    }



    private static CodedBaseObjects.CodedBase buildCodedBase(SummaryObjects.CodedBaseSummary codedBaseSummary) {
        CodedBaseObjects.CodedBase.Builder codedBaseBuilder = CodedBaseObjects.CodedBase.newBuilder();

        codedBaseBuilder.setDataCatalogMeta(codedBaseSummary.getDataCatalogMeta());

        final List<UUID> parsingDetailsIdList =
                codedBaseSummary.getParsingDetailsList().stream().map(ParsingDetails::getInternalId)
                        .collect(Collectors.toList());
        codedBaseBuilder.addAllParsingDetailsIds(parsingDetailsIdList);

        final List<UUID> sourceIds =
                codedBaseSummary.getSourcesList().stream().map(Source::getInternalId).collect(Collectors.toList());
        codedBaseBuilder.addAllSourceIds(sourceIds);

        codedBaseBuilder.setPrimaryActorId(codedBaseSummary.getPrimaryActor().getInternalId());
        List<UUID> supplementaryActors =
                codedBaseSummary.getSupplementaryActorsList().stream().map(BaseObjects.ClinicalActor::getInternalId)
                        .collect(Collectors.toList());
        codedBaseBuilder.addAllSupplementaryActorIds(supplementaryActors);

        final List<UUID> encounterIdList =
                codedBaseSummary.getEncountersList().stream().map(CodedBaseObjects.Encounter::getInternalId)
                        .collect(Collectors.toList());
        codedBaseBuilder.addAllEncounterIds(encounterIdList);
        return codedBaseBuilder.build();
    }
}
