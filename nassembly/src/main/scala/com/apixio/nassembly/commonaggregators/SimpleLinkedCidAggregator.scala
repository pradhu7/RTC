package com.apixio.nassembly.commonaggregators

import com.apixio.model.nassembly.{Aggregator, Base, TransformationMeta}
import com.apixio.nassembly.combinerutils.TransformationFactory

import java.util
import scala.collection.JavaConversions.setAsJavaSet

object SimpleLinkedCidAggregator extends Aggregator {

    override def getGroupIds: util.Set[String] = setAsJavaSet(Set(Base.FromCid))

    override def getPostProcesses: Array[TransformationMeta] = {
      Array(TransformationFactory.rebaseCid)
    }

}
