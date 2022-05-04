package com.apixio.scala.utility.alignment

import java.util.UUID

import com.apixio.bizlogic.patient.assembly.PatientAssembly
import com.apixio.model.patient.{Document, Patient}
import com.apixio.model.profiler.Code
import com.apixio.scala.dw.ApxServices
import com.apixio.scala.logging.ApixioLoggable
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.joda.time.{DateTime, DateTimeZone}

import scala.collection.JavaConversions._

@deprecated("Legacy")
case class DocumentMetadata(orgId: String, patientUUID: String, docUUID: String,
                            mimeType: String, title: String, date: DateTime, metadata: Map[String, String])
{

  val encounterDate: Option[DateTime] = metadata.get("ENCOUNTER_DATE")
    .flatMap(d => try {
      Some(DateTime.parse(d))
    } catch {
      case _: Throwable =>
        try {
          Some(DocumentMetadata.oldDateTimeFormatter.parseDateTime(d))
        } catch {
          case _: Throwable => None
        }
    }).map(_.withZoneRetainFields(DateTimeZone.UTC))

  val encounterDateStart: Option[DateTime] = metadata.get("ENCOUNTER_DATE_START")
    .map(DateTime.parse).map(_.withZoneRetainFields(DateTimeZone.UTC))
    .orElse(encounterDate)

  val encounterDateEnd: Option[DateTime] = metadata.get("ENCOUNTER_DATE_END")
    .map(DateTime.parse).map(_.withZoneRetainFields(DateTimeZone.UTC))
    .orElse(encounterDate)
  val providerType: Option[Code] = metadata.get("ENCOUNTER_PROVIDER_TYPE")
    .flatMap(DocumentMetadata.parseProviderTypeCode)

  val totalPages: Int = metadata.getOrElse("totalPages", "1").toInt
}

object DocumentMetadata extends ApixioLoggable {
  setupLog(getClass.getCanonicalName)

  val providerTypes = Set("I", "O", "P", "01", "02", "10", "20")
  val oldDateTimeFormatter: DateTimeFormatter = DateTimeFormat.forPattern("ddMMMYYYY:HH:mm:ss.SSS") // to support old style

  def parseProviderTypeCode(code: String): Option[Code] =
    Option(code).map(_.split("\\^")).filter(_.length == 4)
    .filter(l => l(3) == Code.RAPSPROV || l(3) == Code.FFSPROV)
    .filter(l => providerTypes.contains(l.head))
    .map(l => Code(l.head, l(3)))

  def getMetadata(patUUID: String, docUUID: String, pdsId: String): Option[DocumentMetadata] = {
    try {
      Option(ApxServices.patientLogic.getMergedPatientSummaryForCategory(pdsId, UUID.fromString(patUUID), PatientAssembly.PatientCategory.DOCUMENT_META.getCategory, docUUID))
        .map(_.getDocuments.iterator)
        .filter(_.hasNext)
        .map(_.next)
        .map(doc => DocumentMetadata(pdsId, patUUID, doc))
    } catch {
      case e: Throwable =>
        error(msg = e.getMessage, context = e.getStackTrace.toString)
        None
    }
  }

  def apply(pdsId: String, patUUID: String, doc: Document): DocumentMetadata = {
    val title = doc.getDocumentTitle
    val date = doc.getDocumentDate
    val mimeType = Option(doc.getDocumentContents).filter(_.nonEmpty)
      .map(_.get(0).getMimeType).map(_.toLowerCase).orNull
    DocumentMetadata(pdsId, patUUID, doc.getInternalUUID.toString, mimeType, title, date, doc.getMetadata.toMap)
  }

  def getPatientDocumentsMetadata(patUUID: String, pdsId: String, clinincalBatches: Option[List[String]] = None)
  : List[DocumentMetadata] = {
    val pat: Patient = ApxServices.patientLogic.getMergedPatientSummaryForCategory(pdsId, UUID.fromString(patUUID), PatientAssembly.PatientCategory.DOCUMENT_META.getCategory)
    val pats: List[Patient] = List(pat).filterNot(_ == null)

    getDocumentsMetadataFromApo(pats, patUUID, pdsId, clinincalBatches)
  }

  def getDocumentsMetadataFromApo(pats: List[Patient], patUUID: String, pdsId: String, clinicalBatches: Option[List[String]] = None)
  : List[DocumentMetadata] = {
    pats.filter(_.getDocuments != null)
      .flatMap(pat => {
        pat.getDocuments
          .filter(doc => {
            val parsingDetail = pat.getParsingDetailById(doc.getParsingDetailsId)
            clinicalBatches match {
              case None => true
              case Some(list) => Option(parsingDetail).map(_.getSourceUploadBatch).exists(b => list.contains(b))
            }
          })
          .map(doc => DocumentMetadata(pdsId, patUUID, doc)).toList
      })
  }

}
