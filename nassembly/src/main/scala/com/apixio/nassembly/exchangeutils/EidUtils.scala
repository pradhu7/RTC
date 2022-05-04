package com.apixio.nassembly.exchangeutils

import com.apixio.datacatalog.ExternalIdOuterClass.ExternalId
import com.apixio.datacatalog.PatientMetaProto.{PatientMeta => PatientMetaProto}
import com.apixio.util.nassembly.DataCatalogProtoUtils

import java.util.UUID

object EidUtils {

  def getPatientKey(patientMeta: PatientMetaProto): String = {
    val primaryEid = patientMeta.getPrimaryExternalId
    patientKey(primaryEid)
  }

  def patientKey(eId: ExternalId): String = {
    val typeStr = "null" // THIS IS A MUST FOR BACKWARD COMPATIBILITY
    val buff = new StringBuffer("").append(eId.getAssignAuthority).append(eId.getId).append(typeStr)
    buff.toString
  }

  def patientKeyToExternalId(patientKey: String, primaryAssignAuthority: String): ExternalId = {
    val id = patientKey
      .replace("null", "")
      .replace(primaryAssignAuthority, "")

    ExternalId.newBuilder()
      .setAssignAuthority(primaryAssignAuthority)
      .setId(id)
      .build()
  }

  def bytesToExternalId(bytes: Array[Byte]): ExternalId = {
    ExternalId.parseFrom(bytes)
  }

  def updatePatientMetaIds(patientMetaProto: PatientMetaProto, cid: UUID, eidBytes: Array[Byte]): PatientMetaProto = {
    val eid = bytesToExternalId(eidBytes)
    patientMetaProto.toBuilder
      .setPatientId(DataCatalogProtoUtils.convertUuid(cid))
      .setPrimaryExternalId(eid)
      .build()
  }
}
