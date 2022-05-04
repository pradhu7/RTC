package com.apixio.model.profiler

import com.apixio.model.event.transformer.EventTypeListJSONParser
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.ScalaObjectMapper
import org.junit.runner.RunWith
import org.joda.time.LocalDate
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest._
import matchers.should._
import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class WorkItemSpec extends AnyFlatSpec with Matchers {

  implicit val mapper = {
    val m = new ObjectMapper() with ScalaObjectMapper
    m.registerModule(DefaultScalaModule)
    m.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    m.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
    m
  }

  "WorkItem" should "objects should correctly serialize and deserialize" in {
    val wi = WorkItem()
    WorkItem.fromJson(wi.asJson) should equal (wi)
  }

  it should "handle setting score correctly" in {
    var wi : WorkItem = WorkItem()
    wi = wi.score(1.0)
    val nwi = WorkItem.fromJson(wi.asJson)
    nwi should equal (wi)
    nwi.scores(0) should equal (1.0)
  }

  it should "correctly serialize and deserialize with coverage range" in {
    val coverageRange = List(CoverageRange(new LocalDate(2016,1,1), new LocalDate(2016,12,31)))
    val wi = WorkItem(coverage=coverageRange)
    WorkItem.fromJson(wi.asJson) should equal (wi)
  }

  it should "correctly serialize and deserialize with multiple coverage ranges" in {
    val coverageRange = List(
      CoverageRange(new LocalDate(2015,1,1), new LocalDate(2015,6,30)),
      CoverageRange(new LocalDate(2016,1,1), new LocalDate(2016,12,31)),
      CoverageRange(new LocalDate(2016,2,1), new LocalDate(2016,3,1))
    )
    val wi = WorkItem(coverage=coverageRange)
    WorkItem.fromJson(wi.asJson) should equal (wi)
  }

}
