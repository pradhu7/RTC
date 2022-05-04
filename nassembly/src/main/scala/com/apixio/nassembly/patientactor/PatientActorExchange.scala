package com.apixio.nassembly.patientactor

import com.apixio.datacatalog.SummaryObjects
import com.apixio.datacatalog.SummaryObjects.PatientClinicalActorSummary
import com.apixio.model.patient.Patient
import com.apixio.nassembly.apo.APOGenerator
import com.apixio.nassembly.patient.PatientSummaryExchangeBase
import com.apixio.util.nassembly.SummaryUtils
import com.google.protobuf.Descriptors

import java.io.InputStream
import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.collection.JavaConverters._

class PatientActorExchange extends PatientSummaryExchangeBase[PatientClinicalActorSummary] {

  private var protos: Iterable[PatientClinicalActorSummary] = Iterable.empty

  def getProtos: Iterable[PatientClinicalActorSummary] = protos

  def setProtos(actors: Iterable[PatientClinicalActorSummary]): Unit = {
    protos = actors
  }

  override protected def getBase(proto: PatientClinicalActorSummary): SummaryObjects.CodedBaseSummary = {
    val base = proto.getBase
    // no encounters or clinical actors
    SummaryObjects.CodedBaseSummary.newBuilder()
      .setDataCatalogMeta(base.getDataCatalogMeta)
      .setPatientMeta(base.getPatientMeta)
      .addAllSources(base.getSourcesList)
      .addAllParsingDetails(base.getParsingDetailsList)
      .setPrimaryActor(SummaryUtils.normalizePatientClinicalActor(proto))
      .build()
  }


  override def getDataTypeName: String = {
    PatientActorExchange.dataTypeName
  }

  override def getDescriptor: Descriptors.Descriptor = {
    PatientClinicalActorSummary.getDescriptor
  }

  override def fromProto(protoBytes: java.lang.Iterable[Array[Byte]]): Unit = {
    protos = protoBytes.map(PatientClinicalActorSummary.parseFrom)
  }

  override def fromProtoStream(inputStreams: java.lang.Iterable[InputStream]): Unit = {
    protos = inputStreams.map(PatientClinicalActorSummary.parseFrom)
  }


  override def toApo: java.lang.Iterable[Patient] = {
    protos.map(actor => APOGenerator.fromSkinnyActor(PatientActorUtils.wrapAsPatient(Seq(actor)))).asJava
  }

}

object PatientActorExchange {
  val dataTypeName = "patientClinicalActor"
}