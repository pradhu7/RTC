package com.apixio.nassembly.cleantext

import com.apixio.datacatalog.ExtractedTextOuterClass
import com.apixio.datacatalog.SummaryObjects.DocumentMetaSummary
import com.apixio.model.nassembly.Combiner.CombinerInput
import com.apixio.model.nassembly.{Aggregator, Combiner}
import com.apixio.nassembly.documentmeta.{DocumentMetaExchange, DocumentMetaUtils}
import com.apixio.nassembly.extractedtext.{ExtractedTextExchangeBase, ExtractedTextExchangeImpl, ExtractedTextGarbageCollector}
import combinerutils.MergedAggregator

import java.util
import scala.collection.JavaConverters._

class NonOcrCleanTextCombiner extends Combiner[NonOcrCleanTextExchangeImpl] {

  override def getDataTypeName: String = {
    NonOcrCleanTextExchangeImpl.dataTypeName
  }

  override def isQueryable: Boolean = false

  override def fromDataTypeToAggregator: util.Map[String, Aggregator] = {
    val extractedTextAggregator = new MergedAggregator(new ExtractedTextGarbageCollector)
    val docMetaAgg = new DocumentMetaAggregator(requireOcr = false)

    // Note: There is a race condition with Blob Persist. We can't read string content from S 3 like with Ocr
    Map(ExtractedTextExchangeImpl.dataTypeName -> extractedTextAggregator,
      DocumentMetaExchange.dataTypeName -> docMetaAgg)
      .asInstanceOf[Map[String, Aggregator]]
      .asJava
  }

  override def combine(combinerInput: CombinerInput): java.lang.Iterable[NonOcrCleanTextExchangeImpl] = {
    //There is only a single proto
    val textOpt: Option[ExtractedTextOuterClass.ExtractedText] = combinerInput.getDataTypeNameToExchanges.get(ExtractedTextExchangeImpl.dataTypeName) match {
      case null => None
      case exchanges => exchanges.toList.map(e => e.asInstanceOf[ExtractedTextExchangeBase]).headOption.map(_.getContent)
    }

    //There is only a single proto
    val docMetaOpt: Option[DocumentMetaSummary] = combinerInput.getDataTypeNameToExchanges.get(DocumentMetaExchange.dataTypeName) match {
      case null => None
      case exchanges =>
        val documents = exchanges.toList.map(e => e.asInstanceOf[DocumentMetaExchange]).flatMap(_.getDocuments.toList)
        DocumentMetaUtils.merge(documents).headOption
    }

    lazy val emptyResult = Iterable.empty[NonOcrCleanTextExchangeImpl].asJava
    (textOpt, docMetaOpt) match {
      case (Some(text), Some(docMeta)) =>
        CleanTextUtils.createCleanText(docMeta, text.getStringContent.toByteArray).map(cleanTextProto => {
          val exchange = new NonOcrCleanTextExchangeImpl
          exchange.setCleanText(cleanTextProto)
          Iterable(exchange).asJava
        })
          .getOrElse(emptyResult)

      case _ =>
        emptyResult
    }
  }
}