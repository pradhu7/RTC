package com.apixio.nassembly.encounter

import com.apixio.model.nassembly.Base.Cid
import com.apixio.nassembly.mergeutils.MergerUtils
import com.apixio.nassembly.model.FilterDeletesMerger

import java.util
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

class EncounterMerger extends FilterDeletesMerger[EncounterExchange] {

  override def getDataTypeName: String = {
    EncounterExchange.dataTypeName
  }

  override def getEditTypeColName: String = "base.dataCatalogMeta.editType"

  override def getGroupIds: util.Set[String] = {
    Set(Cid, "encounterInfo.primaryId")
  }

  override def merge(exchanges: java.util.List[EncounterExchange]): EncounterExchange = {
    def bizLogic(exchanges: java.util.List[EncounterExchange]): EncounterExchange = {
      val protos = exchanges.asScala.flatMap(_.getProtos).toList
      val mergedEncounters = EncounterUtils.mergeEncounterSummaries(protos)
      val exchange = exchanges.get(0)
      exchange.setProtos(mergedEncounters)
      exchange
    }

    MergerUtils.wrapMergerBizlogic[EncounterExchange](exchanges, bizLogic)
  }
}
