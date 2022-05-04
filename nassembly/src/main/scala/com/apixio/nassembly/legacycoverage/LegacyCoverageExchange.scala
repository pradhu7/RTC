package com.apixio.nassembly.legacycoverage

import com.apixio.datacatalog.SummaryObjects
import com.apixio.datacatalog.SummaryObjects.CoverageSummary
import com.apixio.model.patient.Patient
import com.apixio.nassembly.patient.PatientSummaryExchangeBase
import com.google.protobuf.Descriptors

import java.io.InputStream
import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.collection.JavaConverters._

class LegacyCoverageExchange extends PatientSummaryExchangeBase[CoverageSummary] {

  private var protos: Iterable[CoverageSummary] = Iterable.empty

  override def setProtos(summaries: Iterable[CoverageSummary]): Unit = {
    protos = summaries
  }

  override def getProtos: Iterable[CoverageSummary] = {
    protos
  }

  override def getDataTypeName: String = {
    LegacyCoverageExchange.dataTypeName
  }

  override def getDescriptor: Descriptors.Descriptor = {
    CoverageSummary.getDescriptor
  }

  override def fromProto(protoBytes: java.lang.Iterable[Array[Byte]]): Unit = {
    protos = protoBytes.map(CoverageSummary.parseFrom)
  }

  override def fromProtoStream(inputStreams: java.lang.Iterable[InputStream]): Unit = {
    protos = inputStreams.map(CoverageSummary.parseFrom)
  }


  override def toApo: java.lang.Iterable[Patient] = {
    protos.map(c => LegacyCoverageUtils.toApo(List(c))).asJava
  }

  override protected def getBase(proto: CoverageSummary): SummaryObjects.CodedBaseSummary = {
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

object LegacyCoverageExchange {
  val dataTypeName = "legacyCoverage"
}


