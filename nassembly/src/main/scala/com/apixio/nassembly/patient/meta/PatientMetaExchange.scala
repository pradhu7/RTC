package com.apixio.nassembly.patient.meta

import com.apixio.datacatalog.ExternalIdOuterClass
import com.apixio.datacatalog.PatientMetaProto.{PatientMeta => PatientMetaProto}
import com.apixio.model.nassembly.Exchange
import com.apixio.model.nassembly.Exchange.ProtoEnvelop
import com.apixio.nassembly.patient.PatientUtils
import com.google.protobuf.Descriptors
import com.google.protobuf.util.JsonFormat

import java.io.InputStream
import java.util.UUID
import scala.collection.JavaConverters._

class PatientMetaExchange extends Exchange {
  private var proto: PatientMetaProto = _

  // convenient methods to avoid serialization/deserialization
  def setProto(p: PatientMetaProto): Unit = proto = p
  def getProto: PatientMetaProto = proto

  override def getDataTypeName: String = {
    PatientMetaExchange.dataTypeName
  }

  override def getDescriptor: Descriptors.Descriptor = {
    PatientMetaProto.getDescriptor
  }

  override def getCid: String = {
    proto.getPatientId.getUuid
  }

  override def setIds(cid: UUID, primaryEid: Array[Byte]): Unit = {
     proto = PatientUtils.setIds(proto, cid, ExternalIdOuterClass.ExternalId.parseFrom(primaryEid))
  }
  override def fromProto(protoBytes: java.lang.Iterable[Array[Byte]]): Unit = {
    val iterator = protoBytes.iterator()
    if (iterator.hasNext){
      proto = PatientMetaProto.parseFrom(iterator.next())
    }
  }

  override def fromProtoStream(inputStreams: java.lang.Iterable[InputStream]): Unit = {
    val iterator = inputStreams.iterator()
    if (iterator.hasNext){
      proto = PatientMetaProto.parseFrom(iterator.next())
    }
  }

  override def getProtoEnvelops: java.lang.Iterable[ProtoEnvelop] = {
    val oid = ""
    val oidKey = "" // don't care too?
    Iterable(
      new ProtoEnvelop(oid,
        oidKey,
        proto.toByteArray,
        JsonFormat.printer().includingDefaultValueFields().omittingInsignificantWhitespace().print(proto))
    ).asJava
  }
}

object PatientMetaExchange {
  val dataTypeName = "patientMeta"
}
