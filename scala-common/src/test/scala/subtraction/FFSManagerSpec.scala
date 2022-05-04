package com.apixio.scala.subtraction

import com.apixio.model.profiler.{BillTypeModel, Code, CptModel}
import com.apixio.scala.apxapi.AuthSpec
import com.apixio.scala.dw.{ApxConfiguration, ApxServices}
import com.apixio.scala.subtraction.FFSManager.RichProcedure
import com.apixio.scala.utility.Mapping
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, ScalaObjectMapper}
import org.joda.time.DateTime
import org.scalatest.matchers.should.Matchers

class FFSManagerSpec extends AuthSpec with Matchers {
  val pdsId = "430"
  val dosStart = "2017-01-01"
  val dosEnd = "2017-12-31"
  val ffsManager = FFSManager("2017-icd-hcccr", dosStart = Some(DateTime.parse(dosStart)), dosEnd = Some(DateTime.parse(dosEnd)))

  implicit val mapper = {
    val m = new ObjectMapper() with ScalaObjectMapper
    m.registerModule(DefaultScalaModule)
    m.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    m.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
    m
  }
  ApxConfiguration.initializeFromFile("application-dev.yaml")
  ApxServices.init(ApxConfiguration.configuration.get)
  CptModel.init(Mapping.getCptModel())
  BillTypeModel.init(Mapping.getBilltypeModel())(mapper)

  "FFSManagerSpec" should "be able to query patient procedures for FFS Claims" in {
    val patientUUID = "5cc1cb35-e138-4ff3-9781-ebe08521fa06"
    val procedures = FFSManager.getPatientProcedures(patientUUID, pdsId)
    procedures shouldNot be (empty)
    procedures.length shouldBe 9 // this might change if we upload new data
  }

  it should "be able filter patient procedures by batches" in {
    val patientUUID = "5cc1cb35-e138-4ff3-9781-ebe08521fa06"
    val batches = List("430_nick_ffs_claims5")
    val procedures = FFSManager.getPatientProcedures(patientUUID, pdsId, Some(batches))
    procedures shouldNot be (empty)
    procedures.length shouldBe 2
  }

  it should "be able to filter out bad cpt code for professional provider" in {
    // CPT: UNKNOWN
    val patientUUID = "e84a2229-3251-4b64-9316-5591479735b6"
    val procedures = FFSManager.getPatientProcedures(patientUUID, pdsId)
    procedures.length should be (3)

    procedures.flatMap(_.providerType).toSet shouldBe Set(Code("P", Code.FFSPROV), Code("I", Code.FFSPROV))

    ffsManager.cluster(procedures) should be (empty)
  }

  // it should "be able to validate cpt code for professional provider" in {
  //   // CPT: 98940
  //   val patientUUID = "5ecc50cc-b67d-4bae-899c-ae820ff294da"
  //   val procedures = FFSManager.getPatientProcedures(patientUUID, pdsId)
  //   procedures.length should be (2)

  //   procedures.flatMap(_.providerType).toSet shouldBe Set(Code("P", Code.FFSPROV))

  //   ffsManager.cluster(procedures).length should be (1)
  // }

  it should "be able to filter out bad cpt code for Outpatient provider" in {
    // CPT: 88112
    val patientUUID = "328c1993-40c4-4c40-8677-60a614b7ebcf"
    val procedures = FFSManager.getPatientProcedures(patientUUID, pdsId)
    procedures.length should be (1)

    procedures.flatMap(_.providerType) shouldBe List(Code("O", Code.FFSPROV))

    ffsManager.cluster(procedures) should be (empty)
  }

  it should "be able to filter out bad bill type for Outpatient provider" in {
    // CPT: 92937, BILL TYPE: UNKNOWN
    val patientUUID = "d42db7b1-7251-455a-b338-f1c4d31683cb"
    val procedures = FFSManager.getPatientProcedures(patientUUID, pdsId)
    procedures.length should be (1)

    procedures.flatMap(_.providerType).toSet shouldBe Set(Code("O", Code.FFSPROV))

    ffsManager.cluster(procedures) should be (empty)
  }

  it should "be able to validate cpt code and bill type for Outpatient provider" in {
    // CPT: 77290, BILL TYPE: 131
    val patientUUID = "a8e6baed-ed54-4da8-8b60-2ee8782e4a2d"
    val procedures = FFSManager.getPatientProcedures(patientUUID, pdsId)
    procedures.length should be (1)

    procedures.flatMap(_.providerType).toSet shouldBe Set(Code("O", Code.FFSPROV))

    ffsManager.filter(procedures) shouldNot be (empty)
  }

  it should "be able to filter out bad bill type for inpatient provider type" in {
    // BILL TYPE: 131
    val patientUUID = "7b9675b0-967b-4911-a585-3425e5382235"
    val procedures = FFSManager.getPatientProcedures(patientUUID, pdsId)
    procedures.length should be (1)

    procedures.flatMap(_.providerType).toSet shouldBe Set(Code("I", Code.FFSPROV))

    ffsManager.cluster(procedures) should be (empty)
  }

  // it should "be able to validate bill type for inpatient provider type" in {
  //   // BILL TYPE: 111
  //   val batches = List("430_ACA_CLAIMS_TEST_2018")
  //   val patientUUID = "6ca27f3d-ce9e-4091-88bf-56c1d4c12c24"
  //   val procedures = FFSManager.getPatientProcedures(patientUUID, pdsId, Some(batches))
  //   procedures.length should be (1)

  //   procedures.flatMap(_.providerType).toSet shouldBe Set(Code("I", Code.FFSPROV))

  //   ffsManager.cluster(procedures) shouldNot be (empty)
  // }

  it should "take into account delete indicator of the last transaction" in {
    // Data in S3 with delete indicator = true
    val batches = List("430_ACA_CLAIMS_TEST_2018", "430_ACA_CLAIMS_TEST_2018_2")
    val patientUUID = "6ca27f3d-ce9e-4091-88bf-56c1d4c12c24"
    val procedures = FFSManager.getPatientProcedures(patientUUID, pdsId, Some(batches))
    procedures.length should be (0)
  }
}
