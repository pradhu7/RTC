package com.apixio.nassembly.mergeutils

import com.apixio.datacatalog.BaseObjects.ClinicalActor
import com.apixio.datacatalog.DataCatalogMetaOuterClass.DataCatalogMeta
import com.apixio.datacatalog.{ParsingDetailsOuterClass, PatientMetaProto, SourceOuterClass}
import com.apixio.datacatalog.SummaryObjects.CodedBaseSummary
import com.apixio.nassembly.BaseConsolidator
import com.apixio.nassembly.clinicalactor.ClinicalActorUtils

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConverters._

object CodedBaseSummaryUtils {

  def merge(codedBases: Seq[CodedBaseSummary]): CodedBaseSummary = {
    val builder = CodedBaseSummary.newBuilder()

    builder.setDataCatalogMeta(consolidateCatalogMeta(codedBases))
    builder.setPatientMeta(consolidatePatientMeta(codedBases))
    builder.addAllSources(consolidateSources(codedBases).asJava)
    builder.addAllParsingDetails(consolidateParsingDetails(codedBases).asJava)

    consolidateActors(codedBases) match {
      case Nil => // do nothing
      case primary :: supplementary =>
        builder.setPrimaryActor(primary)
        builder.addAllSupplementaryActors(supplementary.asJava)
    }

    builder.build()
  }

  def consolidateActors(codedBases: Seq[CodedBaseSummary]): Iterable[ClinicalActor] = {
    val primaryClinicalActors = codedBases.map(_.getPrimaryActor)
    val supplementaryClinicalActors = codedBases.flatMap(_.getSupplementaryActorsList)
    val allActors = Seq(primaryClinicalActors, supplementaryClinicalActors).flatten.filter(ClinicalActorUtils.notEmpty)
    ClinicalActorUtils.dedupClincalActors(allActors)
  }

  def consolidatePatientMeta(codedBases: Seq[CodedBaseSummary]): PatientMetaProto.PatientMeta = {
    BaseConsolidator.mergePatientMeta(codedBases.map(_.getPatientMeta).asJava)
  }

  def consolidateCatalogMeta(codedBases: Seq[CodedBaseSummary]): DataCatalogMeta = {
    BaseConsolidator.mergeCatalogMeta(codedBases.map(_.getDataCatalogMeta).asJava)
  }

  def consolidateSources(codedBases: Seq[CodedBaseSummary]): Seq[SourceOuterClass.Source] = {
    val sources = codedBases.flatMap(_.getSourcesList).asJava
    BaseConsolidator.dedupSources(sources)
  }

  def consolidateParsingDetails(codedBases: Seq[CodedBaseSummary]): Seq[ParsingDetailsOuterClass.ParsingDetails] = {
    val sources = codedBases.flatMap(_.getParsingDetailsList).asJava
    BaseConsolidator.dedupParsingDetails(sources)
  }

}
