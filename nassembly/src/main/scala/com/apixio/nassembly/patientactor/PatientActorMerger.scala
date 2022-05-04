package com.apixio.nassembly.patientactor

import com.apixio.model.nassembly.Base.Cid
import com.apixio.nassembly.mergeutils.MergerUtils
import com.apixio.nassembly.model.FilterDeletesMerger

import java.util
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

class PatientActorMerger extends FilterDeletesMerger[PatientActorExchange] {

  override def getDataTypeName: String = {
    PatientActorExchange.dataTypeName
  }

  override def getGroupIds: util.Set[String] = Set(Cid, "clinicalActorInfo.primaryId", "clinicalActorInfo.actorRole")

  override def getEditTypeColName: String = "base.dataCatalogMeta.editType"

  override def merge(exchanges: java.util.List[PatientActorExchange]): PatientActorExchange = {
    def bizLogic(exchanges: java.util.List[PatientActorExchange]): PatientActorExchange = {
      val protos = exchanges.asScala.flatMap(_.getProtos).toList
      val mergedActors = PatientActorUtils.mergeClinicalActorSummaries(protos)
      val exchange = exchanges.get(0) //wrapper protects us from empty list
      exchange.setProtos(mergedActors)
      exchange

    }
    MergerUtils.wrapMergerBizlogic[PatientActorExchange](exchanges, bizLogic)
  }

}
