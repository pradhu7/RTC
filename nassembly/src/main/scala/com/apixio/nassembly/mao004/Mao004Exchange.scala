package com.apixio.nassembly.mao004

import com.apixio.datacatalog.SummaryObjects
import com.apixio.datacatalog.SummaryObjects.Mao004Summary
import com.apixio.nassembly.patient.PatientSummaryExchangeBase
import com.google.protobuf.Descriptors

import java.io.InputStream
import scala.collection.JavaConversions.iterableAsScalaIterable

class Mao004Exchange extends PatientSummaryExchangeBase[Mao004Summary] {

  private var protos: Iterable[Mao004Summary] = Iterable.empty

  override def setProtos(summaries: Iterable[Mao004Summary]): Unit = {
    protos = summaries
  }

  override def getProtos: Iterable[Mao004Summary] = {
    protos
  }

  override def getDataTypeName: String = {
    Mao004Exchange.dataTypeName
  }

  override def getDescriptor: Descriptors.Descriptor = {
    Mao004Summary.getDescriptor
  }

  override def fromProto(protoBytes: java.lang.Iterable[Array[Byte]]): Unit = {
    protos = protoBytes.map(Mao004Summary.parseFrom)
  }

  override def fromProtoStream(inputStreams: java.lang.Iterable[InputStream]): Unit = {
    protos = inputStreams.map(Mao004Summary.parseFrom)
  }

  override protected def getBase(proto: Mao004Summary): SummaryObjects.CodedBaseSummary = {
    proto.getBase
  }
}

object Mao004Exchange {
  val dataTypeName = "mao004"
}


