package com.apixio.nassembly.prescription

import com.apixio.datacatalog.CodedBaseObjects.Encounter
import com.apixio.datacatalog.ExternalIdOuterClass.ExternalId
import com.apixio.datacatalog.PatientProto.CodedBasePatient
import com.apixio.datacatalog.SkinnyPatientProto.PatientPrescription
import com.apixio.datacatalog.SummaryObjects.{ClinicalActorSummary, PrescriptionSummary}
import com.apixio.datacatalog._
import com.apixio.nassembly.BaseConsolidator
import com.apixio.nassembly.clinicalactor.ClinicalActorUtils
import com.apixio.nassembly.encounter.EncounterUtils
import com.apixio.nassembly.mergeutils.CodedBaseSummaryUtils
import com.apixio.nassembly.util.BaseEntityUtil
import com.apixio.util.nassembly.SummaryUtils

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConverters._

object PrescriptionUtils {

  /**
   * Creates a wrapper proto by deduplicating the sources, parsing details, and clinical actors
   *
   * @param summaries List of PrescriptionSummary claims within the same date range
   * @return
   */

  def wrapAsPatient(summaries: List[PrescriptionSummary]): PatientPrescription = {
    val patientPrescriptionBuilder = PatientPrescription.newBuilder
    if (summaries.isEmpty) return patientPrescriptionBuilder.build

    val clinicalActors = CodedBaseSummaryUtils.consolidateActors(summaries.map(_.getBase))

    val rawParsingDetails = summaries.flatMap(getParsingDetailsList)
      .asJava
    val parsingDetails = BaseConsolidator.dedupParsingDetails(rawParsingDetails)

    val rawSources = summaries.flatMap(getSourceList).asJava
    val sources = BaseConsolidator.dedupSources(rawSources)

    val rawEncounterList = summaries.flatMap(getEncountersList)
    val encounters = EncounterUtils.mergeEncounters(rawEncounterList)

    val rawPatientMeta = summaries.map(getPatientMeta).asJava
    val patientMeta = BaseConsolidator.mergePatientMeta(rawPatientMeta)
    val rawCatalogMeta = summaries.map(getCodedBase(_).getDataCatalogMeta).asJava
    val catalogMeta = BaseConsolidator.mergeCatalogMeta(rawCatalogMeta)
      .toBuilder
      .setOriginalId(ExternalId.newBuilder().setAssignAuthority("Prescriptions").build())
      .build()

    val normalizedPrescriptions = summaries.map(SummaryUtils.normalizePrescription)

    val codedBase: CodedBasePatient = BaseEntityUtil
      .buildCodedBasePatient(patientMeta, catalogMeta, parsingDetails, sources,
        clinicalActors, encounters)

    patientPrescriptionBuilder.setBase(codedBase)
    patientPrescriptionBuilder.addAllPrescriptions(normalizedPrescriptions.asJava)
    patientPrescriptionBuilder.build
  }

  def getPatientId(PrescriptionSummaryList: List[PrescriptionSummary]): UUIDOuterClass.UUID = {
    if (PrescriptionSummaryList.isEmpty) return null

    val patientIds = PrescriptionSummaryList.map(getPatientMeta(_).getPatientId)
    if (patientIds.size > 1) throw new Exception("These don't belong to the same patient")
    patientIds.iterator.next
  }

  def separateEncounters(summaries: List[PrescriptionSummary]): Iterable[SummaryObjects.EncounterSummary] = {
    val parsingDetails = consolidateParsingDetails(summaries)
    val sources = consolidateSources(summaries)
    val encounters = consolidateEncounters(summaries)
    val actors = CodedBaseSummaryUtils.consolidateActors(summaries.map(_.getBase))
    val patientMeta = consolidatePatientMeta(summaries)
    encounters
      .map(e => SummaryUtils.createEncounterSummary(e, patientMeta, parsingDetails, sources, actors.toList.asJava))
  }

  def consolidateParsingDetails(labResultSummaries: List[PrescriptionSummary]): java.util.List[ParsingDetailsOuterClass.ParsingDetails] = {
    val parsingDetailsList = labResultSummaries.flatMap(getParsingDetailsList).asJava
    BaseConsolidator.dedupParsingDetails(parsingDetailsList)
  }

  def consolidateSources(summaries: List[PrescriptionSummary]): java.util.List[SourceOuterClass.Source] = {
    val sources = summaries.flatMap(getSourceList).asJava
    BaseConsolidator.dedupSources(sources)
  }

  def consolidateEncounters(summaries: List[PrescriptionSummary]): Iterable[Encounter] = {
    val encountersList = summaries.flatMap(getEncountersList)
    EncounterUtils.mergeEncounters(encountersList)
  }

  def consolidatePatientMeta(labResultSummaries: Seq[PrescriptionSummary]): PatientMetaProto.PatientMeta = {
    BaseConsolidator.mergePatientMeta(labResultSummaries.map(getPatientMeta).asJava)
  }

  private def getCodedBase(PrescriptionSummary: PrescriptionSummary) = {
    PrescriptionSummary.getBase
  }

  private def getEncountersList(summary: PrescriptionSummary) = {
    getCodedBase(summary).getEncountersList
  }

  private def getPatientMeta(summary: PrescriptionSummary) = {
    getCodedBase(summary).getPatientMeta
  }

  private def getSupplementaryActorList(summary: PrescriptionSummary) = {
    getCodedBase(summary).getSupplementaryActorsList
  }

  private def getPrimaryActor(summary: PrescriptionSummary) = {
    getCodedBase(summary).getPrimaryActor
  }

  private def getSourceList(summary: PrescriptionSummary) = {
    getCodedBase(summary).getSourcesList
  }

  private def getParsingDetailsList(summary: PrescriptionSummary) = {
    getCodedBase(summary).getParsingDetailsList
  }

}