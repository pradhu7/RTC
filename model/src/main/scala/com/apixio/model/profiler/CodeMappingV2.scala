package com.apixio.model.profiler

import java.io.InputStream

import com.apixio.model.utility.{DateFunctions, DateModel}
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.ScalaObjectMapper
import org.joda.time.{DateTime, DateTimeZone}

import scala.util.matching.Regex

case class CodeMappingV2(
      @JsonProperty("f") from: Code = Code(),
      @JsonProperty("t") to: Code = Code(),
      @JsonProperty("v") version: String = "",
      @JsonProperty("u") unidirectional: Boolean = false,
      @JsonProperty("start") start: String = "",
      @JsonProperty("end") end: String = "") extends DateModel {

  @Override
  def getStartDate(): DateTime = DateTime.parse(start, mappingDtf).withZone(DateTimeZone.UTC)

  @Override
  def getEndDate(): DateTime = DateTime.parse(end, mappingDtf).withZone(DateTimeZone.UTC)

  def asJson()(implicit mapper: ObjectMapper) = mapper.writeValueAsString(this)
}

object CodeMappingV2 extends DateFunctions {
  import VersionRegex._
  private var mappings : Map[(Code,String),List[V2MappingStruct]] = Map()
  private var reversedUnidirectionalMappings: Map[(Code,String),List[V2MappingStruct]] = Map()
  private var versions : Map[Code,List[String]] = Map()

  val MAMappingType = "icd-hcc"
  val CRMappingType = "icd-hcccrv2"
  val MANormalVersionRegex = ("^[0-9]{4}-" + CodeMappingV2.MAMappingType + "$").r
  val CRNormalVersionRegex = ("^[0-9]{4}-" + CodeMappingV2.CRMappingType + "$").r
  val SNOMEDType = "icd-snomed"

  def isNormalMAMappingType(version: String) = version == MAMappingType || (MANormalVersionRegex matches version)
  def isNormalCRMappingType(version: String) = version == CRMappingType || (CRNormalVersionRegex matches version)

  def normalizeMappingType(version: String): String = version match {
    case maMapping if isNormalMAMappingType(maMapping) => MAMappingType
    case crMapping if isNormalCRMappingType(crMapping) => CRMappingType
    case otherMapping => otherMapping
  }

  def init(mappingsData: InputStream)(implicit mapper: ObjectMapper with ScalaObjectMapper): Unit =  {
    val data = mapper.readValue[List[CodeMappingV2]](mappingsData)
    this.synchronized {
      mappings = data.flatMap(row => row.unidirectional match {
        case true => List(((row.from, row.version), V2MappingStruct(row.to, row.getStartDate, row.getEndDate)))
        case false => List(((row.from, row.version), V2MappingStruct(row.to, row.getStartDate, row.getEndDate)),
          ((row.to, row.version), V2MappingStruct(row.from, row.getStartDate, row.getEndDate))) //create bi-directional mappings
      }).groupBy(_._1).map(x => x._1 -> x._2.map(y => y._2))
      versions = mappings.keys.groupBy(_._1).map(x => x._1 -> x._2.map(_._2).toList.distinct)

      reversedUnidirectionalMappings = data.flatMap(row => row.unidirectional match {
        case true => List(((row.to, row.version), V2MappingStruct(row.from, row.getStartDate, row.getEndDate)))
        case false => List.empty
      }).groupBy(_._1).map(x => x._1 -> x._2.map(y => y._2))
    }
  }

  def versions(c: Code) : List[String] = versions.getOrElse(c, List())

  def get(c:Code, v: String, date: Option[DateTime]): List[Code] = {
    val normalizedMappingType: String = normalizeMappingType(v)
    mappings.getOrElse((c, normalizedMappingType), List.empty[V2MappingStruct])
      .filter(mappingStruct => filterDate(mappingStruct, date))
      .map(mappingStruct => mappingStruct.code)
  }

  def getReversed(c: Code, v: String, date: Option[DateTime]): List[Code] = {
    val normalizedMappingType: String = normalizeMappingType(v)
    reversedUnidirectionalMappings.getOrElse((c, normalizedMappingType), List.empty[V2MappingStruct])
      .filter(mappingStruct => filterDate(mappingStruct, date))
      .map(mappingStruct => mappingStruct.code)
  }

  def getMappings(): Map[(Code,String),List[V2MappingStruct]] = mappings

  def fromJson(obj: String)(implicit mapper: ObjectMapper with ScalaObjectMapper) : CodeMappingV2 = mapper.readValue[CodeMappingV2](obj)

}

case class V2MappingStruct(code: Code, start: DateTime, end: DateTime) extends DateModel {
  @Override
  def getStartDate(): DateTime = start

  @Override
  def getEndDate(): DateTime = end
}

object VersionRegex {
  implicit class RichRegex(val underlying: Regex) extends AnyVal {
    def matches(target: String): Boolean = underlying.pattern.matcher(target).matches
  }
}