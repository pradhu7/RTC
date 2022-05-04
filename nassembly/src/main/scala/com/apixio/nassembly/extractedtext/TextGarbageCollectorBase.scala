package com.apixio.nassembly.extractedtext

import com.apixio.model.nassembly.Base.{Cid, Oid}
import com.apixio.model.nassembly.{GarbageCollector, SortedFieldOrder}

import scala.collection.JavaConversions._

trait TextGarbageCollectorBase extends GarbageCollector {

  override def getGroupByFieldNames: java.util.Set[String] = {
    Set(Cid, Oid)
  }

  override def getSortedFields: Array[SortedFieldOrder] = {
    Array() // take latest
  }

}