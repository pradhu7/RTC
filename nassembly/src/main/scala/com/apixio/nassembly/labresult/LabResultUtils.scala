package com.apixio.nassembly.labresult

import com.apixio.datacatalog.CodedBaseObjects.{Encounter, LabResultCBO}
import com.apixio.datacatalog.ExternalIdOuterClass.ExternalId
import com.apixio.datacatalog.PatientProto.CodedBasePatient
import com.apixio.datacatalog.SkinnyPatientProto.PatientLabResult
import com.apixio.datacatalog.SummaryObjects.{ClinicalActorSummary, LabResultSummary}
import com.apixio.datacatalog._
import com.apixio.model.patient.Patient
import com.apixio.nassembly.BaseConsolidator
import com.apixio.nassembly.apo.APOGenerator
import com.apixio.nassembly.clinicalactor.ClinicalActorUtils
import com.apixio.nassembly.encounter.EncounterUtils
import com.apixio.nassembly.mergeutils.CodedBaseSummaryUtils
import com.apixio.nassembly.util.BaseEntityUtil
import com.apixio.util.nassembly.SummaryUtils

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConverters._

object LabResultUtils {

  /**
   * Creates a wrapper proto by deduplicating the sources, parsing details, and clinical actors
   *
   * @param labResultSummaryList List of LabResultSummary claims within the same date range
   * @return
   */

  def wrapAsPatient(labResultSummaryList: List[LabResultSummary]): PatientLabResult = {
    val lrSpBuilder = PatientLabResult.newBuilder
    if (labResultSummaryList.isEmpty) return lrSpBuilder.build

    val clinicalActors = CodedBaseSummaryUtils.consolidateActors(labResultSummaryList.map(_.getBase))

    val rawParsingDetails = labResultSummaryList.flatMap(summary => getParsingDetailsList(summary))
      .asJava
    val parsingDetails = BaseConsolidator.dedupParsingDetails(rawParsingDetails)

    val rawSources = labResultSummaryList.flatMap(summary => getSourceList(summary)).asJava
    val sources = BaseConsolidator.dedupSources(rawSources)

    val rawEncounterList = labResultSummaryList.flatMap(summaryList => getEncountersList(summaryList))
    val encounters = EncounterUtils.mergeEncounters(rawEncounterList)

    val rawPatientMeta = labResultSummaryList.map(summaryList => getPatientMeta(summaryList)).asJava
    val patientMeta = BaseConsolidator.mergePatientMeta(rawPatientMeta)
    val rawCatalogMeta = labResultSummaryList.map(summaryList => getCodedBase(summaryList)
      .getDataCatalogMeta).asJava

    val catalogMeta = BaseConsolidator.mergeCatalogMeta(rawCatalogMeta)
      .toBuilder
      .setOriginalId(ExternalId.newBuilder().setAssignAuthority("LabResults").build())
      .build()

    val normalizedAndConsolidatedLabResultBaseObj = normalizeAndConsolidateLabResults(labResultSummaryList)

    val codedBase: CodedBasePatient = BaseEntityUtil
      .buildCodedBasePatient(patientMeta, catalogMeta, parsingDetails, sources,
        clinicalActors, encounters)

    lrSpBuilder.setBase(codedBase)
    lrSpBuilder.addAllLabResults(normalizedAndConsolidatedLabResultBaseObj.asJava)
    lrSpBuilder.build
  }

  def normalizeAndConsolidateLabResults(labResultSummaryList: List[LabResultSummary]): Seq[LabResultCBO] = {
    labResultSummaryList.map(SummaryUtils.normalizeLabResults)
  }

  def getPatientId(labResultSummaryList: List[LabResultSummary]): UUIDOuterClass.UUID = {
    if (labResultSummaryList.isEmpty) return null

    val patientIds = labResultSummaryList.map(summary => getPatientMeta(summary).getPatientId)
    if (patientIds.size > 1) throw new Exception("These don't belong to the same patient")
    patientIds.iterator.next
  }

  def separateEncounters(summaries: List[LabResultSummary]): Iterable[SummaryObjects.EncounterSummary] = {
    val parsingDetails = consolidateParsingDetails(summaries)
    val sources = consolidateSources(summaries)
    val encounters = consolidateEncounters(summaries)
    val actors = CodedBaseSummaryUtils.consolidateActors(summaries.map(_.getBase))
    val patientMeta = consolidatePatientMeta(summaries)
    encounters
      .map(e => SummaryUtils.createEncounterSummary(e, patientMeta, parsingDetails, sources, actors.toList.asJava))
  }

  def consolidateParsingDetails(labResultSummaries: List[LabResultSummary]): java.util.List[ParsingDetailsOuterClass.ParsingDetails] = {
    val parsingDetailsList = labResultSummaries.flatMap(summary => getParsingDetailsList(summary)).asJava
    BaseConsolidator.dedupParsingDetails(parsingDetailsList)
  }

  def consolidateSources(summaries: List[LabResultSummary]): java.util.List[SourceOuterClass.Source] = {
    val sources = summaries.flatMap(summary => getSourceList(summary)).asJava
    BaseConsolidator.dedupSources(sources)
  }

  def consolidateEncounters(summaries: List[LabResultSummary]): Iterable[Encounter] = {
    val encountersList = summaries.flatMap(summary => getEncountersList(summary))
    EncounterUtils.mergeEncounters(encountersList)
  }

  def consolidatePatientMeta(labResultSummaries: Seq[LabResultSummary]): PatientMetaProto.PatientMeta = {
    BaseConsolidator.mergePatientMeta(labResultSummaries.map(summary => getPatientMeta(summary)).asJava)
  }

  private def getCodedBase(labResultSummary: LabResultSummary) = {
    labResultSummary.getBase
  }

  private def getEncountersList(summary: LabResultSummary) = {
    getCodedBase(summary).getEncountersList
  }

  private def getPatientMeta(summary: LabResultSummary) = {
    getCodedBase(summary).getPatientMeta
  }

  private def getSupplementaryActorList(summary: LabResultSummary) = {
    getCodedBase(summary).getSupplementaryActorsList
  }

  private def getPrimaryActor(summary: LabResultSummary) = {
    getCodedBase(summary).getPrimaryActor
  }

  private def getSourceList(summary: LabResultSummary) = {
    getCodedBase(summary).getSourcesList
  }

  private def getParsingDetailsList(summary: LabResultSummary) = {
    getCodedBase(summary).getParsingDetailsList
  }

}