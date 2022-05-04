package com.apixio.nassembly.biometricvalue

import com.apixio.datacatalog.BaseObjects.ClinicalActor
import com.apixio.datacatalog.CodedBaseObjects.{BiometricValue, Encounter}
import com.apixio.datacatalog.ExternalIdOuterClass.ExternalId
import com.apixio.datacatalog.PatientProto.CodedBasePatient
import com.apixio.datacatalog.SkinnyPatientProto.BiometricValues
import com.apixio.datacatalog.SummaryObjects.{BiometricValueSummary, ClinicalActorSummary}
import com.apixio.datacatalog._
import com.apixio.nassembly.BaseConsolidator
import com.apixio.nassembly.clinicalactor.ClinicalActorUtils
import com.apixio.nassembly.encounter.EncounterUtils
import com.apixio.nassembly.util.BaseEntityUtil
import com.apixio.util.nassembly.SummaryUtils

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConverters._
import scala.math.Ordering.Implicits.seqDerivedOrdering

object BiometricValueUtils {

  def mergeBiometricValues(values: List[BiometricValueSummary]): Iterable[BiometricValueSummary] = {
    // Just like AllergyMerger, uniqueness is entirety of vital sign
    values.groupBy(a => a.getVitalSign)
      .values
      .map(summaries =>
        summaries.maxBy(allergySummary => allergySummary.getBase.getParsingDetailsList.map(pd => pd
          .getParsingDate)
        ))
  }

  def wrapAsPatient(bioValues: List[BiometricValueSummary]): BiometricValues = {
    val builder = BiometricValues.newBuilder
    if (bioValues.isEmpty) return builder.build
    val clinicalActors = consolidateActors(bioValues)
    val parsingDetails = consolidateParsingDetails(bioValues)
    val sources = consolidateSources(bioValues)
    val encounters = consolidateEncounters(bioValues)
    val patientMeta = consolidatePatientMeta(bioValues)
    val catalogMeta = BaseConsolidator.mergeCatalogMeta(bioValues.map(_.getBase.getDataCatalogMeta).asJava)
      .toBuilder
      .setOriginalId(ExternalId.newBuilder().setAssignAuthority("BiometricValues").build())
      .build()

    val normalizedBiometrics = normalizeBiometrics(bioValues)

    val codedBase: CodedBasePatient = BaseEntityUtil
      .buildCodedBasePatient(patientMeta, catalogMeta, parsingDetails, sources,
        clinicalActors, encounters)

    builder.setBase(codedBase)
    builder.addAllBiometricValues(normalizedBiometrics.asJava)
    builder.build
  }

  def consolidateActors(bioValues: List[BiometricValueSummary]): Iterable[ClinicalActor] = {
    val primaryClinicalActors = bioValues.map(p => p.getBase.getPrimaryActor)
    val supplementaryClinicalActors = bioValues.flatMap(p => p.getBase.getSupplementaryActorsList)
    val allActors = Seq(primaryClinicalActors, supplementaryClinicalActors).flatten.filter(ClinicalActorUtils.notEmpty)
    ClinicalActorUtils.dedupClincalActors(allActors)
  }

  def consolidateEncounters(bioValues: List[BiometricValueSummary]): Iterable[Encounter] = {
    val encounters = bioValues.flatMap(_.getBase.getEncountersList)
    EncounterUtils.mergeEncounters(encounters)
  }

  def consolidateParsingDetails(bioValues: List[BiometricValueSummary]): java.util.List[ParsingDetailsOuterClass.ParsingDetails] = {
    val parsingDetails = bioValues.flatMap(p => p.getBase.getParsingDetailsList)
    BaseConsolidator.dedupParsingDetails(parsingDetails.asJava)
  }

  def consolidateSources(bioValues: List[BiometricValueSummary]): java.util.List[SourceOuterClass.Source] = {
    val sources = bioValues.flatMap(p => p.getBase.getSourcesList)
    BaseConsolidator.dedupSources(sources.asJava)
  }


  def separateActors(values: List[BiometricValueSummary]): Iterable[ClinicalActorSummary] = {
    val actors = consolidateActors(values)
    val parsingDetails = consolidateParsingDetails(values)
    val sources = consolidateSources(values)

    actors.map(a => SummaryUtils.createClinicalActorSummary(a, parsingDetails, sources))
  }

  def separateEncounters(values: List[BiometricValueSummary]): Iterable[SummaryObjects.EncounterSummary] = {
    val parsingDetails = consolidateParsingDetails(values)
    val sources = consolidateSources(values)
    val encounters = consolidateEncounters(values)
    val actors = consolidateActors(values)
    val patientMeta = consolidatePatientMeta(values)
    encounters
      .map(e => SummaryUtils.createEncounterSummary(e, patientMeta, parsingDetails, sources, actors.toList.asJava))
  }

  def consolidatePatientMeta(values: Seq[BiometricValueSummary]): PatientMetaProto.PatientMeta = {
    BaseConsolidator.mergePatientMeta(values.map(_.getBase.getPatientMeta).asJava)
  }


  /**
   *
   * @param biometricValues List of denormalized biometricvalues
   * @return
   */
  def normalizeBiometrics(biometricValues: Seq[BiometricValueSummary]): Seq[BiometricValue] = {
    biometricValues.map(SummaryUtils.normalizeBiometricValue)
  }


}