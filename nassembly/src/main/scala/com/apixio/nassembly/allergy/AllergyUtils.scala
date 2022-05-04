package com.apixio.nassembly.allergy

import com.apixio.datacatalog.BaseObjects.ClinicalActor
import com.apixio.datacatalog.CodedBaseObjects.Allergy
import com.apixio.datacatalog.ExternalIdOuterClass.ExternalId
import com.apixio.datacatalog.PatientProto.CodedBasePatient
import com.apixio.datacatalog.SkinnyPatientProto.Allergies
import com.apixio.datacatalog.SummaryObjects.{AllergySummary, ClinicalActorSummary}
import com.apixio.nassembly.BaseConsolidator
import com.apixio.nassembly.clinicalactor.ClinicalActorUtils
import com.apixio.nassembly.encounter.EncounterUtils
import com.apixio.nassembly.util.BaseEntityUtil
import com.apixio.util.nassembly.SummaryUtils

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConverters._
import scala.math.Ordering.Implicits.seqDerivedOrdering

object AllergyUtils {

  def mergeAllergies(allergies: List[AllergySummary]): Iterable[AllergySummary] = {
    allergies.groupBy(a => a.getAllergyInfo)
      .values
      .map(summaries =>
        summaries.maxBy(allergySummary => allergySummary.getBase.getParsingDetailsList.map(pd => pd
          .getParsingDate)
        ))
  }

  private def getClinicalActorSummary(summary: AllergySummary,
                                      actor: ClinicalActor) = {
    SummaryUtils
      .createClinicalActorSummary(actor, summary.getBase.getParsingDetailsList,
        summary.getBase.getSourcesList)
  }

  private def getPrimaryClinicalActorSummary(a: AllergySummary) = {
    getClinicalActorSummary(a, a.getBase.getPrimaryActor)
  }

  def wrapAsPatient(allergies: List[AllergySummary]): Allergies = {
    val builder = Allergies.newBuilder
    if (allergies.isEmpty) return builder.build
    val clinicalActors = consolidateActors(allergies)
    val parsingDetails = BaseConsolidator.dedupParsingDetails(allergies.flatMap(_.getBase
      .getParsingDetailsList).asJava)
    val sources = BaseConsolidator.dedupSources(allergies.flatMap(_.getBase.getSourcesList).asJava)
    val encounters = EncounterUtils.mergeEncounters(allergies.flatMap(_.getBase.getEncountersList))
    val patientMeta = BaseConsolidator.mergePatientMeta(allergies.map(_.getBase.getPatientMeta).asJava)
    val catalogMeta = BaseConsolidator.mergeCatalogMeta(allergies.map(_.getBase.getDataCatalogMeta).asJava)
      .toBuilder
      .setOriginalId(ExternalId.newBuilder().setAssignAuthority("Allergies").build())
      .build()


    val normalizedAllergies = normalizeAllergies(allergies)

    builder.addAllAllergies(normalizedAllergies.asJava)

    val codedBase: CodedBasePatient = BaseEntityUtil
      .buildCodedBasePatient(patientMeta, catalogMeta, parsingDetails, sources,
        clinicalActors, encounters)

    builder.setBase(codedBase)
    builder.build
  }

  def consolidateActors(allergies: List[AllergySummary]): Iterable[ClinicalActor] = {
    val primaryClinicalActors = allergies.map(p => p.getBase.getPrimaryActor)
    val supplementaryClinicalActors = allergies.flatMap(p => p.getBase.getSupplementaryActorsList)
    val allActors = Seq(primaryClinicalActors, supplementaryClinicalActors).flatten.filter(ClinicalActorUtils.notEmpty)
    ClinicalActorUtils.dedupClincalActors(allActors)
  }

  /**
   * We need to normalize them
   *
   * @param allergies List of denormalized allergies
   * @return
   */
  def normalizeAllergies(allergies: Seq[AllergySummary]): Seq[Allergy] = {
    allergies.map(SummaryUtils.normalizeAllergy)
  }


}