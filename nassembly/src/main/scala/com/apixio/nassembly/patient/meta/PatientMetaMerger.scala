package com.apixio.nassembly.patient.meta

import com.apixio.nassembly.BaseConsolidator
import com.apixio.nassembly.mergeutils.MergerUtils
import com.apixio.nassembly.model.Merger

import scala.collection.JavaConverters._

class PatientMetaMerger extends Merger[PatientMetaExchange] {

  override def getDataTypeName: String = {
    PatientMetaExchange.dataTypeName
  }

  override def merge(exchanges: java.util.List[PatientMetaExchange]): PatientMetaExchange = {
    def bizLogic(exchanges: java.util.List[PatientMetaExchange]): PatientMetaExchange = {
      val protos = exchanges.asScala.map(_.getProto).toList
      val mergedProto = BaseConsolidator.mergePatientMeta(protos.asJava)
      val exchange = exchanges.get(0)
      exchange.setProto(mergedProto)
      exchange
    }

    // Handles null checking, size 0, and size 1
    MergerUtils.wrapMergerBizlogic[PatientMetaExchange](exchanges, bizLogic)
  }
}