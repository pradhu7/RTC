package com.apixio.scala.subtraction

import com.apixio.scala.apxapi.AuthSpec
import org.scalatest.matchers.should.Matchers

class RapsManagerSpec  extends AuthSpec with Matchers {
  val patientUUID = "1d2d063c-7f3e-4c19-afb7-24ba13a91e4a"
  val pdsId = "1174"

  "ProblemsGetter" should "correctly query problems for patient" in {
    val problems = RapsManager.getPatientProblems(patientUUID, pdsId)
    problems.length shouldBe 60

    val problemsByBatch = RapsManager.getPatientProblems(patientUUID, pdsId, claimsBatches = Some(List("1174_zach_raps_6")))
    problemsByBatch.length shouldBe 60

    val problemsFakeByBatch = RapsManager.getPatientProblems(patientUUID, pdsId, claimsBatches = Some(List("1174_doesnt_exist")))
    problemsFakeByBatch.length shouldBe 0
  }

}
