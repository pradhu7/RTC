package com.apixio.nassembly.ffsclaims

import com.apixio.datacatalog.BaseObjects.ClinicalActor
import com.apixio.datacatalog.CodedBaseObjects.{Encounter, FfsClaimCBO}
import com.apixio.datacatalog.DataCatalogMetaOuterClass.DataCatalogMeta
import com.apixio.datacatalog.FfsClaimOuterClass.FfsClaim
import com.apixio.datacatalog.PatientProto.CodedBasePatient
import com.apixio.datacatalog.SkinnyPatientProto.PatientFfsClaims
import com.apixio.datacatalog.SummaryObjects.{EncounterSummary, FfsClaimSummary}
import com.apixio.datacatalog.{ParsingDetailsOuterClass, PatientMetaProto, SourceOuterClass, UUIDOuterClass}
import com.apixio.nassembly.BaseConsolidator
import com.apixio.nassembly.clinicalactor.ClinicalActorUtils
import com.apixio.nassembly.encounter.EncounterUtils
import com.apixio.nassembly.procedure.ProcedureUtils
import com.apixio.nassembly.util.BaseEntityUtil
import com.apixio.util.nassembly.{CaretParser, DateBucketUtils, SummaryUtils}

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConverters._

object FfsClaimUtils {


  /**
   * Creates a wrapper proto by deduplicating the sources, parsing details, encounters, and clinical actors
   *
   * @param ffsClaims List of ffsClaims within the same date range
   * @return
   */
  def wrapAsPatient(dateBucket: String, ffsClaims: List[FfsClaimSummary]): PatientFfsClaims = {
    val procedureBuilder = PatientFfsClaims.newBuilder
    if (ffsClaims.isEmpty) return procedureBuilder.build

    val clinicalActors = consolidateActors(ffsClaims)
    val parsingDetails = BaseConsolidator
      .dedupParsingDetails(ffsClaims.flatMap(_.getBase.getParsingDetailsList).asJava)
    val sources = BaseConsolidator.dedupSources(ffsClaims.flatMap(_.getBase.getSourcesList).asJava)
    val encounters = EncounterUtils.mergeEncounters(ffsClaims.flatMap(_.getBase.getEncountersList))
    val patientMeta = BaseConsolidator.mergePatientMeta(ffsClaims.map(_.getBase.getPatientMeta).asJava)
    val catalogMeta = BaseConsolidator.mergeCatalogMeta(ffsClaims.map(_.getBase.getDataCatalogMeta).asJava)
      .toBuilder
      .setOriginalId(DateBucketUtils.dateBucketId(FfsClaimExchange.dataTypeName, dateBucket))
      .build()

    val normalizedAndConsolidatedffsClaims = normalizeAndConsolidateFfsClaims(
      ffsClaims.filterNot(_.getFfsClaim.getProcedureInfo.getDeleteIndicator))

    procedureBuilder.addAllFfsClaims(normalizedAndConsolidatedffsClaims.asJava)

    val basePatient: CodedBasePatient = BaseEntityUtil.buildCodedBasePatient(patientMeta, catalogMeta,
      parsingDetails, sources, clinicalActors, encounters)

    procedureBuilder.setBase(basePatient)
    procedureBuilder.build
  }

  def getPatientId(ffsClaims: List[FfsClaimSummary]): UUIDOuterClass.UUID = {
    if (ffsClaims.isEmpty) return null
    val patientIds = ffsClaims.map(_.getBase.getPatientMeta.getPatientId)
    if (patientIds.size > 1) throw new Exception("These don't belong to the same patient")
    patientIds.iterator.next
  }

  def consolidateActors(ffsClaims: List[FfsClaimSummary]): Iterable[ClinicalActor] = {
    val primaryClinicalActors = ffsClaims.map(p => p.getBase.getPrimaryActor)
    val supplementaryClinicalActors = ffsClaims.flatMap(p => p.getBase.getSupplementaryActorsList)
    val allActors = Seq(primaryClinicalActors, supplementaryClinicalActors).flatten.filter(ClinicalActorUtils.notEmpty)
    ClinicalActorUtils.dedupClincalActors(allActors)
  }

  def separateEncounters(ffsClaims: List[FfsClaimSummary]): Iterable[EncounterSummary] = {
    val parsingDetails = consolidateParsingDetails(ffsClaims)
    val sources = consolidateSources(ffsClaims)
    val encounters = consolidateEncounters(ffsClaims)
    val actors = consolidateActors(ffsClaims)
    val patientMeta = consolidatePatientMeta(ffsClaims)
    encounters
      .map(e => SummaryUtils.createEncounterSummary(e, patientMeta, parsingDetails, sources, actors.toList.asJava))
  }

  def consolidatePatientMeta(values: Seq[FfsClaimSummary]): PatientMetaProto.PatientMeta = {
    BaseConsolidator.mergePatientMeta(values.map(_.getBase.getPatientMeta).asJava)
  }

  def consolidateEncounters(ffsClaimsummaries: List[FfsClaimSummary]): Iterable[Encounter] = {
    val encounters = ffsClaimsummaries.flatMap(_.getBase.getEncountersList)
    EncounterUtils.mergeEncounters(encounters)
  }

  def consolidateParsingDetails(summaries: List[FfsClaimSummary]): java.util.List[ParsingDetailsOuterClass.ParsingDetails] = {
    val parsingDetails = summaries.flatMap(p => p.getBase.getParsingDetailsList)
    BaseConsolidator.dedupParsingDetails(parsingDetails.asJava)
  }

  def consolidateSources(summaries: List[FfsClaimSummary]): java.util.List[SourceOuterClass.Source] = {
    val sources = summaries.flatMap(p => p.getBase.getSourcesList)
    BaseConsolidator.dedupSources(sources.asJava)
  }

  /**
   * ffsClaimsummaries are flattened by supporting diagnosis code
   * We need to normalize them and then concat the lists together for the ffs claim identity
   *
   * @param ffsClaims List of denormalized ffsClaims
   * @return
   */
  def normalizeAndConsolidateFfsClaims(ffsClaims: Seq[FfsClaimSummary]): Iterable[FfsClaimCBO] = {
    val normalized = ffsClaims.map(SummaryUtils.normalizeFfsClaim)
    consolidateSupportDiagnosis(normalized)
  }

  private def consolidateSupportDiagnosis(ffsClaims: Seq[FfsClaimCBO]): Iterable[FfsClaimCBO] = {
    ffsClaims.groupBy(p => getIdentity(p.getFfsClaim, p.getBase.getDataCatalogMeta)).map {
      case (_, ffsClaims) =>
        val codes = ffsClaims.flatMap(_.getSupportingDiagnosisCodesList)
          .filterNot(c => {
            c.getCode.isEmpty || c.getSystem.isEmpty || c.getSystemOid.isEmpty // Not valid
          })
          .distinct
        ffsClaims.head.toBuilder.clearSupportingDiagnosisCodes().addAllSupportingDiagnosisCodes(codes.asJava).build()
    }
  }

  private def getIdentity(ffsClaim: FfsClaim, catalogMeta: DataCatalogMeta): Seq[String] = {
    ProcedureUtils.getIdentity(ffsClaim.getProcedureInfo, catalogMeta) :+ CaretParser.toString(ffsClaim.getBillingInfo.getBillType)
  }

}
