package com.apixio.scala.apxapi

import scala.util.Success
import com.apixio.scala.dw.ApxServices
import org.json4s.DefaultFormats

import scala.util.Try

class Patientsvc(connection :ServiceConnection, tm: ApxSession, options: Map[String,String] = Map()) extends BaseService(connection, tm, options) {

  implicit val formats = DefaultFormats

  def pauseFindings(projectId: String, findingsId:Seq[String]) = {
    post[String](s"/findings/projects/$projectId/pause",headers = Map("Content-Type"->"application/json"), data = ApxServices.mapper.writeValueAsBytes((findingsId)))
  }

  def unpauseFindings(projectId: String, findingsId:Seq[String] ) = {
    post[String](s"/findings/projects/$projectId/unpause", headers = Map("Content-Type"->"application/json"), data = ApxServices.mapper.writeValueAsBytes((findingsId)))
  }

  def getPausedFindings(projectId: String) = {
    Try(get[Seq[String]](s"/projects/$projectId/pause", headers = Map("Content-Type"->"application/json"))) match {
      case Success(paused: Seq[String]) => paused
      case _ => Seq.empty[String]
    }
  }

  def getPredictionsCount(projectId:String) = {
    get[Long](s"/projects/$projectId/predictions/count")
  }

  def getSubtractionsCount(projectId:String) = {
    get[Long](s"/projects/$projectId/subtraction-count")
  }

  def getPatientEligibility(patientUUID: String, pdsUUID: String) = {
    get[String](s"/patient/pds/${pdsUUID}/eligibility/${patientUUID}")
  }

  def getPatientDemographics(patientUUID: String, pdsUUID: String) = {
    get[String](s"/patient/pds/${pdsUUID}/demographics/${patientUUID}")
  }

  def getPatientForProject(patientUUID: String, projectUUID: String) = {
    get[String](s"/projects/${projectUUID}/patients/${patientUUID}")
  }

  def deletePatientPredictions(patientUUID: String, projectId: String) = {
    delete[Boolean](s"/annotation/patient/${patientUUID}/projectid/${projectId}")
  }
}
