package com.apixio.model.profiler

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.joda.time.DateTime
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest._
import matchers.should._

@RunWith(classOf[JUnitRunner])
class MonthMapSpec extends AnyFlatSpec with Matchers {

  val dosStart = DateTime.parse("2015-01-01T00:00:00Z")
  val dosEnd = DateTime.parse("2015-12-31T00:00:00Z")

  "MonthMap" should "correctly handle setting and checking a date" in {
    val m = new MonthMap(dosStart, dosEnd)
    val d = DateTime.parse("2015-03-21T00:00:00Z")
    m.set(d)
    m.isSet(d) should be (true)
  }

  it should "not have anything set when initially created" in {
    val m = new MonthMap(dosStart, dosEnd)
    m.toLong should be (0L)
    m.isSet(dosStart) should be (false)
    m.isSet(dosEnd) should be (false)
  }

  it should "allow using different timezones" in {
    val m = new MonthMap(dosStart, DateTime.parse("2015-12-31T00:00:00-0700"))
    val d = DateTime.parse("2015-03-21T00:00:00Z")
    m.set(d)
    m.isSet(d) should be (true)
  }

  it should "not allow setting dates after end date" in {
    val m = new MonthMap(dosStart, dosEnd)
    val d = DateTime.parse("2016-03-21T00:00:00Z")
    m.set(d)
    m.isSet(d) should be (false)
    m.toLong should be (0L)
  }

  it should "allow setting the same date multiple times" in {
    val m = new MonthMap(dosStart, dosEnd)
    val d = DateTime.parse("2015-03-21T00:00:00Z")
    m.set(d)
    m.isSet(d) should be (true)
    m.set(d)
    m.isSet(d) should be (true)
  }

  it should "allow setting and checking multiple months" in {
    val m = new MonthMap(dosStart, dosEnd)
    val d1 = DateTime.parse("2015-03-21T00:00:00Z")
    val d2 = DateTime.parse("2015-12-24T00:00:00Z")
    m.set(d1)
    m.set(d2)
    m.isSet(d1) should be (true)
    m.isSet(d2) should be (true)
  }

  it should "allow spanning across at least 2 years" in {
    val m = new MonthMap(dosStart, DateTime.parse("2016-12-31T00:00:00Z"))
    val d1 = DateTime.parse("2015-03-21T00:00:00Z")
    val d2 = DateTime.parse("2016-12-24T00:00:00Z")
    m.set(d1)
    m.set(d2)
    m.isSet(d1) should be (true)
    m.isSet(d2) should be (true)
  }

  it should "allow creation with all bits set" in {
    val m = new MonthMap(dosStart, dosEnd, true)
    m.all should be (true)
  }

  it should "not have all when only setting a month" in {
    val m = new MonthMap(dosStart, dosEnd)
    val d1 = DateTime.parse("2015-03-21T00:00:00Z")
    m.set(d1)
    m.all should be (false)
  }

  it should "allow setting all months individually and checking allSet" in {
    val m = new MonthMap(dosStart, dosEnd)
    val dates = List("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12").map(x => DateTime.parse(s"2015-${x}-01T00:00:00Z"))
    dates.foreach(x => m.set(x))
    m.all should be (true)
  }

  it should "allow setting all months in one go and each month is already set individually" in {
    val m = new MonthMap(dosStart, dosEnd)
    val dates = List("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12").map(x => s"2015-${x}-01T00:00:00Z")
    m.setAll()
    dates.foreach { x =>
      m.isSet(DateTime.parse(x)) should be (true)
    }
  }

  it should "allow a size of 1" in {
    val m = new MonthMap(dosStart, dosStart)
    m.set(dosStart)
    m.isSet(dosStart) should be (true)
  }

  it should "not allow end before start" in {
    an [AssertionError] should be thrownBy new MonthMap(dosEnd, dosStart)
  }

  it should "allow getting the vector" in {
    val m = new MonthMap(dosStart, dosEnd)
    m.toLong should be (0L)
    m.setAll()
    m.toLong should be (4095L)
  }

  it should "allow loading from a vector" in {
    val m = new MonthMap(dosStart, dosEnd)
    m.load(1L)
    m.isSet(dosStart) should be (true)
  }

  it should "allow mutating or operation" in {
    val m1 = new MonthMap(dosStart, dosEnd)
    val m2 = new MonthMap(dosStart, dosEnd)
    val d = DateTime.parse("2015-03-21T00:00:00Z")
    m2.set(d)
    m1.or(m2)
    m1.isSet(d) should be (true)
  }

  it should "allow mutating or operation on preloaded data" in {
    val m1 = new MonthMap(dosStart, dosEnd)
    m1.load(4L)
    val m2 = new MonthMap(dosStart, dosEnd)
    m2.load(8L)
    m1.or(m2)
    m1.toLong should be (12L)
  }

  it should "allow mutating and operation" in {
    val m1 = new MonthMap(dosStart, dosEnd)
    val m2 = new MonthMap(dosStart, dosEnd)
    val d = DateTime.parse("2015-03-21T00:00:00Z")
    m2.set(d)
    m1.and(m2)
    m1.isSet(d) should be (false)
  }

  it should "allow mutating xor operation" in {
    val m1 = new MonthMap(dosStart, dosEnd)
    val m2 = new MonthMap(dosStart, dosEnd)
    val d = DateTime.parse("2015-03-21T00:00:00Z")
    m2.set(d)
    m1.xor(m2)
    m1.isSet(d) should be (true)
    m1.xor(m1)
    m1.isSet(d) should be (false)
  }
}
