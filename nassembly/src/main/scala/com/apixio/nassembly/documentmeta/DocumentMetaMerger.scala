package com.apixio.nassembly.documentmeta

import com.apixio.datacatalog.SummaryObjects.DocumentMetaSummary
import com.apixio.model.nassembly.Base.Cid
import com.apixio.nassembly.mergeutils.MergerUtils
import com.apixio.nassembly.model.FilterDeletesMerger

import java.util
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

class DocumentMetaMerger extends FilterDeletesMerger[DocumentMetaExchange] {

  override def getDataTypeName: String = {
    DocumentMetaExchange.dataTypeName
  }

  override def getGroupIds(): util.Set[String] = {
    Set(Cid, "documentMeta.uuid.uuid")
  }


  override def merge(exchanges: java.util.List[DocumentMetaExchange]): DocumentMetaExchange = {
    def bizLogic(exchanges: java.util.List[DocumentMetaExchange]): DocumentMetaExchange = {
      val protos: List[DocumentMetaSummary] = exchanges.asScala.flatMap(_.getDocuments).toList
      val mergedDocuments = DocumentMetaUtils.merge(protos)
      val exchange = exchanges.get(0)
      exchange.setDocuments(mergedDocuments)
      exchange
    }
    MergerUtils.wrapMergerBizlogic[DocumentMetaExchange](exchanges, bizLogic)
  }
}
