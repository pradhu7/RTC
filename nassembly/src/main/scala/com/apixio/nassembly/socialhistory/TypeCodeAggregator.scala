package com.apixio.nassembly.socialhistory

import com.apixio.model.nassembly.{Base, TransformationMeta}
import com.apixio.nassembly.combinerutils.TransformationFactory
import com.apixio.nassembly.commonaggregators.DeleteIndicatorAggregator

import scala.collection.JavaConversions._

object TypeCodeAggregator extends DeleteIndicatorAggregator {

  override def getGroupIds: java.util.Set[String] = Set(Base.Cid, ProtoTypeCodeColName)

  override def getPostProcesses: Array[TransformationMeta] = {
    // SocialHistoryInfo.type.code -> code when we groupBy
    val rename = TransformationFactory.rename(ProtoTypeCodeColName, PersistedTypeCodeColName)
    val fillInNulls = TransformationFactory.fillInNull(PersistedTypeCodeColName, NullCode)
    Array(rename, fillInNulls)
  }

  val ProtoTypeCodeColName = "socialHistoryInfo.type.code"
  val PersistedTypeCodeColName = "typeCode"

  val NullCode = "-444" // So we know it was missing
}

