package com.apixio.nassembly.clinicalactor

import com.apixio.datacatalog.SummaryObjects.ClinicalActorSummary
import com.apixio.model.nassembly.Exchange
import com.apixio.model.nassembly.Exchange.ProtoEnvelop
import com.apixio.nassembly.patient.PatientUtils
import com.apixio.util.nassembly.CaretParser
import com.google.protobuf.Descriptors
import com.google.protobuf.util.JsonFormat

import java.io.InputStream
import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.collection.JavaConverters._

class ClinicalActorExchange extends Exchange {

  private var protos: Iterable[ClinicalActorSummary] = Iterable.empty

  def getActors: Iterable[ClinicalActorSummary] = protos

  def setActors(actors: Iterable[ClinicalActorSummary]): Unit = {
    protos = actors
  }

  override def getDataTypeName: String = {
    ClinicalActorExchange.dataTypeName
  }

  override def getDomainName: String = {
    ClinicalActorExchange.domainName
  }

  override def getDescriptor: Descriptors.Descriptor = {
    ClinicalActorSummary.getDescriptor
  }

  override def getCid: String = {
    val raw = CaretParser.toString(protos.head.getClinicalActorInfo.getPrimaryId)
    if (raw.isEmpty) {
      CaretParser.toString(protos.head.getDataCatalogMeta.getOriginalId)
    } else {
      raw
    }
  }

  override def fromProto(protoBytes: java.lang.Iterable[Array[Byte]]): Unit = {
    protos = protoBytes.map(ClinicalActorSummary.parseFrom)
  }

  override def fromProtoStream(inputStreams: java.lang.Iterable[InputStream]): Unit = {
    protos = inputStreams.map(ClinicalActorSummary.parseFrom)
  }

  override def getProtoEnvelops: java.lang.Iterable[ProtoEnvelop] = {
    protos.map(actor => {
      val parsingDetailsList = actor.getParsingDetailsList
      val oid = actor.getDataCatalogMeta.getOid
      val oidKey = PatientUtils.getOidKeyFromParsingDetailsList(parsingDetailsList)

      new ProtoEnvelop(oid,
        oidKey,
        actor.toByteArray,
        JsonFormat.printer().includingDefaultValueFields().omittingInsignificantWhitespace().print(actor))
    }).asJava
  }
}

object ClinicalActorExchange {
  val dataTypeName = "clinicalActor"
  val domainName = "npiActor"
}