package com.apixio.nassembly.apo.converterutils

import com.apixio.converters.ccda.utils.CCDAUtils
import com.apixio.datacatalog.YearMonthDayOuterClass.YearMonthDay
import com.apixio.util.nassembly.DataCatalogProtoUtils
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.slf4j.LoggerFactory

import scala.util.{Success, Try}

object CommonUtils extends Serializable {

  private val logger = LoggerFactory.getLogger(getClass.getName)

  private val customFormats = Seq(
    "YYYY-MM-dd",
    "YYYY/MM/dd",
    "MM-dd-YYYY",
    "MM/dd/YYYY",
    "MM-dd-YYYY HH:mm:ss aa",
    "ddMMMyyyy:HH:mm:ss",
    "ddMMMyyyy:HH:mm:ss.SSS",
    "ddMMMyyyy",
    "MMM dd yyyy HH:mmaa",
    "yyyy-MM-dd'T'HH:mm:ssZ",
    "MM-dd-yyyy'T'HH:mm:ssZ",
    "MM-dd-yyyy HH:mm:ssZ",
    "yyyy-MM-dd HH:mm:ssZ",
    "yyyy-MM-dd HH:mm:ss aa",
    "MM/dd/YYYY HH:mm:ss aa",
    "YYYY/mm/DD HH:mm:ss aa")

  val dateTimeFormats: Seq[String] = (customFormats ++ CCDAUtils.datePatterns).distinct

  // Expects the date as inside dateTimeFormats
  def parseDateString(date: String): Option[YearMonthDay] = {
    val cleansedDate = date.replace("\n", "") // ccda seem to have newlines

    if (cleansedDate != "NA" && cleansedDate.nonEmpty) { // save time and logs for known bad values
      val dateTimeOption: Option[DateTime] = dateTimeFormats.foldLeft(Option.empty[DateTime]) {
        case (acc, each) =>
          if (acc.isDefined) acc
          else {
            val df = DateTimeFormat.forPattern(each)
            parseDateString(cleansedDate, df)
          }
      }
      dateTimeOption.map(DataCatalogProtoUtils.fromDateTime)
      match {
        case Some(yearMonthDay) => Some(yearMonthDay)
        case None =>
          logger.error(s"Can't convert $cleansedDate") // reduce number of logs
          None
      }
    }
    else {
      None
    }
  }

  private def parseDateString(date: String, dateFormat: DateTimeFormatter): Option[DateTime] = {
    Try(dateFormat.parseDateTime(date)) match {
      case Success(value) => Some(value)
      case _ => None
    }
  }

  def parseRemaining(remainingString: String): Int = {
    if (remainingString.contains(":") && remainingString.contains("T")) {
      remainingString.split('T')(0).toInt
    } else if (remainingString.contains(":") && remainingString.contains(" ")) {
      remainingString.split(" ")(0).toInt
    } else {
      remainingString.toInt
    }
  }
  def parseDate(year: Int, month: Int, day: Int): YearMonthDay = {
    DataCatalogProtoUtils.fromDateTime(new DateTime(year, month, day, 0, 0))
  }

}
