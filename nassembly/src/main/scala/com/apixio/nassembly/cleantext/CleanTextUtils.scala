package com.apixio.nassembly.cleantext

import com.apixio.dao.utility.DocCacheElemUtility
import com.apixio.datacatalog.CleanTextOuterClass.CleanText
import com.apixio.datacatalog.SummaryObjects.DocumentMetaSummary
import com.apixio.model.blob.BlobType
import com.apixio.nassembly.patient.ExtractionUtils
import com.apixio.protobuf.Doccacheelements.DocCacheElementListProto
import com.google.protobuf.ByteString
import org.slf4j.{Logger, LoggerFactory}

import java.nio.charset.StandardCharsets
import java.util.UUID
import scala.collection.JavaConverters._

object CleanTextUtils {

  private val logger: Logger = LoggerFactory.getLogger(getClass)

  /**
   * Create a CleanText Proto if the documentMeta summary has the real parsed data and the extracted text is not empty
   * @param documentMetaSummary merged document meta
   * @param extractedText extracted text
   * @return
   */
  def createCleanText(documentMetaSummary: DocumentMetaSummary, extractedText: Array[Byte]): Option[CleanText] = {
    if (documentMetaSummary.getBase.getParsingDetailsCount > 0 && !extractedText.isEmpty) {
      // This means the documentMeta is real docMeta and not just ocr metadata
      val docCacheElements = ExtractionUtils.createDocCacheElementFromProto(
        documentMetaSummary,
        new String(extractedText, StandardCharsets.UTF_8))

      val docCacheElementListBytes = DocCacheElementListProto.newBuilder()
        .addAllData(docCacheElements.asJava)
        .build()
        .toByteArray

      val builder = CleanText.newBuilder()
      builder.setDocumentId(documentMetaSummary.getDocumentMeta.getUuid)
      builder.setPatientMeta(documentMetaSummary.getBase.getPatientMeta)
      builder.setDocCacheElementListProto(ByteString.copyFrom(docCacheElementListBytes))

      Some(builder.build())
    } else {
      val docId = documentMetaSummary.getDocumentMeta.getUuid.getUuid
      logger.warn(s"Failed to create Clean Text, docId:$docId, isEmpty:${extractedText.isEmpty}")
      None
    }

  }

  // For persisting to S3
  def getBlobType(cleanText: CleanText): BlobType = {
    val docUUID = UUID.fromString(cleanText.getDocumentId.getUuid)
    DocCacheElemUtility.getBlobTypeForCleanTextProtobufBase64(docUUID)
  }

  def getBlobId(cleanText: CleanText): String = {
    getBlobType(cleanText).getID
  }

}
