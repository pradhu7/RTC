package com.apixio.scala.subtraction

import com.apixio.model.profiler._
import com.apixio.scala.apxapi.Project
import org.joda.time.DateTime
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class RAPSSubtractionSpec extends AnyFlatSpec with Matchers {

  def dt(s: String) : DateTime = DateTime.parse(s)
  def ce(c: Code, cd: DateTime, tr: String, cl: String, pd: String, td: Option[String] = None, d: Boolean = false, e: Option[String] = None) : EventTypeX =
    EventTypeX(Map.empty,
      EvidenceTypeX(List("transactionDate" -> td, "transactionType" -> (if (d) Some("DELETE") else None), "transactionStatusCode" -> e,
          "processingDate" -> Some(pd), "version" -> Some(RAPSSubtraction.version)).filter(_._2.nonEmpty).map(x => x._1 -> x._2.get).toMap, false,
        ReferenceTypeX(RAPSSubtraction.rapsType, tr)),
      FactTypeX(c, TimeRangeTypeX(cd, cd), Map()),
      ReferenceTypeX("RiskAdjustmentInsuranceClaim", cl),
      null
    )

  val proj = Project(0.0, "", "", "2015-12-31", "2015-01-01", "", "", "", "", "", "", "", "", "2017", "",
    "", Map.empty, 0.0, 0.0, "", true, "", "hcc")

  "RAPSSubtraction" should "handle extraction fields on generated event" in {
    val event = ce(Code("25000",Code.ICD9), dt("2015-03-21"), "1", "1", "2016-10-30")
    val sub = new RAPSSubtraction(proj, "")
    RAPSSubtraction.getVersion(event) should equal (RAPSSubtraction.version)
    RAPSSubtraction.getEvidenceType(event) should equal (RAPSSubtraction.rapsType)
    RAPSSubtraction.getTransactionDate(event).compareTo(RAPSSubtraction.defaultDate) should equal (0)
    RAPSSubtraction.getEvidenceSource(event) should equal ("1")
    sub.clusterId(List(event)) should equal ("1")
  }

  it should "filter and process a single event" in {
    val sub = new RAPSSubtraction(proj, "")
    val res = sub.process(sub.filter(List(ce(Code("25000",Code.ICD9), dt("2015-03-21"), "1", "1", "2016-10-30"))))
    res.size should equal (1)
    res.head.ineligible.toLong should equal (4095)
  }

  it should "join a transaction together" in {
    val sub = new RAPSSubtraction(proj, "")
    val event = ce(Code("25000",Code.ICD9), dt("2015-03-21"), "1", "1", "2016-10-30")
    val res = sub.process(sub.filter(List(event, event)))
    res.size should equal (1)
    res.head.ineligible.toLong should equal (4095)
  }

  it should "join a cluster together" in {
    val sub = new RAPSSubtraction(proj, "")
    val e1 = ce(Code("25000",Code.ICD9), dt("2015-03-21"), "1", "1", "2016-10-30", Some("2016-10-30"))
    val e2 = ce(Code("25000",Code.ICD9), dt("2015-03-21"), "2", "1", "2016-10-31", Some("2016-10-31"))
    val res = sub.process(sub.filter(List(e1, e2)))
    res.size should equal (1)
    res.head.ineligible.toLong should equal (4095)
  }

  it should "handle deletes" in {
    val sub = new RAPSSubtraction(proj, "")
    val e1 = ce(Code("25000",Code.ICD9), dt("2015-03-21"), "1", "1", "2016-10-30", Some("2016-10-30"))
    val e2 = ce(Code("25000",Code.ICD9), dt("2015-03-21"), "2", "1", "2016-10-31", Some("2016-10-31"), true)
    val res = sub.process(sub.filter(List(e1, e2)))
    res.size should equal (0)
  }

  it should "handle multiple clusters" in {
    val sub = new RAPSSubtraction(proj, "")
    val e1 = ce(Code("25000",Code.ICD9), dt("2015-03-21"), "1", "1", "2016-10-30", Some("2016-10-30"))
    val e2 = ce(Code("E1152",Code.ICD10), dt("2015-04-21"), "2", "2", "2016-10-31", Some("2016-10-31"))
    val res = sub.process(sub.filter(List(e1, e2)))
    res.size should equal (2)
    res.head.ineligible.toLong should equal (4095)
    res.last.ineligible.toLong should equal (4095)
  }

  it should "filter out events with errors" in {
    val sub = new RAPSSubtraction(proj, "")
    val e1 = ce(Code("25000",Code.ICD9), dt("2015-03-21"), "1", "1", "2016-10-30", e = Some("421"))
    val res = sub.process(sub.filter(List(e1)))
    res.size should equal (0)
  }

  it should "keep valid events" in {
    val sub = new RAPSSubtraction(proj, "")
    val e1 = ce(Code("25000",Code.ICD9), dt("2015-03-21"), "1", "1", "2016-10-30", e = Some("421"))
    val e2 = ce(Code("25000",Code.ICD9), dt("2015-03-21"), "1", "1", "2016-10-30")
    val res = sub.process(sub.filter(List(e1, e2)))
    res.size should equal (1)
    res.head.ineligible.toLong should equal (4095)
  }

  it should "throw for unknown errors" in {
    val sub = new RAPSSubtraction(proj, "")
    val e1 = ce(Code("25000",Code.ICD9), dt("2015-03-21"), "1", "1", "2016-10-30", e = Some("200, 400"))
    assertThrows[AssertionError]{
      sub.process(sub.filter(List(e1)))
    }
  }

  it should "filter out multiple errors" in {
    val sub = new RAPSSubtraction(proj, "")
    val e1 = ce(Code("25000",Code.ICD9), dt("2015-03-21"), "1", "1", "2016-10-30", e = Some("400,408,421"))
    val res = sub.process(sub.filter(List(e1)))
    res.size should equal (0)
  }

  it should "filter out multiple errors, keeping valid code" in {
    val sub = new RAPSSubtraction(proj, "")
    val e1 = ce(Code("25000",Code.ICD9), dt("2015-03-21"), "1", "1", "2016-10-30", e = Some("400,408,421"))
    val e2 = ce(Code("25000",Code.ICD9), dt("2015-03-21"), "1", "1", "2016-10-30")
    val res = sub.process(sub.filter(List(e1, e2)))
    res.size should equal (1)
    res.head.ineligible.toLong should equal (4095)
  }


  it should "join in providers" in {
    val sub = new RAPSSubtraction(proj, "")
    val e1 = ce(Code("25000",Code.ICD9), dt("2015-03-21"), "1", "1", "2016-10-30", Some("2016-10-30"))
    val e2 = ce(Code("10",Code.RAPSPROV), dt("2015-03-21"), "1", "1", "2016-10-30", Some("2016-10-30"))
    val res = sub.process(sub.filter(List(e1, e2)))
    res.size should equal (1)
    res.head.providerType should equal (Code("10",Code.RAPSPROV))
    res.head.ineligible.toLong should equal (4095)
  }

  it should "correctly not process legacy RAPS" in {
    val sub = new RAPSSubtraction(proj, "")
    val e1 = (new LegacySubtractionSpec()).ce(Code("25000",Code.ICD9), dt("2015-03-21"))
    val res = sub.process(sub.filter(List(e1)))
    res.size should equal (0)
  }
}
