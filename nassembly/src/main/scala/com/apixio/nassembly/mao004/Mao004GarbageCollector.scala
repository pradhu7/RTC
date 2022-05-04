package com.apixio.nassembly.mao004

import com.apixio.model.nassembly.Base.Cid
import com.apixio.model.nassembly.SortedFieldOrder
import com.apixio.model.nassembly.{GarbageCollector, TransformationMeta}
import com.apixio.nassembly.combinerutils.TransformationFactory
import com.apixio.nassembly.model.FilterDeletesGarbageCollector
import com.apixio.nassembly.util.DataConstants

import scala.collection.JavaConversions._

class Mao004GarbageCollector extends FilterDeletesGarbageCollector {

  override def getDataTypeName: String = {
    Mao004Exchange.dataTypeName
  }

  override def getGroupByFieldNames: java.util.Set[String] = {
    Set(Cid, "base.dataCatalogMeta.originalId", "mao004.problemInfo.code.code", "mao004.problemInfo.code.system", "mao004.problemInfo.startDate", "mao004.problemInfo.endDate", "mao004.encounterClaimType")
  }

  override def getSortedFields: Array[SortedFieldOrder] = {
    val transactionDate = new SortedFieldOrder("mao004.reportDate", true)
    val creationDate = new SortedFieldOrder(DataConstants.SOURCE_CREATION_DATE, true)
    val parsingDate = new SortedFieldOrder(DataConstants.PARSING_DETAILS_DATE, true)
    Array(transactionDate, creationDate, parsingDate)
  }

  override def getPostProcesses: Array[TransformationMeta] = {
    // Filter out procedures with delete indicator
    val deleteIndicator = TransformationFactory.markAsDelete("mao004.problemInfo.deleteIndicator", "true")
    super.getPostProcesses ++ Array(deleteIndicator)
  }

}