package com.apixio.nassembly.patient

import com.apixio.datacatalog.ExternalIdOuterClass.ExternalId
import com.apixio.datacatalog._
import com.apixio.ensemble.impl.common.{DocCacheElement, SmartXUUID}
import com.apixio.ensemble.service.data.RichPatient
import com.apixio.model.patient._
import com.apixio.nassembly.apo.CodedBaseConverter
import com.apixio.nassembly.documentmeta.DocumentMetaUtils
import com.apixio.protobuf.Doccacheelements
import com.apixio.protobuf.Doccacheelements.DocCacheElementProto
import com.apixio.util.nassembly.SummaryUtils
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.Iterable
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

object ExtractionUtils {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def string2ExternalIdProto(externalId: String): ExternalIdOuterClass.ExternalId = {
    val parts = externalId.split(":")
    val builder = ExternalId.newBuilder()
    builder.setAssignAuthority(parts(0))
    builder.setId(parts(1))
    builder.build()
  }



  //// ------------------- Proto -------------------- ////

  def createDocCacheElementFromProto(documentMeta: SummaryObjects.DocumentMetaSummary, stringContent: String): Iterable[DocCacheElementProto] = {
    val apo = DocumentMetaUtils.toApo(Seq(documentMeta))
    val document = {
      val normalized = SummaryUtils.normalizeDocumentMeta(documentMeta)
      CodedBaseConverter.convertDocument(normalized)
    }

    if (documentMeta.getDocumentMeta.hasOcrMetadata) {
      document.setStringContent(stringContent)
    } else {
      document.setMetaTag("textextracted", stringContent)
    }

    createDocCacheElements(apo, document, documentMeta.getBase.getDataCatalogMeta.getPdsId)
  }

  def createDocCacheElements(patient: Patient, doc: Document, pdsId: String): Iterable[DocCacheElementProto] = {
    // Rich Patient isn't setting externalIds, so we can ourselves [this function is private in richPatient]
    def externalId2String(externalId:ExternalID) = Array(
      Option(externalId.getAssignAuthority),
      Option(externalId.getId)
    ).flatten.mkString(":") match {
      case s:String if s.isEmpty => "Apixio:UNKNOWN_ID"
      case s:String => s
    }

    val richPatient = RichPatient(patient)

    Try {
      val docCacheElementIterable: Iterable[DocCacheElement] = richPatient.generateDocCacheElementsForDoc(doc, pdsId)
      docCacheElementIterable.flatMap(element => {
        convertToDocCacheElementProto(element).map(proto => {
          val externalIds = patient.getExternalIDs.map(externalId2String)
          if (externalIds.nonEmpty) {
            proto.toBuilder
              .clearExternalIds()
              .addAllExternalIds(externalIds.asJava)
              .build
          }
          else {
            proto
          }
        })
      })
    }
  } match {
    case Success(value) =>
      value
    case Failure(ex) =>
      logger.warn("Error while generating cleanText", ex)
      throw ex
  }

  /**
   * Rewrite because i'm presently using the wrong class definition of doccache element proto
   * @param element DocCacheElement
   * @return
   */
  def convertToDocCacheElementProto(element: DocCacheElement):Option[Doccacheelements.DocCacheElementProto] =
    Option(element) match {
      case Some(elem) if Option(elem.getPatientId).nonEmpty && Option(elem.getDocumentId).nonEmpty =>
        val builder = DocCacheElementProto.newBuilder()
        builder.setPatientUUID(SmartXUUID(elem.getPatientId).uuid.toString)
        builder.setDocumentUUID(SmartXUUID(elem.getDocumentId).uuid.toString)
          .setOriginalId(elem.getOriginalId)
          .setEncId(elem.getEncounterId)
          .setDocumentDate(elem.getDocumentDate.toDate.getTime)
          .setSourceId(elem.getSourceId)
          .setTitle(elem.getTitle)
          .setPageNumber(elem.getPageNumber)
          .setExtractionType(elem.getExtractionType)
          .setContent(elem.getContent)
          .setOrg(elem.getOrganization)
        Option(elem.getExternalIds) match {
          case Some(l) if l.nonEmpty => builder.addAllExternalIds(l)
          case _ => // Do nothing.
        }
        Some(builder.build())
      case None => None
    }


}
