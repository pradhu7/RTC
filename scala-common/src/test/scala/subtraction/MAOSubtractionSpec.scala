package com.apixio.scala.subtraction

import com.apixio.model.profiler._
import com.apixio.scala.apxapi.Project
import org.joda.time.DateTime
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
  * Created by dnguyen on 12/20/16.
  */
class MAOSubtractionSpec extends AnyFlatSpec with Matchers {
  val project = Project(0.0, null, null, "2015-12-31", "2015-01-01", "id", "name", "orgId",
    null, null, null, null, null, "2016", "pdsExternalID", null, Map(), 0, 0, null, true, null, "hcc")
  val start = DateTime.parse("2015-01-01")
  val end = DateTime.parse("2015-12-31")

  val mao = new MAOSubtraction(project, "someone")

  "MAOSubtraction" should "add codes by claims" in {
    val events = List[EventTypeX](
      MAOEvent("1", "1", 1, "I234", "2015-07-07", "2016-01-01"),
      MAOEvent("1", "1", 1, "G100", "2015-07-07", "2016-01-01"),
      MAOEvent("2", "1", 1, "G100", "2015-05-01", "2016-02-01")
    )

    val claims = MAOSubtraction.claimsByMAOs(start, end, events).map(x => x.copy(ineligible = OrderedMonthMap(x.ineligible))).toSet
    val expected = Set(
      subtractEvent("I234", "2015-07-07", "2016-01-01", (1 to 12).toList, "2016-01-01"),
      subtractEvent("G100", "2015-07-07", "2016-02-01", (1 to 12).toList, "2016-02-01")
    )
    claims shouldBe expected
  }

  it should "delete codes" in {
    val events = List[EventTypeX](
      MAOEvent("1", "1", 1, "I234", "2015-07-07", "2016-01-01"),
      MAOEvent("1", "2", 1, "I234", "2015-07-07", "2016-01-02", true)
    )
    MAOSubtraction.claimsByMAOs(start, end, events) shouldBe empty
  }

  it should "void transaction codes" in {
    val events = List[EventTypeX](
      MAOEvent("1", "1", 1, "I234", "2015-07-07", "2016-01-01"),
      MAOEvent("1", "2", 1, "G100", "2015-07-07", "2016-01-02"),
      MAOEvent("1", "3", 2, "G100", "2015-07-07", "2016-01-03")
    )
    MAOSubtraction.claimsByMAOs(start, end, events) shouldBe empty
  }

  it should "replace transaction codes" in {
    val events = List[EventTypeX](
      MAOEvent("1", "1", 1, "I234", "2015-07-07", "2016-01-01"),
      MAOEvent("1", "1", 1, "K49", "2015-07-07", "2016-01-01"),
      MAOEvent("1", "2", 1, "G100", "2015-07-07", "2016-01-02"),
      MAOEvent("1", "3", 3, "K50", "2015-07-07", "2016-01-03")
    )
    val processed = MAOSubtraction.claimsByMAOs(start, end, events).map(x => x.copy(ineligible = OrderedMonthMap(x.ineligible)))
    val expected = List(
      subtractEvent("K50", "2015-07-07", "2016-01-03", (1 to 12).toList, "2016-01-03")
    )
    processed shouldBe expected
  }

  it should "collapse claims tree into codes" in {
    val events = List[EventTypeX](
      MAOEvent("1", "1", 1, "G100", "2015-07-07", "2016-01-01"),
      MAOEvent("1", "1", 1, "K50",  "2015-07-07", "2016-01-01"),
      MAOEvent("2", "1", 4, "I234", "2015-07-07", "2016-02-01", parentId = "1"),
      MAOEvent("2", "2", 4, "G100", "2015-07-07", "2016-02-01", true, parentId = "1"),
      MAOEvent("3", "2", 4, "I234", "2015-07-07", "2016-02-02", true, parentId = "1"),
      MAOEvent("3", "3", 5, "", "2015-07-07", "2016-02-02", parentId = "1") // SUPPRESS THE DELETE
    )
    MAOSubtraction.claimsByMAOs(start, end, events).map(x => x.copy(ineligible = OrderedMonthMap(x.ineligible))).toSet shouldBe Set(
      subtractEvent("K50", "2015-07-07", "2016-01-01", (1 to 12).toList, "2016-01-01"),
      subtractEvent("I234", "2015-07-07", "2016-02-01", (1 to 12).toList, "2016-02-01")
    )
  }

  def subtractEvent(code: String, dos: String, transactionDate: String, months: List[Int], loadedDate: String = null, add: Boolean = true) = {
    val mm = new MonthMap(start, end)
    mm.load(months.map(x => 1 << (x-1)).foldLeft(0)(_|_))
    SubtractionEvent(
      Code(code.split("-").head, code.split("-").tail.headOption.getOrElse(Code.ICD10)),
      DateTime.parse(dos),
      DateTime.parse(transactionDate),
      null,
      add,
      OrderedMonthMap(mm),
      MAOSubtraction.maoType,
      DateTime.parse(if (loadedDate ==null) transactionDate else loadedDate)
    )
  }
}

case class MAOEvent(claimId: String, logicalId: String, switchType: Int, code: String, time: String, transactionDate: String, deleted: Boolean = false, parentId: String = null, a: Map[String,String] = Map(), e: Map[String,String] = Map())
case class MaoBuilder(code: String, dos: String, transactionDate: String, add: Boolean, months: List[Int], loadedDate: String)

object MAOEvent {
  implicit def ebToEventTypeX(p: MAOEvent): EventTypeX = new EventTypeX(
    p.a,
    EvidenceTypeX(p.e ++ Map("transactionType" -> (if (p.deleted) "DELETE" else "ADD"), "encounterTypeSwitch" -> p.switchType.toString, "transactionDate" -> p.transactionDate, "processingDate" -> p.transactionDate, "parentSourceId" -> p.parentId), inferred = true, ReferenceTypeX(MAOSubtraction.maoType, p.logicalId)),
    FactTypeX(Code(p.code.split("-").head, p.code.split("-").tail.headOption.getOrElse(Code.ICD10)), TimeRangeTypeX(DateTime.parse(p.time), DateTime.parse(p.time)), Map()),
    ReferenceTypeX("RiskAdjustmentInsuranceClaim", p.claimId),
    ReferenceTypeX("patient", "someone")
  )
}

class OrderedMonthMap(s: DateTime, e: DateTime, m: Long) extends MonthMap(s,e) {
  this.load(m)
  override def equals(that: scala.Any): Boolean = that match {
    case mm: MonthMap => this.toLong() == mm.toLong() && this.startRaw == mm.startRaw && this.endRaw == mm.endRaw
    case _ => false
  }
}

object OrderedMonthMap {
  def apply(mm: MonthMap): OrderedMonthMap = new OrderedMonthMap(mm.startRaw, mm.endRaw, mm.toLong())
}
