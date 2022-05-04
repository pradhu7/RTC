package com.apixio.nassembly.socialhistory

import com.apixio.datacatalog.ExternalIdOuterClass.ExternalId
import com.apixio.datacatalog.PatientProto.CodedBasePatient
import com.apixio.datacatalog.SkinnyPatientProto.SocialHistories
import com.apixio.datacatalog.SummaryObjects.SocialHistorySummary
import com.apixio.datacatalog._
import com.apixio.nassembly.BaseConsolidator
import com.apixio.nassembly.clinicalactor.ClinicalActorUtils
import com.apixio.nassembly.encounter.EncounterUtils
import com.apixio.nassembly.util.BaseEntityUtil
import com.apixio.util.nassembly.SummaryUtils

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConverters._

object SocialHistoryUtils {

  // Normalize data and wrap as a patient proto
  def wrapAsPatient(typeCode: String, socialHistories: Seq[SocialHistorySummary]): SocialHistories = {
    val builder = SocialHistories.newBuilder
    if (socialHistories.isEmpty) return builder.build

    val parsingDetails = consolidateParsingDetails(socialHistories)
    val sources = consolidateSources(socialHistories)
    val actors = consolidateActors(socialHistories)
    val encounters = consolidateEncounters(socialHistories)

    val patientMeta = BaseConsolidator.mergePatientMeta(socialHistories.map(_.getBase.getPatientMeta)
      .asJava)
    val catalogMeta = BaseConsolidator.mergeCatalogMeta(socialHistories.map(_.getBase.getDataCatalogMeta)
      .asJava)
      .toBuilder
      .setOriginalId(ExternalId.newBuilder().setId(typeCode).setAssignAuthority(s"SocialHistory_${TypeCodeAggregator.PersistedTypeCodeColName}").build())
      .build()


    val normalizedSocialHistories = socialHistories.map(SummaryUtils.normalizeSocialHistory)

    val codedBase: CodedBasePatient = BaseEntityUtil
      .buildCodedBasePatient(patientMeta, catalogMeta, parsingDetails, sources,
        actors, encounters)

    builder.addAllSocialHistories(normalizedSocialHistories.asJava)
    builder.setBase(codedBase)
    builder.build
  }

  def consolidateParsingDetails(socialHistories: Seq[SocialHistorySummary]): java.util.List[ParsingDetailsOuterClass.ParsingDetails] = {
    val parsingDetails = socialHistories.flatMap(p => p.getBase.getParsingDetailsList)
    BaseConsolidator.dedupParsingDetails(parsingDetails.asJava)
  }

  def consolidateSources(socialHistories: Seq[SocialHistorySummary]): java.util.List[SourceOuterClass.Source] = {
    val sources = socialHistories.flatMap(p => p.getBase.getSourcesList)
    BaseConsolidator.dedupSources(sources.asJava)
  }

  def consolidateEncounters(socialHistories: Seq[SocialHistorySummary]): List[CodedBaseObjects.Encounter] = {
    val encounters = socialHistories.flatMap(p => p.getBase.getEncountersList)
    EncounterUtils.mergeEncounters(encounters.toList).toList
  }

  def consolidateActors(socialHistories: Seq[SocialHistorySummary]): List[BaseObjects.ClinicalActor] = {
    val primaryActors = socialHistories.map(p => p.getBase.getPrimaryActor)
    val supplementaryActors = socialHistories.flatMap(sh => sh.getBase.getSupplementaryActorsList)
    ClinicalActorUtils.mergeClinicalActors((primaryActors ++ supplementaryActors).toList).toList
  }

  def separateActors(socialHistories: Seq[SocialHistorySummary]): List[SummaryObjects.ClinicalActorSummary] = {
    val parsingDetails = consolidateParsingDetails(socialHistories)
    val sources = consolidateSources(socialHistories)
    val actors = consolidateActors(socialHistories)
    actors.map(a => SummaryUtils.createClinicalActorSummary(a, parsingDetails, sources))
  }

  def separateEncounters(socialHistories: Seq[SocialHistorySummary]): List[SummaryObjects.EncounterSummary] = {
    val parsingDetails = consolidateParsingDetails(socialHistories)
    val sources = consolidateSources(socialHistories)
    val actors = consolidateActors(socialHistories)
    val encounters = consolidateEncounters(socialHistories)
    val patientMeta = consolidatePatientMeta(socialHistories)
    encounters.map(e => SummaryUtils.createEncounterSummary(e, patientMeta, parsingDetails, sources, actors.asJava))
  }

  def consolidatePatientMeta(socialHistories: Seq[SocialHistorySummary]): PatientMetaProto.PatientMeta = {
    BaseConsolidator.mergePatientMeta(socialHistories.map(_.getBase.getPatientMeta).asJava)
  }
}
