package com.apixio.nassembly.encounter

import com.apixio.datacatalog.SummaryObjects
import com.apixio.datacatalog.SummaryObjects.EncounterSummary
import com.apixio.model.patient.Patient
import com.apixio.nassembly.patient.PatientSummaryExchangeBase
import com.google.protobuf.Descriptors

import java.io.InputStream
import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.collection.JavaConverters._

class EncounterExchange extends PatientSummaryExchangeBase[EncounterSummary] {

  private var protos: Iterable[EncounterSummary] = Iterable.empty

  override def setProtos(labResults: Iterable[EncounterSummary]): Unit = {
    protos = labResults
  }
  override def getProtos: Iterable[EncounterSummary] = protos

  override def getDataTypeName: String = {
    EncounterExchange.dataTypeName
  }

  override def getDescriptor: Descriptors.Descriptor = {
    EncounterSummary.getDescriptor
  }

  override def fromProto(protoBytes: java.lang.Iterable[Array[Byte]]): Unit = {
    protos = protoBytes.map(EncounterSummary.parseFrom)
  }

  override def fromProtoStream(inputStreams: java.lang.Iterable[InputStream]): Unit = {
    protos = inputStreams.map(EncounterSummary.parseFrom)
  }


  override def toApo: java.lang.Iterable[Patient] = {
    Iterable(EncounterUtils.toApo(protos.toList)).asJava
  }

  override protected def getBase(proto: EncounterSummary): SummaryObjects.CodedBaseSummary = {
    val base = proto.getBase
    // no encounters or clinical actors
    SummaryObjects.CodedBaseSummary.newBuilder()
      .setDataCatalogMeta(base.getDataCatalogMeta)
      .setPatientMeta(base.getPatientMeta)
      .addAllSources(base.getSourcesList)
      .addAllParsingDetails(base.getParsingDetailsList)
      .build()
  }
}

object EncounterExchange {
  val dataTypeName = "encounter"
}