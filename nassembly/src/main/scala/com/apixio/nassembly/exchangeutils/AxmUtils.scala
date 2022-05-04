package com.apixio.nassembly.exchangeutils

import com.apixio.datacatalog.ClinicalCodeOuterClass
import com.apixio.datacatalog.ExternalIdOuterClass.ExternalId
import com.apixio.nassembly.util.DataConstants

object AxmUtils extends Serializable {

  def createExternalId(raw: String, separator: String = DataConstants.SEP_CARET): ExternalId = {
    val rawExternalIdParts = raw.split(separator)
    val externalIdBuilder = ExternalId.newBuilder()

    rawExternalIdParts.length match {
      case 1 =>
        assignAuthorityOfExternalId(0)
      case 3 =>
        setIdOfExternalId(0)
        //index 1 is ignored based on CaretParser logic for historic reasons
        assignAuthorityOfExternalId(2)
      case _ => throw new IllegalArgumentException("Invalid ExternalID")
    }

    def assignAuthorityOfExternalId(index: Int): Unit = {
      val authorityId = rawExternalIdParts(index).trim
      if (authorityId.isEmpty) {
        throw new IllegalArgumentException("ExternalID.assignAuthorityOfExternalId is empty")
      }
      externalIdBuilder.setAssignAuthority(rawExternalIdParts(index).trim)
    }

    def setIdOfExternalId(index: Int): Unit = {
      val id = rawExternalIdParts(index).trim
      if (id.isEmpty) {
        throw new IllegalArgumentException("ExternalID.id is empty")
      }
      externalIdBuilder.setId(id)
    }

    externalIdBuilder.build()
  }

  // Axm related
  def createCode(raw: String, separator: String = DataConstants.SEP_CARET): Option[ClinicalCodeOuterClass.ClinicalCode] = {
    val parts = raw.split(separator)
    if (parts.size == 4) {
      Some(createClinicalCode(parts(0), parts(1), parts(2), parts(3)))
    } else {
      None
    }
  }

  // AXM related
  private def createClinicalCode(name: String, code: String, system: String, systemOid: String): ClinicalCodeOuterClass.ClinicalCode = {
    ClinicalCodeOuterClass.ClinicalCode.newBuilder()
      .setDisplayName(name)
      .setCode(code)
      .setSystem(system)
      .setSystemOid(systemOid)
      .build()
  }

}
