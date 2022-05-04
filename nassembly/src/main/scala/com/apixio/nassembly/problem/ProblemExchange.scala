package com.apixio.nassembly.problem

import com.apixio.datacatalog.SummaryObjects
import com.apixio.datacatalog.SummaryObjects.ProblemSummary
import com.apixio.model.patient.Patient
import com.apixio.nassembly.patient.PatientSummaryExchangeBase
import com.google.protobuf.Descriptors

import java.io.InputStream
import scala.collection.JavaConversions.iterableAsScalaIterable

class ProblemExchange extends PatientSummaryExchangeBase[ProblemSummary] {

  private var protos: Iterable[ProblemSummary] = Iterable.empty

  override def setProtos(summaries: Iterable[ProblemSummary]): Unit = {
    protos = summaries
  }

  override def getProtos: Iterable[ProblemSummary] = {
    protos
  }

  override def getDataTypeName: String = {
    ProblemExchange.dataTypeName
  }

  override def getDescriptor: Descriptors.Descriptor = {
    ProblemSummary.getDescriptor
  }

  override def fromProto(protoBytes: java.lang.Iterable[Array[Byte]]): Unit = {
    protos = protoBytes.map(ProblemSummary.parseFrom)
  }

  override def fromProtoStream(inputStreams: java.lang.Iterable[InputStream]): Unit = {
    protos = inputStreams.map(ProblemSummary.parseFrom)
  }

  override protected def getBase(proto: ProblemSummary): SummaryObjects.CodedBaseSummary = {
    proto.getBase
  }

  override def toApo: java.lang.Iterable[Patient] = {
    throw new UnsupportedOperationException("problem to APO not supported")
  }

}

object ProblemExchange {
  val dataTypeName = "problem"
}


