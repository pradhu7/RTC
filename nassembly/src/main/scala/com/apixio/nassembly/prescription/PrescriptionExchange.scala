package com.apixio.nassembly.prescription

import com.apixio.datacatalog.SummaryObjects
import com.apixio.datacatalog.SummaryObjects.PrescriptionSummary
import com.apixio.nassembly.patient.PatientSummaryExchangeBase
import com.google.protobuf.Descriptors

import java.io.InputStream
import scala.collection.JavaConversions.iterableAsScalaIterable

class PrescriptionExchange extends PatientSummaryExchangeBase[PrescriptionSummary] {

  private var protos: Iterable[PrescriptionSummary] = Iterable.empty

  override def setProtos(summaries: Iterable[PrescriptionSummary]): Unit = {
    protos = summaries
  }

  override def getProtos: Iterable[PrescriptionSummary] = {
    protos
  }

  override def getDataTypeName: String = {
    PrescriptionExchange.dataTypeName
  }

  override def getDescriptor: Descriptors.Descriptor = {
    PrescriptionSummary.getDescriptor
  }

  override def fromProto(protoBytes: java.lang.Iterable[Array[Byte]]): Unit = {
    protos = protoBytes.map(PrescriptionSummary.parseFrom)
  }

  override def fromProtoStream(inputStreams: java.lang.Iterable[InputStream]): Unit = {
    protos = inputStreams.map(PrescriptionSummary.parseFrom)
  }


  override protected def getBase(proto: PrescriptionSummary): SummaryObjects.CodedBaseSummary = {
    proto.getBase
  }
}

object PrescriptionExchange {
  val dataTypeName = "prescription"
}



