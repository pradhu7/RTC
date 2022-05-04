package com.apixio.nassembly.patient.partial

import com.apixio.model.nassembly.Base.{Cid, Oid}
import com.apixio.model.nassembly.{SortedFieldOrder, TransformationMeta}
import com.apixio.nassembly.combinerutils.TransformationFactory
import com.apixio.nassembly.model.Merger
import com.apixio.nassembly.util.DataConstants

import scala.collection.JavaConversions._

class PartialPatientMerger extends Merger[PartialPatientExchange] {


  override def getDataTypeName: String = {
    PartialPatientExchange.dataTypeName
  }

  override def getGroupIds: java.util.Set[String] = {
    Set(Cid, Oid)
  }

  // We need to write for bad batch. But we only need cid and oid
  override def getAggregatorPostProcesses: Array[TransformationMeta] = {
    Array(TransformationFactory.clearJsonData)
  }


  override def merge(exchanges: java.util.List[PartialPatientExchange]): PartialPatientExchange = {
    // Because we're persisting to C* as a partial patient
    val exchange = new PartialPatientExchange

    exchanges
      .map(e => e.getPatient)
      .filterNot(_.equals(null)) //filter nulls
      .sortBy(_.getBase.getDataCatalogMeta.getLastEditTime)
      .lastOption // don't use maxBy because of possible empty list
      .map(PartialPatientExchange.clearExtractedText) // purge extracted text
      .foreach(exchange.setPatient) // store in the exchange

    exchange
  }




}