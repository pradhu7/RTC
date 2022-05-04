package com.apixio.scala.subtraction

import com.apixio.model.profiler._
import com.apixio.scala.apxapi.Project
import org.joda.time.DateTime
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class LegacySubtractionSpec extends AnyFlatSpec with Matchers {

  def dt(s: String) : DateTime = DateTime.parse(s)
  def ce(c: Code, cd: DateTime) : EventTypeX =
    EventTypeX(Map.empty,
      EvidenceTypeX(Map.empty, false, null),
      FactTypeX(c, TimeRangeTypeX(cd, cd), Map()),
      null,
      null
    )

  val proj = Project(0.0, "", "", "2015-12-31", "2015-01-01", "", "", "", "", "", "", "", "", "2017", "",
    "", Map.empty, 0.0, 0.0, "", true, "", "hcc")

  "LegacySubtraction" should "handle extraction fields on generated event" in {
    val event = ce(Code("25000",Code.ICD9), dt("2015-03-21"))
    val sub = new LegacySubtraction(proj, "")
    RAPSSubtraction.getVersion(event) should equal (RAPSSubtraction.unknown)
    RAPSSubtraction.getEvidenceType(event) should equal (RAPSSubtraction.unknown)
    RAPSSubtraction.getTransactionDate(event).compareTo(RAPSSubtraction.defaultDate) should equal (0)
    val tid = RAPSSubtraction.getEvidenceSource(event)
    tid.size should be > 20
    tid.contains(sub.clusterId(List(event))) should equal (true)
  }

  it should "filter and process a single event" in {
    val sub = new LegacySubtraction(proj, "")
    val res = sub.process(sub.filter(List(ce(Code("25000",Code.ICD9), dt("2015-03-21")))))
    res.size should equal (1)
    res.head.ineligible.toLong should equal (4095)
  }

  it should "correctly handle a new RAPS event" in {
    val sub = new LegacySubtraction(proj, "")
    val event = (new RAPSSubtractionSpec()).ce(Code("25000",Code.ICD9), dt("2015-03-21"), "1", "1", "2016-10-30")
    val res = sub.process(sub.filter(List(event)))
    res.size should equal (1)
    res.head.ineligible.toLong should equal (4095)
  }
}
