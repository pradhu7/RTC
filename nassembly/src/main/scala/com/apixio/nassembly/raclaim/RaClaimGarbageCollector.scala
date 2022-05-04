package com.apixio.nassembly.raclaim

import com.apixio.model.nassembly.Base.Cid
import com.apixio.model.nassembly.{SortedFieldOrder, TransformationMeta}
import com.apixio.nassembly.combinerutils.TransformationFactory
import com.apixio.nassembly.model.FilterDeletesGarbageCollector
import com.apixio.nassembly.util.DataConstants

import scala.collection.JavaConversions._

class RaClaimGarbageCollector extends FilterDeletesGarbageCollector {

  override def getDataTypeName: String = {
    RaClaimExchange.dataTypeName
  }

  override def getGroupByFieldNames: java.util.Set[String] = {
    Set(Cid, "base.dataCatalogMeta.originalId", "raClaim.problemInfo.code.code", "raClaim.problemInfo.code.system", "raClaim.problemInfo.startDate", "raClaim.problemInfo.endDate", "raClaim.providerType")
  }

  override def getSortedFields: Array[SortedFieldOrder] = {
    val transactionDate = new SortedFieldOrder("raClaim.transactionDate", true)
    val creationDate = new SortedFieldOrder(DataConstants.SOURCE_CREATION_DATE, true)
    val parsingDate = new SortedFieldOrder(DataConstants.PARSING_DETAILS_DATE, true)
    Array(transactionDate, creationDate, parsingDate)
  }

  override def getPostProcesses: Array[TransformationMeta] = {
    // Filter out procedures with delete indicator
    val deleteIndicator = TransformationFactory.markAsDelete("raClaim.problemInfo.deleteIndicator", "true")
    super.getPostProcesses ++ Array(deleteIndicator)
  }

}