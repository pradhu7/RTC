package com.apixio.nassembly.problem

import com.apixio.datacatalog.BaseObjects.ClinicalActor
import com.apixio.datacatalog.CodedBaseObjects.{Encounter, ProblemCBO}
import com.apixio.datacatalog.DataCatalogMetaOuterClass.DataCatalogMeta
import com.apixio.datacatalog.PatientProto.CodedBasePatient
import com.apixio.datacatalog.SkinnyPatientProto.PatientProblems
import com.apixio.datacatalog.SummaryObjects.{ClinicalActorSummary, EncounterSummary, ProblemSummary}
import com.apixio.datacatalog._
import com.apixio.nassembly.BaseConsolidator
import com.apixio.nassembly.clinicalactor.ClinicalActorUtils
import com.apixio.nassembly.encounter.EncounterUtils
import com.apixio.nassembly.util.BaseEntityUtil
import com.apixio.util.nassembly.{DateBucketUtils, SummaryUtils}

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConverters._

object ProblemUtils {

  /**
   * Creates a wrapper proto by deduplicating the sources, parsing details, encounters, and clinical actors
   *
   * @param problems List of problems within the same date range
   * @return
   */
  def wrapAsPatient(dateBucket: String, problems: List[ProblemSummary]): PatientProblems = {
    val problemBuilder = PatientProblems.newBuilder
    if (problems.isEmpty) return problemBuilder.build

    val clinicalActors = consolidateActors(problems)
    val parsingDetails = BaseConsolidator
      .dedupParsingDetails(problems.flatMap(_.getBase.getParsingDetailsList).asJava)
    val sources = BaseConsolidator.dedupSources(problems.flatMap(_.getBase.getSourcesList).asJava)
    val encounters = EncounterUtils.mergeEncounters(problems.flatMap(_.getBase.getEncountersList))
    val patientMeta = BaseConsolidator.mergePatientMeta(problems.map(_.getBase.getPatientMeta).asJava)
    val catalogMeta = BaseConsolidator.mergeCatalogMeta(problems.map(_.getBase.getDataCatalogMeta).asJava)
      .toBuilder
      .setOriginalId(DateBucketUtils.dateBucketId(ProblemExchange.dataTypeName, dateBucket))
      .build()

    val normalizedAndConsolidatedProblems = normalizeAndConsolidateProblems(
      problems.filterNot(_.getProblemInfo.getDeleteIndicator))

    problemBuilder.addAllProblems(normalizedAndConsolidatedProblems.asJava)

    val basePatient: CodedBasePatient = BaseEntityUtil.buildCodedBasePatient(patientMeta, catalogMeta,
      parsingDetails, sources, clinicalActors, encounters)

    problemBuilder.setBase(basePatient)
    problemBuilder.build
  }

  def getPatientId(problems: List[ProblemSummary]): UUIDOuterClass.UUID = {
    if (problems.isEmpty) return null
    val patientIds = problems.map(_.getBase.getPatientMeta.getPatientId)
    if (patientIds.size > 1) throw new Exception("These don't belong to the same patient")
    patientIds.iterator.next
  }

  def consolidateActors(problems: List[ProblemSummary]): Iterable[ClinicalActor] = {
    val primaryClinicalActors = problems.map(p => p.getBase.getPrimaryActor)
    val supplementaryClinicalActors = problems.flatMap(p => p.getBase.getSupplementaryActorsList)
    val allActors = Seq(primaryClinicalActors, supplementaryClinicalActors).flatten.filter(ClinicalActorUtils.notEmpty)
    ClinicalActorUtils.dedupClincalActors(allActors)
  }

  def separateEncounters(problems: List[ProblemSummary]): Iterable[EncounterSummary] = {
    val parsingDetails = consolidateParsingDetails(problems)
    val sources = consolidateSources(problems)
    val encounters = consolidateEncounters(problems)
    val actors = consolidateActors(problems)
    val patientMeta = consolidatePatientMeta(problems)
    encounters
      .map(e => SummaryUtils.createEncounterSummary(e, patientMeta, parsingDetails, sources, actors.toList.asJava))
  }

  def consolidatePatientMeta(values: Seq[ProblemSummary]): PatientMetaProto.PatientMeta = {
    BaseConsolidator.mergePatientMeta(values.map(_.getBase.getPatientMeta).asJava)
  }

  def consolidateEncounters(problemSummaries: List[ProblemSummary]): Iterable[Encounter] = {
    val encounters = problemSummaries.flatMap(_.getBase.getEncountersList)
    EncounterUtils.mergeEncounters(encounters)
  }

  def consolidateParsingDetails(summaries: List[ProblemSummary]): java.util.List[ParsingDetailsOuterClass.ParsingDetails] = {
    val parsingDetails = summaries.flatMap(p => p.getBase.getParsingDetailsList)
    BaseConsolidator.dedupParsingDetails(parsingDetails.asJava)
  }

  def consolidateSources(summaries: List[ProblemSummary]): java.util.List[SourceOuterClass.Source] = {
    val sources = summaries.flatMap(p => p.getBase.getSourcesList)
    BaseConsolidator.dedupSources(sources.asJava)
  }

  /**
   * ProblemSummaries are flattened by supporting diagnosis code
   * We need to normalize them and then concat the lists together for the problem identity
   *
   * @param problems List of denormalized problems
   * @return
   */
  def normalizeAndConsolidateProblems(problems: Seq[ProblemSummary]): Iterable[ProblemCBO] = {
    problems.map(SummaryUtils.normalizeProblem)
  }
}
