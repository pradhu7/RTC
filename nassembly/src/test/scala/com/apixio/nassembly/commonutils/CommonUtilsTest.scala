package com.apixio.nassembly.commonutils

import com.apixio.nassembly.apo.converterutils.CommonUtils
import com.apixio.util.nassembly.DataCatalogProtoUtils
import org.joda.time.DateTime
import org.scalatest.flatspec.AnyFlatSpec

class CommonUtilsTest extends AnyFlatSpec {

  val year = 2021
  val month = 1
  val day = 2
  val srcDate = DataCatalogProtoUtils.fromDateTime(new DateTime(year, month, day, 0, 0))

  "Date parser" should "parse the given dates" in {
    // Expects the date as
    // 1) "YYYY-MM-DD" or "YYYY/MM/DD"
    // 2) "YYYY-MM-DDTHH:mm:ssZ
    // 3) MM-DD-YYYY or "MM/DD/YYYY"
    // 4) MM-DD-YYYY HH:mm:ss AM
    // 5) ddMMMyyyy:HH:mm:ss

    val yearMonthDayDates = Seq("2021-01-02","2021/01/02", "2021-01-02T23:01:30Z", "2021-01-02 23:01:30Z", "2021-01-02 23:01:30 AM", "2021/01/02 23:01:30 AM")
    val monthDayYearDates = Seq("01-02-2021", "01/02/2021", "01-02-2021T23:01:30Z", "01-02-2021 23:01:30Z", "01-02-2021 23:01:30 AM", "01/02/2021 23:01:30 AM")
    val MMMDates = Seq("02JAN2021:23:01:30.000", "02JAN2021:23:01:30")
    val aaDates = Seq("Dec 30 2014 12:00AM", "Aug 13 2014 12:00PM")
    val invalidDates = Seq("NULL", "D", "01-2021-02")

    yearMonthDayDates.foreach(dateString => {
      testDateString(dateString)
    })

    monthDayYearDates.foreach(dateString => {
      testDateString(dateString)
    })

    MMMDates.foreach(dateString => {
      testDateString(dateString)
    })

    aaDates.foreach(dateString => {

    })

    invalidDates.foreach(dateString => {
      val parsedYMD = CommonUtils.parseDateString(dateString)
      assert(parsedYMD.isEmpty)
    })
  }

  def testDateString(dateString: String): Unit = {
    val parsedYMD = CommonUtils.parseDateString(dateString)
    assert(parsedYMD.nonEmpty)
    assert(parsedYMD.get.getDay == day)
    assert(parsedYMD.get.getMonth == month)
    assert(parsedYMD.get.getYear == year)
  }

}
