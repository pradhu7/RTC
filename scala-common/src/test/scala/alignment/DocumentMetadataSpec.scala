package com.apixio.scala.utility.alignment

import com.apixio.model.profiler.Code
import com.apixio.scala.apxapi.AuthSpec
import org.scalatest.matchers.should._

class DocumentMetadataSpec extends AuthSpec with Matchers {
  val patient = "ee05ad5c-f52c-4104-875a-d9813c13ca01"
  val pdsId = "1723"

  "DocumentMetadata" should "be able to get all document metadata for a patient" in {
    login(custops)

    val docs = DocumentMetadata.getPatientDocumentsMetadata(patient, pdsId)
    docs.map(_.docUUID).toSet shouldBe Set("0a30857b-fd7a-4c19-b081-8086e6cb9a19", "9d7baa40-bf42-466e-bb28-9233ddc2f702", "04fb0edc-255b-4068-8e93-83b2d3169f46")
  }

  // current document DAO does not populate parsing detail data to support filtering by batch
  ignore should "be able to filter document metadata for a patient by upload batch" in {
    login(custops)

    val docs = DocumentMetadata.getPatientDocumentsMetadata(patient, pdsId, clinincalBatches = Some(List("1723_INTEGRATION_AUTOMATION_DOCUMENTS_CCDA_Test4")))
    docs.map(_.docUUID).toSet shouldBe Set("0a30857b-fd7a-4c19-b081-8086e6cb9a19")
  }

  it should "be able to parse provider type" in {
    val s1 = "10^10^PROVIDER_TYPE_RAPS^2.25.427343779826048612975555299345414078866"
    val s2 = "I^I^PROVIDER_TYPE_FFS^2.25.986811684062365523470895812567751821389"

    DocumentMetadata.parseProviderTypeCode(s1) shouldBe Some(Code("10", Code.RAPSPROV))
    DocumentMetadata.parseProviderTypeCode(s2) shouldBe Some(Code("I", Code.FFSPROV))
  }

  it should "Ignore invalid provider code" in {
    val codes = List(
      "9^9^PROVIDER_TYPE_RAPS^2.25.427343779826048612975555299345414078866",
      "10^9^PROVIDER_TYPE_RAPS^2.25.427343779826048612975555299345414078865",
      "L^I^PROVIDER_TYPE_FFS^2.25.986811684062365523470895812567751821389",
      "I^I^PROVIDER_TYPE_FFS^2.25.98681168406236552347089581256775182138"
    )

    codes.flatMap(DocumentMetadata.parseProviderTypeCode) shouldBe empty
  }
}
