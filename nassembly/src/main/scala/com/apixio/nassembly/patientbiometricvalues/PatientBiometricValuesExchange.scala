package com.apixio.nassembly.patientbiometricvalues

import com.apixio.datacatalog.SkinnyPatientProto.BiometricValues
import com.apixio.datacatalog.SummaryObjects.BiometricValueSummary
import com.apixio.model.nassembly.Exchange
import com.apixio.model.nassembly.Exchange.ProtoEnvelop
import com.apixio.model.patient.Patient
import com.apixio.nassembly.apo.APOGenerator
import com.apixio.nassembly.biometricvalue.BiometricValueUtils
import com.google.protobuf.util.JsonFormat
import com.google.protobuf.{Descriptors, GeneratedMessageV3}

import java.io.InputStream
import scala.collection.JavaConverters._

class PatientBiometricValuesExchange extends Exchange {

  private var protos: Iterable[BiometricValues] = _

  def getProtos: Iterable[BiometricValues] = protos

  def buildBVWrapper(bioValues: Iterable[BiometricValueSummary]): Unit = {
    protos = Iterable(BiometricValueUtils.wrapAsPatient(bioValues.toList))
  }

  override def getDataTypeName: String = {
    PatientBiometricValuesExchange.dataTypeName
  }

  override def getDescriptor: Descriptors.Descriptor = {
    BiometricValues.getDescriptor
  }

  override def getCid: String = {
    protos.head.getBase.getPatientMeta.getPatientId.getUuid
  }

  override def fromProto(protoBytes: java.lang.Iterable[Array[Byte]]): Unit = {
    val iterator = protoBytes.iterator()
    protos = iterator.asScala.map(BiometricValues.parseFrom).toIterable
  }

  override def fromProtoStream(inputStreams: java.lang.Iterable[InputStream]): Unit = {
    val iterator = inputStreams.iterator()
    protos = iterator.asScala.map(BiometricValues.parseFrom).toIterable
  }

  override def getProtoEnvelops: java.lang.Iterable[ProtoEnvelop] = {
    protos.map(proto => {
      val byteData = proto.toByteArray
      val oid = "" // Means nothing for combined datatypes
      val oidKey = "" // Means nothing for combined datatypes
      val jsonData = JsonFormat.printer().includingDefaultValueFields().omittingInsignificantWhitespace().print(proto)
      new ProtoEnvelop(oid, oidKey, byteData, jsonData)
    }).asJava
  }

  override def toApo: java.lang.Iterable[Patient] = {
    Iterable(APOGenerator.mergeSkinny(protos.map(_.asInstanceOf[GeneratedMessageV3]).toList.asJava)).asJava
  }
}

object PatientBiometricValuesExchange {
  val dataTypeName = "patientBiometricValues"
}




