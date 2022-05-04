package com.apixio.model.quality

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.ScalaObjectMapper
import java.io.InputStream

import com.apixio.model.profiler.Code

case class ConjectureModel(
                     @JsonProperty("c") code: String = "",
                     @JsonProperty("d") description:String = "",
                     @JsonProperty("v") version: String = "",
                     @JsonProperty("af") annotationFields: Seq[AnnotationField] = Seq.empty[AnnotationField])
                      {

  def asCode() : Code = Code(code, Code.QUALITYCONJECTURE + version)

  def asJson()(implicit mapper: ObjectMapper) = mapper.writeValueAsString(this)
}

object ConjectureModel {
  private var model : Map[Code,ConjectureModel] = Map()

  def init(modelData : InputStream)(implicit mapper: ObjectMapper with ScalaObjectMapper) : Unit =  {
    model.synchronized {
      val data = mapper.readValue[List[ConjectureModel]](modelData)
      model = data.map(x => x.asCode -> x).toMap
    }
  }

  def get(c: Code) : Option[ConjectureModel] = model.get(c)

  def getAll() : List[ConjectureModel] = model.values.toList

  def fromJson(obj: String)(implicit mapper: ObjectMapper with ScalaObjectMapper) : ConjectureModel = mapper.readValue[ConjectureModel](obj)
}

case class AnnotationField(@JsonProperty("n") field: String, @JsonProperty("r") required: Boolean){
  def asJson()(implicit mapper: ObjectMapper) = mapper.writeValueAsString(this)
}

object AnnotationField {

  def fromJson(obj: String)(implicit mapper: ObjectMapper with ScalaObjectMapper) : AnnotationField = mapper.readValue[AnnotationField](obj)
}
