package com.apixio.nassembly.patientimmunization

import com.apixio.datacatalog.CodedBaseObjects.{Encounter, ImmunizationCBO}
import com.apixio.datacatalog.ExternalIdOuterClass.ExternalId
import com.apixio.datacatalog.PatientProto.CodedBasePatient
import com.apixio.datacatalog.SkinnyPatientProto.PatientImmunization
import com.apixio.datacatalog.SummaryObjects.{ClinicalActorSummary, ImmunizationSummary}
import com.apixio.datacatalog._
import com.apixio.nassembly.BaseConsolidator
import com.apixio.nassembly.clinicalactor.ClinicalActorUtils
import com.apixio.nassembly.encounter.EncounterUtils
import com.apixio.nassembly.mergeutils.CodedBaseSummaryUtils
import com.apixio.nassembly.util.BaseEntityUtil
import com.apixio.util.nassembly.SummaryUtils

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConverters._

object ImmunizationUtils {

  /**
   * Creates a wrapper proto by deduplicating the sources, parsing details, and clinical actors
   *
   * @param summaries List of ImmunizationSummary claims within the same date range
   * @return
   */

  def wrapAsPatient(summaries: List[ImmunizationSummary]): PatientImmunization = {
    val immuSpBuilder = PatientImmunization.newBuilder
    if (summaries.isEmpty) return immuSpBuilder.build

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
      .setOriginalId(ExternalId.newBuilder().setAssignAuthority("Immunizations").build())
      .build()

    val normalizedAndConsolidatedBaseObj = normalizeAndConsolidateSummaries(summaries)

    val codedBase: CodedBasePatient = BaseEntityUtil
      .buildCodedBasePatient(patientMeta, catalogMeta, parsingDetails, sources,
        clinicalActors, encounters)

    immuSpBuilder.setBase(codedBase)
    immuSpBuilder.addAllImmunizations(normalizedAndConsolidatedBaseObj.asJava)
    immuSpBuilder.build
  }

  def normalizeAndConsolidateSummaries(summaries: List[ImmunizationSummary]): Seq[ImmunizationCBO] = {
    summaries.map(SummaryUtils.normalizeImmunization)
  }

  def getPatientId(summaries: List[ImmunizationSummary]): UUIDOuterClass.UUID = {
    if (summaries.isEmpty) return null

    val patientIds = summaries.map(summary => getPatientMeta(summary).getPatientId)
    if (patientIds.size > 1) throw new Exception("These don't belong to the same patient")
    patientIds.iterator.next
  }

  def separateEncounters(summaries: List[ImmunizationSummary]): Iterable[SummaryObjects.EncounterSummary] = {
    val parsingDetails = consolidateParsingDetails(summaries)
    val sources = consolidateSources(summaries)
    val encounters = consolidateEncounters(summaries)
    val actors = CodedBaseSummaryUtils.consolidateActors(summaries.map(_.getBase))
    val patientMeta = consolidatePatientMeta(summaries)
    encounters
      .map(e => SummaryUtils.createEncounterSummary(e, patientMeta, parsingDetails, sources, actors.toList.asJava))
  }

  def consolidateParsingDetails(summaries: List[ImmunizationSummary]): java.util.List[ParsingDetailsOuterClass.ParsingDetails] = {
    val parsingDetailsList = summaries.flatMap(summary => getParsingDetailsList(summary)).asJava
    BaseConsolidator.dedupParsingDetails(parsingDetailsList)
  }

  def consolidateSources(summaries: List[ImmunizationSummary]): java.util.List[SourceOuterClass.Source] = {
    val sources = summaries.flatMap(summary => getSourceList(summary)).asJava
    BaseConsolidator.dedupSources(sources)
  }

  def consolidateEncounters(summaries: List[ImmunizationSummary]): Iterable[Encounter] = {
    val encountersList = summaries.flatMap(summary => getEncountersList(summary))
    EncounterUtils.mergeEncounters(encountersList)
  }

  def consolidatePatientMeta(summaries: Seq[ImmunizationSummary]): PatientMetaProto.PatientMeta = {
    BaseConsolidator.mergePatientMeta(summaries.map(summary => getPatientMeta(summary)).asJava)
  }

  private def getCodedBase(ImmunizationSummary: ImmunizationSummary) = {
    ImmunizationSummary.getBase
  }

  private def getEncountersList(summary: ImmunizationSummary) = {
    getCodedBase(summary).getEncountersList
  }

  private def getPatientMeta(summary: ImmunizationSummary) = {
    getCodedBase(summary).getPatientMeta
  }

  private def getSupplementaryActorList(summary: ImmunizationSummary) = {
    getCodedBase(summary).getSupplementaryActorsList
  }

  private def getPrimaryActor(summary: ImmunizationSummary) = {
    getCodedBase(summary).getPrimaryActor
  }

  private def getSourceList(summary: ImmunizationSummary) = {
    getCodedBase(summary).getSourcesList
  }

  private def getParsingDetailsList(summary: ImmunizationSummary) = {
    getCodedBase(summary).getParsingDetailsList
  }

}