package com.apixio.nassembly.cleantext

import com.apixio.dao.utility.PageUtility
import com.apixio.datacatalog.SummaryObjects.DocumentMetaSummary
import com.apixio.model.nassembly.Combiner.CombinerInput
import com.apixio.model.nassembly._
import com.apixio.nassembly.documentmeta.DocumentMetaExchange

import java.util
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

class OcrCleanTextCombiner extends Combiner[OcrCleanTextExchangeImpl] {

  override def getDataTypeName: String = {
    OcrCleanTextExchangeImpl.dataTypeName
  }

  // This data will not be queryable in delta lake!
  override def isQueryable: Boolean = false


  override def fromDataTypeToAggregator: util.Map[String, Aggregator] = {
    Map(DocumentMetaExchange.dataTypeName -> new DocumentMetaAggregator(requireOcr = true))
  }

  override def getAccessors: util.List[Accessor[_ <: Any]] = {
    List(S3StringContentAccessor)
  }

  override def combine(combinerInput: CombinerInput): java.lang.Iterable[OcrCleanTextExchangeImpl] = {
    //There is only a single proto
    val textOpt: Option[Array[Byte]] = combinerInput.getPassThruData.get(PageUtility.STRING_CONTENT) match {
      case null => Option.empty[Array[Byte]]
      case bytes => Some(bytes.asInstanceOf[Array[Byte]])
    }

    //There is only a single proto
    val docMetaOpt: Option[DocumentMetaSummary] = combinerInput.getDataTypeNameToExchanges.get(DocumentMetaExchange.dataTypeName) match {
      case null => None
      case exchanges => exchanges.toList.map(e => e.asInstanceOf[DocumentMetaExchange]).headOption.map(_.getDocuments.toList).flatMap(_.headOption)
    }

    lazy val emptyResult = Iterable.empty[OcrCleanTextExchangeImpl].asJava
    (textOpt, docMetaOpt) match {
      case (Some(text), Some(docMeta)) =>
        CleanTextUtils.createCleanText(docMeta, text).map(cleanTextProto => {
          val exchange = new OcrCleanTextExchangeImpl
          exchange.setCleanText(cleanTextProto)
          Iterable(exchange).asJava
        })
          .getOrElse(emptyResult)

      case _ =>
        emptyResult
    }
  }

}