package com.apixio.model.profiler

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.ScalaObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import scala.collection.mutable.ListBuffer
import java.io.InputStream

case class CptModel(
                     @JsonProperty("c") code: String = "",
                     @JsonProperty("s") system: String = "",
                     @JsonProperty("v") version: String = "",
                     // {market}_{year} for example MA_2018, CR_2019
                     @JsonProperty("d") description: String = ""
                   ) {
  val asCode: Code = Code(code=code, system=system)
}

object CptModel {
  private var model: List[CptModel] = List.empty
  private var codesByVersion: Map[String, Map[Code, CptModel]] = Map.empty

  def init(modelData : InputStream)(implicit mapper: ObjectMapper with ScalaObjectMapper) : Unit =  {
    model.synchronized {
      model = mapper.readValue[List[CptModel]](modelData)
      // .view.force is needed because .mapValues() is lazy and we don't like being lazy
      codesByVersion = model.groupBy(_.version).mapValues(_.map(m => m.asCode -> m).toMap).view.force
    }
  }

  def get(version: String, c: Code) : Option[CptModel] = codesByVersion.getOrElse(version, Map.empty).get(c)

  def isValid(version: String, c: Code): Boolean = codesByVersion.getOrElse(version, Map.empty).contains(c)

  def getAll(version: Option[String] = None) : List[CptModel] = version match {
    case None => model
    case Some(s) => codesByVersion.getOrElse(s, Map.empty).values.toList
  }

  def getAllCodes(version: Option[String] = None) : List[Code] = version match {
    case None => model.map(_.asCode).distinct
    case Some(s) => codesByVersion.getOrElse(s, Map.empty).keys.toList
  }

  def getAllVersions(): List[String] = codesByVersion.keys.toList
}
