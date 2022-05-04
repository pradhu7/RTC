package com.apixio.nassembly.patientlabresults

import com.apixio.datacatalog.SkinnyPatientProto.PatientLabResult
import com.apixio.datacatalog.SummaryObjects.LabResultSummary
import com.apixio.model.nassembly.Exchange
import com.apixio.model.nassembly.Exchange.ProtoEnvelop
import com.apixio.model.patient.Patient
import com.apixio.nassembly.apo.APOGenerator
import com.apixio.nassembly.labresult.LabResultUtils
import com.google.protobuf.util.JsonFormat
import com.google.protobuf.{Descriptors, GeneratedMessageV3}

import java.io.InputStream
import scala.collection.JavaConverters._

class PatientLabResultsExchange extends Exchange {

  private var protos: Iterable[PatientLabResult] = _

  def getLabResults: Iterable[PatientLabResult] = protos

  def buildWrapper(labResultSummary: Iterable[LabResultSummary]): Unit = {
    protos = Iterable(LabResultUtils.wrapAsPatient(labResultSummary.toList))
  }

  override def getDataTypeName: String = {
    PatientLabResultsExchange.dataTypeName
  }

  override def getDescriptor: Descriptors.Descriptor = {
    PatientLabResult.getDescriptor
  }

  override def getCid: String = {
    protos.head.getBase.getPatientMeta.getPatientId.getUuid
  }

  override def fromProto(protoBytes: java.lang.Iterable[Array[Byte]]): Unit = {
    val iterator = protoBytes.iterator()
    protos = iterator.asScala.map(PatientLabResult.parseFrom).toIterable
  }

  override def fromProtoStream(inputStreams: java.lang.Iterable[InputStream]): Unit = {
    val iterator = inputStreams.iterator()
    protos = iterator.asScala.map(PatientLabResult.parseFrom).toIterable
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

object PatientLabResultsExchange {
  val dataTypeName = "patientLabResults"
}