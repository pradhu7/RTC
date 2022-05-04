package com.apixio.nassembly.labresult

import com.apixio.datacatalog.SummaryObjects
import com.apixio.datacatalog.SummaryObjects.LabResultSummary
import com.apixio.nassembly.patient.PatientSummaryExchangeBase
import com.google.protobuf.Descriptors

import java.io.InputStream
import scala.collection.JavaConversions.iterableAsScalaIterable

class LabResultExchange extends PatientSummaryExchangeBase[LabResultSummary] {

  private var protos: Iterable[LabResultSummary] = Iterable.empty

  override def setProtos(labResults: Iterable[LabResultSummary]): Unit = {
    protos = labResults
  }
  override def getProtos: Iterable[LabResultSummary] = protos


  override def getDataTypeName: String = {
    LabResultExchange.dataTypeName
  }

  override def getDescriptor: Descriptors.Descriptor = {
    LabResultSummary.getDescriptor
  }

  override def fromProto(protoBytes: java.lang.Iterable[Array[Byte]]): Unit = {
    protos = protoBytes.map(LabResultSummary.parseFrom)
  }

  override def fromProtoStream(inputStreams: java.lang.Iterable[InputStream]): Unit = {
    protos = inputStreams.map(LabResultSummary.parseFrom)
  }

  override protected def getBase(proto: LabResultSummary): SummaryObjects.CodedBaseSummary = proto.getBase
}

object LabResultExchange {
  val dataTypeName = "labResult"
}



