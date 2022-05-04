package com.apixio.nassembly.cleantext

import com.apixio.datacatalog.CleanTextOuterClass.CleanText
import com.apixio.model.nassembly.Exchange
import com.apixio.model.nassembly.Exchange.ProtoEnvelop
import com.google.protobuf.Descriptors
import com.google.protobuf.util.JsonFormat

import java.io.InputStream
import scala.collection.JavaConverters._


trait CleanTextExchangeBase extends Exchange {
  private var proto: CleanText = _

  def getCleanText: CleanText = proto

  def setCleanText(cleanText: CleanText): Unit = {
    proto = cleanText
  }

  override def getDescriptor: Descriptors.Descriptor = {
    CleanText.getDescriptor
  }

  override def getCid: String = {
    proto.getPatientMeta.getPatientId.getUuid
  }

  override def fromProto(protoBytes: java.lang.Iterable[Array[Byte]]): Unit = {
    proto = CleanText.parseFrom(protoBytes.asScala.head)
  }

  override def fromProtoStream(inputStreams: java.lang.Iterable[InputStream]): Unit = {
    proto = CleanText.parseFrom(inputStreams.asScala.head)
  }

  override def getProtoEnvelops: java.lang.Iterable[ProtoEnvelop] = {
    val oid = proto.getDocumentId.getUuid
    val oidKey = ""
    Iterable(new ProtoEnvelop(oid,
      oidKey,
      proto.toByteArray,
      JsonFormat.printer().includingDefaultValueFields().omittingInsignificantWhitespace().print(proto))
    ).asJava
  }
}
