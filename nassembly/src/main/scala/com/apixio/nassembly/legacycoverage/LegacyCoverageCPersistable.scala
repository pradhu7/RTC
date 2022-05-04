package com.apixio.nassembly.legacycoverage

import com.apixio.model.nassembly.CPersistable.ColValueWithPersistentField
import com.apixio.model.nassembly.{AssemblySchema, CPersistable}

import java.util
import scala.collection.JavaConversions._

class LegacyCoverageCPersistable extends CPersistable[LegacyCoverageExchange] {
  override def getDataTypeName: String = {
    LegacyCoverageExchange.dataTypeName
  }



  override def getSchema(): AssemblySchema = {
    // Just store coverage coming from a diff archive uuid separately
    val col1: AssemblySchema.AssemblyCol = new AssemblySchema.AssemblyCol("healthPlanName", AssemblySchema.ColType.StringType)
    val col2: AssemblySchema.AssemblyCol = new AssemblySchema.AssemblyCol("oid", AssemblySchema.ColType.StringType)
    val col3: AssemblySchema.AssemblyCol = new AssemblySchema.AssemblyCol("startDate", AssemblySchema.ColType.LongType)
    val col4: AssemblySchema.AssemblyCol = new AssemblySchema.AssemblyCol("endDate", AssemblySchema.ColType.LongType)
    val cols:  Array[AssemblySchema.AssemblyCol] = Array(col1, col2, col3, col4)

    new AssemblySchema(cols)
  }

  override def clusteringColumnsWithValues(exchange: LegacyCoverageExchange): util.List[ColValueWithPersistentField] = {
    val cols: Array[AssemblySchema.AssemblyCol] = getSchema().getClusteringCols()
    val proto = exchange.getProtos.head
    val coverageInfo = proto.getCoverageInfo

    // Values
    val healthPlanName = coverageInfo.getHealthPlanName
    val oid = proto.getBase.getDataCatalogMeta.getOid
    val startDate = coverageInfo.getStartDate.getEpochMs
    val endDate = coverageInfo.getEndDate.getEpochMs

    // Columns
    val col1: ColValueWithPersistentField = new ColValueWithPersistentField(healthPlanName, cols(0).getName)
    val col2: ColValueWithPersistentField = new ColValueWithPersistentField(oid, cols(1).getName)
    val col3: ColValueWithPersistentField = new ColValueWithPersistentField(startDate, cols(2).getName)
    val col4: ColValueWithPersistentField = new ColValueWithPersistentField(endDate, cols(3).getName)
    List(col1, col2, col3, col4)
  }
}
