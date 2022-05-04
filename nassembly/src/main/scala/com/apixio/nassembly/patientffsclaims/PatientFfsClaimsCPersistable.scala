package com.apixio.nassembly.patientffsclaims

import com.apixio.model.nassembly.CPersistable.ColValueWithPersistentField
import com.apixio.model.nassembly.{AssemblySchema, CPersistable}
import com.apixio.nassembly.combinerutils.DataBuckets

import java.util
import scala.collection.JavaConversions._

class PatientFfsClaimsCPersistable extends CPersistable[PatientFfsClaimsExchange] {
  override def getDataTypeName: String = {
    PatientFfsClaimsExchange.dataTypeName
  }

  override def getSchema: AssemblySchema = {
    // YearQuarter is the logical name of the column. You shouldn't care about its physical name (what's the name in Cassandra)
    val col: AssemblySchema.AssemblyCol = new AssemblySchema.AssemblyCol(DataBuckets.YearQuarter.toString, AssemblySchema.ColType.StringType)
    val cols:  Array[AssemblySchema.AssemblyCol] = Array(col)

    new AssemblySchema(cols)
  }

  override def clusteringColumnsWithValues(exchange: PatientFfsClaimsExchange): util.List[ColValueWithPersistentField] = {
    val cols: Array[AssemblySchema.AssemblyCol] = getSchema().getClusteringCols
    val dateBucket = exchange.getffsClaims.head.getBase.getDataCatalogMeta.getOriginalId.getId
    val dateBucketField: ColValueWithPersistentField = new ColValueWithPersistentField(dateBucket, cols(0).getName)
    List(dateBucketField)
  }
}
