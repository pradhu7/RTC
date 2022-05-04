package com.apixio.nassembly.procedure

import com.apixio.nassembly.commonaggregators.QuarterlyDateBucketAggregator

object QuarterlyProcedureAggregator extends QuarterlyDateBucketAggregator {

  override def getDateBucketColNames: Array[String] = Array("procedureInfo.performedOn", "procedureInfo.endDate")

}
