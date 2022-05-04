package com.apixio.nassembly.documentmeta

import com.apixio.datacatalog.SummaryObjects.DocumentMetaSummary
import com.apixio.model.nassembly.Exchange
import com.apixio.model.nassembly.Exchange.ProtoEnvelop
import com.apixio.model.patient.Patient
import com.apixio.nassembly.exchangeutils.EidUtils
import com.apixio.nassembly.patient.PatientUtils
import com.google.protobuf.Descriptors
import com.google.protobuf.util.JsonFormat

import java.io.InputStream
import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.collection.JavaConverters._

class DocumentMetaExchange extends Exchange {

  private var protos: Iterable[DocumentMetaSummary] = Iterable.empty

  def getDocuments: Iterable[DocumentMetaSummary] = protos

  def setDocuments(docs: Iterable[DocumentMetaSummary]): Unit = {
    protos = docs
  }

  override def getDataTypeName: String = {
    DocumentMetaExchange.dataTypeName
  }

  override def getDescriptor: Descriptors.Descriptor = {
    DocumentMetaSummary.getDescriptor
  }

  override def getPrimaryEid: String = {
    EidUtils.getPatientKey(protos.iterator.next.getBase.getPatientMeta)
  }

  override def getCid: String = {
    protos.iterator.next.getBase.getPatientMeta.getPatientId.getUuid
  }

  override def fromProto(protoBytes: java.lang.Iterable[Array[Byte]]): Unit = {
    protos = protoBytes.map(DocumentMetaSummary.parseFrom)
  }

  override def fromProtoStream(inputStreams: java.lang.Iterable[InputStream]): Unit = {
    protos = inputStreams.map(DocumentMetaSummary.parseFrom)
  }

  override def getProtoEnvelops: java.lang.Iterable[ProtoEnvelop] = {
    protos.map(doc => {
      val oid = doc.getBase.getDataCatalogMeta.getOid
      val oidKey = PatientUtils.getDocumentKey(doc)
      new ProtoEnvelop(oid,
        oidKey,
        doc.toByteArray,
        JsonFormat.printer().includingDefaultValueFields().omittingInsignificantWhitespace().print(doc))
    }).asJava
  }

  override def toApo: java.lang.Iterable[Patient] = {
    // Re merge until we clean up old data
    Iterable(DocumentMetaUtils.toApo(DocumentMetaUtils.merge(protos.toList))).asJava
  }
}

object DocumentMetaExchange {
  val dataTypeName = "documentMeta"
}