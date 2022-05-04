package com.apixio.nassembly.patient

import com.apixio.datacatalog.PatientProto.{Patient => PatientProto}
import com.apixio.datacatalog.SkinnyPatientProto.BiometricValues
import com.apixio.model.nassembly.Exchange.ProtoEnvelop
import com.apixio.model.nassembly.{AssemblyContext, Exchange}
import com.apixio.model.patient.Patient
import com.apixio.nassembly.apo.APOGenerator
import com.apixio.nassembly.exchangeutils.ApoToProtoConverter
import com.google.protobuf.Descriptors
import com.google.protobuf.util.JsonFormat

import java.io.InputStream
import scala.collection.JavaConverters._


// This is used for reading data from New Assembly and converting back to an APO
class BackwardsCompatibleApoExchange extends Exchange {

  private var protos: Iterable[PatientProto] = _

  override def getDataTypeName: String = {
    BackwardsCompatibleApoExchange.dataTypeName
  }

  override def fromProto(protoBytes: java.lang.Iterable[Array[Byte]]): Unit = {
    val iterator = protoBytes.iterator()
    protos = iterator.asScala.map(PatientProto.parseFrom).toIterable
  }

  override def fromProtoStream(inputStreams: java.lang.Iterable[InputStream]): Unit = {
    val iterator = inputStreams.iterator()
    protos = iterator.asScala.map(PatientProto.parseFrom).toIterable
  }


  override def parse(apo: Patient, ac: AssemblyContext): Unit = {
    val converter = new ApoToProtoConverter()
    protos = Array(converter.convertAPO(apo, ac.pdsId()))
  }


  override def toApo: java.lang.Iterable[Patient] = {
    Iterable(APOGenerator.fromProto(MergeUtils.mergePatients(protos.toList, complexMerge = false))).asJava
  }

  /**
   * Get the Descriptor of the proto
   *
   * @return
   */
  override def getDescriptor: Descriptors.Descriptor = PatientProto.getDescriptor

  /**
   * Get the class id (group id) of the assembly exchange
   *
   * @return the class id (such as patient id)
   */
  override def getCid: String = {
    protos.head.getBase.getPatientMeta.getPatientId.getUuid
  }

  /**
   * Array of ProtoEnvelops
   *
   * @return
   */
  override def getProtoEnvelops: java.lang.Iterable[ProtoEnvelop] = {
    protos.map(proto => {
      val byteData = proto.toByteArray
      val oid = proto.getBase.getDataCatalogMeta.getOid
      val oidKey = "" // Means nothing for combined datatypes
      val jsonData = JsonFormat.printer().includingDefaultValueFields().omittingInsignificantWhitespace().print(proto)
      new ProtoEnvelop(oid, oidKey, byteData, jsonData)
    }).asJava
  }
}

object BackwardsCompatibleApoExchange {
  // Doesn't matter as this isn't used outside of backwards compatible APIs
  val dataTypeName = "backwardsCompatibleApo444"
}