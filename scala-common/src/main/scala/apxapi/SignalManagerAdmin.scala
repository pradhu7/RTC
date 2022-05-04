package com.apixio.scala.apxapi

import com.apixio.scala.dw.ApxServices
import org.json4s.JsonAST
import org.json4s.JsonAST.{JArray, JValue}
import org.json4s.jackson.JsonMethods.parse

class SignalManagerAdmin(connection :ServiceConnection, tm: ApxSession, options: Map[String,String] = Map()) extends BaseService(connection, tm, options) {


  def getDocsets(projectId: String):List[JsonAST.JValue] = {
    parse (get[String](s"/projdatasets?criteriaType=PROJECT_UUID&criteria=$projectId")) match {
      case JArray(arr) => arr
      case unknown => throw new MatchError(s"Unsupported: $unknown")
    }
  }


  //patientUuids array of strings
  def getPatientsFromDocset(dsId:String):Seq[String] = {
    getDocset(dsId, includePatientUUIDs = true)("patientUuids").asInstanceOf[Seq[String]]
  }

  def getDocUUIDsFromDocset(dsId:String):Seq[String] ={
    getDocset(dsId, includeDocUUIDs = true)("documentUuids").asInstanceOf[Seq[String]]

  }

  def getDocset(dsId:String, includeDocUUIDs:Boolean=false, includePatientUUIDs:Boolean=false): Map[String,Any] = {
    parse (get[String](s"/projdatasets/$dsId?includeDocUUIDs=${includeDocUUIDs}&includePatientUUIDs=${includePatientUUIDs}")) match {
      case value:JValue => {
        value.values.asInstanceOf[Map[String,Any]]
      }
      case unknown => throw new MatchError(s"Unsupported: $unknown")
    }
  }

  def getPatsets(projectId: String): List[JsonAST.JValue] = getDocsets(projectId)
  def getPatientsFromPatset(dsId:String):Seq[String] = getPatientsFromDocset(dsId)
  def getPatset(dsId:String, includeDocUUIDs:Boolean=false, includePatientUUIDs:Boolean=false): Map[String,Any] = {
    getDocset(dsId, includeDocUUIDs, includePatientUUIDs)
  }

  //Be advised that some times smas returns Null/None for patient ID. One possible way to recover is to use
  //patientLogic(e.g ApxServices.patientLogic.getPatientUUIDByDocumentUUID)
  def getPatientDocumentMapFromDocsetV2(dsId: String): Map[String, Any] = {
    getDocsetV2(dsId, includeDocumentPatientMap = true)("docPatMap").asInstanceOf[Map[String, Any]]
  }

  //V2 to support dual docset tools. signalmgradmin-reporting 1.5.6
  def getPatientsFromDocsetV2(dsId:String):Seq[String] = {
    getDocsetV2(dsId, includePatientUUIDs = true)("patientUuids").asInstanceOf[Seq[String]]
  }

  def getDocUUIDsFromDocsetV2(dsId:String):Seq[String] ={
    getDocsetV2(dsId, includeDocUUIDs = true)("documentUuids").asInstanceOf[Seq[String]]

  }

  def getDocsetV2(dsId: String, includeDocUUIDs: Boolean = false, includePatientUUIDs: Boolean = false, includeDocumentPatientMap: Boolean = false): Map[String, Any] = {
    parse(get[String](s"/projdatasets/$dsId/V2?includeDocUUIDs=$includeDocUUIDs&includePatientUUIDs=$includePatientUUIDs&includeDocumentPatientMap=$includeDocumentPatientMap")) match {
      case value: JValue => {
        value.values.asInstanceOf[Map[String, Any]]
      }
      case unknown => throw new MatchError(s"Unsupported: $unknown")
    }
  }

  def createDocset(docset_name: String,
                   customer_uuid: String,
                   project_uuid: String,
                   short_pds_id: String,
                   criteria: List[String],
                   criteria_type: String,
                   data_type: String = "FULL_CHART",
                   prediction_engine: String = "MA",
                   prediction_engine_variant: String = "V22",
                   doc_titles: List[String] = List.empty[String],
                   doc_pat_uuids: Map[String, String] = null,
                   eligibility: String = null,
                   doc_date_override: String = null,
                   user: String = ""): String = {
    val dataMap = Map("projectDataSetName" -> docset_name,
      "customerUuid" -> customer_uuid,
      "projectUuid" -> project_uuid,
      "pdsId" -> short_pds_id,
      "dataType" -> data_type,
      "criteria" -> criteria,
      "criteriaType" -> criteria_type,
      "documentTitles" -> doc_titles,
      "documentPatientUuids" -> doc_pat_uuids,
      "eligibility" -> eligibility,
      "documentDateOverride" -> doc_date_override,
      "predictionEngine" -> prediction_engine,
      "predictionEngineVariant" -> prediction_engine_variant,
      "creator" -> user)
    val data = ApxServices.mapper.writeValueAsBytes(dataMap)
    post[String](path = s"/projdatasets", headers = Map("Content-Type" -> "application/json"), data = data)
  }
}