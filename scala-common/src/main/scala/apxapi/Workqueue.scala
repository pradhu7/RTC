package com.apixio.scala.apxapi

import com.apixio.scala.dw.{ApxServices,ProjectHistoryRequest}
import org.json4s.JArray
import org.json4s.jackson.JsonMethods.{parse, _}

class Workqueue(connection: ServiceConnection, tm: ApxSession, options: Map[String,String] = Map()) extends BaseService(connection, tm, options) {
  def version() = {
    get[String](s"/version", auth = false)
  }

  def statusQueue(project: String, node: String) = {
    get[String](s"/projects/${project}/nodes/${node}/status")
  }

  def setProjectNodesFilters(project: String, node: String, filter: String, payload: Map[String, Any]) = {
    post[String](s"/projects/${project}/nodes/${node}/filters/${filter}", data = ApxServices.mapper.writeValueAsBytes(payload), headers = Map("content-type" -> "application/json"))
  }

    def projectRemaining(project: String, node: String) = {
    get[String](s"/projects/${project}/nodes/${node}/remaining")
  }

  def projectNodes(project: String) = {
    get[String](s"/projects/${project}/nodes")
  }

  def workflow(project: String) = {
    get[Map[String, Any]](s"/projects/${project}/workflow")
  }

  def getNextWorkItem  = {
    parse(get[String](s"/next-work-item")) match {
      case JArray(arr) => arr
      case unknown => throw new MatchError(s"Unsupported: $unknown")
    }
  }

  def reloadNode(project: String, node: String) = {
    post[String](s"/projects/${project}/nodes/${node}/reload")
  }

  def getNextWorkItemForProject(project: String) = {
    parse(get[String](s"/projects/$project/next-work-item")) match {
      case JArray(arr) => arr
      case unknown => throw new MatchError(s"Unsupported: $unknown")
    }
  }

  def index(patient: String, project: String, event: String): Unit = {
    val payload: Map[String, String] = Map("patientUUID" -> patient, "eventType" -> event, "projectId" -> project)
    post("/indexer", data = ApxServices.mapper.writeValueAsBytes(payload))
  }

  def indexHigh(patient: String, project: String, event: String): Unit = {
    val payload: Map[String, String] = Map("patientUUID" -> patient, "eventType" -> event, "projectId" -> project)
    post("/indexer-high", data = ApxServices.mapper.writeValueAsBytes(payload))
  }

  def indexWorkItem(patient: String, project: String, event: String): Unit = {
    val payload: Map[String, String] = Map("patientUUID" -> patient, "eventType" -> event, "projectId" -> project)
    post("/indexer-workitem", data = ApxServices.mapper.writeValueAsBytes(payload))
  }

  def getWorkItem(workItemId: String, projectId: String) = {
    parse(get[String](s"/projects/$projectId/work-item/$workItemId")) match {
      case wi => wi
      case unknown => throw new MatchError(s"Unsupported: $unknown")
    }
  }

  def projectPhasess(project: String) = {
    get[String](s"/projects/${project}/phases")
  }

  def getHistoryWorkItems(projectId: String, payload: Map[String, Any]) = {
    post[String](s"/projects/${projectId}/search-history", data = ApxServices.mapper.writeValueAsBytes(payload), headers = Map("content-type" -> "application/json"))
  }

  def getHistoryWorkItemsCount(projectId: String, payload: Map[String, Any]) = {
    post[String](s"/projects/${projectId}/search-history-count", data = ApxServices.mapper.writeValueAsBytes(payload), headers = Map("content-type" -> "application/json"))
  }

  def getWorkQueueWorkItem(projectId: String, wid: String) = {
    get[String](s"/projects/${projectId}/work-item/${wid}")
  }

  def setWorkflow(project: String, payload: Map[String, Any]) = {
    post[String](s"/projects/${project}/workflow", data = ApxServices.mapper.writeValueAsBytes(payload))
  }

  def getHistoryWorkItemsCombined(projectId: String, payload: Map[String, Any]) = {
    post[String](s"/projects/${projectId}/search-history-combined", data = ApxServices.mapper.writeValueAsBytes(payload), headers = Map("content-type" -> "application/json"))
  }

  def getNewProjectHistory(projectId: String, payload: ProjectHistoryRequest) = {
    post[String](s"/projects/${projectId}/new-project-history", data = ApxServices.mapper.writeValueAsBytes(payload), headers = Map("content-type" -> "application/json"))
  }

  def getWorkQueueItemsByPatient(projectId: String, patientId: String) = {
    get[String](s"/projects/${projectId}/patient/${patientId}/predictions")
  }

  def getWorkQueueItemsByPatientByNode(projectId: String, patientId: String, nodeId: String) = {
    get[String](s"/projects/${projectId}/patient/${patientId}/${nodeId}")
  }

  def setChartReviewLeaseTime(projectId: String, payload: Map[String, Any]) = {
    post[String](s"/projects/${projectId}/nodes/chart-review/lease-time", data = ApxServices.mapper.writeValueAsBytes(payload))
  }

  def setProjectCustomNodes(projectId: String, payload: Map[String, Any]) = {
    post[String](s"/projects/${projectId}/custom-nodes", data = ApxServices.mapper.writeValueAsBytes(payload), headers = Map("content-type" -> "application/json"))
  }

  def setNodesProperties(projectId: String, nodeId: String, payload: Map[String, Any]) = {
    put[String](s"/projects/${projectId}/nodes/${nodeId}/properties", data = ApxServices.mapper.writeValueAsBytes(payload), headers = Map("content-type" -> "application/json"))
  }
}
