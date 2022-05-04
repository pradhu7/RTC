package com.apixio.scala.apxapi

import com.apixio.scala.dw.ApxConfiguration
import org.scalatest.matchers.should.Matchers

/**
  * Created by dnguyen on 11/2/16.
  */
class UserAccountsSpec extends AuthSpec with Matchers {

  val projId = "PRHCCMANUAL_8a903e81-5a53-46b4-8739-cb8b75197c01"

  val apxapi = login(custops)

  "UserAccounts" should "be able to query list of projects" in {
    val projects = apxapi.useraccounts.getProjects()
    assert(projects.nonEmpty)

    val projectsByOrg = apxapi.useraccounts.getProjects(Some(orgId))
    assert(projectsByOrg.nonEmpty)
    projectsByOrg.filterNot(_.organizationID == orgId) shouldBe empty
  }

  // it should "be able to get project and update project property" in {
  //   login(session)
  //   val project = ApxApi.internal.useraccounts.getProject(projId)
  //   project.id shouldEqual(projId)

  //   val pdsProperties = ApxApi.internal.useraccounts.getPdsProperties(project.patientDataSetID)
  //   pdsProperties shouldNot be(empty)

  //   //
  //   val originProp = project.properties.getOrElse("gen", Map.empty).get("codecollector")
  //   ApxApi.internal.useraccounts.putProjectProperty(projId, "codecollector", Some("CollectorAll"))

  //   val updatedProj = ApxApi.internal.useraccounts.getProject(projId)
  //   val updatedProp = project.properties.getOrElse("gen", Map.empty).get("codecollector")
  //   updatedProp shouldBe Some("CollectorAll")

  //   ApxApi.internal.useraccounts.putProjectProperty(projId, "codecollector", originProp)
  // }

  // it should "add user to project " in {
  //   login(session)
  //   ApxApi.internal.useraccounts.addProjectMember(projId, coder1, true, List("Code"))

  //   val u = ApxApi.internal.useraccounts.getProjectMember(projId, coder1)
  //   u.nonEmpty shouldBe true
  //   u.get.phases shouldEqual List("Code")
  //   u.get.active shouldBe true
  // }

  // it should "get project list for user" in {
  //   val projects = ApxApi.internal.useraccounts.getProjectForUser(coder1)
  //   projects shouldNot be(empty)
  //   projects.filter(_.projId == projId) shouldNot be(empty)
  // }

  // it should "get user list for project" in {
  //   val users = ApxApi.internal.useraccounts.getProjectMembers(projId)
  //   users shouldNot be(empty)
  //   users.filter(_.userid == coder1) shouldNot be(empty)
  // }

  // it should "add phase to user in a project " in {
  //   login(session)
  //   ApxApi.internal.useraccounts.addUserToProjectPhase(projId, coder1, "QA1")

  //   val u = ApxApi.internal.useraccounts.getProjectMember(projId, coder1)
  //   u.nonEmpty shouldBe true
  //   u.get.phases shouldEqual List("Code", "QA1")
  //   u.get.active shouldBe true
  // }

  // it should "remove user phase in a project " in {
  //   login(session)
  //   ApxApi.internal.useraccounts.removeUserFromProjectPhase(projId, coder1, "QA1")

  //   val u = ApxApi.internal.useraccounts.getProjectMember(projId, coder1)
  //   u.nonEmpty shouldBe true
  //   u.get.phases shouldEqual List("Code")
  //   u.get.active shouldBe true
  // }

  // it should "remove user from a project " in {
  //   login(session)
  //   ApxApi.internal.useraccounts.removeProjectMember(projId, coder1)

  //   val u = ApxApi.internal.useraccounts.getProjectMember(projId, coder1)
  //   u.isEmpty shouldBe true
  // }

  // it should "remove all users from a project " in {
  //   login(session)

  //   // readd user to project
  //   ApxApi.internal.useraccounts.addProjectMember(projId, coder1, true, List("Code"))
  //   val u = ApxApi.internal.useraccounts.getProjectMember(projId, coder1)
  //   u.nonEmpty shouldBe true

  //   ApxApi.internal.useraccounts.removeAllMembers(projId)

  //   val users = ApxApi.internal.useraccounts.getProjectMembers(projId)
  //   users.isEmpty shouldBe true
  // }

  it should "get extract batches for PDS" in {
    val pds = "O_00000000-0000-0000-0000-000000000472"

    val batches = apxapi.useraccounts.getExtractBatchesForPDS(pds)

    batches.nonEmpty shouldBe true
  }

  it should "get upload batches for PDS" in {
    val pds = "O_00000000-0000-0000-0000-000000000472"

    val batches = apxapi.useraccounts.getUploadBatchesForPDS(pds)

    batches.nonEmpty shouldBe true
  }

  /*
  it should "get extract batches for Project" in {
    login(session)
    val project = "PRHCC_23bca026-497b-40d0-9d06-1bdade683281"

    val batches = ApxApi.internal.useraccounts.getExtractBatchesForProject(project)

    batches.nonEmpty shouldBe true
  }
  */

}
