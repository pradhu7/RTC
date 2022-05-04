package com.apixio.nassembly.patientfamilyhistory

import com.apixio.datacatalog.CodedBaseObjects.{Encounter, FamilyHistoryCBO}
import com.apixio.datacatalog.ExternalIdOuterClass.ExternalId
import com.apixio.datacatalog.PatientProto.CodedBasePatient
import com.apixio.datacatalog.SkinnyPatientProto.PatientFamilyHistory
import com.apixio.datacatalog.SummaryObjects.FamilyHistorySummary
import com.apixio.datacatalog._
import com.apixio.nassembly.BaseConsolidator
import com.apixio.nassembly.encounter.EncounterUtils
import com.apixio.nassembly.mergeutils.CodedBaseSummaryUtils
import com.apixio.nassembly.util.BaseEntityUtil
import com.apixio.util.nassembly.SummaryUtils

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConverters._

object FamilyHistoryUtils {

  /**
   * Creates a wrapper proto by deduplicating the sources, parsing details, and clinical actors
   *
   * @param summaries List of FamilyHistorySummary claims within the same date range
   * @return
   */

  def wrapAsPatient(summaries: List[FamilyHistorySummary]): PatientFamilyHistory = {
    val fhSpBuilder = PatientFamilyHistory.newBuilder
    if (summaries.isEmpty) return fhSpBuilder.build

    val clinicalActors = CodedBaseSummaryUtils.consolidateActors(summaries.map(_.getBase))

    val rawParsingDetails = summaries.flatMap(summary => getParsingDetailsList(summary))
      .asJava
    val parsingDetails = BaseConsolidator.dedupParsingDetails(rawParsingDetails)

    val rawSources = summaries.flatMap(summary => getSourceList(summary)).asJava
    val sources = BaseConsolidator.dedupSources(rawSources)

    val rawEncounterList = summaries.flatMap(summaryList => getEncountersList(summaryList))
    val encounters = EncounterUtils.mergeEncounters(rawEncounterList)

    val rawPatientMeta = summaries.map(summaryList => getPatientMeta(summaryList)).asJava
    val patientMeta = BaseConsolidator.mergePatientMeta(rawPatientMeta)
    val rawCatalogMeta = summaries.map(summaryList => getCodedBase(summaryList)
      .getDataCatalogMeta).asJava
    val catalogMeta = BaseConsolidator.mergeCatalogMeta(rawCatalogMeta)
      .toBuilder
      .setOriginalId(ExternalId.newBuilder().setAssignAuthority("FamilyHistory").build())
      .build()

    val normalizedAndConsolidatedBaseObj = normalizeAndConsolidateSummaries(summaries)

    val codedBase: CodedBasePatient = BaseEntityUtil
      .buildCodedBasePatient(patientMeta, catalogMeta, parsingDetails, sources,
        clinicalActors, encounters)

    fhSpBuilder.setBase(codedBase)
    fhSpBuilder.addAllFamilyHistories(normalizedAndConsolidatedBaseObj.asJava)
    fhSpBuilder.build
  }

  def normalizeAndConsolidateSummaries(summaries: List[FamilyHistorySummary]): Seq[FamilyHistoryCBO] = {
    summaries.map(SummaryUtils.normalizeFamilyHistories(_))
  }

  def getPatientId(FamilyHistorySummaryList: List[FamilyHistorySummary]): UUIDOuterClass.UUID = {
    if (FamilyHistorySummaryList.isEmpty) return null

    val patientIds = FamilyHistorySummaryList.map(summary => getPatientMeta(summary).getPatientId)
    if (patientIds.size > 1) throw new Exception("These don't belong to the same patient")
    patientIds.iterator.next
  }

  def separateEncounters(summaries: List[FamilyHistorySummary]): Iterable[SummaryObjects.EncounterSummary] = {
    val parsingDetails = consolidateParsingDetails(summaries)
    val sources = consolidateSources(summaries)
    val encounters = consolidateEncounters(summaries)
    val actors = CodedBaseSummaryUtils.consolidateActors(summaries.map(_.getBase))
    val patientMeta = consolidatePatientMeta(summaries)
    encounters
      .map(e => SummaryUtils.createEncounterSummary(e, patientMeta, parsingDetails, sources, actors.toList.asJava))
  }

  def consolidateParsingDetails(summaries: List[FamilyHistorySummary]): java.util.List[ParsingDetailsOuterClass.ParsingDetails] = {
    val parsingDetailsList = summaries.flatMap(summary => getParsingDetailsList(summary)).asJava
    BaseConsolidator.dedupParsingDetails(parsingDetailsList)
  }

  def consolidateSources(summaries: List[FamilyHistorySummary]): java.util.List[SourceOuterClass.Source] = {
    val sources = summaries.flatMap(summary => getSourceList(summary)).asJava
    BaseConsolidator.dedupSources(sources)
  }

  def consolidateEncounters(summaries: List[FamilyHistorySummary]): Iterable[Encounter] = {
    val encountersList = summaries.flatMap(summary => getEncountersList(summary))
    EncounterUtils.mergeEncounters(encountersList)
  }

  def consolidatePatientMeta(summaries: Seq[FamilyHistorySummary]): PatientMetaProto.PatientMeta = {
    BaseConsolidator.mergePatientMeta(summaries.map(summary => getPatientMeta(summary)).asJava)
  }

  private def getCodedBase(FamilyHistorySummary: FamilyHistorySummary) = {
    FamilyHistorySummary.getBase
  }

  private def getEncountersList(summary: FamilyHistorySummary) = {
    getCodedBase(summary).getEncountersList
  }

  private def getPatientMeta(summary: FamilyHistorySummary) = {
    getCodedBase(summary).getPatientMeta
  }

  private def getSupplementaryActorList(summary: FamilyHistorySummary) = {
    getCodedBase(summary).getSupplementaryActorsList
  }

  private def getPrimaryActor(summary: FamilyHistorySummary) = {
    getCodedBase(summary).getPrimaryActor
  }

  private def getSourceList(summary: FamilyHistorySummary) = {
    getCodedBase(summary).getSourcesList
  }

  private def getParsingDetailsList(summary: FamilyHistorySummary) = {
    getCodedBase(summary).getParsingDetailsList
  }

}