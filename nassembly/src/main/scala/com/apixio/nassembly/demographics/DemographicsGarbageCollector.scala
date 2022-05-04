package com.apixio.nassembly.demographics

import com.apixio.model.nassembly.Base.Cid
import com.apixio.model.nassembly.SortedFieldOrder
import com.apixio.nassembly.model.FilterDeletesGarbageCollector
import com.apixio.nassembly.util.DataConstants

import scala.collection.JavaConverters._

class DemographicsGarbageCollector extends FilterDeletesGarbageCollector {

  override def getDataTypeName: String = DemographicsExchange.dataTypeName

  override def getGroupByFieldNames: java.util.Set[String] = {
    Set(Cid, "base.patientMeta.externalIds", "demographicsInfo").asJava
  }

  override def getSortedFields: Array[SortedFieldOrder] = {
    val creationDate = new SortedFieldOrder(DataConstants.SOURCE_CREATION_DATE, true)
	  val parsingDate = new SortedFieldOrder(DataConstants.PARSING_DETAILS_DATE, true)
    Array(creationDate, parsingDate)
  }
}
