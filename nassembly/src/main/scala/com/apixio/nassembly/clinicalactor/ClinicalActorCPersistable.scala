//package com.apixio.nassembly.clinicalactor
//
//import com.apixio.model.nassembly.CPersistable.ColValueWithPersistentField
//import com.apixio.model.nassembly.{AssemblySchema, CPersistable}
//
//import java.util
//import scala.collection.JavaConversions._
//
//class ClinicalActorCPersistable extends CPersistable[ClinicalActorExchange] {
//
//  override def getDataTypeName: String = {
//    ClinicalActorExchange.dataTypeName
//  }
//
//  override def getSchema: AssemblySchema = {
//    val col1: AssemblySchema.AssemblyCol = new AssemblySchema.AssemblyCol("role", AssemblySchema.ColType.StringType)
//    val cols:  Array[AssemblySchema.AssemblyCol] = Array(col1);
//
//    new AssemblySchema(cols)
//  }
//
//
//  // just use default "col" name for schema
//  override def clusteringColumnsWithValues(exchange: ClinicalActorExchange): util.List[ColValueWithPersistentField] = {
//    val cols: Array[AssemblySchema.AssemblyCol] = getSchema.getClusteringCols
//    val role: ColValueWithPersistentField = new ColValueWithPersistentField(exchange.getActors.head.getClinicalActorInfo.getActorRole, cols(0).getName)
//    List(role)
//  }
//
//}
