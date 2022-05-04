package com.apixio.nassembly.prescription

import com.apixio.model.nassembly.Base.Cid
import com.apixio.model.nassembly.SortedFieldOrder
import com.apixio.nassembly.model.FilterDeletesGarbageCollector
import com.apixio.nassembly.util.DataConstants

import scala.collection.JavaConversions._

class PrescriptionGarbageCollector extends FilterDeletesGarbageCollector {

  val FIELD_NAME_LAB_RESULT = "prescriptionInfo" // value is coming from SummaryObjects.proto
  val SORT_DESCENDING = true

  override def getDataTypeName: String = {
    PrescriptionExchange.dataTypeName
  }

  override def getGroupByFieldNames: java.util.Set[String] = {
    Set(Cid, FIELD_NAME_LAB_RESULT)
  }

  override def getSortedFields: Array[SortedFieldOrder] = {
    val creationDate = new SortedFieldOrder(DataConstants.SOURCE_CREATION_DATE, SORT_DESCENDING)
    val parsingDate = new SortedFieldOrder(DataConstants.PARSING_DETAILS_DATE, SORT_DESCENDING)
    Array(creationDate, parsingDate)
  }

}