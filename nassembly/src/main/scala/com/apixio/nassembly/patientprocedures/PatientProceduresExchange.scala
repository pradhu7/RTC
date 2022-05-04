package com.apixio.nassembly.patientprocedures

import com.apixio.datacatalog.SkinnyPatientProto.PatientProcedure
import com.apixio.datacatalog.SummaryObjects.ProcedureSummary
import com.apixio.model.nassembly.Exchange
import com.apixio.model.nassembly.Exchange.ProtoEnvelop
import com.apixio.model.patient.Patient
import com.apixio.nassembly.apo.APOGenerator
import com.apixio.nassembly.procedure.ProcedureUtils
import com.google.protobuf.util.JsonFormat
import com.google.protobuf.{Descriptors, GeneratedMessageV3}

import java.io.InputStream
import scala.collection.JavaConverters._

class PatientProceduresExchange extends Exchange {

  private var protos: Iterable[PatientProcedure] = _

  def getProcedures: Iterable[PatientProcedure] = protos

  def buildSkinnyPatient(dateBucket: String, procedures : Iterable[ProcedureSummary]): Unit = {
    protos = Iterable(ProcedureUtils.wrapAsPatient(dateBucket, procedures.toList))
  }

  override def getDataTypeName: String = {
    PatientProceduresExchange.dataTypeName
  }

  override def getDescriptor: Descriptors.Descriptor = {
    PatientProcedure.getDescriptor
  }

  override def getCid: String = {
    protos.head.getBase.getPatientMeta.getPatientId.getUuid
  }

  override def fromProto(protoBytes: java.lang.Iterable[Array[Byte]]): Unit = {
    val iterator = protoBytes.iterator()
    protos = iterator.asScala.map(PatientProcedure.parseFrom).toIterable
  }

  override def fromProtoStream(inputStreams: java.lang.Iterable[InputStream]): Unit = {
    val iterator = inputStreams.iterator()
    protos = iterator.asScala.map(PatientProcedure.parseFrom).toIterable
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

object PatientProceduresExchange {
  val dataTypeName = "patientProcedures"
}
