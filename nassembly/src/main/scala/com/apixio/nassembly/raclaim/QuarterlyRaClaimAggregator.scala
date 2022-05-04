package com.apixio.nassembly.raclaim

import com.apixio.nassembly.commonaggregators.QuarterlyDateBucketAggregator

object QuarterlyRaClaimAggregator extends QuarterlyDateBucketAggregator {

  override def getDateBucketColNames: Array[String] = Array("raClaim.problemInfo.endDate",
    "raClaim.problemInfo.diagnosisDate",
    "raClaim.problemInfo.startDate")
}