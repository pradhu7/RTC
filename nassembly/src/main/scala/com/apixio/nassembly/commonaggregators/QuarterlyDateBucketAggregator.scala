package com.apixio.nassembly.commonaggregators

import com.apixio.model.nassembly.{Base, TransformationMeta}
import com.apixio.nassembly.combinerutils.{DataBuckets, TransformationFactory}
import com.apixio.util.nassembly.DateBucketUtils

import scala.collection.JavaConversions._

trait QuarterlyDateBucketAggregator extends DeleteIndicatorAggregator {

  override def getPreProcesses: Array[TransformationMeta] = {
    super.getPreProcesses() ++ Array(dateBucketTransformation)
  }

  override def getGroupIds: java.util.Set[String] = Set(Base.Cid, DataBuckets.YearQuarter.toString)

  override def getPostProcesses: Array[TransformationMeta] = {
    Array(fillInNulls)
  }

  val dateBucketTransformation: TransformationMeta = TransformationFactory.yearlyQuarterFallback(getDateBucketColNames)
  val fillInNulls: TransformationMeta = TransformationFactory.fillInNull(DataBuckets.YearQuarter.toString, DateBucketUtils.EMPTY_YEAR_QUARTER)

  def getDateBucketColNames: Array[String]
}
