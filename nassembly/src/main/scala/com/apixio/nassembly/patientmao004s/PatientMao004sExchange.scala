package com.apixio.nassembly.patientmao004s

import com.apixio.datacatalog.SkinnyPatientProto.PatientMao004s
import com.apixio.datacatalog.SummaryObjects.Mao004Summary
import com.apixio.model.nassembly.Exchange
import com.apixio.model.nassembly.Exchange.ProtoEnvelop
import com.apixio.model.patient.Patient
import com.apixio.nassembly.apo.APOGenerator
import com.apixio.nassembly.mao004.Mao004Utils
import com.google.protobuf.util.JsonFormat
import com.google.protobuf.{Descriptors, GeneratedMessageV3}

import java.io.InputStream
import scala.collection.JavaConverters._

class PatientMao004sExchange extends Exchange {

  private var protos: Iterable[PatientMao004s] = _

  def getMao004s: Iterable[PatientMao004s] = protos

  def buildSkinnyPatient(dateBucket: String, procedures : Iterable[Mao004Summary]): Unit = {
    protos = Iterable(Mao004Utils.wrapAsPatient(dateBucket, procedures.toList))
  }

  override def getDataTypeName: String = {
    PatientMao004sExchange.dataTypeName
  }

  override def getDescriptor: Descriptors.Descriptor = {
    PatientMao004s.getDescriptor
  }

  override def getCid: String = {
    protos.head.getBase.getPatientMeta.getPatientId.getUuid
  }

  override def fromProto(protoBytes: java.lang.Iterable[Array[Byte]]): Unit = {
    val iterator = protoBytes.iterator()
    protos = iterator.asScala.map(PatientMao004s.parseFrom).toIterable
  }

  override def fromProtoStream(inputStreams: java.lang.Iterable[InputStream]): Unit = {
    val iterator = inputStreams.iterator()
    protos = iterator.asScala.map(PatientMao004s.parseFrom).toIterable
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

object PatientMao004sExchange {
  val dataTypeName = "patientMao004s"
}


