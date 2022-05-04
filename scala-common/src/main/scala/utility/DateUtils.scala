package com.apixio.scala.utility

import java.text.ParseException
import javax.activation.UnsupportedDataTypeException

import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, ISODateTimeFormat, DateTimeFormatter}

import scala.util.Try

/**
 * Class to parse out a number as epoch.
 * @author rbelcinski@apixio.com
 */
class MyEpochConverter extends DateTimeFormatter(null,null) {
  /**
   * Perform string conversion.
   * @param text The text to convert.
   * @return The DateTime object.
   */
  override def parseDateTime(text:String):DateTime = {
    if (text.length != 13)
      throw new Exception("MyEpochConverter thinks string is too short")
    else
      new DateTime(text.toLong)
  }
}

/**
 * This is a class to use to determine how to format a date.
 * @param f The formatter.
 * @param c A simple callback to see if a string might be parse-able by the formatter f.
 */
case class DateCheck(f:DateTimeFormatter,c:Option[(String)=>Boolean])

/**
 * This class is a container for date handling utilities.
 * @author rbelcinski@apixio.com
 */
object DateUtils {
  /**
   * An obviously default value.
   */
  val default = new DateTime(0,1,1,0,0,0)
  /**
   * This is the collection of supported formats.
   */
  private val formats = Array(
    DateCheck(ISODateTimeFormat.dateTime(),Some((s:String) => s.length > 10)),
    DateCheck(DateTimeFormat.forPattern("M/d/Y"),Some((s:String) => s.length < 11 && s.contains("/"))),
    DateCheck(new MyEpochConverter(),None))
  /**
   * Test for default value.
   * @param dt The date object to test.
   * @return true if default value.
   */
  def isDefault(dt:DateTime) = {
    dt.year().get() == 0 &&
    dt.monthOfYear().get() == 0 &&
    dt.dayOfMonth().get() == 0 &&
    dt.hourOfDay().get() == 0 &&
    dt.minuteOfHour().get() == 0 &&
    dt.secondOfMinute().get() == 0
  }
  /**
   * Make a "now" string.
   * @return A "now" string.
   */
  def nowString = DateTime.now().toString(formats(0).f)
  /**
   * Attempt to parse a date time using various formats.
   * @param s The string to parse.
   * @return The date time object, or NOW on failure.
   */
  def parseDate(s:String) = {
    if (s == "N/A")
      null
    else {
      // This is the return value:
      var r = (false,DateTime.now())
      // Look for one that works:
      val worked = formats.find(chk => {
        // Apply a simple filter:
        val pass = chk.c match {
          case Some(fl) => fl(s)
          case None => true
        }
        // Parse:
        if (pass) r = Try((true,chk.f.parseDateTime(s))).getOrElse((false,DateTime.now()))
        // Did it work?
        r._1
      })
      // Done:
      r._2
    }
  }
  /**
   * Attempt to parse a date time using various formats.
   * @param s The string to parse.
   * @return The date time object.
   */
  def parseDateOrThrow(s:String) = {
    // This is the return value:
    var r = (false,DateTime.now())
    // Look for one that works:
    val worked = formats.find(chk => {
      // Apply a simple filter:
      val pass = chk.c match {
        case Some(fl) => fl(s)
        case None => true
      }
      // Parse:
      if (pass) r = Try((true,chk.f.parseDateTime(s))).getOrElse((false,DateTime.now()))
      // Did it work?
      r._1
    })
    // Done:
    if (worked.isDefined)
      r._2
    else
    // Throw:
      throw new ParseException("Could not parse date",0)
  }
}
