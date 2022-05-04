package com.apixio.nassembly.legacycoverage

import com.apixio.model.nassembly.Base.{Cid, Oid}
import com.apixio.model.nassembly.SortedFieldOrder
import com.apixio.nassembly.model.FilterDeletesGarbageCollector
import com.apixio.nassembly.util.DataConstants

import scala.collection.JavaConverters._


class LegacyCoverageGC extends FilterDeletesGarbageCollector {

  override def getDataTypeName: String = LegacyCoverageExchange.dataTypeName

  override def getGroupByFieldNames: java.util.Set[String] = {
    Set(Cid, Oid, "coverageInfo.healthPlanName", "coverageInfo.startDate", "coverageInfo.endDate").asJava
  }

  override def getSortedFields: Array[SortedFieldOrder] = {
    val creationDate = new SortedFieldOrder(DataConstants.SOURCE_CREATION_DATE, true)
    val parsingDate = new SortedFieldOrder(DataConstants.PARSING_DETAILS_DATE, true)
    Array(creationDate, parsingDate)
  }

}