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
class FactModelSpec extends AnyFlatSpec with Matchers {

  implicit val mapper = {
    val m = new ObjectMapper() with ScalaObjectMapper
    m.registerModule(DefaultScalaModule)
    m.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    m.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
    m
  }
  var mappings : List[FactModel] = List()

  "FactModel" should "be able to be loaded from " in {
    FactModel.init(TestUtils.getCodeMappingIfNotExist(TestUtils.QUALITYFACT_MODEL))
    mappings = FactModel.getAll
    mappings should not be empty
  }

  "FactModel" should "have a valid earliest date for all facts" in {
    FactModel.init(TestUtils.getCodeMappingIfNotExist(TestUtils.QUALITYFACT_MODEL))
    mappings.foreach { m =>
      m.earliestDayEligible should not be (null)
      assert(m.earliestDayEligible.map(_.matches("\\d{2}-\\d{2}")).getOrElse(true))
      val measurementYear = "2018"
      val earliestDate = m.getEligibilityStartDate(measurementYear)
      earliestDate should not be (null)
    }
  }

  it should "should have a description for all facts" in {
    mappings.foreach { m =>
      m.description should not be (null)
      m.description should not be empty
      m.description.size should be > 2
    }
  }

  it should "should have a year eligibility for all facts" in {
    mappings.foreach { m =>
      assert(m.yearsEligible.isValidInt)
    }
  }

}
