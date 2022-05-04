package com.apixio.scala.apxapi

import com.apixio.model.profiler.Code
import com.apixio.scala.dw.ApxServices

class DataOrchestrator(connection :ServiceConnection, tm: ApxSession, options: Map[String,String] = Map()) extends BaseService(connection, tm, options) {
  def getArchiveDocument(pdsId: String, id: String) : String = {
    get[String](s"/document/${pdsId}/archive/${id}")
  }

  def postArchiveDocument(pdsId: String, data: String) : String = {
    post[String](s"/document/${pdsId}/archive", data = data.getBytes, headers=Map("content-type" -> "application/octet-stream"))
  }

  def documentMetadata(uuid: String, pdsId: Option[String] = None, patientId: Option[String] = None) : Map[String,Any] = {
    val params = Map("pdsId" -> pdsId, "patientId" -> patientId).filter(_._2.nonEmpty).mapValues(_.get)
    get[Map[String,Any]](s"/document/${uuid}/metadata", params = params)
  }

  def demographics(uuid: String, pdsId: Option[String] = None): Map[String,Any] = {
    get[Map[String,Any]](s"/patient/${uuid}/demographics", params = Map("pdsId" -> pdsId).filter(_._2.nonEmpty).mapValues(_.get))
  }

  def getPage(uuid: String, page: String, pdsId: Option[String]): Array[Byte] = {
    // why can't be type inferred here?
    val queryParams: Map[String,String] = pdsId match {
      case Some(id) => Map("pdsId" -> id)
      case _ => Map()
    }

    get[Array[Byte]](s"/document/${uuid}/file/${page}", params=queryParams)
  }

  def getClaimCache(proj: Project) : Map[String,List[(Code,Long)]] = {
    def getClaimHandling(proj: Project) : String =
      proj.properties.get("gen").flatMap(_.get("claimstype").map(_.toString)).getOrElse("byevent")

    val claimHandling = getClaimHandling(proj)
    if (claimHandling.startsWith("bycache"))
      ApxServices.mapper.readValue[Map[String, List[(Code, Long)]]](getArchiveDocument(proj.pdsExternalID, claimHandling.split(",").last))
    else
      Map()
  }

  def simpleContent(uuid: String): Array[Byte] = {
    get[Array[Byte]](s"/document/${uuid}/simpleContent")
  }

  def file(uuid: String): Array[Byte] = {
    get[Array[Byte]](s"/document/${uuid}/file")
  }

  def patientObject(patientId: String, includeStringContent: Option[Boolean] = None) = {
    val params = includeStringContent.map(x => Map("includeStringContent" -> x.toString)).getOrElse(Map.empty)
    get[String](s"/patient/$patientId/apo", params = params)
  }

  def searchDemographics(pdsId: String, payload: Map[String, Any]) = {
    post[String](s"/search/${pdsId}/demographics", data = ApxServices.mapper.writeValueAsBytes(payload), headers = Map("content-type" -> "application/json"))
  }
}
