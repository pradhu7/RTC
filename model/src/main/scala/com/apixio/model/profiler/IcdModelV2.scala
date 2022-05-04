package com.apixio.model.profiler

import com.apixio.model.utility.{DateFunctions, DateModel}
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.ScalaObjectMapper
import org.joda.time.{DateTime, DateTimeZone}

import java.io.InputStream

case class IcdModelV2(
     @JsonProperty("c") code: String = "",
     @JsonProperty("s") system: String = "",
     @JsonProperty("d") description: String = "",
     @JsonProperty("start") start: String = "",
     @JsonProperty("end") end: String = "") extends DateModel {

  @Override
  def getStartDate(): DateTime = DateTime.parse(start, mappingDtf).withZone(DateTimeZone.UTC)

  @Override
  def getEndDate(): DateTime = DateTime.parse(end, mappingDtf).withZone(DateTimeZone.UTC)

  def asCode() : Code = Code(code, system)

  def asJson()(implicit mapper: ObjectMapper) = mapper.writeValueAsString(this)
}

object IcdModelV2 extends DateFunctions {
  private var model : Map[Code,List[IcdModelV2]] = Map()

  def init(modelData : InputStream)(implicit mapper: ObjectMapper with ScalaObjectMapper) : Unit =  {
    model.synchronized {
      val data = mapper.readValue[List[IcdModelV2]](modelData)
      model = data.groupBy(_.asCode())
    }
  }

  def get(c: Code, dateTime: Option[DateTime]): Option[List[IcdModelV2]] = {
    model.get(c) match {
      case Some(icdModels) =>
        val filteredCodes: List[IcdModelV2] = icdModels.filter(icdModel => filterDate(icdModel, dateTime))
        Option(filteredCodes).filter(_.nonEmpty)
      case None => None
    }
  }

  def getAll() : List[IcdModelV2] = model.values.flatten.toList

  def fromJson(obj: String)(implicit mapper: ObjectMapper with ScalaObjectMapper) : IcdModelV2 = mapper.readValue[IcdModelV2](obj)
}