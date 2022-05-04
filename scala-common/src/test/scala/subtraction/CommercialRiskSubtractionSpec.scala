package com.apixio.scala.subtraction

import com.apixio.model.profiler._
import com.apixio.scala.apxapi.Project
import com.apixio.scala.dw.{ApxConfiguration, ApxServices}
import com.apixio.scala.utility.Mapping
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, ScalaObjectMapper}
import org.joda.time.DateTime
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CommercialRiskSubtractionSpec extends AnyFlatSpec with Matchers {

  implicit val mapper = {
    val m = new ObjectMapper() with ScalaObjectMapper
    m.registerModule(DefaultScalaModule)
    m.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    m.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
    m
  }
  ApxConfiguration.initializeFromFile("application-dev.yaml")
  ApxServices.init(ApxConfiguration.configuration.get)
  CptModel.init(Mapping.getCptModel())
  BillTypeModel.init(Mapping.getBilltypeModel())(mapper)

  def dt(s: String) : DateTime = DateTime.parse(s)
  def ce(c: Code, cd: DateTime, tr: String, cl: String, pd: String, td: String, d: Boolean = false) : EventTypeX =
    EventTypeX(Map.empty,
      EvidenceTypeX(List("transactionDate" -> Some(td), "transactionType" -> (if (d) Some("DELETE") else None), "processingDate" -> Some(pd),
          "version" -> Some(CommercialRiskSubtraction.version)).filter(_._2.nonEmpty).map(x => x._1 -> x._2.get).toMap, false,
        ReferenceTypeX(RAPSSubtraction.rapsType, tr)),
      FactTypeX(c, TimeRangeTypeX(cd, cd), Map()),
      ReferenceTypeX("FeeForServiceInsuranceClaim", cl),
      null
    )

  val proj = Project(0.0, "", "", "2015-12-31", "2015-01-01", "", "", "", "", "", "", "", "", "2016", "",
    "", Map.empty, 0.0, 0.0, "", true, "", "hcc")

  "CommercialRiskSubtraction" should "handle extraction fields on generated event" in {
    val event = ce(Code("25000",Code.ICD9), dt("2015-03-21"), "1", "1", "2016-10-30", "2016-10-30")
    val sub = new CommercialRiskSubtraction(proj, "")
    RAPSSubtraction.getVersion(event) should equal (CommercialRiskSubtraction.version)
  }

  it should "filter and process a single event" in {
    val sub = new CommercialRiskSubtraction(proj, "")
    val res = sub.process(sub.filter(List(ce(Code("25000",Code.ICD9), dt("2015-03-21"), "1", "1", "2016-10-30", "2016-10-30"))))
    res.size should equal (0)
  }

  // it should "process a professional transaction" in {
  //   val events = List(ce(Code("25000",Code.ICD9), dt("2015-03-21"), "1", "1", "2016-10-30", "2016-10-30"),
  //                     ce(Code("P",Code.FFSPROV), dt("2015-03-21"), "1", "1", "2016-10-30", "2016-10-30"),
  //                     ce(Code("00100",Code.CPT), dt("2015-03-21"), "1", "1", "2016-10-30", "2016-10-30"))
  //   val sub = new CommercialRiskSubtraction(proj, "")
  //   val res = sub.process(sub.filter(events))
  //   res.size should equal (1)
  //   res.head.ineligible.toLong should equal (4095)
  // }

  // it should "process an inpatient transaction" in {
  //   val events = List(ce(Code("25000",Code.ICD9), dt("2015-03-21"), "1", "1", "2016-10-30", "2016-10-30"),
  //                     ce(Code("I",Code.FFSPROV), dt("2015-03-21"), "1", "1", "2016-10-30", "2016-10-30"),
  //                     ce(Code("114",Code.BILLTYPE), dt("2015-03-21"), "1", "1", "2016-10-30", "2016-10-30"),
  //                     ce(Code("00100",Code.CPT), dt("2015-03-21"), "1", "1", "2016-10-30", "2016-10-30"))
  //   val sub = new CommercialRiskSubtraction(proj, "")
  //   val res = sub.process(sub.filter(events))
  //   res.size should equal (1)
  //   res.head.ineligible.toLong should equal (4095)
  // }

  // it should "process an outpatient transaction" in {
  //   val events = List(ce(Code("25000",Code.ICD9), dt("2015-03-21"), "1", "1", "2016-10-30", "2016-10-30"),
  //                     ce(Code("O",Code.FFSPROV), dt("2015-03-21"), "1", "1", "2016-10-30", "2016-10-30"),
  //                     ce(Code("766",Code.BILLTYPE), dt("2015-03-21"), "1", "1", "2016-10-30", "2016-10-30"),
  //                     ce(Code("00100",Code.CPT), dt("2015-03-21"), "1", "1", "2016-10-30", "2016-10-30"))
  //   val sub = new CommercialRiskSubtraction(proj, "")
  //   val res = sub.process(sub.filter(events))
  //   res.size should equal (1)
  //   res.head.ineligible.toLong should equal (4095)
  // }

  it should "process an inpatient transaction without billtype" in {
    val events = List(ce(Code("25000",Code.ICD9), dt("2015-03-21"), "1", "1", "2016-10-30", "2016-10-30"),
                      ce(Code("I",Code.FFSPROV), dt("2015-03-21"), "1", "1", "2016-10-30", "2016-10-30"),
                      ce(Code("00100",Code.CPT), dt("2015-03-21"), "1", "1", "2016-10-30", "2016-10-30"))
    val sub = new CommercialRiskSubtraction(proj, "")
    val res = sub.process(sub.filter(events))
    res.size should equal (0)
  }

  it should "process an inpatient transaction without valid billtype" in {
    val events = List(ce(Code("25000",Code.ICD9), dt("2015-03-21"), "1", "1", "2016-10-30", "2016-10-30"),
                      ce(Code("O",Code.FFSPROV), dt("2015-03-21"), "1", "1", "2016-10-30", "2016-10-30"),
                      ce(Code("114",Code.BILLTYPE), dt("2015-03-21"), "1", "1", "2016-10-30", "2016-10-30"),
                      ce(Code("00100",Code.CPT), dt("2015-03-21"), "1", "1", "2016-10-30", "2016-10-30"))
    val sub = new CommercialRiskSubtraction(proj, "")
    val res = sub.process(sub.filter(events))
    res.size should equal (0)
  }

  // it should "process multiple transactions in one cluster" in {
  //   val events = List(ce(Code("25000",Code.ICD9), dt("2015-03-21"), "1", "1", "2016-10-30", "2016-10-30"),
  //                     ce(Code("I",Code.FFSPROV), dt("2015-03-21"), "1", "1", "2016-10-30", "2016-10-30"),
  //                     ce(Code("114",Code.BILLTYPE), dt("2015-03-21"), "1", "1", "2016-10-30", "2016-10-30"),
  //                     ce(Code("00100",Code.CPT), dt("2015-03-21"), "1", "1", "2016-10-30", "2016-10-30"),
  //                     ce(Code("25001",Code.ICD9), dt("2015-03-21"), "2", "1", "2016-10-31", "2016-10-31"),
  //                     ce(Code("I",Code.FFSPROV), dt("2015-03-21"), "2", "1", "2016-10-31", "2016-10-31"),
  //                     ce(Code("114",Code.BILLTYPE), dt("2015-03-21"), "2", "1", "2016-10-31", "2016-10-31"),
  //                     ce(Code("00100",Code.CPT), dt("2015-03-21"), "2", "1", "2016-10-31", "2016-10-31"))
  //   val sub = new CommercialRiskSubtraction(proj, "")
  //   val res = sub.process(sub.filter(events))
  //   res.size should equal (2)
  //   res.head.ineligible.toLong should equal (4095)
  //   res.head.code should equal (Code("25000",Code.ICD9))
  //   res.last.code should equal (Code("25001",Code.ICD9))
  // }

  // it should "process multiple clusters" in {
  //   val events = List(ce(Code("25000",Code.ICD9), dt("2015-03-21"), "1", "1", "2016-10-30", "2016-10-30"),
  //                     ce(Code("I",Code.FFSPROV), dt("2015-03-21"), "1", "1", "2016-10-30", "2016-10-30"),
  //                     ce(Code("114",Code.BILLTYPE), dt("2015-03-21"), "1", "1", "2016-10-30", "2016-10-30"),
  //                     ce(Code("00100",Code.CPT), dt("2015-03-21"), "1", "1", "2016-10-30", "2016-10-30"),
  //                     ce(Code("25001",Code.ICD9), dt("2015-03-21"), "2", "2", "2016-10-31", "2016-10-31"),
  //                     ce(Code("I",Code.FFSPROV), dt("2015-03-21"), "2", "2", "2016-10-31", "2016-10-31"),
  //                     ce(Code("114",Code.BILLTYPE), dt("2015-03-21"), "2", "2", "2016-10-31", "2016-10-31"),
  //                     ce(Code("00100",Code.CPT), dt("2015-03-21"), "2", "2", "2016-10-31", "2016-10-31"))
  //   val sub = new CommercialRiskSubtraction(proj, "")
  //   val res = sub.process(sub.filter(events))
  //   res.size should equal (2)
  //   res.head.ineligible.toLong should equal (4095)
  //   res.last.ineligible.toLong should equal (4095)
  // }

  it should "process an invalid cpt transaction" in {
    val events = List(ce(Code("25000",Code.ICD9), dt("2015-03-21"), "1", "1", "2016-10-30", "2016-10-30"),
                      ce(Code("P",Code.FFSPROV), dt("2015-03-21"), "1", "1", "2016-10-30", "2016-10-30"),
                      ce(Code("G0290",Code.CPT), dt("2015-03-21"), "1", "1", "2016-10-30", "2016-10-30"))
    val sub = new CommercialRiskSubtraction(proj, "")
    val res = sub.process(sub.filter(events))
    res.size should equal (0)
  }

  it should "handle deletes" in {
    val events = List(ce(Code("25000",Code.ICD9), dt("2015-03-21"), "1", "1", "2016-10-30", "2016-10-30", true),
                      ce(Code("26000",Code.ICD9), dt("2015-03-21"), "1", "1", "2016-10-30", "2016-10-30"),
                      ce(Code("P",Code.FFSPROV), dt("2015-03-21"), "1", "1", "2016-10-30", "2016-10-30"),
                      ce(Code("00100",Code.CPT), dt("2015-03-21"), "1", "1", "2016-10-30", "2016-10-30"))
    val sub = new CommercialRiskSubtraction(proj, "")
    val res = sub.process(sub.filter(events))
    res.size should equal (0)
  }

  // it should "handle the production case" in {
  //   val proj2 = Project(0.0, "", "", "2016-12-31", "2016-01-01", "", "", "", "", "", "", "", "", "2017", "",
  //     "", Map.empty, 0.0, 0.0, "", true, "", "hcc")
  //   val events = List(ce(Code("99213",Code.CPT), dt("2016-07-21"), "1", "1", "2017-01-31", "2016-07-21"),
  //                     ce(Code("99396",Code.CPT), dt("2016-07-21"), "1", "1", "2017-01-31", "2016-07-21"),
  //                     ce(Code("E119",Code.ICD10), dt("2016-07-21"), "1", "1", "2017-01-31", "2016-07-21"),
  //                     ce(Code("E785",Code.ICD10), dt("2016-07-21"), "1", "1", "2017-01-31", "2016-07-21"),
  //                     ce(Code("F330",Code.ICD10), dt("2016-07-21"), "1", "1", "2017-01-31", "2016-07-21"),
  //                     ce(Code("P",Code.FFSPROV), dt("2016-07-21"), "1", "1", "2017-01-31", "2016-07-21"),
  //                     ce(Code("P",Code.FFSPROV), dt("2016-07-21"), "1", "1", "2017-01-31", "2016-07-21"),
  //                     ce(Code("P",Code.FFSPROV), dt("2016-07-21"), "1", "1", "2017-01-31", "2016-07-21"),
  //                     ce(Code("P",Code.FFSPROV), dt("2016-07-21"), "1", "1", "2017-01-31", "2016-07-21"),
  //                     ce(Code("P",Code.FFSPROV), dt("2016-07-21"), "1", "1", "2017-01-31", "2016-07-21"),
  //                     ce(Code("Z0000",Code.ICD10), dt("2016-07-21"), "1", "1", "2017-01-31", "2016-07-21"),
  //                     ce(Code("Z1211",Code.ICD10), dt("2016-07-21"), "1", "1", "2017-01-31", "2016-07-21"))
  //   val sub = new CommercialRiskSubtraction(proj2, "")
  //   val res = sub.process(sub.filter(events))
  //   res.size should equal (events.filter(_.fact.code.isIcd).size)
  // }

}
