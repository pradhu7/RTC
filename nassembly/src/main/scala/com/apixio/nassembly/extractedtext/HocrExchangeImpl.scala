package com.apixio.nassembly.extractedtext

import com.apixio.model.nassembly.{AssemblyContext, Exchange}
import com.apixio.nassembly.exchangeutils.EidUtils
import com.apixio.nassembly.patient.meta.PatientMetaExchange

import java.util
import java.util.UUID
import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.collection.JavaConverters._

class HocrExchangeImpl extends ExtractedTextExchangeBase {


  override def getDataTypeName: String = {
    HocrExchangeImpl.dataTypeName
  }

  override def getPrimaryEid: String = {
    EidUtils.getPatientKey(proto.getPatientMeta)
  }

  override def getEidExchange: Exchange = {
    val exchange = new PatientMetaExchange
    exchange.setProto(proto.getPatientMeta)

    exchange
  }

  override def getExternalIds: Array[Array[Byte]] = {
    proto.getPatientMeta.getExternalIdsList
      .map(_.toByteArray)
      .toArray
  }

  override def getParts(ac: AssemblyContext): util.Map[String, _ <: java.lang.Iterable[Exchange]] = {
    Map.empty[String, java.lang.Iterable[Exchange]].asJava
  }

  override def setIds(cid: UUID, primaryEid: Array[Byte]): Unit = {
    proto = {
      val newPatientMeta = EidUtils.updatePatientMetaIds(proto.getPatientMeta, cid, primaryEid)

      proto
        .toBuilder
        .setPatientMeta(newPatientMeta)
        .build()
    }
  }

}

object HocrExchangeImpl {
  val dataTypeName = "hocr"

  val blobImageType = "cleantext_protobuf_b64"
}