package com.apixio.scala.logging

import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._

import org.joda.time.DateTime

import java.util

/**
 * Classes with this trait will emit their logs to a dynamically growable string list for
 * testing purposes.
 * @note This class is useful for testing components that output to a logging facility and
 * we want to test that those entries are actually being made.
 * @author rbelcinski@apixio.com
 */
trait ListLoggable extends Loggable {
  /**
   * Print informational message to the log.
   * @param msg The message to print.
   * @param context An optional context.
   */
  override def info(msg: String, context:String = "") = ListLoggable.lock.synchronized {
    if (Loggable.enableLogging)
      ListLoggable.entries += s"${DateTime.now()}-$context-[INFO]: $msg"
  }
  /**
   * Print warning message to the log.
   * @param msg The message to print.
   * @param context An optional context.
   */
  override def warn(msg: String, context:String = "") = ListLoggable.lock.synchronized {
    if (Loggable.enableLogging)
      ListLoggable.entries += s"${DateTime.now()}-$context-[WARN]: $msg"
  }
  /**
   * Print error message to the log.
   * @param msg The message to print.
   * @param context An optional context.
   */
  override def error(msg: String, context:String = "") = ListLoggable.lock.synchronized {
    if (Loggable.enableLogging)
      ListLoggable.entries += s"${DateTime.now()}-$context-[ERROR]: $msg"
  }
  /**
   * Print debug message to the log.
   * @param msg The message to print.
   * @param context An optional context.
   */
  override def debug(msg: String, context: String = ""): Unit = ListLoggable.lock.synchronized {
    if (Loggable.enableLogging)
      ListLoggable.entries += s"${DateTime.now()}-$context-[DEBUG]: $msg"
  }
  /**
   * Print error message to the log.
   * @param m The event to print.
   * @param context An optional context.
   */
  override def event(m: util.Map[String, Object], context: String = "", eventName: String = ""): Unit = ListLoggable.lock.synchronized {
    val s = m.map(kvp => s"${kvp._1}=${kvp._2.toString}").mkString(",")
    ListLoggable.entries += s"${DateTime.now()}-$context-[EVENT]: $s"
  }
}

/**
 * Container for static fields for the ListLoggable class.
 */
object ListLoggable {
  /**
   * This is used to control thread access.
   */
  val lock = new Object()
  /**
   * This is the collection of entries.
   */
  val entries = ListBuffer[String]()
  /**
   * Clear all entries.
   */
  def clear() = lock.synchronized { entries.clear() }
}
