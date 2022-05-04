package com.apixio.model.profiler

import com.apixio.model.utility.{DateFunctions, DateModel}
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.ScalaObjectMapper
import org.joda.time.{DateTime, DateTimeZone}

import java.io.InputStream

case class SnomedModel(@JsonProperty("c") code: String = "",
                       @JsonProperty("s") system: String = "",
                       @JsonProperty("d") description: String = "",
                       @JsonProperty("start") start: String = "",
                       @JsonProperty("end") end: String = "",
                       @JsonProperty("mid") module_id: String = "",
                       @JsonProperty("tid") type_id: String = "",
                       @JsonProperty("cs_id") case_significance_id: String = "") extends DateModel {
  def getStartDate(): DateTime = DateTime.parse(start, mappingDtf).withZone(DateTimeZone.UTC)

  def getEndDate(): DateTime = DateTime.parse(end, mappingDtf).withZone(DateTimeZone.UTC)

  def asCode() : Code = Code(code, system)

  def asJson()(implicit mapper: ObjectMapper) = {
    mapper.writeValueAsString(this)
  }
}

object SnomedModel extends DateFunctions {
  private var model : Map[Code, List[SnomedModel]] = Map()

  def init(modelData : InputStream)(implicit mapper: ObjectMapper with ScalaObjectMapper) : Unit =  {
    model.synchronized {
      val data = mapper.readValue[List[SnomedModel]](modelData)
      model = data.groupBy(_.asCode())
    }
  }

  def get(c: Code, dateTime: Option[DateTime]): Option[List[SnomedModel]] = {
    model.get(c) match {
      case Some(snomedModels) =>
        val filteredCodes: List[SnomedModel] = snomedModels.filter(model => filterDate(model, dateTime))
        Option(filteredCodes).filter(_.nonEmpty)
      case None => None
    }
  }

  def getAll() : List[SnomedModel] = model.values.flatten.toList

  def fromJson(obj: String)(implicit mapper: ObjectMapper with ScalaObjectMapper): SnomedModel = mapper.readValue[SnomedModel](obj)
}