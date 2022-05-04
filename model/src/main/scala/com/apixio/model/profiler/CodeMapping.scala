package com.apixio.model.profiler

import java.io.InputStream

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.ScalaObjectMapper
import com.fasterxml.jackson.annotation.JsonProperty

import scala.beans.BeanProperty

case class CodeMapping(
    @JsonProperty("f") from: Code = Code(),
    @JsonProperty("t") to: Code = Code(),
    @JsonProperty("v") version: String = "",
    @JsonProperty("u") unidirectional: Boolean = false) {

  def asJson()(implicit mapper: ObjectMapper) = mapper.writeValueAsString(this)
}

object CodeMapping {
  private var mappings : Map[(Code,String),List[Code]] = Map()
  private var versions : Map[Code,List[String]] = Map()

  def init(mappingsData: InputStream)(implicit mapper: ObjectMapper with ScalaObjectMapper) : Unit =  {
    mappings.synchronized {
      val data = mapper.readValue[List[CodeMapping]](mappingsData)
      mappings = data.flatMap( x=> x.unidirectional match {
        case true => List(((x.from, x.version), x.to))
        case false => List(((x.from, x.version), x.to), ((x.to, x.version), x.from)) //create bidrectional mappings
      }).groupBy(_._1).map(x => x._1 -> x._2.map(_._2)).toMap
      versions = mappings.keys.groupBy(_._1).map(x => x._1 -> x._2.map(_._2).toList.distinct).toMap
    }
  }

  def versions(c: Code) : List[String] = versions.getOrElse(c, List())

  def get(c: Code, v: String) : List[Code] = mappings.getOrElse((c, v), List())

  def getMappings() : Map[(Code,String),List[Code]] = mappings

  def fromJson(obj: String)(implicit mapper: ObjectMapper with ScalaObjectMapper) : CodeMapping = mapper.readValue[CodeMapping](obj)
}
