package com.apixio.scala.utility.hcc

import com.apixio.model.profiler.{Code, HccModel}
import com.apixio.scala.apxapi.{Project}
import com.apixio.scala.dw.ApxServices

@deprecated("Legacy: see com.apixio.app.routing.WorkItemManager")
object SyntheticCodeManager {
  def getSyntheticHandling(project: Project): Option[String] =
    project.properties.getOrElse("gen", Map.empty).get("synthetic").map(_.asInstanceOf[String]).filter(_.nonEmpty)

  def codeFromKey(key: String): Option[Code] = {
    Option(key).filter(_.nonEmpty).map(_.replace(" ", "").trim.split("-")).filter(_.length == 2)
      .map(c => Code(c(0), c(1)))
  }

  def parseRawMapping(s: String): Map[Code, List[Code]] = {
    val codes = ApxServices.mapper.readValue[List[HccModel]](s)
    codes.map(c => c.asCode() -> c.childrenCodes()).toMap
  }

  def parseInlineMapping(s: String): Map[Code, List[Code]] =
    parseRawMapping(s.split(",").tail.mkString(","))

  def parseSyntheticModel(s: String): Map[Code, List[Code]] = {
    Option(s).filter(_.startsWith(Code.HCC)) match {
      case None => Map.empty
      case _ =>
        val model = s.substring(Code.HCC.length)
        val codes = HccModel.getAll().filter(_.version == model)
        codes.map(c => c.asCode() -> c.childrenCodes()).toMap
    }
  }

  def getCodeFamilies(project: Project): Map[Code, List[Code]] = {
    getSyntheticHandling(project) match {
      case Some(s) if s.startsWith("bycache,") => // querying from data orchestrator
        parseRawMapping(s)
      case Some(s) if s.startsWith("inline,") => // inline mapping for simple synthetic hierarchial system only
        parseInlineMapping(s)
      case Some(s) if s.startsWith(Code.HCC) => parseSyntheticModel(s)
      case _ => Map.empty
    }
  }
}
