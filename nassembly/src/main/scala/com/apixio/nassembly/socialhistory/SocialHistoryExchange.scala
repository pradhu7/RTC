package com.apixio.nassembly.socialhistory

import com.apixio.datacatalog.SummaryObjects
import com.apixio.datacatalog.SummaryObjects.SocialHistorySummary
import com.apixio.model.patient.Patient
import com.apixio.nassembly.patient.PatientSummaryExchangeBase
import com.google.protobuf.Descriptors

import java.io.InputStream
import scala.collection.JavaConversions.iterableAsScalaIterable

class SocialHistoryExchange extends PatientSummaryExchangeBase[SocialHistorySummary] {

  private var protos: Iterable[SocialHistorySummary] = Iterable.empty

  override def setProtos(summaries: Iterable[SocialHistorySummary]): Unit = {
    protos = summaries
  }

  override def getProtos: Iterable[SocialHistorySummary] = {
    protos
  }

  override def getDataTypeName: String = {
    SocialHistoryExchange.dataTypeName
  }

  override def getDescriptor: Descriptors.Descriptor = {
    SocialHistorySummary.getDescriptor
  }


  override def fromProto(protoBytes: java.lang.Iterable[Array[Byte]]): Unit = {
    protos = protoBytes.map(SocialHistorySummary.parseFrom)
  }

  override def fromProtoStream(inputStreams: java.lang.Iterable[InputStream]): Unit = {
    protos = inputStreams.map(SocialHistorySummary.parseFrom)
  }

  override def toApo: java.lang.Iterable[Patient] = {
    throw new UnsupportedOperationException("Social History to APO not supported")
  }

  override protected def getBase(summary: SocialHistorySummary): SummaryObjects.CodedBaseSummary = {
    summary.getBase
  }
}

object SocialHistoryExchange {
  val dataTypeName = "socialHistory"
}