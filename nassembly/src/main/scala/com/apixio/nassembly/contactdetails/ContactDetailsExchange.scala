package com.apixio.nassembly.contactdetails

import com.apixio.datacatalog.SummaryObjects
import com.apixio.datacatalog.SummaryObjects.ContactDetailsSummary
import com.apixio.model.patient.Patient
import com.apixio.nassembly.patient.PatientSummaryExchangeBase
import com.google.protobuf.Descriptors

import java.io.InputStream
import scala.collection.JavaConversions.iterableAsScalaIterable

class ContactDetailsExchange extends PatientSummaryExchangeBase[ContactDetailsSummary] {

  private var protos: Iterable[ContactDetailsSummary] = Iterable.empty

  override def setProtos(summaries: Iterable[ContactDetailsSummary]): Unit = {
    protos = summaries
  }

  override def getProtos: Iterable[ContactDetailsSummary] = {
    protos
  }

  override def getDataTypeName: String = {
    ContactDetailsExchange.dataTypeName
  }

  override def getDescriptor: Descriptors.Descriptor = {
    ContactDetailsSummary.getDescriptor
  }

  override def fromProto(protoBytes: java.lang.Iterable[Array[Byte]]): Unit = {
    protos = protoBytes.map(ContactDetailsSummary.parseFrom)
  }

  override def fromProtoStream(inputStreams: java.lang.Iterable[InputStream]): Unit = {
    protos = inputStreams.map(ContactDetailsSummary.parseFrom)
  }

  override def toApo: java.lang.Iterable[Patient] = {
    throw new UnsupportedOperationException("Contact Details to APO not supported")
  }

  override protected def getBase(proto: ContactDetailsSummary): SummaryObjects.CodedBaseSummary = {
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

object ContactDetailsExchange {
  val dataTypeName = "patientContactInfo"
}


