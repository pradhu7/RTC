package com.apixio.nassembly.raclaim

import com.apixio.datacatalog.BaseObjects.ClinicalActor
import com.apixio.datacatalog.CodedBaseObjects.{Encounter, RaClaimCBO}
import com.apixio.datacatalog.PatientProto.CodedBasePatient
import com.apixio.datacatalog.SkinnyPatientProto.PatientRaClaims
import com.apixio.datacatalog.SummaryObjects.{ClinicalActorSummary, EncounterSummary, RaClaimSummary}
import com.apixio.datacatalog._
import com.apixio.nassembly.BaseConsolidator
import com.apixio.nassembly.clinicalactor.ClinicalActorUtils
import com.apixio.nassembly.encounter.EncounterUtils
import com.apixio.nassembly.util.BaseEntityUtil
import com.apixio.util.nassembly.{DateBucketUtils, SummaryUtils}

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConverters._

object RaClaimUtils {

  /**
   * Creates a wrapper proto by deduplicating the sources, parsing details, encounters, and clinical actors
   *
   * @param raClaims List of raClaims within the same date range
   * @return
   */
  def wrapAsPatient(dateBucket: String, raClaims: List[RaClaimSummary]): PatientRaClaims = {
    val raClaimBuilder = PatientRaClaims.newBuilder
    if (raClaims.isEmpty) return raClaimBuilder.build

    val clinicalActors = consolidateActors(raClaims)
    val parsingDetails = BaseConsolidator
      .dedupParsingDetails(raClaims.flatMap(_.getBase.getParsingDetailsList).asJava)
    val sources = BaseConsolidator.dedupSources(raClaims.flatMap(_.getBase.getSourcesList).asJava)
    val encounters = EncounterUtils.mergeEncounters(raClaims.flatMap(_.getBase.getEncountersList))
    val patientMeta = BaseConsolidator.mergePatientMeta(raClaims.map(_.getBase.getPatientMeta).asJava)
    val catalogMeta = BaseConsolidator.mergeCatalogMeta(raClaims.map(_.getBase.getDataCatalogMeta).asJava)
      .toBuilder
      .setOriginalId(DateBucketUtils.dateBucketId(RaClaimExchange.dataTypeName, dateBucket))
      .build()

    val normalizedAndConsolidatedRaClaims = normalizeAndConsolidateRaClaims(
      raClaims.filterNot(_.getRaClaim.getProblemInfo.getDeleteIndicator))

    raClaimBuilder.addAllRaClaims(normalizedAndConsolidatedRaClaims.asJava)

    val basePatient: CodedBasePatient = BaseEntityUtil.buildCodedBasePatient(patientMeta, catalogMeta,
      parsingDetails, sources, clinicalActors, encounters)

    raClaimBuilder.setBase(basePatient)
    raClaimBuilder.build
  }

  def getPatientId(raClaims: List[RaClaimSummary]): UUIDOuterClass.UUID = {
    if (raClaims.isEmpty) return null
    val patientIds = raClaims.map(_.getBase.getPatientMeta.getPatientId)
    if (patientIds.size > 1) throw new Exception("These don't belong to the same patient")
    patientIds.iterator.next
  }

  def consolidateActors(raClaims: List[RaClaimSummary]): Iterable[ClinicalActor] = {
    val primaryClinicalActors = raClaims.map(p => p.getBase.getPrimaryActor)
    val supplementaryClinicalActors = raClaims.flatMap(p => p.getBase.getSupplementaryActorsList)
    val allActors = Seq(primaryClinicalActors, supplementaryClinicalActors).flatten.filter(ClinicalActorUtils.notEmpty)
    ClinicalActorUtils.dedupClincalActors(allActors)
  }

  def separateEncounters(raClaims: List[RaClaimSummary]): Iterable[EncounterSummary] = {
    val parsingDetails = consolidateParsingDetails(raClaims)
    val sources = consolidateSources(raClaims)
    val encounters = consolidateEncounters(raClaims)
    val actors = consolidateActors(raClaims)
    val patientMeta = consolidatePatientMeta(raClaims)
    encounters
      .map(e => SummaryUtils.createEncounterSummary(e, patientMeta, parsingDetails, sources, actors.toList.asJava))
  }

  def consolidatePatientMeta(values: Seq[RaClaimSummary]): PatientMetaProto.PatientMeta = {
    BaseConsolidator.mergePatientMeta(values.map(_.getBase.getPatientMeta).asJava)
  }

  def consolidateEncounters(raClaimSummaries: List[RaClaimSummary]): Iterable[Encounter] = {
    val encounters = raClaimSummaries.flatMap(_.getBase.getEncountersList)
    EncounterUtils.mergeEncounters(encounters)
  }

  def consolidateParsingDetails(summaries: List[RaClaimSummary]): java.util.List[ParsingDetailsOuterClass.ParsingDetails] = {
    val parsingDetails = summaries.flatMap(p => p.getBase.getParsingDetailsList)
    BaseConsolidator.dedupParsingDetails(parsingDetails.asJava)
  }

  def consolidateSources(summaries: List[RaClaimSummary]): java.util.List[SourceOuterClass.Source] = {
    val sources = summaries.flatMap(p => p.getBase.getSourcesList)
    BaseConsolidator.dedupSources(sources.asJava)
  }

  /**
   * RaClaimSummaries are flattened by supporting diagnosis code
   * We need to normalize them and then concat the lists together for the raClaim identity
   *
   * @param raClaims List of denormalized raClaims
   * @return
   */
  def normalizeAndConsolidateRaClaims(raClaims: Seq[RaClaimSummary]): Iterable[RaClaimCBO] = {
    raClaims.map(SummaryUtils.normalizeRaClaim)
  }
}
