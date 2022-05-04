package com.apixio.scala.logging

import org.joda.time.DateTime

import java.util

import scala.collection.JavaConversions._

/**
 * Classes with this trait will use println to emit their logs.
 * @author rbelcinski@apixio.com
 */
trait PrintLineLoggable extends Loggable {
  /**
   * Print informational message to the log.
   * @param msg The message to print.
   * @param context An optional context.
   */
  override def info(msg:String, context:String = "") =
    if (Loggable.enableLogging) println(s"${DateTime.now()}-$context-[INFO]: $msg")
  /**
   * Print warning message to the log.
   * @param msg The message to print.
   * @param context An optional context.
   */
  override def warn(msg:String, context:String = "") =
    if (Loggable.enableLogging) println(s"${DateTime.now()}-$context-[WARN]: $msg")
  /**
   * Print error message to the log.
   * @param msg The message to print.
   * @param context An optional context.
   */
  override def error(msg:String, context:String = "") =
    if (Loggable.enableLogging) println(s"${DateTime.now()}-$context-[ERROR]: $msg")
  /**
   * Print debug message to the log.
   * @param msg The message to print.
   * @param context An optional context.
   */
  override def debug(msg: String, context: String = ""): Unit =
    if (Loggable.enableLogging) println(s"${DateTime.now()}-$context-[DEBUG]: $msg")
  /**
   * Print error message to the log.
   * @param m The event to print.
   * @param context An optional context.
   */
  override def event(m: util.Map[String, Object], context: String = "", eventName: String = ""): Unit =
    if (Loggable.enableLogging) {
      val s = m.map(kvp => s"${kvp._1}=${kvp._2.toString}").mkString(",")
      println(s"${DateTime.now()}-$context-[EVENT]: $s")
    }
}
