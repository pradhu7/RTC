package com.apixio.nassembly.ffsclaims

import com.apixio.datacatalog.SummaryObjects
import com.apixio.datacatalog.SummaryObjects.FfsClaimSummary
import com.apixio.model.patient.Patient
import com.apixio.nassembly.patient.PatientSummaryExchangeBase
import com.google.protobuf.Descriptors

import java.io.InputStream
import scala.collection.JavaConversions.iterableAsScalaIterable

class FfsClaimExchange extends PatientSummaryExchangeBase[FfsClaimSummary] {

  private var protos: Iterable[FfsClaimSummary] = Iterable.empty

  override def setProtos(summaries: Iterable[FfsClaimSummary]): Unit = {
    protos = summaries
  }

  override def getProtos: Iterable[FfsClaimSummary] = {
    protos
  }

  override def getDataTypeName: String = {
    FfsClaimExchange.dataTypeName
  }

  override def getDescriptor: Descriptors.Descriptor = {
    FfsClaimSummary.getDescriptor
  }

  override def fromProto(protoBytes: java.lang.Iterable[Array[Byte]]): Unit = {
    protos = protoBytes.map(FfsClaimSummary.parseFrom)
  }

  override def fromProtoStream(inputStreams: java.lang.Iterable[InputStream]): Unit = {
    protos = inputStreams.map(FfsClaimSummary.parseFrom)
  }

  override def toApo: java.lang.Iterable[Patient] = {
    throw new UnsupportedOperationException("ffsClaim to APO not supported")
  }

  override protected def getBase(proto: FfsClaimSummary): SummaryObjects.CodedBaseSummary = {
    proto.getBase
  }

}

object FfsClaimExchange {
  val dataTypeName = "ffsClaim"
}
