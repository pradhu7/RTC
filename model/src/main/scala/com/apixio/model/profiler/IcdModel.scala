package com.apixio.model.profiler

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.ScalaObjectMapper
import java.io.InputStream

case class IcdModel(
    @JsonProperty("c") code:String = "",
    @JsonProperty("s") system:String = "",
    @JsonProperty("d") description:String = "") {

  def asCode() : Code = Code(code, system)

  def asJson()(implicit mapper: ObjectMapper) = mapper.writeValueAsString(this)
}

object IcdModel {
  private var model : Map[Code,IcdModel] = Map()

  def init(modelData : InputStream)(implicit mapper: ObjectMapper with ScalaObjectMapper) : Unit =  {
    model.synchronized {
      val data = mapper.readValue[List[IcdModel]](modelData)
      model = data.map(x => x.asCode -> x).toMap
    }
  }

  def get(c: Code) : Option[IcdModel] = model.get(c)

  def getAll() : List[IcdModel] = model.values.toList

  def fromJson(obj: String)(implicit mapper: ObjectMapper with ScalaObjectMapper) : IcdModel = mapper.readValue[IcdModel](obj)
}
