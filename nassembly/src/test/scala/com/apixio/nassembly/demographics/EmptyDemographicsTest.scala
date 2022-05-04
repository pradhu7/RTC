package com.apixio.nassembly.demographics

import com.apixio.datacatalog.ExternalIdOuterClass.ExternalId
import com.apixio.datacatalog.PatientMetaProto.PatientMeta
import com.apixio.datacatalog.UUIDOuterClass.UUID
import org.junit.runner.RunWith
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should._
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class EmptyDemographicsTest extends AnyFlatSpec with Matchers {


  "Demographics" should "check for alternate Ids" in {
    val uuid = UUID.newBuilder().setUuid(java.util.UUID.randomUUID().toString).build()
    val primaryId = ExternalId.newBuilder().setId("10").setAssignAuthority("Selecao").build()
    val alternateIds = ExternalId.newBuilder().setId("10").setAssignAuthority("Blaugrana").build()

    val patientMetaWithIds = PatientMeta.newBuilder()
      .setPatientId(uuid)
      .setPrimaryExternalId(primaryId)
      .addExternalIds(primaryId)
      .addExternalIds(alternateIds)
      .build()

    assert(!EmptyDemographicUtil.isEmpty(patientMetaWithIds))

    val patientMetaNoIds = PatientMeta.newBuilder()
        .setPatientId(uuid)
        .setPrimaryExternalId(primaryId)
        .addExternalIds(primaryId)
        .build()

    assert(EmptyDemographicUtil.isEmpty(patientMetaNoIds))
  }




}