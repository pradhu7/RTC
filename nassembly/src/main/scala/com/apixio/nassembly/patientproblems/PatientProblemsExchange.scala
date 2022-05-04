package com.apixio.nassembly.patientproblems

import com.apixio.datacatalog.SkinnyPatientProto.PatientProblems
import com.apixio.datacatalog.SummaryObjects.ProblemSummary
import com.apixio.model.nassembly.Exchange
import com.apixio.model.nassembly.Exchange.ProtoEnvelop
import com.apixio.model.patient.Patient
import com.apixio.nassembly.apo.APOGenerator
import com.apixio.nassembly.problem.ProblemUtils
import com.google.protobuf.util.JsonFormat
import com.google.protobuf.{Descriptors, GeneratedMessageV3}

import java.io.InputStream
import scala.collection.JavaConverters._

class PatientProblemsExchange extends Exchange {

  private var protos: Iterable[PatientProblems] = _

  def getProblems: Iterable[PatientProblems] = protos

  def buildSkinnyPatient(dateBucket: String, procedures : Iterable[ProblemSummary]): Unit = {
    protos = Iterable(ProblemUtils.wrapAsPatient(dateBucket, procedures.toList))
  }

  override def getDataTypeName: String = {
    PatientProblemsExchange.dataTypeName
  }

  override def getDescriptor: Descriptors.Descriptor = {
    PatientProblems.getDescriptor
  }

  override def getCid: String = {
    protos.head.getBase.getPatientMeta.getPatientId.getUuid
  }

  override def fromProto(protoBytes: java.lang.Iterable[Array[Byte]]): Unit = {
    val iterator = protoBytes.iterator()
    protos = iterator.asScala.map(PatientProblems.parseFrom).toIterable
  }

  override def fromProtoStream(inputStreams: java.lang.Iterable[InputStream]): Unit = {
    val iterator = inputStreams.iterator()
    protos = iterator.asScala.map(PatientProblems.parseFrom).toIterable
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

object PatientProblemsExchange {
  val dataTypeName = "patientProblems"
}


