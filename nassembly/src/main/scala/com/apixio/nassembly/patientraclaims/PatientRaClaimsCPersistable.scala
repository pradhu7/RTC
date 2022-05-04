package com.apixio.nassembly.patientraclaims

import com.apixio.model.nassembly.CPersistable.ColValueWithPersistentField
import com.apixio.model.nassembly.{AssemblySchema, CPersistable}
import com.apixio.nassembly.combinerutils.DataBuckets

import java.util
import scala.collection.JavaConversions._

class PatientRaClaimsCPersistable extends CPersistable[PatientRaClaimsExchange] {
  override def getDataTypeName: String = {
    PatientRaClaimsExchange.dataTypeName
  }



  override def getSchema(): AssemblySchema = {
    // YearQuarter is the logical name of the column. You shouldn't care about its physical name (what's the name in Cassandra)
    val col: AssemblySchema.AssemblyCol = new AssemblySchema.AssemblyCol(DataBuckets.YearQuarter.toString, AssemblySchema.ColType.StringType)
    val cols:  Array[AssemblySchema.AssemblyCol] = Array(col);

    new AssemblySchema(cols)
  }

  override def clusteringColumnsWithValues(exchange: PatientRaClaimsExchange): util.List[ColValueWithPersistentField] = {
    val cols: Array[AssemblySchema.AssemblyCol] = getSchema().getClusteringCols()
    val dateBucket = exchange.getRaClaims.head.getBase.getDataCatalogMeta.getOriginalId.getId
    val dateBucketField: ColValueWithPersistentField = new ColValueWithPersistentField(dateBucket, cols(0).getName)
    List(dateBucketField)
  }

}
