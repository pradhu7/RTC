package com.apixio.nassembly.patientactor

import com.apixio.model.nassembly.CPersistable.ColValueWithPersistentField
import com.apixio.model.nassembly.{AssemblySchema, CPersistable}

import java.util
import scala.collection.JavaConversions._

class PatientActorCPersistable extends CPersistable[PatientActorExchange] {

  override def getDataTypeName: String = {
    PatientActorExchange.dataTypeName
  }

  override def getSchema: AssemblySchema = {
    val col1: AssemblySchema.AssemblyCol = new AssemblySchema.AssemblyCol("assignAuthority", AssemblySchema.ColType.StringType)
    val col2: AssemblySchema.AssemblyCol = new AssemblySchema.AssemblyCol("id", AssemblySchema.ColType.StringType)
    val col3: AssemblySchema.AssemblyCol = new AssemblySchema.AssemblyCol("role", AssemblySchema.ColType.StringType)
    val cols:  Array[AssemblySchema.AssemblyCol] = Array(col1, col2, col3)

    new AssemblySchema(cols)
  }


  // just use default "col" name for schema
  override def clusteringColumnsWithValues(exchange: PatientActorExchange): util.List[ColValueWithPersistentField] = {
    val cols: Array[AssemblySchema.AssemblyCol] = getSchema.getClusteringCols
    val primaryId = exchange.getProtos.head.getClinicalActorInfo.getPrimaryId
    val assignAuthority: ColValueWithPersistentField = new ColValueWithPersistentField(primaryId.getAssignAuthority, cols(0).getName)
    val id: ColValueWithPersistentField = new ColValueWithPersistentField(primaryId.getId, cols(1).getName)
    val role: ColValueWithPersistentField = new ColValueWithPersistentField(exchange.getProtos.head.getClinicalActorInfo.getActorRole, cols(2).getName)

    List(id, assignAuthority, role)
  }

}
