package com.apixio.nassembly.documentmeta

import com.apixio.datacatalog.BaseObjects.ClinicalActor
import com.apixio.datacatalog.CodedBaseObjects.{DocumentMetaCBO, Encounter}
import com.apixio.datacatalog.DocumentMetaOuterClass.{ContentMetadata, OcrMetadata}
import com.apixio.datacatalog.SkinnyPatientProto.PatientDocuments
import com.apixio.datacatalog.SummaryObjects.{ClinicalActorSummary, CodedBaseSummary, DocumentMetaSummary}
import com.apixio.datacatalog._
import com.apixio.model.patient.Patient
import com.apixio.nassembly.BaseConsolidator
import com.apixio.nassembly.apo.APOGenerator
import com.apixio.nassembly.clinicalactor.ClinicalActorUtils
import com.apixio.nassembly.encounter.EncounterUtils
import com.apixio.nassembly.mergeutils.CodedBaseSummaryUtils
import com.apixio.util.nassembly.{DataCatalogProtoUtils, SummaryUtils}

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConverters._

object DocumentMetaUtils {

  def wrapAsPatient(documentMetas: Seq[DocumentMetaSummary]): PatientDocuments = {
    val builder = PatientDocuments.newBuilder
    if (documentMetas.isEmpty) return builder.build


    val mergedCodedBase = CodedBaseSummaryUtils.merge(documentMetas.map(_.getBase))
    val patientBase = DataCatalogProtoUtils.convertCodedBaseSummary(mergedCodedBase)
    val documents = normalizeAndConsolidateDocumentMeta(documentMetas)

    builder
      .setBase(patientBase)
      .addAllDocuments(documents.asJava)
      .build()
  }

  /**
   * convert list of documentMeta summaries to a java Patient
   * @param documentMetas list of document meta summaries
   * @return null if list is empty
   */
  def toApo(documentMetas: Seq[DocumentMetaSummary]): Patient = {
    if (documentMetas.isEmpty) {
      null
    } else {
      val skinnyPatientProto = wrapAsPatient(documentMetas)
      // Just documents and codedBase
      val patientProto = PatientProto.Patient.newBuilder()
        .setBase(skinnyPatientProto.getBase)
        .addAllDocuments(skinnyPatientProto.getDocumentsList)
        .build()

      APOGenerator.fromProto(patientProto)
    }
  }

  def merge(documentSummaries: Seq[DocumentMetaSummary]): Seq[DocumentMetaSummary] = {
    /**
     * We merge in documents with different oids but same uuid if we load document metadata separately
     * @param docUUID doc uuid
     * @param codedBaseSummary coded base summary
     * @return codedBaseSummary with doc uuid as oid
     */
    def setOid(docUUID: String, codedBaseSummary: CodedBaseSummary): CodedBaseSummary = {
      val catalogMeta = codedBaseSummary.getDataCatalogMeta.toBuilder
        .setOid(docUUID)
        .build()

      codedBaseSummary
        .toBuilder
        .setDataCatalogMeta(catalogMeta)
        .build()
    }

    documentSummaries.filter(_.getDocumentMeta.hasUuid).groupBy(_.getDocumentMeta.getUuid.getUuid).map {
      case (docUuid, dups) =>
        val sorted = dups.sortBy(d =>
          d.getBase.getParsingDetailsList.map(_.getParsingDate).toList match {
          case Nil => -d.getBase.getDataCatalogMeta.getLastEditTime // default is 0
          case dates => -dates.max
        })

        val builder = sorted.head.toBuilder

        val mergedCodedBase = CodedBaseSummaryUtils.merge(sorted.map(_.getBase))
        builder.setBase(setOid(docUuid, mergedCodedBase))

        val sortedDocumentMetas = sorted.map(_.getDocumentMeta)
        val contentBuilder = builder.getDocumentMeta.toBuilder

        setTopLevelFields(contentBuilder, sortedDocumentMetas)
        setDocumentContents(contentBuilder, sortedDocumentMetas)
        setMetadatas(contentBuilder, sortedDocumentMetas)

        builder.build()
        builder.setDocumentMeta(contentBuilder.build())
        builder.build()
    }.toSeq
  }

  private def setTopLevelFields(contentBuilder: DocumentMetaOuterClass.DocumentMeta.Builder, sortedDocumentMetas: Seq[DocumentMetaOuterClass.DocumentMeta]) = {
    sortedDocumentMetas.find(_.getDocumentTitle.nonEmpty).map(_.getDocumentTitle)
      .foreach(contentBuilder.setDocumentTitle)

    sortedDocumentMetas.find(_.getDocumentType.nonEmpty).map(_.getDocumentType)
      .foreach(contentBuilder.setDocumentType)

    sortedDocumentMetas.find(_.hasDocumentDate).map(_.getDocumentDate)
      .foreach(contentBuilder.setDocumentDate)

    sortedDocumentMetas.find(_.hasCode).map(_.getCode)
      .foreach(contentBuilder.setCode)

    val codeTranslations = BaseConsolidator.mergeCodes(sortedDocumentMetas.flatMap(_.getCodeTranslationsList).asJava)
    contentBuilder.addAllCodeTranslations(codeTranslations)
  }

  private def setDocumentContents(contentBuilder: DocumentMetaOuterClass.DocumentMeta.Builder, sortedDocumentMetas: Seq[DocumentMetaOuterClass.DocumentMeta]) = {
    contentBuilder.clearDocumentContents()
    val documentContents = mergeContents(sortedDocumentMetas.flatMap(_.getDocumentContentsList))
    contentBuilder.addAllDocumentContents(documentContents.asJava)
  }

  private def setMetadatas(builder: DocumentMetaOuterClass.DocumentMeta.Builder, sortedDocumentMetas: Seq[DocumentMetaOuterClass.DocumentMeta]): Unit = {
    mergeOcrMetadata(sortedDocumentMetas.filter(_.hasOcrMetadata).map(_.getOcrMetadata))
      .filter(_.getSerializedSize > 0)
      .foreach(builder.setOcrMetadata)

    mergeContentMetadata(sortedDocumentMetas.filter(_.hasContentMeta).map(_.getContentMeta))
      .filter(_.getSerializedSize > 0)
      .foreach(builder.setContentMeta)

    sortedDocumentMetas.find(_.hasEncounterMeta).map(_.getEncounterMeta)
      .filter(_.getSerializedSize > 0)
      .foreach(builder.setEncounterMeta)

    sortedDocumentMetas.find(_.hasHealthPlanMeta).map(_.getHealthPlanMeta)
      .filter(_.getSerializedSize > 0)
      .foreach(builder.setHealthPlanMeta)
  }

  private def mergeOcrMetadata(ocrMetadatas: Seq[OcrMetadata]) : Option[OcrMetadata] = {
    if (ocrMetadatas.isEmpty) {
      None
    } else {
      val merged: OcrMetadata = ocrMetadatas.sortBy(_.getTimestampMs).reverse.reduceLeft {
         (acc, each) =>
          val builder = acc.toBuilder
          // check for default values
          if (acc.getStatus.isEmpty) builder.setStatus(each.getStatus)
          if (acc.getResolution.isEmpty) builder.setResolution(each.getResolution)
          if (acc.getTimestampMs == 0) builder.setTimestampMs(each.getTimestampMs)
          if (acc.getGsVersion.isEmpty) builder.setGsVersion(each.getGsVersion)
          if (acc.hasTotalPages) builder.setTotalPages(each.getTotalPages)
          if (acc.hasAvailablePages) builder.setAvailablePages(each.getAvailablePages)
          if (acc.getOcrVersion.isEmpty) builder.setOcrVersion(each.getOcrVersion)
          builder.build()
      }
      Some(merged)
    }
  }

  private def mergeContentMetadata(contentMetadatas: Seq[ContentMetadata]): Option[ContentMetadata] = {
    if (contentMetadatas.isEmpty) {
      None
    } else {
      val merged: ContentMetadata = contentMetadatas.reduceLeft {
        (acc, each) =>
          val builder = acc.toBuilder
          // check for default values
          if (acc.hasStringContentMs) builder.setStringContentMs(each.getStringContentMs)
          if (acc.hasTextExtractMs) builder.setTextExtractMs(each.getTextExtractMs)

          if (acc.hasDocCacheTsMs) builder.setDocCacheTsMs(each.getDocCacheTsMs)
          if (acc.getDocCacheLevel.isEmpty) builder.setDocCacheLevel(each.getDocCacheLevel)
          if (acc.getDocCacheFormat.isEmpty) builder.setDocCacheFormat(each.getDocCacheFormat)
          builder.build()
      }
      Some(merged)
    }

  }

  private def mergeContents(contentMetas: Seq[DocumentContentOuterClass.DocumentContent]): Seq[DocumentContentOuterClass.DocumentContent] = {
    contentMetas
      .groupBy(c => (c.getLength, c.getHash, c.getUri, c.getMimeType)) // group by "identity"
      .mapValues(_.head)  //take the first
      .values // take the values
      .toList
  }

  def normalizeAndConsolidateDocumentMeta(documentMetas: Seq[DocumentMetaSummary]): Seq[DocumentMetaCBO] = {
    documentMetas.map(SummaryUtils.normalizeDocumentMeta)
  }

  def getPatientId(documentMetas: Seq[DocumentMetaSummary]): UUIDOuterClass.UUID = {
    if (documentMetas.isEmpty) return null

    val patientIds = documentMetas.map(_.getBase.getPatientMeta.getPatientId)
    if (patientIds.size > 1) throw new Exception("These don't belong to the same patient")
    patientIds.iterator.next
  }

  def consolidateActors(documentMetaSummaries: Seq[DocumentMetaSummary]): Iterable[ClinicalActor] = {
    val primaryClinicalActors = documentMetaSummaries.map(_.getBase.getPrimaryActor)
    val supplementaryClinicalActors = documentMetaSummaries.flatMap(_.getBase.getSupplementaryActorsList)
    val allActors = Seq(primaryClinicalActors, supplementaryClinicalActors).flatten.filter(ClinicalActorUtils.notEmpty)
    ClinicalActorUtils.dedupClincalActors(allActors)
  }

  def separateEncounters(summaries: Seq[DocumentMetaSummary]): Iterable[SummaryObjects.EncounterSummary] = {
    val parsingDetails = consolidateParsingDetails(summaries)
    val sources = consolidateSources(summaries)
    val encounters = consolidateEncounters(summaries)
    val actors = consolidateActors(summaries)
    val patientMeta = consolidatePatientMeta(summaries)
    encounters
      .map(e => SummaryUtils.createEncounterSummary(e, patientMeta, parsingDetails, sources, actors.toList.asJava))
  }

  def consolidateParsingDetails(summaries: Seq[DocumentMetaSummary]): java.util.List[ParsingDetailsOuterClass.ParsingDetails] = {
    val parsingDetailsList = summaries.flatMap(_.getBase.getParsingDetailsList).asJava
    BaseConsolidator.dedupParsingDetails(parsingDetailsList)
  }

  def consolidateSources(summaries: Seq[DocumentMetaSummary]): java.util.List[SourceOuterClass.Source] = {
    val sources = summaries.flatMap(_.getBase.getSourcesList).asJava
    BaseConsolidator.dedupSources(sources)
  }

  def consolidateEncounters(summaries: Seq[DocumentMetaSummary]): Iterable[Encounter] = {
    val encountersList = summaries.flatMap(_.getBase.getEncountersList)
    EncounterUtils.mergeEncounters(encountersList.toList)
  }

  def consolidatePatientMeta(summaries: Seq[DocumentMetaSummary]): PatientMetaProto.PatientMeta = {
    BaseConsolidator.mergePatientMeta(summaries.map(_.getBase.getPatientMeta).asJava)
  }
}
