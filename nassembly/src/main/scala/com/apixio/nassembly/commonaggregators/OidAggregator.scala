package com.apixio.nassembly.commonaggregators

import com.apixio.model.nassembly.{Aggregator, Base, TransformationMeta}

import java.util
import scala.collection.JavaConversions.setAsJavaSet

class OidAggregator(preProcesses: Array[TransformationMeta], postProcesses: Array[TransformationMeta]) extends Aggregator {

  override def getPreProcesses: Array[TransformationMeta] = preProcesses
  override def getGroupIds: util.Set[String] = setAsJavaSet(Set(Base.Cid, Base.Oid))
  override def getPostProcesses: Array[TransformationMeta] = postProcesses

}
