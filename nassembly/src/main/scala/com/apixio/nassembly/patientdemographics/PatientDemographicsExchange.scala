package com.apixio.nassembly.patientdemographics

import com.apixio.datacatalog.BaseObjects
import com.apixio.datacatalog.SkinnyPatientProto.Demographics
import com.apixio.datacatalog.SummaryObjects.DemographicsSummary
import com.apixio.model.nassembly.Exchange
import com.apixio.model.nassembly.Exchange.ProtoEnvelop
import com.apixio.model.patient.Patient
import com.apixio.nassembly.apo.APOGenerator
import com.apixio.nassembly.demographics.DemographicsUtils
import com.google.protobuf.util.JsonFormat
import com.google.protobuf.{Descriptors, GeneratedMessageV3}

import java.io.InputStream
import scala.collection.JavaConverters._

class PatientDemographicsExchange extends Exchange {

  private var protos: Iterable[Demographics] = _

  /**
   * Builds a wrapper around SkinnyPatientProto.Demographics to be used to set private 'protos' array.
   * @param demographics demographics
   * @param contactDetails normalized & merged contact details
   */
  def buildDemographicsWrapper(demographics: List[DemographicsSummary], contactDetails: List[BaseObjects.ContactDetails]): Unit = {
    protos = {
      DemographicsUtils.wrapAsSkinnyPatient(demographics, contactDetails) match {
        case Some(skinnyPatient) =>
          Array(skinnyPatient)
        case None =>
          Array.empty[Demographics]
      }
    }
  }

  override def getDescriptor: Descriptors.Descriptor = Demographics.getDescriptor

  override def getCid: String = protos.head.getBasePatient.getPatientMeta.getPatientId.getUuid

  override def fromProto(protoBytes: java.lang.Iterable[Array[Byte]]): Unit = {
    val iterator = protoBytes.iterator()
    protos = iterator.asScala.map(Demographics.parseFrom).toIterable
  }

  override def fromProtoStream(inputStreams: java.lang.Iterable[InputStream]): Unit = {
    val iterator = inputStreams.iterator()
    protos = iterator.asScala.map(Demographics.parseFrom).toIterable
  }

  override def getProtoEnvelops: java.lang.Iterable[Exchange.ProtoEnvelop] = {
    protos.map(proto => {
      val byteData = proto.toByteArray
      val oid = ""
      val oidKey = "" // Means nothing for combined datatypes
      val jsonData = JsonFormat.printer().includingDefaultValueFields().omittingInsignificantWhitespace().print(proto)
      new ProtoEnvelop(oid, oidKey, byteData, jsonData)
    }).asJava
  }

  override def toApo: java.lang.Iterable[Patient] = {
    Iterable(APOGenerator.mergeSkinny(protos.map(_.asInstanceOf[GeneratedMessageV3]).toList.asJava)).asJava
  }

  override def getDataTypeName: String = PatientDemographicsExchange.dataTypeName
}

object PatientDemographicsExchange {
  val dataTypeName = "patientDemographics"
}
