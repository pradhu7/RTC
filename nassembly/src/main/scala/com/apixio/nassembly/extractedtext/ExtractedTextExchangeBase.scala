package com.apixio.nassembly.extractedtext

import com.apixio.datacatalog.ExtractedTextOuterClass.ExtractedText
import com.apixio.model.nassembly.Exchange
import com.apixio.model.nassembly.Exchange.ProtoEnvelop
import com.apixio.nassembly.exchangeutils.EidUtils
import com.google.protobuf.Descriptors
import com.google.protobuf.util.JsonFormat

import java.io.InputStream
import scala.collection.JavaConverters._

trait ExtractedTextExchangeBase extends Exchange {

  protected var proto: ExtractedText = _

  def getContent: ExtractedText = proto

  def setContent(content: ExtractedText): Unit = {
    proto = content
  }

  override def getDescriptor: Descriptors.Descriptor = {
    ExtractedText.getDescriptor
  }

  override def getCid: String = {
    proto.getPatientMeta.getPatientId.getUuid
  }

  override def getPrimaryEid: String = {
    EidUtils.getPatientKey(proto.getPatientMeta)
  }

  override def fromProto(protoBytes: java.lang.Iterable[Array[Byte]]): Unit = {
    proto = ExtractedText.parseFrom(protoBytes.asScala.head)
  }

  override def fromProtoStream(inputStreams: java.lang.Iterable[InputStream]): Unit = {
    proto = ExtractedText.parseFrom(inputStreams.asScala.head)
  }

  override def getExternalIds: Array[Array[Byte]] = {
    proto.getPatientMeta.getExternalIdsList
      .asScala
      .filterNot(_.getSerializedSize == 0)
      .map(_.toByteArray)
      .toArray
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
