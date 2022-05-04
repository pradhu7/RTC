package com.apixio.model.quality

import com.apixio.model.utils.TestUtils
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.ScalaObjectMapper
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest._
import matchers.should._

@RunWith(classOf[JUnitRunner])
class MeasureModelSpec extends AnyFlatSpec with Matchers {

  implicit val mapper = {
    val m = new ObjectMapper() with ScalaObjectMapper
    m.registerModule(DefaultScalaModule)
    m.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    m.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
    m
  }
  var mappings : List[MeasureModel] = List()

  "MeasureModel" should "be able to be loaded from resources" in {
    MeasureModel.init(TestUtils.getCodeMappingIfNotExist(TestUtils.QUALITYMEASURE_MODEL))
    mappings = MeasureModel.getAll
    mappings should not be empty
  }

  it should "should have a description for all measure" in {
    mappings.foreach { m =>
      m.description should not be (null)
      m.description should not be empty
      m.description.size should be > 2
    }
  }

}
