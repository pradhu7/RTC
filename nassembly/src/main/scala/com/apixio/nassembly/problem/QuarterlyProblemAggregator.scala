package com.apixio.nassembly.problem

import com.apixio.nassembly.commonaggregators.QuarterlyDateBucketAggregator

object QuarterlyProblemAggregator extends QuarterlyDateBucketAggregator {

  override def getDateBucketColNames: Array[String] = Array("problemInfo.endDate", "problemInfo.diagnosisDate", "problemInfo.startDate")
}