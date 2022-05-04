package com.apixio.nassembly.documentmeta

import scala.collection.SortedSet

object DocMetadataKeys extends Enumeration {

  // Top level
  val DATE_ASSUMED = Value("dateAssumed")
  val DOCUMENT_TYPE = Value("DOCUMENT_TYPE")

  // Encounter Metadata
  val DOCUMENT_FORMAT = Value("DOCUMENT_FORMAT")
  val DOCUMENT_SIGNED_STATUS = Value("DOCUMENT_SIGNED_STATUS")
  val DOCUMENT_AUTHOR = Value("DOCUMENT_AUTHOR")
  val DOCUMENT_UPDATE = Value("DOCUMENT_UPDATE")
  val ENCOUNTER_DATE = Value("ENCOUNTER_DATE")
  val PROVIDER_TYPE = Value("PROVIDER_TYPE")
  val PROVIDER_TITLE = Value("PROVIDER_TITLE")
  val PROVIDER_ID = Value("PROVIDER_ID")
  val DOCUMENT_SIGNED_DATE = Value("DOCUMENT_SIGNED_DATE")

  val ENCOUNTER_PROVIDER_TYPE = Value("ENCOUNTER_PROVIDER_TYPE")
  val ENCOUNTER_DATE_START = Value("ENCOUNTER_DATE_START")
  val ENCOUNTER_DATE_END = Value("ENCOUNTER_DATE_END")

  val LST_FILED_INST_DTTM = Value("LST_FILED_INST_DTTM")
  val ENT_INST_LOCAL_DTTM = Value("ENT_INST_LOCAL_DTTM")
  val UPD_AUT_LOCAL_DTTM = Value("UPD_AUT_LOCAL_DTTM")
  val NOT_FILETM_LOC_DTTM = Value("NOT_FILETM_LOC_DTTM")
  val DOCUMENT_UPDATE_DATE = Value("DOCUMENT_UPDATE_DATE")
  val ENTRY_INSTANT_DTTM = Value("ENTRY_INSTANT_DTTM")
  val UPD_AUTHOR_INS_DTTM = Value("UPD_AUTHOR_INS_DTTM")
  val NOTE_FILE_TIME_DTTM = Value("NOTE_FILE_TIME_DTTM")
  val ENCOUNTER_ID = Value("ENCOUNTER_ID")
  val ENCOUNTER_NAME = Value("ENCOUNTER_NAME")
  val NOTE_TYPE = Value("NOTE_TYPE")


  // OCR Metadata
  val OCR_STATUS = Value("ocr.status")
  val OCR_RESOLUTION = Value("ocrResolution")
  val OCR_CHILD_EXIT_CODE = Value("ocr.child.exitcode")
  val OCR_CHILD_EXIT_CODE_STR = Value("ocr.child.exitcode_str")
  val OCR_TIMSTAMP = Value("ocr.ts")
  val GS_VERSION = Value("gsVersion")
  val TOTAL_PAGES = Value("totalPages")
  val AVAILABLE_PAGES = Value("availPages")
  val PIPELINE_VERSION = Value("pipeline.version")

  // HEALTH PLAN METADATA
  val ALTERNATE_CHART_ID = Value("ALTERNATE_CHART_ID")
  val CHART_TYPE = Value("CHART_TYPE")


  // Content Extract Meta
  val DOC_CACHE_LEVEL = Value("doccacheelem_s3_ext.level")
  val DOC_CACHE_FORMAT = Value("doccacheelem_s3_ext.format")
  val DOC_CACHE_TS = Value("doccacheelem_s3_ext.ts")
  val STRING_CONTENT_TS = Value("s3_ext.ts.string_content")
  val LEVEL = Value("s3_ext.level")
  val TEXT_EXTRACTED = Value("textextracted")
  val TEXT_EXTRACTED_TS = Value("s3_ext.ts.text_extracted")
}

object DocMetadataHelper {
  val typedMetadataKeys: SortedSet[String] = DocMetadataKeys.values.map(_.toString)
}
