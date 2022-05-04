package com.apixio.nassembly.patient.partial

import com.apixio.datacatalog.PatientProto
import com.apixio.model.nassembly.Exchange
import com.apixio.model.nassembly.Exchange.ProtoEnvelop
import com.apixio.model.patient.Patient
import com.apixio.nassembly.apo.APOGenerator
import com.apixio.nassembly.patient.{PatientUtils, SeparatorUtils}
import com.google.protobuf.Descriptors
import com.google.protobuf.util.JsonFormat

import java.io.InputStream

import scala.collection.JavaConverters._

class AllExchange extends Exchange {

  private var protos: Iterable[PatientProto.Patient] = _

  override def getDataTypeName: String = {
    AllExchange.dataTypeName
  }

  override def getDescriptor: Descriptors.Descriptor = {
    PatientProto.Patient.getDescriptor
  }

  override def fromProto(protoBytes: java.lang.Iterable[Array[Byte]]): Unit = {
    val iterator = protoBytes.iterator()
    protos = iterator.asScala.map(PatientProto.Patient.parseFrom).toIterable
  }

  override def fromProtoStream(inputStreams: java.lang.Iterable[InputStream]): Unit = {
    val iterator = inputStreams.iterator()
    protos = iterator.asScala.map(PatientProto.Patient.parseFrom).toIterable
  }

  override def getProtoEnvelops: java.lang.Iterable[ProtoEnvelop] = {
    protos.map(proto => {
      val byteData = proto.toByteArray
      val parsingDetails = proto.getBase.getParsingDetailsList
      val oid = PatientUtils.getOidFromParsingDetailsList(parsingDetails)
      val oidKey = SeparatorUtils.separateDocuments(proto).headOption match {
        case Some(doc) => PatientUtils.getDocumentKey(doc)
        case None => PatientUtils.getOidKeyFromParsingDetailsList(parsingDetails)
      }

      val jsonData = JsonFormat.printer().includingDefaultValueFields().omittingInsignificantWhitespace().print(proto)
      new ProtoEnvelop(oid, oidKey, byteData, jsonData)
    }).asJava
  }

  /*
  This method just converts to apos and doesn't not merger them. That is important.
   */
  override def toApo: java.lang.Iterable[Patient] = {
    val apos: Iterable[Patient] = protos.map(proto => APOGenerator.fromProto(hackProto(proto)))
    apos.asJava
  }

  // Remove new data when it comes to make merge easier for science until they refactor
  // This is done because the partial patient should not be used for science
  private def hackProto(patientProto: PatientProto.Patient): PatientProto.Patient = {
    patientProto
      .toBuilder
      .clearPrimaryDemographics()
      .clearAlternateDemographics()
      .clearCoverage()
      .clearFamilyHistories()
      .clearImmunizations()
      .clearAllergies()
      .clearExtractedText()
      .build()
  }

  /**
   * Get the patientId
   * Return null if none exists
   */
  override def getCid: String = {
    protos.find(_.getBase.hasPatientMeta)
      .map(_.getBase.getPatientMeta.getPatientId.getUuid).orNull
  }
}

object AllExchange {
  val dataTypeName = "all"
}
