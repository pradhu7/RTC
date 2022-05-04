package com.apixio.nassembly.mao004

import com.apixio.datacatalog.BaseObjects.ClinicalActor
import com.apixio.datacatalog.CodedBaseObjects.{Encounter, Mao004CBO}
import com.apixio.datacatalog.PatientProto.CodedBasePatient
import com.apixio.datacatalog.SkinnyPatientProto.PatientMao004s
import com.apixio.datacatalog.SummaryObjects.{EncounterSummary, Mao004Summary}
import com.apixio.datacatalog._
import com.apixio.nassembly.BaseConsolidator
import com.apixio.nassembly.clinicalactor.ClinicalActorUtils
import com.apixio.nassembly.encounter.EncounterUtils
import com.apixio.nassembly.util.BaseEntityUtil
import com.apixio.util.nassembly.{DateBucketUtils, SummaryUtils}

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConverters._

object Mao004Utils {

  /**
   * Creates a wrapper proto by deduplicating the sources, parsing details, encounters, and clinical actors
   *
   * @param mao004s List of mao004s within the same date range
   * @return
   */
  def wrapAsPatient(dateBucket: String, mao004s: List[Mao004Summary]): PatientMao004s = {
    val mao004Builder = PatientMao004s.newBuilder
    if (mao004s.isEmpty) return mao004Builder.build

    val clinicalActors = consolidateActors(mao004s)
    val parsingDetails = BaseConsolidator
      .dedupParsingDetails(mao004s.flatMap(_.getBase.getParsingDetailsList).asJava)
    val sources = BaseConsolidator.dedupSources(mao004s.flatMap(_.getBase.getSourcesList).asJava)
    val encounters = EncounterUtils.mergeEncounters(mao004s.flatMap(_.getBase.getEncountersList))
    val patientMeta = BaseConsolidator.mergePatientMeta(mao004s.map(_.getBase.getPatientMeta).asJava)
    val catalogMeta = BaseConsolidator.mergeCatalogMeta(mao004s.map(_.getBase.getDataCatalogMeta).asJava)
      .toBuilder
      .setOriginalId(DateBucketUtils.dateBucketId(Mao004Exchange.dataTypeName, dateBucket))
      .build()

    val normalizedAndConsolidatedMao004s = normalizeAndConsolidateMao004s(
      mao004s.filterNot(_.getMao004.getProblemInfo.getDeleteIndicator))

    mao004Builder.addAllMao004S(normalizedAndConsolidatedMao004s.asJava)

    val basePatient: CodedBasePatient = BaseEntityUtil.buildCodedBasePatient(patientMeta, catalogMeta,
      parsingDetails, sources, clinicalActors, encounters)

    mao004Builder.setBase(basePatient)
    mao004Builder.build
  }

  def getPatientId(mao004s: List[Mao004Summary]): UUIDOuterClass.UUID = {
    if (mao004s.isEmpty) return null
    val patientIds = mao004s.map(_.getBase.getPatientMeta.getPatientId)
    if (patientIds.size > 1) throw new Exception("These don't belong to the same patient")
    patientIds.iterator.next
  }

  def consolidateActors(mao004s: List[Mao004Summary]): Iterable[ClinicalActor] = {
    val primaryClinicalActors = mao004s.map(p => p.getBase.getPrimaryActor)
    val supplementaryClinicalActors = mao004s.flatMap(p => p.getBase.getSupplementaryActorsList)
    val allActors = Seq(primaryClinicalActors, supplementaryClinicalActors).flatten.filter(ClinicalActorUtils.notEmpty)
    ClinicalActorUtils.dedupClincalActors(allActors)
  }

  def separateEncounters(mao004s: List[Mao004Summary]): Iterable[EncounterSummary] = {
    val parsingDetails = consolidateParsingDetails(mao004s)
    val sources = consolidateSources(mao004s)
    val encounters = consolidateEncounters(mao004s)
    val actors = consolidateActors(mao004s)
    val patientMeta = consolidatePatientMeta(mao004s)
    encounters
      .map(e => SummaryUtils.createEncounterSummary(e, patientMeta, parsingDetails, sources, actors.toList.asJava))
  }

  def consolidatePatientMeta(values: Seq[Mao004Summary]): PatientMetaProto.PatientMeta = {
    BaseConsolidator.mergePatientMeta(values.map(_.getBase.getPatientMeta).asJava)
  }

  def consolidateEncounters(mao004Summaries: List[Mao004Summary]): Iterable[Encounter] = {
    val encounters = mao004Summaries.flatMap(_.getBase.getEncountersList)
    EncounterUtils.mergeEncounters(encounters)
  }

  def consolidateParsingDetails(summaries: List[Mao004Summary]): java.util.List[ParsingDetailsOuterClass.ParsingDetails] = {
    val parsingDetails = summaries.flatMap(p => p.getBase.getParsingDetailsList)
    BaseConsolidator.dedupParsingDetails(parsingDetails.asJava)
  }

  def consolidateSources(summaries: List[Mao004Summary]): java.util.List[SourceOuterClass.Source] = {
    val sources = summaries.flatMap(p => p.getBase.getSourcesList)
    BaseConsolidator.dedupSources(sources.asJava)
  }

  /**
   * Mao004Summaries are flattened by supporting diagnosis code
   * We need to normalize them and then concat the lists together for the mao004 identity
   *
   * @param mao004s List of denormalized mao004s
   * @return
   */
  def normalizeAndConsolidateMao004s(mao004s: Seq[Mao004Summary]): Iterable[Mao004CBO] = {
    mao004s.map(SummaryUtils.normalizeMao004)
  }
}
