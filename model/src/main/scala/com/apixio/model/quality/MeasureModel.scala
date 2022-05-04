package com.apixio.model.quality

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.ScalaObjectMapper
import java.io.InputStream

import com.apixio.model.profiler.Code

case class MeasureModel(
                            @JsonProperty("c") code: String = "",
                            @JsonProperty("d") description:String = "",
                            @JsonProperty("v") version: String = "") {

  def asCode() : Code = Code(code, Code.QUALITYMEASURE + version)

  def asJson()(implicit mapper: ObjectMapper) = mapper.writeValueAsString(this)
}

object MeasureModel {
  private var model : Map[Code,MeasureModel] = Map()

  def init(modelData : InputStream)(implicit mapper: ObjectMapper with ScalaObjectMapper) : Unit =  {
    model.synchronized {
      val data = mapper.readValue[List[MeasureModel]](modelData)
      model = data.map(x => x.asCode -> x).toMap
    }
  }

  def get(c: Code) : Option[MeasureModel] = model.get(c)

  def getAll() : List[MeasureModel] = model.values.toList

  def fromJson(obj: String)(implicit mapper: ObjectMapper with ScalaObjectMapper) : MeasureModel = mapper.readValue[MeasureModel](obj)
}
