package com.apixio.nassembly.commonaggregators

import com.apixio.model.nassembly.{Aggregator, TransformationMeta}
import com.apixio.nassembly.combinerutils.TransformationFactory

trait DeleteIndicatorAggregator extends Aggregator {

  def getEditTypeColName = "base.dataCatalogMeta.editType"

  val editTypeFiler = s"$getEditTypeColName != 'DELETE'"

  override def getPreProcesses: Array[TransformationMeta] = Array(TransformationFactory.filter(editTypeFiler))

}
