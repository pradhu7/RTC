package com.apixio.nassembly.familyhistory

import com.apixio.model.nassembly.Base.Cid
import com.apixio.model.nassembly.SortedFieldOrder
import com.apixio.nassembly.model.FilterDeletesGarbageCollector
import com.apixio.nassembly.util.DataConstants

import scala.collection.JavaConversions._

class FamilyHistoryGarbageCollector extends FilterDeletesGarbageCollector {

  override def getDataTypeName: String = {
    FamilyHistoryExchange.dataTypeName
  }

  override def getGroupByFieldNames: java.util.Set[String] = {
    Set(Cid, "familyHistoryInfo")
  }

  override def getSortedFields: Array[SortedFieldOrder] = {
    val creationDate = new SortedFieldOrder(DataConstants.SOURCE_CREATION_DATE, true)
    val parsingDate = new SortedFieldOrder(DataConstants.PARSING_DETAILS_DATE, true)
    Array(creationDate, parsingDate)
  }

}