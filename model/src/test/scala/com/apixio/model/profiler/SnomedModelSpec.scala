package com.apixio.model.profiler

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, ScalaObjectMapper}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SnomedModelSpec extends AnyFlatSpec with Matchers {

  implicit val mapper = {
    val m = new ObjectMapper() with ScalaObjectMapper
    m.registerModule(DefaultScalaModule)
    m.registerModule(new JodaModule)
    m.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    m.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
    m
  }

  "SnomedModel" should "SnomedModel serialize and deserialize" in {
    val snomedModel = SnomedModel("", "", "", "2020-01-01", "2020-09-30", "", "", "")
    val json = snomedModel.asJson
    val obj: SnomedModel = SnomedModel.fromJson(json)
    assert(obj.start == "2020-01-01")
    assert(obj.end == "2020-09-30")
  }
}
