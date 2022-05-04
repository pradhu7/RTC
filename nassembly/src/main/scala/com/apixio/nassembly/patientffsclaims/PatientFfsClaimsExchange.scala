package com.apixio.nassembly.patientffsclaims

import com.apixio.datacatalog.SkinnyPatientProto.PatientFfsClaims
import com.apixio.datacatalog.SummaryObjects.FfsClaimSummary
import com.apixio.model.nassembly.Exchange
import com.apixio.model.nassembly.Exchange.ProtoEnvelop
import com.apixio.model.patient.Patient
import com.apixio.nassembly.apo.APOGenerator
import com.apixio.nassembly.ffsclaims.FfsClaimUtils
import com.google.protobuf.util.JsonFormat
import com.google.protobuf.{Descriptors, GeneratedMessageV3}

import java.io.InputStream
import scala.collection.JavaConverters._

class PatientFfsClaimsExchange extends Exchange {

  private var protos: Iterable[PatientFfsClaims] = _

  def getffsClaims: Iterable[PatientFfsClaims] = protos

  def buildSkinnyPatient(dateBucket: String, ffsClaims : Iterable[FfsClaimSummary]): Unit = {
    protos = Iterable(FfsClaimUtils.wrapAsPatient(dateBucket, ffsClaims.toList))
  }

  override def getDataTypeName: String = {
    PatientFfsClaimsExchange.dataTypeName
  }

  override def getDescriptor: Descriptors.Descriptor = {
    PatientFfsClaims.getDescriptor
  }

  override def getCid: String = {
    protos.head.getBase.getPatientMeta.getPatientId.getUuid
  }

  override def fromProto(protoBytes: java.lang.Iterable[Array[Byte]]): Unit = {
    val iterator = protoBytes.iterator()
    protos = iterator.asScala.map(PatientFfsClaims.parseFrom).toIterable
  }

  override def fromProtoStream(inputStreams: java.lang.Iterable[InputStream]): Unit = {
    val iterator = inputStreams.iterator()
    protos = iterator.asScala.map(PatientFfsClaims.parseFrom).toIterable
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

object PatientFfsClaimsExchange {
  val dataTypeName = "patientFfsClaims"
}
