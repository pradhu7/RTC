package com.apixio.nassembly.patientfamilyhistory

import com.apixio.datacatalog.SkinnyPatientProto.PatientFamilyHistory
import com.apixio.datacatalog.SummaryObjects.FamilyHistorySummary
import com.apixio.model.nassembly.Exchange
import com.apixio.model.nassembly.Exchange.ProtoEnvelop
import com.apixio.model.patient.Patient
import com.apixio.nassembly.apo.APOGenerator
import com.google.protobuf.util.JsonFormat
import com.google.protobuf.{Descriptors, GeneratedMessageV3}

import java.io.InputStream
import scala.collection.JavaConverters._

class PatientFamilyHistoryExchange extends Exchange {

  private var protos: Iterable[PatientFamilyHistory] = _

  def getFamilyHistories: Iterable[PatientFamilyHistory] = protos

  def buildWrapper(FamilyHistorySummary: Iterable[FamilyHistorySummary]): Unit = {
    protos = Iterable(FamilyHistoryUtils.wrapAsPatient(FamilyHistorySummary.toList))
  }

  override def getDataTypeName: String = {
    PatientFamilyHistoryExchange.dataTypeName
  }

  override def getDescriptor: Descriptors.Descriptor = {
    PatientFamilyHistory.getDescriptor
  }

  override def getCid: String = {
    protos.head.getBase.getPatientMeta.getPatientId.getUuid
  }

  override def fromProto(protoBytes: java.lang.Iterable[Array[Byte]]): Unit = {
    val iterator = protoBytes.iterator()
    protos = iterator.asScala.map(PatientFamilyHistory.parseFrom).toIterable
  }

  override def fromProtoStream(inputStreams: java.lang.Iterable[InputStream]): Unit = {
    val iterator = inputStreams.iterator()
    protos = iterator.asScala.map(PatientFamilyHistory.parseFrom).toIterable
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

object PatientFamilyHistoryExchange {
  val dataTypeName = "patientFamilyHistory"
}