package com.apixio.scala.utility.hcc

import com.apixio.scala.apxapi.{AuthSpec, Project}
import com.apixio.scala.dw.ApxServices
import org.scalatest.matchers.should.Matchers

class ClaimsManagerSpec extends AuthSpec with Matchers {
  val dosStart = "2017-01-01T00:00:00Z"
  val dosEnd = "2017-12-31T00:00:00Z"
  val projectId = "PRHCC_Claims-manager-test"

  val project: Project = ApxServices.mapper.readValue[Project](
    s"""
       |{
       |  "dosStart":"$dosStart",
       |  "dosEnd":"$dosEnd",
       |  "paymentYear":"2018",
       |  "type": "hcc",
       |  "id": "$projectId",
       |  "pdsExternalID": "430",
       |  "properties": {
       |    "gen": {
       |      "icdmapping": "2017-icd-hcccr",
       |      "apxcatmapping": "2017-apxcat-hcccr1",
       |      "findingscorer": "com.apixio.hcc.elasticbundler.scorers.FlatFindingScorer",
       |      "workitemscorer": "com.apixio.hcc.elasticbundler.scorers.RandomWorkItemScorer"
       |    }
       |  }
       |}""".stripMargin)

  val patientUUID = "6de5a925-d36c-44c8-be4a-f7dfcf1b26b6"

  // "ClaimsManager" should "be able to get linkable claims" in {
  //   ClaimsManager.filterLinkableClaims(project)(Right(patientUUID)) shouldNot be(empty)
  // }
}
