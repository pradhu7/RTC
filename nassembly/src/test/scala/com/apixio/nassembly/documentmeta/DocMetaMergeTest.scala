package com.apixio.nassembly.documentmeta

import com.apixio.nassembly.exchangeutils.DocumentMetaTestUtil
import org.junit.runner.RunWith
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should._
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class DocMetaMergeTest extends AnyFlatSpec with Matchers {


  "Document Meta Merge" should "Merge ocr metadata with real metadata" in {
    val og = DocumentMetaTestUtil.generateDocumentMetaSummary
    val ocr = DocumentMetaTestUtil.generateDocumentMetaOCRSummary
    val merged = DocumentMetaUtils.merge(Seq(og, ocr))
    assert(merged.size == 1)
    val head = merged.head
    val docMeta = head.getDocumentMeta

    assert(docMeta.hasDocumentDate)
    assert(docMeta.getDocumentTitle.nonEmpty)

    assert(docMeta.hasOcrMetadata)
    val ocrMetadata = docMeta.getOcrMetadata

    assert(ocrMetadata.getResolution.nonEmpty)
    assert(ocrMetadata.getTimestampMs > 0)
  }

}