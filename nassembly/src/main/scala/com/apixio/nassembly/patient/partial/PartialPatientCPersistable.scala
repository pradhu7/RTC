package com.apixio.nassembly.patient.partial

import com.apixio.model.nassembly.Base.Oid
import com.apixio.model.nassembly.CPersistable.ColValueWithPersistentField
import com.apixio.model.nassembly.{AssemblySchema, CPersistable}

import java.util
import scala.collection.JavaConversions._


class PartialPatientCPersistable extends CPersistable[PartialPatientExchange] {
  override def getDataTypeName: String = {
    PartialPatientExchange.dataTypeName
  }



  override def getSchema(): AssemblySchema = {
    // typeCode is the logical name of the column. You shouldn't care about its physical name (what's the name in Cassandra)
    val docId: AssemblySchema.AssemblyCol = new AssemblySchema.AssemblyCol("documentId", AssemblySchema.ColType.StringType)
    val cols:  Array[AssemblySchema.AssemblyCol] = Array(docId);

    val ttlFromNowInSec = 60 * 60 * 24 * 275 // 9 months
    new AssemblySchema("internal", cols, null, ttlFromNowInSec)
  }

  override def clusteringColumnsWithValues(exchange: PartialPatientExchange): util.List[ColValueWithPersistentField] = {
    val cols: Array[AssemblySchema.AssemblyCol] = getSchema().getClusteringCols()
    val oid = exchange.getProtoEnvelops.head.getOid
    val docId: ColValueWithPersistentField = new ColValueWithPersistentField(oid, cols(0).getName)
    List(docId)
  }
}
