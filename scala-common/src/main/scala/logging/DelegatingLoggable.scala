package com.apixio.scala.logging

import java.util

/**
 * This logger delegates its functionality to another object that implements logging.
 * @author rbelcinski@apixio.com
 */
trait DelegatingLoggable extends Loggable {
  /**
   * This is the name that was passed as part of setup.
   */
  var contextName:String = ""
  /**
   * Perform all needed facility setups.
   * @param name A name for the logger.
   */
  override def setupLog(name:String) = DelegatingLoggable.theLogger match {
    case Some(log) =>
      log.setupLog(name)
      contextName = name
    case _ => // Do nothing...
  }
  /**
   * Print informational message to the log.
   * @param msg The message to print.
   * @param context An optional context.
   */
  override def info(msg:String, context:String = "") = DelegatingLoggable.theLogger match {
    case Some(log) => context match {
      case "" | null => log.info(msg, contextName)
      case _ => log.info(msg, context)
    }
    case _ => // Do nothing...
  }
  /**
   * Print warning message to the log.
   * @param msg The message to print.
   * @param context An optional context.
   */
  override def warn(msg:String, context:String = "") = DelegatingLoggable.theLogger match {
    case Some(log) => context match {
      case "" | null => log.warn(msg, contextName)
      case _ => log.warn(msg, context)
    }
    case _ => // Do nothing...
  }
  /**
   * Print error message to the log.
   * @param msg The message to print.
   * @param context An optional context.
   */
  override def error(msg:String, context:String = "") = DelegatingLoggable.theLogger match {
    case Some(log) => context match {
      case "" | null => log.error(msg, contextName)
      case _ => log.error(msg, context)
    }
    case _ => // Do nothing...
  }
  /**
   * Print debug message to the log.
   * @param msg The message to print.
   * @param context An optional context.
   */
  override def debug(msg: String, context: String): Unit = DelegatingLoggable.theLogger match {
    case Some(log) => context match {
      case "" | null => log.debug(msg, contextName)
      case _ => log.debug(msg, context)
    }
    case _ => // Do nothing...
  }
  /**
   * Print error message to the log.
   * @param m The event to print.
   * @param context An optional context.
   */
  override def event(m: util.Map[String, Object], context:String = "", eventName:String = "") =
    DelegatingLoggable.theLogger match {
      case Some(log) => context match {
        case "" | null => log.event(m, contextName, eventName)
        case _ => log.event(m, context, eventName)
      }
      case _ => // Do nothing...
    }
}

/**
 * This logger delegates its functionality to another object that implements
 * logging.
 * @author rbelcinski@apixio.com
 */
object DelegatingLoggable {
  /**
   * Objects in the delegating logger will use this instance.
   */
  var theLogger:Option[Loggable] = None
}
