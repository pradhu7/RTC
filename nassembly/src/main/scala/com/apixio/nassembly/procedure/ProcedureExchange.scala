package com.apixio.nassembly.procedure

import com.apixio.datacatalog.SummaryObjects
import com.apixio.datacatalog.SummaryObjects.ProcedureSummary
import com.apixio.nassembly.patient.PatientSummaryExchangeBase
import com.google.protobuf.Descriptors

import java.io.InputStream
import scala.collection.JavaConversions.iterableAsScalaIterable

class ProcedureExchange extends PatientSummaryExchangeBase[ProcedureSummary] {

  private var protos: Iterable[ProcedureSummary] = Iterable.empty

  override def setProtos(summaries: Iterable[ProcedureSummary]): Unit = {
    protos = summaries
  }

  override def getProtos: Iterable[ProcedureSummary] = {
    protos
  }

  override def getDataTypeName: String = {
    ProcedureExchange.dataTypeName
  }

  override def getDescriptor: Descriptors.Descriptor = {
    ProcedureSummary.getDescriptor
  }

  override def fromProto(protoBytes: java.lang.Iterable[Array[Byte]]): Unit = {
    protos = protoBytes.map(ProcedureSummary.parseFrom)
  }

  override def fromProtoStream(inputStreams: java.lang.Iterable[InputStream]): Unit = {
    protos = inputStreams.map(ProcedureSummary.parseFrom)
  }

  override protected def getBase(proto: ProcedureSummary): SummaryObjects.CodedBaseSummary = {
    proto.getBase
  }
}

object ProcedureExchange {
  val dataTypeName = "procedure"
}
