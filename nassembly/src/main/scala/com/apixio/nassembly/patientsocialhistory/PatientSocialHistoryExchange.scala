package com.apixio.nassembly.patientsocialhistory

import com.apixio.datacatalog.SkinnyPatientProto.SocialHistories
import com.apixio.datacatalog.SummaryObjects.SocialHistorySummary
import com.apixio.model.nassembly.Exchange
import com.apixio.model.nassembly.Exchange.ProtoEnvelop
import com.apixio.model.patient.Patient
import com.apixio.nassembly.apo.APOGenerator
import com.apixio.nassembly.socialhistory.SocialHistoryUtils
import com.google.protobuf.util.JsonFormat
import com.google.protobuf.{Descriptors, GeneratedMessageV3}

import java.io.InputStream
import scala.collection.JavaConverters._

class PatientSocialHistoryExchange extends Exchange {

  private var protos: Iterable[SocialHistories] = _

  def getHistories: Iterable[SocialHistories] = protos

  def buildWrapper(typeCode: String, histories : Iterable[SocialHistorySummary]): Unit = {
    protos = Iterable(SocialHistoryUtils.wrapAsPatient(typeCode, histories.toSeq))
  }

  override def getDataTypeName: String = {
    PatientSocialHistoryExchange.dataTypeName
  }

  override def getDescriptor: Descriptors.Descriptor = {
    SocialHistories.getDescriptor
  }

  override def getCid: String = {
    protos.head.getBase.getPatientMeta.getPatientId.getUuid
  }

  override def fromProto(protoBytes: java.lang.Iterable[Array[Byte]]): Unit = {
    val iterator = protoBytes.iterator()
    protos = iterator.asScala.map(SocialHistories.parseFrom).toIterable
  }

  override def fromProtoStream(inputStreams: java.lang.Iterable[InputStream]): Unit = {
    val iterator = inputStreams.iterator()
    protos = iterator.asScala.map(SocialHistories.parseFrom).toIterable
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

object PatientSocialHistoryExchange {
  val dataTypeName = "patientSocialHistory"
}
