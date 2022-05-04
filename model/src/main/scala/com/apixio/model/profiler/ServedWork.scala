package com.apixio.model.profiler

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.ScalaObjectMapper

case class ServedWork(
    @JsonProperty("transactionId") transactionId: String = "",
    @JsonProperty("workitem") workitem: WorkItem = WorkItem(),
    @JsonProperty("metadata") metadata: Map[String,Any] = Map()) {

  def asJson()(implicit mapper: ObjectMapper) = mapper.writeValueAsString(this)
}

object ServedWork {
  def fromJson(obj: String)(implicit mapper: ObjectMapper with ScalaObjectMapper) : ServedWork = mapper.readValue[ServedWork](obj)
}
