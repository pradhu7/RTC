package com.apixio.model.quality

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.ScalaObjectMapper
import java.io.InputStream

import org.joda.time.DateTime
import com.apixio.model.profiler.Code

case class FactModel(
                      @JsonProperty("c") code: String = "",
                      @JsonProperty("d") description: String = "",
                      @JsonProperty("v") version: String = "",
                      @JsonProperty("date") earliestDayEligible: Option[String] = None, // "MM-DD"
                      @JsonProperty("y") yearsEligible: Int = 0){

  def asCode() : Code = Code(code, Code.QUALITYFACT + version)

  def getEligibilityStartDate(measurementYear: String): DateTime = {
    earliestDayEligible.map(ed => DateTime.parse(s"${measurementYear}-${ed}"))
      .getOrElse(DateTime.parse(s"${measurementYear}-01-01"))
      .minusYears(yearsEligible)
  }

  def asJson()(implicit mapper: ObjectMapper) = mapper.writeValueAsString(this)
}

object FactModel {
  private var model : Map[Code,FactModel] = Map()

  def init(modelData : InputStream)(implicit mapper: ObjectMapper with ScalaObjectMapper) : Unit =  {
    model.synchronized {
      val data = mapper.readValue[List[FactModel]](modelData)
      model = data.map(x => x.asCode -> x).toMap
    }
  }

  def get(c: Code) : Option[FactModel] = model.get(c)

  def getAll() : List[FactModel] = model.values.toList

  def fromJson(obj: String)(implicit mapper: ObjectMapper with ScalaObjectMapper) : FactModel = mapper.readValue[FactModel](obj)
}
