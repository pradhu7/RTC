package com.apixio.nassembly.encounter

import com.apixio.model.nassembly.CPersistable.ColValueWithPersistentField
import com.apixio.model.nassembly.{AssemblySchema, CPersistable}

import java.util
import scala.collection.JavaConversions._

class EncounterCPersistable extends CPersistable[EncounterExchange] {
  override def getDataTypeName: String = {
    EncounterExchange.dataTypeName
  }



  override def getSchema(): AssemblySchema = {
    val col1: AssemblySchema.AssemblyCol = new AssemblySchema.AssemblyCol("assignAuthority", AssemblySchema.ColType.StringType)
    val col2: AssemblySchema.AssemblyCol = new AssemblySchema.AssemblyCol("id", AssemblySchema.ColType.StringType)
    val cols:  Array[AssemblySchema.AssemblyCol] = Array(col1, col2)

    new AssemblySchema(cols)
  }

  override def clusteringColumnsWithValues(exchange: EncounterExchange): util.List[ColValueWithPersistentField] = {
    val cols: Array[AssemblySchema.AssemblyCol] = getSchema().getClusteringCols()
    val primaryId = exchange.getProtos.head.getEncounterInfo.getPrimaryId
    val assignAuthority: ColValueWithPersistentField = new ColValueWithPersistentField(primaryId.getAssignAuthority, cols(0).getName)
    val id: ColValueWithPersistentField = new ColValueWithPersistentField(primaryId.getId, cols(1).getName)

    List(id, assignAuthority)
  }
}
