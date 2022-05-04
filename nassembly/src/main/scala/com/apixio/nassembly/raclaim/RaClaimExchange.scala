package com.apixio.nassembly.raclaim

import com.apixio.datacatalog.SummaryObjects
import com.apixio.datacatalog.SummaryObjects.RaClaimSummary
import com.apixio.nassembly.patient.PatientSummaryExchangeBase
import com.google.protobuf.Descriptors

import java.io.InputStream
import scala.collection.JavaConversions.iterableAsScalaIterable

class RaClaimExchange extends PatientSummaryExchangeBase[RaClaimSummary] {

  private var protos: Iterable[RaClaimSummary] = Iterable.empty

  override def setProtos(summaries: Iterable[RaClaimSummary]): Unit = {
    protos = summaries
  }

  override def getProtos: Iterable[RaClaimSummary] = {
    protos
  }

  override def getDataTypeName: String = {
    RaClaimExchange.dataTypeName
  }

  override def getDescriptor: Descriptors.Descriptor = {
    RaClaimSummary.getDescriptor
  }

  override def fromProto(protoBytes: java.lang.Iterable[Array[Byte]]): Unit = {
    protos = protoBytes.map(RaClaimSummary.parseFrom)
  }

  override def fromProtoStream(inputStreams: java.lang.Iterable[InputStream]): Unit = {
    protos = inputStreams.map(RaClaimSummary.parseFrom)
  }

  override protected def getBase(proto: RaClaimSummary): SummaryObjects.CodedBaseSummary = {
    proto.getBase
  }
}

object RaClaimExchange {
  val dataTypeName = "raClaim"
}


