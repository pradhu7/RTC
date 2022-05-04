package com.apixio.nassembly.problem

import com.apixio.model.nassembly.Base.Cid
import com.apixio.model.nassembly.{SortedFieldOrder, TransformationMeta}
import com.apixio.nassembly.combinerutils.TransformationFactory
import com.apixio.nassembly.model.FilterDeletesGarbageCollector
import com.apixio.nassembly.util.DataConstants

import scala.collection.JavaConversions._

class ProblemGarbageCollector extends FilterDeletesGarbageCollector {

  override def getDataTypeName: String = {
    ProblemExchange.dataTypeName
  }

  override def getGroupByFieldNames: java.util.Set[String] = {
    Set(Cid, "base.dataCatalogMeta.originalId", "problemInfo.code.code", "problemInfo.code.system", "problemInfo.startDate", "problemInfo.endDate")
  }

  override def getSortedFields: Array[SortedFieldOrder] = {
    val creationDate = new SortedFieldOrder(DataConstants.SOURCE_CREATION_DATE, true)
    val parsingDate = new SortedFieldOrder(DataConstants.PARSING_DETAILS_DATE, true)
    Array(creationDate, parsingDate)
  }

  override def getPostProcesses: Array[TransformationMeta] = {
    // Filter out procedures with delete indicator
    val deleteIndicator = TransformationFactory.markAsDelete("problemInfo.deleteIndicator", "true")
    super.getPostProcesses ++ Array(deleteIndicator)
  }

}