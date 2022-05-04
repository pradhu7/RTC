package com.apixio.nassembly.patientsocialhistory

import com.apixio.model.nassembly.CPersistable.ColValueWithPersistentField
import com.apixio.model.nassembly.{AssemblySchema, CPersistable}
import com.apixio.nassembly.socialhistory.TypeCodeAggregator

import java.util
import scala.collection.JavaConversions._

class PatientSocialHistoryCPersistable extends CPersistable[PatientSocialHistoryExchange] {
  override def getDataTypeName: String = {
    PatientSocialHistoryExchange.dataTypeName
  }



  override def getSchema(): AssemblySchema = {
    // typeCode is the logical name of the column. You shouldn't care about its physical name (what's the name in Cassandra)
    val col: AssemblySchema.AssemblyCol = new AssemblySchema.AssemblyCol(TypeCodeAggregator.PersistedTypeCodeColName, AssemblySchema.ColType.StringType)
    val cols:  Array[AssemblySchema.AssemblyCol] = Array(col);

    new AssemblySchema(cols)
  }

  override def clusteringColumnsWithValues(exchange: PatientSocialHistoryExchange): util.List[ColValueWithPersistentField] = {
    val cols: Array[AssemblySchema.AssemblyCol] = getSchema().getClusteringCols()
    val typeCode = exchange.getHistories.head.getBase.getDataCatalogMeta.getOriginalId.getId
    List(new ColValueWithPersistentField(typeCode, cols(0).getName))
  }
}
