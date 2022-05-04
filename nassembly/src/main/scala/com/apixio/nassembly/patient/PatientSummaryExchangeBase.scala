package com.apixio.nassembly.patient

import com.apixio.datacatalog.PatientMetaProto.PatientMeta
import com.apixio.datacatalog.SummaryObjects.CodedBaseSummary
import com.apixio.model.nassembly.Exchange
import com.apixio.model.nassembly.Exchange.ProtoEnvelop
import com.apixio.nassembly.BaseConsolidator
import com.apixio.nassembly.exchangeutils.EidUtils
import com.apixio.nassembly.patient.meta.PatientMetaExchange
import com.apixio.nassembly.util.ExternalIdUtil
import com.google.protobuf.util.JsonFormat

import scala.collection.JavaConverters._

trait PatientSummaryExchangeBase[T <: com.google.protobuf.GeneratedMessageV3] extends Exchange {

  def setProtos(protos: Iterable[T]): Unit

  def getProtos: Iterable[T]

  protected def getBase(proto: T): CodedBaseSummary

  private def getBases: Iterable[CodedBaseSummary] = getProtos.map(getBase)

  override def getExternalIds: Array[Array[Byte]] = {
    ExternalIdUtil.getExternalIds(getBases.map(_.getPatientMeta).toSeq)
      .map(_.toByteArray)
  }

  override def getProtoEnvelops: java.lang.Iterable[ProtoEnvelop] = {
    getProtos.map(summary => {
      val parsingDetailsList = getBase(summary).getParsingDetailsList
      val oid = getBase(summary).getDataCatalogMeta.getOid
      val oidKey = PatientUtils.getOidKeyFromParsingDetailsList(parsingDetailsList)

      new ProtoEnvelop(oid,
        oidKey,
        summary.toByteArray,
        JsonFormat.printer().includingDefaultValueFields().omittingInsignificantWhitespace().print(summary))
    }).asJava
  }

  private def getPatientMetaFromSummary: PatientMeta = {
    val patientMeta = BaseConsolidator.mergePatientMeta(getBases.map(_.getPatientMeta).toList.asJava)
    patientMeta
  }

  override def getCid: String = {
    getPatientMetaFromSummary.getPatientId.getUuid
  }

  override def getPrimaryEid: String = {
    EidUtils.getPatientKey(getPatientMetaFromSummary)
  }

  override def getEidExchange: Exchange = {
    val exchange = new PatientMetaExchange
    exchange.setProto(getPatientMetaFromSummary)
    exchange
  }

}
