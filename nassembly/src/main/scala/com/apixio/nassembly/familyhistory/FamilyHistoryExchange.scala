package com.apixio.nassembly.familyhistory

import com.apixio.datacatalog.SummaryObjects
import com.apixio.datacatalog.SummaryObjects.FamilyHistorySummary
import com.apixio.nassembly.patient.PatientSummaryExchangeBase
import com.google.protobuf.Descriptors

import java.io.InputStream
import scala.collection.JavaConversions.iterableAsScalaIterable

class FamilyHistoryExchange extends PatientSummaryExchangeBase[FamilyHistorySummary] {

  private var protos: Iterable[FamilyHistorySummary] = Iterable.empty

  override def setProtos(summaries: Iterable[FamilyHistorySummary]): Unit = {
    protos = summaries
  }

  override def getProtos: Iterable[FamilyHistorySummary] = {
    protos
  }

  override def getDataTypeName: String = {
    FamilyHistoryExchange.dataTypeName
  }

  override def getDescriptor: Descriptors.Descriptor = {
    FamilyHistorySummary.getDescriptor
  }

  override def fromProto(protoBytes: java.lang.Iterable[Array[Byte]]): Unit = {
    protos = protoBytes.map(FamilyHistorySummary.parseFrom)
  }

  override def fromProtoStream(inputStreams: java.lang.Iterable[InputStream]): Unit = {
    protos = inputStreams.map(FamilyHistorySummary.parseFrom)
  }

  override protected def getBase(proto: FamilyHistorySummary): SummaryObjects.CodedBaseSummary = {
    proto.getBase
  }
}

object FamilyHistoryExchange {
  val dataTypeName: String = "familyHistory"
}



