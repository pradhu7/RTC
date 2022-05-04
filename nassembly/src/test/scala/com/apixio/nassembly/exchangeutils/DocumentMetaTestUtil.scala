package com.apixio.nassembly.exchangeutils

import com.apixio.dao.patient2.PatientUtility
import com.apixio.datacatalog.{PatientProto, SummaryObjects}
import com.apixio.model.patient.{ClinicalCode, Document, ExternalID, ParserType, ParsingDetail, Source}
import com.apixio.nassembly.documentmeta.{DocMetadataKeys, DocumentMetaUtils}
import com.apixio.nassembly.exchangeutils.MockApoUtil._
import com.apixio.nassembly.exchangeutils.TestVerificationUtil._
import com.apixio.nassembly.patient.SeparatorUtils
import com.apixio.util.nassembly.DataCatalogProtoUtils
import com.google.protobuf.util.JsonFormat
import org.joda.time.DateTime

import java.util.UUID
import scala.collection.JavaConverters._

object DocumentMetaTestUtil {

  val fakeCode2: ClinicalCode = generateClinicalCode
  val otherOriginalId: ExternalID = generateExternalOriginalId
  val sourceDate: DateTime = DateTime.now()
  var srcDocument: Document = null
  val parsingDetail: ParsingDetail = generateParsingDetails(DateTime.now.minusYears(10), ParserType.APO)

  def generateDocumentMeta(patientSource: Source): Document = {
    val document = new Document()
    document.setInternalUUID(UUID.randomUUID())
    document.setDocumentDate(sourceDate)
    document.setDocumentTitle("Test Title")
    document.setStringContent("hello there")
    document.setCode(generateClinicalCode)
    document.setCodeTranslations(List(fakeCode2).asJava)
    document.setOriginalId(generateExternalOriginalId)
    document.setOtherOriginalIds(List(otherOriginalId).asJava)

    document.setMetaTag("CHART_TYPE", "pdf")
    document.setMetaTag("doccacheelem_s3_ext.level", "doc")
    document.setMetaTag("dateAssumed", "true")
    document.setMetaTag("DOCUMENT_SIGNED_DATE", "2012-10-01")
    document.setMetaTag("DEPRECATED_TAG", "DEPRECATED!!")

    document.setSourceId(patientSource.getSourceId)
    document.setParsingDetailsId(parsingDetail.getParsingDetailsId)
    srcDocument = document
    document
  }

  def assertDocumentMeta(proto: PatientProto.Patient,
                         oidKey: String): Unit = {
    val separatedDocsProto = SeparatorUtils.separateDocuments(proto)
    assert(separatedDocsProto.size == 1)
    val protoDoc = separatedDocsProto.head
    val protoDocMeta = protoDoc.getDocumentMeta
    assert(!protoDocMeta.getOcrMetadata.hasTotalPages)
    assert(protoDocMeta.getHealthPlanMeta.getChartType == "pdf")
    assert(protoDocMeta.getContentMeta.getDocCacheLevel == "doc")
    assert(protoDocMeta.getDateAssumed)
    assert(DataCatalogProtoUtils.dateToYYYYMMDD(protoDocMeta.getEncounterMeta.getDocumentSignedDate) == "2012-10-01")

    val patientDoubledConverted = DocumentMetaUtils.toApo(separatedDocsProto.toList)
    val newOidKey = PatientUtility.getDocumentKey(patientDoubledConverted)
    assert(newOidKey == oidKey)

    val doubledConvertDocuments = patientDoubledConverted.getDocuments.asScala.toList
    assert(doubledConvertDocuments.size == 1)
    val doubleConvertedDocument = doubledConvertDocuments.head
    assert(doubleConvertedDocument.getMetaTag(DocMetadataKeys.TOTAL_PAGES.toString) == null)
    assert(doubleConvertedDocument.getMetaTag(DocMetadataKeys.CHART_TYPE.toString) == "pdf")
    assert(doubleConvertedDocument.getMetaTag(DocMetadataKeys.DOC_CACHE_LEVEL.toString) == "doc")
    assert(doubleConvertedDocument.getMetaTag(DocMetadataKeys.DATE_ASSUMED.toString) == "true")
    assert(doubleConvertedDocument.getMetaTag(DocMetadataKeys.DOCUMENT_SIGNED_DATE.toString) == "2012-10-01")
    assert(doubleConvertedDocument.getDocumentDate == sourceDate)
    verifyExternalOriginalId(protoDoc.getBase.getDataCatalogMeta.getOriginalId, doubleConvertedDocument
      .getOriginalId)
    assert(doubleConvertedDocument.getOtherOriginalIds.get(0).equals(otherOriginalId))
  }

  def generateDocumentMetaSummary: SummaryObjects.DocumentMetaSummary = {
    val json = """{ "base": { "dataCatalogMeta": { "lastEditTime": "1631905220801", "editType": "ACTIVE", "pdsId": "1723", "originalId": { "id": "", "assignAuthority": "sample_txt.txt" }, "otherOriginalIds": [] }, "patientMeta": { "patientId": { "uuid": "e93fa08e-3cd9-45fc-ae0e-fbdcff99e596" }, "primaryExternalId": { "id": "sample_txt_1", "assignAuthority": "PAA_ID" }, "externalIds": [{ "id": "sample_txt_1", "assignAuthority": "PAA_ID" }] }, "parsingDetails": [{ "uploadBatchId": "1723_INTEGRATION_AUTOMATION_B64_CCDA_REGRESSION1", "sourceFileArchiveUUID": { "uuid": "031e6e91-60b8-431b-8d4d-8353ca2de17c" }, "sourceFileHash": "789ad1748f89404db24e9bd974061adcfb13f00a", "version": "1.0", "parsingDate": "1631905220741", "parserType": "APX", "internalId": { "uuid": "4ed24720-ddea-373d-9b4f-ebbeca1b3233" } }], "sources": [{ "system": "EHR", "type": "", "organization": { "name": "1723", "alternateIds": [] }, "creationDate": "1583910000000", "parsingDetailsId": { "uuid": "4ed24720-ddea-373d-9b4f-ebbeca1b3233" }, "internalId": { "uuid": "f053e098-fa07-3209-828c-feb1302fb28a" } }], "encounters": [], "supplementaryActors": [] }, "documentMeta": { "uuid": { "uuid": "031e6e91-60b8-431b-8d4d-8353ca2de17c" }, "documentTitle": "Sample TXT", "documentDate": { "year": 2020, "month": 3, "day": 11, "epochMs": "1583910000000" }, "dateAssumed": false, "documentType": "", "codeTranslations": [], "documentContents": [{ "length": "103", "hash": "789ad1748f89404db24e9bd974061adcfb13f00a", "mimeType": "text/plain", "uri": "", "parsingDetailsId": { "uuid": "4ed24720-ddea-373d-9b4f-ebbeca1b3233" }, "sourceId": { "uuid": "f053e098-fa07-3209-828c-feb1302fb28a" } }], "encounterMeta": { "documentFormat": "", "documentSignedStatus": "", "documentAuthor": "", "providerType": "", "providerTitle": "", "providerId": "", "encounterId": "", "encounterName": "", "noteType": "" }, "ocrMetadata": { "status": "", "resolution": "", "childExitCode": 0, "childExitCodeStr": "", "ocrVersion": "444-SNAPSHOT", "stringContentMs": "1631905220852", "textExtractMs": { "value": "1631905220852" } }, "healthPlanMeta": { "chartType": "" } } }"""
    val builder = SummaryObjects.DocumentMetaSummary.newBuilder()
    JsonFormat.parser().ignoringUnknownFields().merge(json, builder)
    builder.build()
  }

  def generateDocumentMetaOCRSummary: SummaryObjects.DocumentMetaSummary = {
    val json = """{ "base": { "dataCatalogMeta": { "lastEditTime": "1631905673398", "editType": "ACTIVE", "pdsId": "1723", "otherOriginalIds": [] }, "patientMeta": { "externalIds": [{ "id": "sample_txt_1", "assignAuthority": "PAA_ID" }] }, "parsingDetails": [], "sources": [], "encounters": [], "supplementaryActors": [] }, "documentMeta": { "uuid": { "uuid": "031e6e91-60b8-431b-8d4d-8353ca2de17c" }, "documentTitle": "", "dateAssumed": false, "documentType": "", "codeTranslations": [], "documentContents": [], "ocrMetadata": { "status": "200", "resolution": "300", "childExitCode": 0, "childExitCodeStr": "", "timestampMs": "1631905673398", "gsVersion": "", "totalPages": {"value": 2}, "availablePages": {"value": 2}, "ocrVersion": "1.0", "stringContentMs": {"value": "1631905673398"}, "docCacheLevel": "", "docCacheFormat": "" } } }"""
    val builder = SummaryObjects.DocumentMetaSummary.newBuilder()
    JsonFormat.parser().ignoringUnknownFields().merge(json, builder)
    builder.build()
  }
}