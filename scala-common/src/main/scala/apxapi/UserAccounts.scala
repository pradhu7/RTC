package com.apixio.scala.apxapi

//import scala.collection.immutable.Map

import com.apixio.restbase.web.BaseException
import com.apixio.scala.dw.ApxServices
import org.joda.time.DateTime

import scala.collection.JavaConverters._

//I'm not sure if this is all what a project is, but this seems the spec coming back from the endpoint
case class Project(budget: Double, dataSource: String, deadline: String, dosEnd: String, dosStart: String, id: String, name: String, organizationID: String,
                   organizationName: String, passType: String, patientDataSetID: String, patientList: String, docFilterList: String, paymentYear: String, pdsExternalID: String,
                   pdsName: String, var properties: Map[String,Map[String,Any]], raf: Double, rawRaf: Double, state: String, status: Boolean, sweep: String, `type`: String) {
  def pack(): java.util.Map[String,Object] = {
    (Map[String,AnyRef]("dosEnd" -> dosEnd, "dosStart" -> dosStart , "id" -> id, "name" -> name, "organizationID" -> organizationID, "organizationName" -> organizationName,
      "patientDataSetID" -> patientDataSetID, "patientList" -> patientList, "docFilterList" -> docFilterList, "paymentYear" -> paymentYear, "pdsExternalID" -> pdsExternalID,
      "pdsName" -> pdsName, "sweep" -> sweep, "type" -> `type`) ++ properties.flatMap(p => p._2.map(x => s"properties.${p._1}.${x._1}" -> x._2.asInstanceOf[AnyRef])).toMap
    ).asJava
  }

  val pds = pdsExternalID
  lazy val start = if (dosStart == null) null else DateTime.parse(dosStart)
  lazy val end = if (dosEnd == null) null else DateTime.parse(dosEnd)
  lazy val batches : List[String] = Option(properties).flatMap(_.get("gen")).flatMap(_.get("batches")).map(_.toString.split(",").filter(_.size > 0).toList).getOrElse(List.empty)
  lazy val claimsbatches : List[String] = Option(properties).flatMap(_.get("gen")).flatMap(_.get("claimsbatches")).map(_.toString.split(",").filter(_.size > 0).toList).getOrElse(List.empty)
  lazy val icdMapping = Option(properties).flatMap(_.get("gen")).flatMap(_.get("icdmapping").map(_.toString)) match {
    case Some(mapping) => mapping
    case None =>
      Option(paymentYear) match {
        case Some(py) if py == "2016" || py == "2017" => "2016-icd-hcc"
        case Some(py) if py == "2015" => "2015-icd-hcc"
        case _ => ApxServices.configuration.application.get("icdMapping").map(_.toString).getOrElse("2016-icd-hcc")
      }
  }
}

case class UserRoleProject(active: Boolean, phases: List[String], projId: String, projName: String, projectStatus: Boolean, userID: String, userid: String)
case class Role(roleID: Map[String,String], roleName: String, target: Map[String,String])
case class User(accountState: String, createdAt: String, emailAddress: String, id: String, roles: List[String], state: String, userID: String)
case class UserOrg(description: String, id: String, isActive: Boolean, name: String, `type`: String)
case class Batch(id: String, name: String, description: String, parentBatch: String, modelVersion: String, bad: Boolean, pds: String, pipelineVersion: String, sourceSystem: String, startDate: Long, closeDate: Long, properties: Map[String,Any])

class UserAccounts(connection :ServiceConnection, tm: ApxSession, options: Map[String,String] = Map()) extends BaseService(connection, tm, options) {
  final val ReviewerRoleValue = "REVIEWER"

  def signin(username: String, password: String) : String = {
    val res = post[Map[String,String]]("/auths",
                                       headers = jsonContent,
                                       data = asJsonBytes(Map("email" -> username, "password" -> password)),
                                       auth = false)
    res("token")
  }

  def getProjects(orgId: Option[String] = None) : List[Project] = {
    val queryParam = orgId.map("?orgID=" + _).getOrElse("")
    get[List[Project]](s"/projects${queryParam}")
  }

  def getProject(projId: String) : Project = {
    get[Project](s"/projects/${projId}", params=Map("properties" -> "all"))
  }

  def getProjectForUser(username: String) : List[UserRoleProject] = {
    get[List[UserRoleProject]](s"/projects/users/${username}")
  }

  def setProjectStatus(projId: String, status: Boolean) = {
    put[String](s"/projects/$projId", headers = jsonContent, data = ApxServices.mapper.writeValueAsBytes(Map("status" -> status)))
  }

  def getProjectPropertiesBag(bag: String = "gen") = {
    get[Map[String, Map[String, Any]]](s"/projects/properties/$bag")
  }

  def putProjectProperty(project: String, name: String, value: Option[Any], bag: String = "gen") = {
    value match {
      case None =>
        delete[String](s"/projects/$project/properties/$bag/$name")
      case Some(v) =>
        put[String](s"/projects/$project/properties/$bag/$name", data = asJsonBytes(Map("value" -> v)), headers = jsonContent)
    }
  }

  def getUserRoles(username: String = "me"): List[Role] = {
    get[List[Role]](s"/users/${username}/roles")
  }

  def getUser(username: String = "me") : User = {
    get[User](s"/users/${username}")
  }

  def setMemberActiveStatus(projectId: String, userEmail: String, active: Boolean) = {
    getProjectMember(projectId, userEmail) match {
      case Some(u) => if (u.active != active) {
        addProjectMember(projectId, userEmail, active, u.phases)
      }
      case None => throw BaseException.badRequest(s"Failed to find user {} for project {}", userEmail, projectId)
    }
  }

  def getUserOrgs(username: String = "me") : List[UserOrg] = {
    get[List[UserOrg]](s"/users/${username}/org")
  }

  def getUserOrgProperties(userOrgId: String) : Map[String, Any] = {
    get[Map[String, Any]] (s"/uorgs/$userOrgId/properties")
  }

  def removeAllMembers(projectId: String) = {
    delete[String](s"/projects/$projectId/users")
  }

  def getProjectMembers(projectId: String): List[UserRoleProject] = {
    get[List[UserRoleProject]](s"/projects/$projectId/members")
  }

  def getProjectMember(projectId: String, userId: String): Option[UserRoleProject] = {
    get[List[UserRoleProject]](s"/projects/$projectId/members").find(_.userid == userId)
  }

  def addProjectMember(projectId: String, userEmail: String, active: Boolean, phases: List[String]): Unit = {
    val body = Map("active" -> active, "phases" -> phases)
    put[String](s"/projects/$projectId/members/${userEmail}", data = ApxServices.mapper.writeValueAsBytes(body), headers = jsonContent)
  }

  def addUserToProjectPhase(projectId: String, userEmail: String, phase: String): UserRoleProject = {
    // get the user
    return getProjectMember(projectId, userEmail) match {
      case Some(u) => {
        if (!u.phases.contains(phase)) {
          debug(s"${getClass.getTypeName}:addUserToProject adding $phase in project $projectId to user $userEmail")
          debug(s"${getClass.getTypeName}:addUserToProject updating project $projectId with user $userEmail")
          addProjectMember(projectId, userEmail, u.active, u.phases :+ phase)
          grantUserPermissions(projectId, userEmail, ReviewerRoleValue)
        }
        u.copy(phases = u.phases :+ phase)
      }
      case None => {
        debug(s"${getClass.getTypeName}:addUserToProject creating a new user in project $projectId with user $userEmail phase ${phase.toString}")
        addProjectMember(projectId, userEmail, true, List(phase))
        grantUserPermissions(projectId, userEmail, ReviewerRoleValue)
        new UserRoleProject(true, List(phase), projectId, null, true, null, userEmail)
      }
    }
  }

  def removeUserFromProjectPhase(projectId: String, userEmail: String, phase: String): Unit = {
    // remove user from coding phase
    getProjectMember(projectId, userEmail) match {
      case Some(u) => {
        if (u.phases.contains(phase)) {
          addProjectMember(projectId, userEmail, u.active, u.phases.filterNot(_ == phase))
        } else {
          debug("Did not update user as user did not had it assigned")
        }
      }
      case None => throw BaseException.badRequest(s"Failed to find user {} for project {}", userEmail, projectId)
    }
  }

  def removeProjectMember(projectId: String, userEmail: String): Unit = {
    delete[String](s"/projects/${projectId.toString}/users/$userEmail")
  }

  def grantUserPermissions(projectId: String, userEmail: String, role: String): Unit = {
    debug(s"${getClass.getTypeName}:grantUserPermissions granting user $userEmail role $role in project $projectId")
    put[String](s"/projects/${projectId.toString}/users/$userEmail/roles/$role", headers = jsonContent)
  }

  def revokeUserPermission(projectId: String, userEmail: String, role: String): Unit = {
    debug(s"${getClass.getCanonicalName}:revokeUserPermission removing role for user $userEmail role $role in project $projectId")
    delete[String](s"/projects/${projectId.toString}/users/$userEmail/roles/$role")
  }

  def getPdsProperties(pdsId: String): Map[String,String] = {
    get[Map[String,String]](s"/patientdatasets/$pdsId/properties/")
  }

  def getExtractBatchesForPDS(pdsId: String): List[Batch] = {
    get[List[Batch]](s"/batches/extract/pds/${pdsId}")
  }

  def getUploadBatchesForPDS(pdsId: String): List[Batch] = {
    get[List[Batch]](s"/batches/upload/pds/${pdsId}")
  }

  def getExtractBatchesForProject(projId: String): List[Batch] = {
    get[List[Batch]](s"/batches/extract/project/${projId}")
  }
}
