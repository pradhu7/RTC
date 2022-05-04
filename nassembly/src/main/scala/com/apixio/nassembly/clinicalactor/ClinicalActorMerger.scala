package com.apixio.nassembly.clinicalactor

import com.apixio.model.nassembly.Base.Cid
import com.apixio.model.nassembly.Exchange
import com.apixio.nassembly.mergeutils.MergerUtils
import com.apixio.nassembly.model.FilterDeletesMerger

import java.util
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

class ClinicalActorMerger extends FilterDeletesMerger[ClinicalActorExchange] {

  override def getDataTypeName: String = {
    ClinicalActorExchange.dataTypeName
  }

  override def getGroupIds: util.Set[String] = Set(Cid, "clinicalActorInfo.actorRole")

  override def getEditTypeColName: String = "dataCatalogMeta.editType"

  override def merge(exchanges: java.util.List[ClinicalActorExchange]): ClinicalActorExchange = {
    def bizLogic(exchanges: java.util.List[ClinicalActorExchange]): ClinicalActorExchange = {
      val protos = exchanges.asScala.flatMap(_.getActors).toList
      val mergedActors = ClinicalActorUtils.mergeClinicalActorSummaries(protos)
      val exchange = exchanges.get(0) //wrapper protects us from empty list
      exchange.setActors(mergedActors)
      exchange

    }
    MergerUtils.wrapMergerBizlogic[ClinicalActorExchange](exchanges, bizLogic)
  }

}