package com.apixio.nassembly.allergy

import com.apixio.datacatalog.SummaryObjects
import com.apixio.datacatalog.SummaryObjects.AllergySummary
import com.apixio.model.patient.Patient
import com.apixio.nassembly.patient.PatientSummaryExchangeBase
import com.google.protobuf.Descriptors

import java.io.InputStream
import scala.collection.JavaConversions.iterableAsScalaIterable

class AllergyExchange extends PatientSummaryExchangeBase[AllergySummary] {

  private var protos: Iterable[AllergySummary] = Iterable.empty

  override def setProtos(summaries: Iterable[AllergySummary]): Unit = {
    protos = summaries
  }

  override def getProtos: Iterable[AllergySummary] = {
    protos
  }

  override def getDataTypeName: String = {
    AllergyExchange.dataTypeName
  }

  override def getDescriptor: Descriptors.Descriptor = {
    AllergySummary.getDescriptor
  }

  override def fromProto(protoBytes: java.lang.Iterable[Array[Byte]]): Unit = {
    protos = protoBytes.map(AllergySummary.parseFrom)
  }

  override def fromProtoStream(inputStreams: java.lang.Iterable[InputStream]): Unit = {
    protos = inputStreams.map(AllergySummary.parseFrom)
  }

  override def toApo: java.lang.Iterable[Patient] = {
    throw new UnsupportedOperationException("Allergy to APO not supported")
  }

  override protected def getBase(proto: AllergySummary): SummaryObjects.CodedBaseSummary = {
    proto.getBase
  }
}

object AllergyExchange {
  val dataTypeName = "allergy"
}