package com.apixio.scala.subtraction

import com.apixio.model.profiler.{Code, MonthMap}
import com.apixio.scala.apxapi.Project
import com.apixio.scala.dw.ApxServices
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, ScalaObjectMapper}
import org.joda.time.DateTime
import org.scalatest.DoNotDiscover
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

@DoNotDiscover
class SubtractionSpec extends AnyFlatSpec with Matchers {

  implicit val mapper = {
    val m = new ObjectMapper() with ScalaObjectMapper
    m.registerModule(DefaultScalaModule)
    m.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    m.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
    m
  }

  ApxServices.setupObjectMapper(new ObjectMapper())
  ApxServices.setupDefaultModels

  def dt(s: String) : DateTime = DateTime.parse(s)

  val proj = Project(0.0, "", "", "2015-12-31", "2015-01-01", "", "", "", "", "", "", "", "", "2017", "",
    "", Map[String,Map[String,Any]]("gen" -> Map[String,Any]("claimstype" -> "bycommon,empty")), 0.0, 0.0, "", true, "", "hcc")

  "Subtraction" should "handle just empty" in {
    Subtraction(proj, "").events() shouldBe empty
  }

  it should "handle some fancy empty combos" in {
    val proj2 = Project(0.0, "", "", "2015-12-31", "2015-01-01", "", "", "", "", "", "", "", "", "2017", "",
      "", Map[String,Map[String,Any]]("gen" -> Map[String,Any]("claimstype" -> "bycommon,(empty|empty|empty)&(empty&empty)")), 0.0, 0.0, "", true, "", "hcc")
    Subtraction(proj2, "").events() shouldBe empty
  }

  it should "be able to OR two lists of subtraction events together" in {
    val mm1 = new MonthMap(proj.start, proj.end)
    mm1.set(dt("2015-03-21"))
    val mm2 = new MonthMap(proj.start, proj.end, true)
    val events = List(
      SubtractionEvent(Code("25000",Code.ICD9), dt("2015-03-21"), dt("2015-12-30"), null, true, mm1, "TEST", dt("2016-12-30")),
      SubtractionEvent(Code("25000",Code.ICD9), dt("2015-03-21"), dt("2015-12-30"), Code("10",Code.RAPSPROV), true, mm2, "TEST", dt("2016-10-30")))
    val res = Subtraction(proj, "").or(List(events(0)), List(events(1)))
    res.size should be (1)
    res.head.ineligible.toLong should be (mm2.toLong)
    res.head.source should be ("TEST")
    res.head.loaded should be (events(0).loaded)
  }

  it should "be able to AND two lists of subtraction events together" in {
    val mm1 = new MonthMap(proj.start, proj.end)
    mm1.set(dt("2015-03-21"))
    val mm2 = new MonthMap(proj.start, proj.end, true)
    val events = List(
      SubtractionEvent(Code("25000",Code.ICD9), dt("2015-03-21"), dt("2015-12-30"), null, true, mm1, "TEST", dt("2016-12-30")),
      SubtractionEvent(Code("25000",Code.ICD9), dt("2015-03-21"), dt("2015-12-30"), Code("10",Code.RAPSPROV), true, mm2, "TEST", dt("2016-10-30")))
    val res = Subtraction(proj, "").and(List(events(0)), List(events(1)))
    res.size should be (1)
    res.head.ineligible.toLong should be (mm1.toLong)
    res.head.source should be ("TEST")
    res.head.loaded should be (events(0).loaded)
  }

  it should "be able to AND two lists of subtraction events together with no results" in {
    val mm1 = new MonthMap(proj.start, proj.end)
    mm1.set(dt("2015-03-21"))
    val mm2 = new MonthMap(proj.start, proj.end, true)
    val events = List(
      SubtractionEvent(Code("25000",Code.ICD9), dt("2015-03-21"), dt("2015-12-30"), null, true, mm1, "TEST", dt("2016-12-30")),
      SubtractionEvent(Code("25001",Code.ICD9), dt("2015-03-21"), dt("2015-12-30"), Code("10",Code.RAPSPROV), true, mm2, "TEST", dt("2016-10-30")))
    val res = Subtraction(proj, "").and(List(events(0)), List(events(1)))
    res.size should be (0)
  }

  it should "be able to process multiple subtraction events" in {
    val mm1 = new MonthMap(proj.start, proj.end)
    mm1.set(dt("2015-03-21"))
    val mm2 = new MonthMap(proj.start, proj.end, true)
    val events = List(
      SubtractionEvent(Code("25000",Code.ICD9), dt("2015-03-21"), dt("2015-12-30"), null, true, mm1, "TEST", dt("2016-12-30")),
      SubtractionEvent(Code("25001",Code.ICD9), dt("2015-03-21"), dt("2015-12-30"), Code("10",Code.RAPSPROV), true, mm2, "TEST", dt("2016-10-30")))
    val res = Subtraction(proj, "").process(events)
    res.size should be (1)
    res.head.fact.code should be (Code("19", "HCCV22"))
    res.head.evidence.source.uri should be ("TEST")
  }

  it should "be able to process multiple subtraction events with deletes" in {
    val mm1 = new MonthMap(proj.start, proj.end)
    mm1.set(dt("2015-03-21"))
    val mm2 = new MonthMap(proj.start, proj.end, true)
    val events = List(
      SubtractionEvent(Code("25000",Code.ICD9), dt("2015-03-21"), dt("2015-12-30"), null, true, mm1, "TEST", dt("2016-12-30")),
      SubtractionEvent(Code("25082",Code.ICD9), dt("2015-03-21"), dt("2015-12-30"), Code("10",Code.RAPSPROV), false, mm2, "TEST", dt("2016-10-30")))
    val res = Subtraction(proj, "").process(events)
    res.size should be (1)
    res.head.fact.code should be (Code("19", "HCCV22"))
    res.head.evidence.source.uri should be ("TEST")
  }

  it should "be able to process multiple subtraction events with multiple sources of HCCs" in {
    val mm1 = new MonthMap(proj.start, proj.end)
    mm1.set(dt("2015-03-21"))
    val mm2 = new MonthMap(proj.start, proj.end, true)
    val events = List(
      SubtractionEvent(Code("25000",Code.ICD9), dt("2015-03-21"), dt("2015-12-30"), null, true, mm1, "TEST", dt("2016-12-30")),
      SubtractionEvent(Code("E1152",Code.ICD10), dt("2015-03-21"), dt("2015-12-30"), Code("10",Code.RAPSPROV), true, mm2, "TEST", dt("2016-10-30")))
    val res = Subtraction(proj, "").process(events)
    val expected = List("19", "18", "106", "107", "108", "161", "189").map(x => Code(x, "HCCV22")).toSet
    res.size should be (expected.size)
    res.map(_.fact.code).toSet should be (expected)
    res.head.evidence.source.uri should be ("TEST")
  }

  it should "be able to process AND expression" in {
    val proj1 = Project(0.0, "", "", "2015-12-31", "2015-01-01", "", "", "", "", "", "", "", "", "2017", "",
  "", Map[String,Map[String,Any]]("gen" -> Map[String,Any]("claimstype" -> "bycommon,raps&mao")), 0.0, 0.0, "", true, "", "hcc")
    val mm1 = new MonthMap(proj.start, proj.end)
    mm1.set(dt("2015-03-21"))
    val mm2 = new MonthMap(proj.start, proj.end, true)

    val events = List(
      SubtractionEvent(Code("25000",Code.ICD9), dt("2015-03-21"), dt("2015-12-30"), null, true, mm1, "TEST", dt("2016-12-30")),
      SubtractionEvent(Code("E1152",Code.ICD10), dt("2015-03-21"), dt("2015-12-30"), Code("10",Code.RAPSPROV), true, mm2, "TEST", dt("2016-10-30")))

    val events2 = List(
      SubtractionEvent(Code("25001",Code.ICD9), dt("2015-03-21"), dt("2015-12-30"), null, true, mm1, "TEST", dt("2016-12-30"))
    )

    val subtraction = Subtraction(proj1, "")
    subtraction.cache.put("raps", events)
    subtraction.cache.put("mao", events)

    val expected = List("19", "18", "106", "107", "108", "161", "189").map(x => Code(x, "HCCV22")).toSet

    subtraction.events().map(_.fact.code).toSet should be (expected)

    subtraction.cache.put("mao", events2)

    subtraction.events() shouldBe (empty)

  }

  it should "be able to process OR expression" in {
    val proj1 = Project(0.0, "", "", "2015-12-31", "2015-01-01", "", "", "", "", "", "", "", "", "2017", "",
  "", Map[String,Map[String,Any]]("gen" -> Map[String,Any]("claimstype" -> "bycommon,raps|mao")), 0.0, 0.0, "", true, "", "hcc")
    val mm1 = new MonthMap(proj.start, proj.end)
    mm1.set(dt("2015-03-21"))
    val mm2 = new MonthMap(proj.start, proj.end, true)

    val events = List(
      SubtractionEvent(Code("25000",Code.ICD9), dt("2015-03-21"), dt("2015-12-30"), null, true, mm1, "TEST", dt("2016-12-30")),
      SubtractionEvent(Code("E1152",Code.ICD10), dt("2015-03-21"), dt("2015-12-30"), Code("10",Code.RAPSPROV), true, mm2, "TEST", dt("2016-10-30")))

    val events2 = List()

    val subtraction = Subtraction(proj1, "")
    subtraction.cache.put("raps", events)
    subtraction.cache.put("mao", events)

    val expected = List("19", "18", "106", "107", "108", "161", "189").map(x => Code(x, "HCCV22")).toSet

    subtraction.events().map(_.fact.code).toSet should be (expected)

    subtraction.cache.put("mao", events2)

    subtraction.events().map(_.fact.code).toSet should be (expected)

  }

  it should "be able to process XOR expression" in {
    val proj1 = Project(0.0, "", "", "2015-12-31", "2015-01-01", "", "", "", "", "", "", "", "", "2017", "",
  "", Map[String,Map[String,Any]]("gen" -> Map[String,Any]("claimstype" -> "bycommon,raps^mao")), 0.0, 0.0, "", true, "", "hcc")
    val mm1 = new MonthMap(proj.start, proj.end)
    mm1.set(dt("2015-03-21"))
    val mm2 = new MonthMap(proj.start, proj.end, true)

    val events = List(
      SubtractionEvent(Code("25000",Code.ICD9), dt("2015-03-21"), dt("2015-12-30"), null, true, mm1, "TEST", dt("2016-12-30")),
      SubtractionEvent(Code("E1152",Code.ICD10), dt("2015-03-21"), dt("2015-12-30"), Code("10",Code.RAPSPROV), true, mm2, "TEST", dt("2016-10-30")))

    val events2 = List(
      SubtractionEvent(Code("00323",Code.ICD9), dt("2015-03-21"), dt("2015-12-30"), null, true, mm1, "TEST", dt("2016-12-30"))
    )

    val subtraction = Subtraction(proj1, "")
    subtraction.cache.put("raps", events)
    subtraction.cache.put("mao", events)

    val expected = List("19", "18", "106", "107", "108", "161", "189", "39").map(x => Code(x, "HCCV22")).toSet

    subtraction.events() shouldBe (empty)

    subtraction.cache.put("mao", events2)

    subtraction.events().map(_.fact.code).toSet should be (expected)

  }

  it should "be able to process ANDNOT expression" in {
    val proj1 = Project(0.0, "", "", "2015-12-31", "2015-01-01", "", "", "", "", "", "", "", "", "2017", "",
  "", Map[String,Map[String,Any]]("gen" -> Map[String,Any]("claimstype" -> "bycommon,raps-mao")), 0.0, 0.0, "", true, "", "hcc")
    val mm1 = new MonthMap(proj.start, proj.end)
    mm1.set(dt("2015-03-21"))
    val mm2 = new MonthMap(proj.start, proj.end, true)

    val events = List(
      SubtractionEvent(Code("25000",Code.ICD9), dt("2015-03-21"), dt("2015-12-30"), null, true, mm1, "TEST", dt("2016-12-30")),
      SubtractionEvent(Code("E1152",Code.ICD10), dt("2015-03-21"), dt("2015-12-30"), Code("10",Code.RAPSPROV), true, mm2, "TEST", dt("2016-10-30")))

    val events2 = List(
      SubtractionEvent(Code("00323",Code.ICD9), dt("2015-03-21"), dt("2015-12-30"), null, true, mm1, "TEST", dt("2016-12-30"))
    )

    val subtraction = Subtraction(proj1, "")
    subtraction.cache.put("raps", events)
    subtraction.cache.put("mao", events)

    val expected = List("19", "18", "106", "107", "108", "161", "189").map(x => Code(x, "HCCV22")).toSet

    subtraction.events() shouldBe (empty)

    subtraction.cache.put("mao", events2)

    subtraction.events().map(_.fact.code).toSet should be (expected)

  }
}
