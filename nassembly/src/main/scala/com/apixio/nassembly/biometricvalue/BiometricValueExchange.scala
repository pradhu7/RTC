package com.apixio.nassembly.biometricvalue

import com.apixio.datacatalog.SummaryObjects
import com.apixio.datacatalog.SummaryObjects.BiometricValueSummary
import com.apixio.model.patient.Patient
import com.apixio.nassembly.patient.PatientSummaryExchangeBase
import com.google.protobuf.Descriptors

import java.io.InputStream
import scala.collection.JavaConversions.iterableAsScalaIterable

class BiometricValueExchange extends PatientSummaryExchangeBase[BiometricValueSummary] {

  private var protos: Iterable[BiometricValueSummary] = Iterable.empty

  override def setProtos(summaries: Iterable[BiometricValueSummary]): Unit = {
    protos = summaries
  }

  override def getProtos: Iterable[BiometricValueSummary] = {
    protos
  }

  override def getDataTypeName: String = {
    BiometricValueExchange.dataTypeName
  }

  override def getDescriptor: Descriptors.Descriptor = {
    BiometricValueSummary.getDescriptor
  }

  override def fromProto(protoBytes: java.lang.Iterable[Array[Byte]]): Unit = {
    protos = protoBytes.map(BiometricValueSummary.parseFrom)
  }

  override def fromProtoStream(inputStreams: java.lang.Iterable[InputStream]): Unit = {
    protos = inputStreams.map(BiometricValueSummary.parseFrom)
  }

  override def toApo: java.lang.Iterable[Patient] = {
    throw new UnsupportedOperationException("Biometric Value to APO not supported")
  }

  override protected def getBase(proto: BiometricValueSummary): SummaryObjects.CodedBaseSummary = {
    proto.getBase
  }
}

object BiometricValueExchange {
  val dataTypeName: String = "biometricValue"
}
