package com.apixio.scala.apxapi


class CV2(connection :ServiceConnection, tm: ApxSession, options: Map[String,String] = Map()) extends BaseService(connection, tm, options) {

  def getPatientUUIDs(orgId: String, externalPatientIds: Array[String], externalSource: String): Map[String,Option[String]] = {
    val params = Map("keyType" -> "externalID", "authority" -> externalSource)
    val patientUUIDList = post[String](s"/pipeline/keys/$orgId/patients", data=externalPatientIds.mkString("\n").getBytes, params=params)
    patientUUIDList.split("\n").map(x => {
      val tokens = x.split("\t")
      tokens(0) -> tokens.lift(2)
    }).toMap
  }

  def getEventProperties(version: String = "current", pdsId: Option[String] = None): Map[String,String] = {
    val params = pdsId.map(x => Map("orgID" -> x)).getOrElse(Map.empty)
    val rawProperties = get[String](s"/pipeline/event/properties/$version", params=params).split("\n")
    
    // First line is date
    rawProperties.tail.filter(_.nonEmpty).map(_.split("=")).filter(_.length > 1)
      .map(x => x.head -> x.tail.mkString("=")).toMap
  }
}