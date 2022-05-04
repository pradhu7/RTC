package com.apixio.model.utility

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest._
import matchers.should._
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.ScalaObjectMapper
import com.apixio.model.patient._
import com.apixio.model.profiler._
import com.apixio.model.utils.TestUtils

import scala.collection.JavaConverters._
import org.joda.time.DateTime

@RunWith(classOf[JUnitRunner])
class ClaimsProcessingSpec extends AnyFlatSpec with Matchers {

  val patient = "somerandom"
  val start = DateTime.parse("2015-01-01")
  val end = DateTime.parse("2015-12-31")
  val icdMapping = "2016-icd-hcc"

  implicit val mapper = {
    val m = new ObjectMapper() with ScalaObjectMapper
    m.registerModule(DefaultScalaModule)
    m.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    m.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
    m
  }

  HccModel.init(TestUtils.getCodeMappingIfNotExist(TestUtils.HCC_MODEL))
  IcdModel.init(TestUtils.getCodeMappingIfNotExist(TestUtils.ICD_MODEL))
  CodeMapping.init(TestUtils.getCodeMappingIfNotExist(TestUtils.CODE_MAPPING))

  def problem(start: DateTime, end: DateTime, c: Code, tdate: String, delete: Boolean, errors: List[String]) : Problem = {
    val p = new Problem()
    val cc = new ClinicalCode()
    val m = Map("TRANSACTION_DATE" -> tdate, "DELETE_INDICATOR" -> delete.toString) ++ errors.zipWithIndex.map(x => (s"TESTING_ERROR${x._2}" -> x._1)).toMap

    cc.setCode(c.code)
    cc.setCodingSystemOID(c.system)

    p.setLastEditDateTime(DateTime.now())
    p.setStartDate(start)
    p.setEndDate(end)
    p.setCode(cc)
    p.setMetadata(m.asJava)
    p
  }

  "ClaimsProcessingSpec" should "roll up a problem into an ineligible event" in {
    val probs = List(problem(DateTime.parse("2015-03-21"), DateTime.parse("2015-03-21"), Code("24900", Code.ICD9), "2016-03-21", false, List()))
    val events = ClaimsProcessing.claimsByProblems(start, end, icdMapping, patient, probs)
    events.size should be (1)
    events.head.isInEligible should be (true)
    events.head.fact.code.code should be ("19")
    events.head.evidence.attributes("monthMap").toLong should be (4095L)
  }

  it should "merge a cluster into an ineligible event" in {
    val probs = List(problem(DateTime.parse("2015-03-21"), DateTime.parse("2015-03-21"), Code("24900", Code.ICD9), "2016-03-21", false, List()),
                     problem(DateTime.parse("2015-03-21"), DateTime.parse("2015-03-21"), Code("24900", Code.ICD9), "2016-03-21", false, List()))
    val events = ClaimsProcessing.claimsByProblems(start, end, icdMapping, patient, probs)
    events.size should be (1)
    events.head.isInEligible should be (true)
    events.head.fact.code.code should be ("19")
    events.head.evidence.attributes("monthMap").toLong should be (4095L)
  }

  it should "merge a cluster with a delete" in {
    val probs = List(problem(DateTime.parse("2015-03-21"), DateTime.parse("2015-03-21"), Code("24900", Code.ICD9), "2016-03-21", true, List()),
                     problem(DateTime.parse("2015-03-21"), DateTime.parse("2015-03-21"), Code("24900", Code.ICD9), "2016-03-20", false, List()))
    val events = ClaimsProcessing.claimsByProblems(start, end, icdMapping, patient, probs)
    events.size should be (0)
  }

  it should "merge a cluster with claim error code" in {
    val probs = List(problem(DateTime.parse("2015-03-21"), DateTime.parse("2015-03-21"), Code("24900", Code.ICD9), "2016-03-21", false, List()),
                     problem(DateTime.parse("2015-03-21"), DateTime.parse("2015-03-21"), Code("24900", Code.ICD9), "2016-03-21", false, List("453")))
    val events = ClaimsProcessing.claimsByProblems(start, end, icdMapping, patient, probs)
    events.size should be (1)
    events.head.isInEligible should be (true)
    events.head.fact.code.code should be ("19")
    events.head.evidence.attributes("monthMap").toLong should be (4095L)
  }

  it should "merge a cluster with date error code" in {
    val probs = List(problem(DateTime.parse("2015-01-21"), DateTime.parse("2015-01-21"), Code("24900", Code.ICD9), "2016-03-21", false, List()),
      problem(DateTime.parse("2015-01-21"), DateTime.parse("2015-01-21"), Code("24900", Code.ICD9), "2016-03-21", false, List("409")))
    val events = ClaimsProcessing.claimsByProblems(start, end, icdMapping, patient, probs)
    events.size should be (1)
    events.head.isInEligible should be (true)
    events.head.fact.code.code should be ("19")
    events.head.evidence.attributes("monthMap").toLong should be (4095L)
  }

  it should "merge a cluster with error codes and deletes" in {
    val probs = List(problem(DateTime.parse("2015-03-21"), DateTime.parse("2015-03-21"), Code("24900", Code.ICD9), "2016-03-21", false, List()),
      problem(DateTime.parse("2015-03-21"), DateTime.parse("2015-03-21"), Code("24900", Code.ICD9), "2016-03-21", true, List()),
      problem(DateTime.parse("2015-03-21"), DateTime.parse("2015-03-21"), Code("24900", Code.ICD9), "2016-03-21", false, List("453")))
    val events = ClaimsProcessing.claimsByProblems(start, end, icdMapping, patient, probs)
    events.size should be (1)
    events.head.isInEligible should be (true)
    events.head.fact.code.code should be ("19")
    events.head.evidence.attributes("monthMap").toLong should be (4095L)
  }

  it should "not merge a cluster with only error and deletes" in {
    val probs = List(problem(DateTime.parse("2015-03-21"), DateTime.parse("2015-03-21"), Code("24900", Code.ICD9), "2016-03-21", true, List()),
      problem(DateTime.parse("2015-03-21"), DateTime.parse("2015-03-21"), Code("24900", Code.ICD9), "2016-03-21", false, List("453")))
    val events = ClaimsProcessing.claimsByProblems(start, end, icdMapping, patient, probs)
    events.size should be (0)
  }

  it should "throw for unknown error" in {
    val probs = List(problem(DateTime.parse("2015-03-21"), DateTime.parse("2015-03-21"), Code("24900", Code.ICD9), "2016-03-21", true, List("123")))
    try { // TODO: Refactor this to user assertThrows[AssertionError] once we update scalatest to at least 2.2.6
      ClaimsProcessing.claimsByProblems(start, end, icdMapping, patient, probs)
      throw(new Error("Should never get here"))
    } catch {
      case _:AssertionError => None
      case e: Throwable => throw(e)
    }
  }

  it should "merge multiple clusters into an ineligible event" in {
    val probs = List(problem(DateTime.parse("2015-03-21"), DateTime.parse("2015-03-21"), Code("24900", Code.ICD9), "2016-03-21", true, List()),
                     problem(DateTime.parse("2015-03-21"), DateTime.parse("2015-03-21"), Code("24900", Code.ICD9), "2016-03-21", false, List("453")),
                     problem(DateTime.parse("2015-01-21"), DateTime.parse("2015-01-21"), Code("24900", Code.ICD9), "2016-03-20", false, List()),
                     problem(DateTime.parse("2015-01-21"), DateTime.parse("2015-01-21"), Code("24900", Code.ICD9), "2016-03-20", false, List("453")))
    val events = ClaimsProcessing.claimsByProblems(start, end, icdMapping, patient, probs)
    events.size should be (1)
    events.head.isInEligible should be (true)
    events.head.fact.code.code should be ("19")
    events.head.evidence.attributes("monthMap").toLong should be (4095L)
  }

  it should "merge multiple clusters into multiple ineligible events (with hoisting)" in {
    val probs = List(problem(DateTime.parse("2015-03-21"), DateTime.parse("2015-03-21"), Code("24900", Code.ICD9), "2016-03-21", true, List()),
                     problem(DateTime.parse("2015-03-21"), DateTime.parse("2015-03-21"), Code("24900", Code.ICD9), "2016-03-21", false, List()),
                     problem(DateTime.parse("2015-01-21"), DateTime.parse("2015-01-21"), Code("496", Code.ICD9), "2016-03-20", false, List()),
                     problem(DateTime.parse("2015-01-21"), DateTime.parse("2015-01-21"), Code("496", Code.ICD9), "2016-03-20", false, List()))
    val events = ClaimsProcessing.claimsByProblems(start, end, icdMapping, patient, probs)
    events.size should be (3)
    events.map(_.fact.code.code).toSet should be (Set("19","111","112"))
    events.foreach { x =>
      x.isInEligible should be (true)
      x.evidence.attributes("monthMap").toLong should be (4095L)
    }
  }

  it should "roll up a problem into a lynty event" in {
    val probs = List(problem(DateTime.parse("2014-03-21"), DateTime.parse("2014-03-21"), Code("24900", Code.ICD9), "2015-03-21", false, List()))
    val events = ClaimsProcessing.lyntyByProblems(start, end, icdMapping, patient, probs)
    events.size should be (1)
    events.head.isLynty should be (true)
    events.head.fact.code.code should be ("19")
  }

  it should "merge a cluster into a lynty event" in {
    val probs = List(problem(DateTime.parse("2014-03-21"), DateTime.parse("2014-03-21"), Code("24900", Code.ICD9), "2015-03-21", false, List()),
                     problem(DateTime.parse("2014-03-21"), DateTime.parse("2014-03-21"), Code("24900", Code.ICD9), "2015-03-21", false, List()))
    val events = ClaimsProcessing.lyntyByProblems(start, end, icdMapping, patient, probs)
    events.size should be (1)
    events.head.isLynty should be (true)
    events.head.fact.code.code should be ("19")
  }

  it should "not merge a cluster into a lynty event with a delete" in {
    val probs = List(problem(DateTime.parse("2014-03-21"), DateTime.parse("2014-03-21"), Code("24900", Code.ICD9), "2015-03-21", true, List()),
                     problem(DateTime.parse("2014-03-21"), DateTime.parse("2014-03-21"), Code("24900", Code.ICD9), "2015-03-21", false, List()))
    val events = ClaimsProcessing.lyntyByProblems(start, end, icdMapping, patient, probs)
    events.size should be (0)
  }

  it should "merge a cluster into a lynty event with error codes" in {
    val probs = List(problem(DateTime.parse("2014-03-21"), DateTime.parse("2014-03-21"), Code("24900", Code.ICD9), "2015-03-21", false, List("421", "423")),
                     problem(DateTime.parse("2014-03-21"), DateTime.parse("2014-03-21"), Code("24900", Code.ICD9), "2015-03-21", false, List("406", "407")))
    val events = ClaimsProcessing.lyntyByProblems(start, end, icdMapping, patient, probs)
    events.size should be (1)
    events.head.isLynty should be (true)
    events.head.fact.code.code should be ("19")
  }

  it should "merge multiple clusters into a lynty event" in {
    val probs = List(problem(DateTime.parse("2014-03-21"), DateTime.parse("2014-03-21"), Code("24900", Code.ICD9), "2015-03-21", true, List()),
                     problem(DateTime.parse("2014-03-21"), DateTime.parse("2014-03-21"), Code("24900", Code.ICD9), "2015-03-21", false, List("453")),
                     problem(DateTime.parse("2014-01-21"), DateTime.parse("2014-01-21"), Code("24900", Code.ICD9), "2015-03-20", false, List()),
                     problem(DateTime.parse("2014-01-21"), DateTime.parse("2014-01-21"), Code("24900", Code.ICD9), "2015-03-20", false, List("453")))
    val events = ClaimsProcessing.lyntyByProblems(start, end, icdMapping, patient, probs)
    events.size should be (1)
    events.head.isLynty should be (true)
    events.head.fact.code.code should be ("19")
  }

  it should "merge multiple clusters into multiple lynty events (without hoisting)" in {
    val probs = List(problem(DateTime.parse("2014-03-21"), DateTime.parse("2014-03-21"), Code("24900", Code.ICD9), "2015-03-21", false, List("453")),
                     problem(DateTime.parse("2016-01-21"), DateTime.parse("2016-01-21"), Code("496", Code.ICD9), "2016-03-20", false, List()),
                     problem(DateTime.parse("2016-01-21"), DateTime.parse("2016-01-21"), Code("496", Code.ICD9), "2016-03-20", false, List("453")))
    val events = ClaimsProcessing.lyntyByProblems(start, end, icdMapping, patient, probs)
    events.size should be (2)
    events.map(_.fact.code.code).toSet should be (Set("19","111"))
    events.foreach { x =>
      x.isLynty should be (true)
    }
  }
}
