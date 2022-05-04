package com.apixio.nassembly.patientimmunization

import com.apixio.datacatalog.SkinnyPatientProto.{PatientImmunization, PatientRaClaims}
import com.apixio.datacatalog.SummaryObjects.ImmunizationSummary
import com.apixio.model.nassembly.Exchange
import com.apixio.model.nassembly.Exchange.ProtoEnvelop
import com.apixio.model.patient.Patient
import com.apixio.nassembly.apo.APOGenerator
import com.google.protobuf.util.JsonFormat
import com.google.protobuf.{Descriptors, GeneratedMessageV3}

import java.io.InputStream
import scala.collection.JavaConverters._

class PatientImmunizationExchange extends Exchange {

  private var protos: Iterable[PatientImmunization] = _

  def getImmunizations: Iterable[PatientImmunization] = protos

  def buildWrapper(ImmunizationSummary: Iterable[ImmunizationSummary]): Unit = {
    protos = Iterable(ImmunizationUtils.wrapAsPatient(ImmunizationSummary.toList))
  }

  override def getDataTypeName: String = {
    PatientImmunizationExchange.dataTypeName
  }

  override def getDescriptor: Descriptors.Descriptor = {
    PatientImmunization.getDescriptor
  }

  override def getCid: String = {
    protos.head.getBase.getPatientMeta.getPatientId.getUuid
  }

  override def fromProto(protoBytes: java.lang.Iterable[Array[Byte]]): Unit = {
    val iterator = protoBytes.iterator()
    protos = iterator.asScala.map(PatientImmunization.parseFrom).toIterable
  }

  override def fromProtoStream(inputStreams: java.lang.Iterable[InputStream]): Unit = {
    val iterator = inputStreams.iterator()
    protos = iterator.asScala.map(PatientImmunization.parseFrom).toIterable
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

object PatientImmunizationExchange {
  val dataTypeName = "patientImmunization"
}