package com.apixio.nassembly.mao004

import com.apixio.nassembly.commonaggregators.QuarterlyDateBucketAggregator

object QuarterlyMao004Aggregator extends QuarterlyDateBucketAggregator {

  override def getDateBucketColNames: Array[String] = Array("mao004.problemInfo.endDate",
    "mao004.problemInfo.diagnosisDate",
    "mao004.problemInfo.startDate")

}
