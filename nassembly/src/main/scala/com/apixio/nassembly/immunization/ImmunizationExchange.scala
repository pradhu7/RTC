package com.apixio.nassembly.immunization

import com.apixio.datacatalog.SummaryObjects
import com.apixio.datacatalog.SummaryObjects.ImmunizationSummary
import com.apixio.model.patient.Patient
import com.apixio.nassembly.apo.APOGenerator
import com.apixio.nassembly.patient.PatientSummaryExchangeBase
import com.apixio.nassembly.patientimmunization.PatientImmunizationCombiner
import com.google.protobuf.{Descriptors, GeneratedMessageV3}

import java.io.InputStream
import scala.collection.JavaConversions.{iterableAsScalaIterable, mapAsJavaMap}
import scala.collection.JavaConverters._

class ImmunizationExchange extends PatientSummaryExchangeBase[ImmunizationSummary] {

  private var protos: Iterable[ImmunizationSummary] = Iterable.empty

  override def setProtos(summaries: Iterable[ImmunizationSummary]): Unit = {
    protos = summaries
  }

  override def getProtos: Iterable[ImmunizationSummary] = {
    protos
  }

  override def getDataTypeName: String = {
    ImmunizationExchange.dataTypeName
  }

  override def getDescriptor: Descriptors.Descriptor = {
    ImmunizationSummary.getDescriptor
  }

  override def fromProto(protoBytes: java.lang.Iterable[Array[Byte]]): Unit = {
    protos = protoBytes.map(ImmunizationSummary.parseFrom)
  }

  override def fromProtoStream(inputStreams: java.lang.Iterable[InputStream]): Unit = {
    protos = inputStreams.map(ImmunizationSummary.parseFrom)
  }

  override protected def getBase(proto: ImmunizationSummary): SummaryObjects.CodedBaseSummary = {
    proto.getBase
  }
}


object ImmunizationExchange {
  val dataTypeName: String = "immunization"
}



