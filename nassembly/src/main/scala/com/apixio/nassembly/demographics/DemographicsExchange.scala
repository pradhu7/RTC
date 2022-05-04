package com.apixio.nassembly.demographics

import com.apixio.datacatalog.SummaryObjects
import com.apixio.datacatalog.SummaryObjects.DemographicsSummary
import com.apixio.model.patient.Patient
import com.apixio.nassembly.patient.PatientSummaryExchangeBase
import com.google.protobuf.Descriptors

import java.io.InputStream
import java.lang
import scala.collection.JavaConversions.iterableAsScalaIterable

class DemographicsExchange extends PatientSummaryExchangeBase[DemographicsSummary] {

  private var protos: Iterable[DemographicsSummary] = Iterable.empty

  override def setProtos(summaries: Iterable[DemographicsSummary]): Unit = {
    protos = summaries
  }

  override def getProtos: Iterable[DemographicsSummary] = {
    protos
  }

  override def getDescriptor: Descriptors.Descriptor = DemographicsSummary.getDescriptor

  override def getDataTypeName: String = DemographicsExchange.dataTypeName

  override def fromProto(protoBytes: lang.Iterable[Array[Byte]]): Unit = {
    protos = protoBytes.map(DemographicsSummary.parseFrom)
  }

  override def fromProtoStream(inputStreams: lang.Iterable[InputStream]): Unit = {
    protos = inputStreams.map(DemographicsSummary.parseFrom)
  }

  override def toApo: java.lang.Iterable[Patient] = {
    throw new UnsupportedOperationException("Demographic to APO not supported")
  }


  override protected def getBase(proto: DemographicsSummary): SummaryObjects.CodedBaseSummary = {
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

object DemographicsExchange {
  val dataTypeName = "demographics"
}
