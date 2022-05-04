package com.apixio.scala.apxapi

import java.util.UUID

import com.apixio.scala.apxapi.ProjectAdminTest._
import com.apixio.scala.apxapi.{ApxApi, AuthCredsSpec, ProjectParams}
import com.apixio.scala.dw.{ApxConfiguration, ApxServices}
import com.fasterxml.jackson.databind.ObjectMapper

class ProjectAdminTest extends AuthSpec {
  ApxConfiguration.initializeFromFile("application-dev.yaml")
  ApxServices.init(ApxConfiguration.configuration.get)
  ApxServices.setupObjectMapper(new ObjectMapper())

  val apxapi = login(coder1)

  // test("should be able to create a new project via ProjectAdmin api") {
  //   // Given
  //   val params = ProjectParams(orgId, pdsId, name, projectType, completionDate, codingType, isDocGap, noteType,
  //     market, submissionYear, submissionMonth, billingUnit, enforceCoverage, claimsType, renewDocument, servingMode,
  //     style, rejectReasons, budget, expectedCharts, analyzedCharts, batches, tagNotes)

  //   // When
  //   val res = apxapi.projectadmin.createHccProject(params)

  //   // Then
  //   res.get("uuid").nonEmpty
  // }
}

object ProjectAdminTest {
  val orgId = "UO_15fae641-d1c3-4805-b320-f2a7c73cb764"
  val pdsId = "O_00000000-0000-0000-0000-000000001022"
  val name  = "test_" + UUID.randomUUID().toString
  val projectType: String = "Application"
  val completionDate = "01/31/2019"
  val codingType: String = "FirstPass"
  val isDocGap = false
  val noteType: String = "Charts"
  val market: String = "MA"
  val submissionYear = 2019
  val submissionMonth: String = "January"
  val budget = 1000000000.00
  val expectedCharts = 10000
  val analyzedCharts = 10000
  val billingUnit: String = "Charts"
  val enforceCoverage = false
  val claimsType: String = "ByProblem"
  val renewDocument: String = "Default"
  val servingMode: String = "ShowAll"
  val style: String = "Identifier"
  val rejectReasons: List[String] = List("Admin - Patient info mismatch", "Admin - DOS invalid or unknown")
  val batches: String = ""
  val tagNotes: String = ""

  def newProjectParams = ProjectParams(orgId, pdsId, name, projectType, completionDate, codingType, isDocGap, noteType,
    market, submissionYear, submissionMonth, billingUnit, enforceCoverage, claimsType, renewDocument, servingMode,
    style, rejectReasons, budget, expectedCharts, analyzedCharts, batches, tagNotes)
}
