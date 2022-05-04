package com.apixio.nassembly.procedure

import com.apixio.datacatalog.BaseObjects.ClinicalActor
import com.apixio.datacatalog.CodedBaseObjects.{Encounter, ProcedureCBO}
import com.apixio.datacatalog.DataCatalogMetaOuterClass.DataCatalogMeta
import com.apixio.datacatalog.PatientProto.CodedBasePatient
import com.apixio.datacatalog.SkinnyPatientProto.PatientProcedure
import com.apixio.datacatalog.SummaryObjects.{ClinicalActorSummary, EncounterSummary, ProcedureSummary}
import com.apixio.datacatalog._
import com.apixio.nassembly.BaseConsolidator
import com.apixio.nassembly.clinicalactor.ClinicalActorUtils
import com.apixio.nassembly.encounter.EncounterUtils
import com.apixio.nassembly.util.BaseEntityUtil
import com.apixio.util.nassembly.{CaretParser, DateBucketUtils, SummaryUtils}

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConverters._

object ProcedureUtils {

  /**
   * Creates a wrapper proto by deduplicating the sources, parsing details, encounters, and clinical actors
   *
   * @param procedures List of procedures within the same date range
   * @return
   */
  def wrapAsPatient(dateBucket: String, procedures: List[ProcedureSummary]): PatientProcedure = {
    val procedureBuilder = PatientProcedure.newBuilder
    if (procedures.isEmpty) return procedureBuilder.build

    val clinicalActors = consolidateActors(procedures)
    val parsingDetails = BaseConsolidator
      .dedupParsingDetails(procedures.flatMap(_.getBase.getParsingDetailsList).asJava)
    val sources = BaseConsolidator.dedupSources(procedures.flatMap(_.getBase.getSourcesList).asJava)
    val encounters = EncounterUtils.mergeEncounters(procedures.flatMap(_.getBase.getEncountersList))
    val patientMeta = BaseConsolidator.mergePatientMeta(procedures.map(_.getBase.getPatientMeta).asJava)
    val catalogMeta = BaseConsolidator.mergeCatalogMeta(procedures.map(_.getBase.getDataCatalogMeta).asJava)
      .toBuilder
      .setOriginalId(DateBucketUtils.dateBucketId(ProcedureExchange.dataTypeName, dateBucket))
      .build()

    val normalizedAndConsolidatedProcedures = normalizeAndConsolidateProcedures(
      procedures.filterNot(_.getProcedureInfo.getDeleteIndicator))

    procedureBuilder.addAllProcedures(normalizedAndConsolidatedProcedures.asJava)

    val basePatient: CodedBasePatient = BaseEntityUtil.buildCodedBasePatient(patientMeta, catalogMeta,
      parsingDetails, sources, clinicalActors, encounters)

    procedureBuilder.setBase(basePatient)
    procedureBuilder.build
  }

  def getPatientId(procedures: List[ProcedureSummary]): UUIDOuterClass.UUID = {
    if (procedures.isEmpty) return null
    val patientIds = procedures.map(_.getBase.getPatientMeta.getPatientId)
    if (patientIds.size > 1) throw new Exception("These don't belong to the same patient")
    patientIds.iterator.next
  }

  def consolidateActors(procedures: List[ProcedureSummary]): Iterable[ClinicalActor] = {
    val primaryClinicalActors = procedures.map(p => p.getBase.getPrimaryActor)
    val supplementaryClinicalActors = procedures.flatMap(p => p.getBase.getSupplementaryActorsList)
    val allActors = Seq(primaryClinicalActors, supplementaryClinicalActors).flatten.filter(ClinicalActorUtils.notEmpty)
    ClinicalActorUtils.dedupClincalActors(allActors)
  }

  def separateEncounters(procedures: List[ProcedureSummary]): Iterable[EncounterSummary] = {
    val parsingDetails = consolidateParsingDetails(procedures)
    val sources = consolidateSources(procedures)
    val encounters = consolidateEncounters(procedures)
    val actors = consolidateActors(procedures)
    val patientMeta = consolidatePatientMeta(procedures)
    encounters
      .map(e => SummaryUtils.createEncounterSummary(e, patientMeta, parsingDetails, sources, actors.toList.asJava))
  }

  def consolidatePatientMeta(values: Seq[ProcedureSummary]): PatientMetaProto.PatientMeta = {
    BaseConsolidator.mergePatientMeta(values.map(_.getBase.getPatientMeta).asJava)
  }

  def consolidateEncounters(procedureSummaries: List[ProcedureSummary]): Iterable[Encounter] = {
    val encounters = procedureSummaries.flatMap(_.getBase.getEncountersList)
    EncounterUtils.mergeEncounters(encounters)
  }

  def consolidateParsingDetails(summaries: List[ProcedureSummary]): java.util.List[ParsingDetailsOuterClass.ParsingDetails] = {
    val parsingDetails = summaries.flatMap(p => p.getBase.getParsingDetailsList)
    BaseConsolidator.dedupParsingDetails(parsingDetails.asJava)
  }

  def consolidateSources(summaries: List[ProcedureSummary]): java.util.List[SourceOuterClass.Source] = {
    val sources = summaries.flatMap(p => p.getBase.getSourcesList)
    BaseConsolidator.dedupSources(sources.asJava)
  }

  /**
   * ProcedureSummaries are flattened by supporting diagnosis code
   * We need to normalize them and then concat the lists together for the procedure identity
   *
   * @param procedures List of denormalized procedures
   * @return
   */
  def normalizeAndConsolidateProcedures(procedures: Seq[ProcedureSummary]): Iterable[ProcedureCBO] = {
    val normalized = procedures.map(SummaryUtils.normalizeProcedure)
    consolidateSupportDiagnosis(normalized)
  }

  private def consolidateSupportDiagnosis(procedures: Seq[ProcedureCBO]): Iterable[ProcedureCBO] = {
    procedures.groupBy(p => getIdentity(p.getProcedureInfo, p.getBase.getDataCatalogMeta)).map {
      case (_, procedures) =>
        val codes = procedures.flatMap(_.getSupportingDiagnosisCodesList)
          .filterNot(c => {
            c.getCode.isEmpty || c.getSystem.isEmpty || c.getSystemOid.isEmpty // Not valid
          })
          .distinct
        procedures.head.toBuilder.clearSupportingDiagnosisCodes().addAllSupportingDiagnosisCodes(codes.asJava).build()
    }
  }

  def getIdentity(procedureInfo: ProcedureProto.ProcedureInfo, catalogMeta: DataCatalogMeta): Seq[String] = {
    List(
      CaretParser.toString(catalogMeta.getOriginalId),
      procedureInfo.getPerformedOn.getEpochMs.toString,
      procedureInfo.getEndDate.getEpochMs.toString,
      CaretParser.toString(procedureInfo.getCode)
    )
  }
}
