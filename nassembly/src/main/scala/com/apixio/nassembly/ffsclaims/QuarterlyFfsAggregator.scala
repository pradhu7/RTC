package com.apixio.nassembly.ffsclaims

import com.apixio.nassembly.commonaggregators.QuarterlyDateBucketAggregator

object QuarterlyFfsAggregator extends QuarterlyDateBucketAggregator {

  override def getDateBucketColNames: Array[String] = Array("ffsClaim.procedureInfo.performedOn", "ffsClaim.procedureInfo.endDate")

}