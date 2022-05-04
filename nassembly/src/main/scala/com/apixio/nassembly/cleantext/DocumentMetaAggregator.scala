package com.apixio.nassembly.cleantext

import com.apixio.model.nassembly.{Aggregator, Base, TransformationMeta}
import com.apixio.nassembly.combinerutils.TransformationFactory
import combinerutils.PreAggregated

import java.util
import scala.collection.JavaConversions.setAsJavaSet

class DocumentMetaAggregator(requireOcr: Boolean) extends Aggregator with PreAggregated {
  val ocrCondition: String = if (requireOcr) {
    "documentMeta.ocrMetadata is not null"
  } else {
    "documentMeta.ocrMetadata is null"
  }
  val textExtractCondition = "documentMeta.contentMeta is not null" // Doc metadata after the fact shouldn't trigger doccache
  val filter: TransformationMeta = TransformationFactory.filter(s"$ocrCondition AND $textExtractCondition")

  override def getPreProcesses: Array[TransformationMeta] = Array(filter)

  override def getGroupIds: util.Set[String] = setAsJavaSet(Set(Base.Cid, Base.Oid))
}