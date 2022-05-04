package com.apixio.scala.logging

import java.util

import com.apixio.logger.fluentd.FluentAppender
import com.apixio.logger.graphite.GraphiteAppender
import com.apixio.logger.{EventLogger, StandardMetricsLayout}
import org.apache.log4j.{AsyncAppender, Logger}

import scala.collection.JavaConverters._
import scala.collection.mutable.HashMap

trait ApixioLoggable extends Loggable {
  /**
   * The embedded Apixio logger.
   */
  var log:Option[EventLogger] = None
  /**
   * Perform all needed facility setups.
   * @param name A name for the logger.
   */
  override def setupLog(name:String) = {
    // Set up Apixio log:
    if (Loggable.enableLogging) {
      // Check the static collection:
      ApixioLoggable.loggerMap.synchronized {
        if (!ApixioLoggable.loggerMap.contains(name)) {
          // Make a new logger:
          val newLogger = EventLogger.getLogger(name)
          // Stash?
          if (log == null || log.isEmpty)
            log = Some(newLogger)
          // Attach needed appender:
          newLogger.addAppenders(Logger.getRootLogger)
          // Push to collection:
          ApixioLoggable.loggerMap.put(name, newLogger)
        } else {
          // Pull existing reference:
          if (log.isEmpty) log = ApixioLoggable.loggerMap.get(name)
        }
      }
    }
  }
  /**
   * Fill a map with JVM metrics
   * @param m A map to fill.
   */
  override def addJvmMetricsToEventMap(m: util.Map[String, Object]) =
    StandardMetricsLayout.addJVMMetrics(m)
  /**
   * Get the logger for the specified context.
   * @param context The context to log for.
   * @return The logger, or none.
   */
  private def resolve(context:String):Option[EventLogger] =
    ApixioLoggable.loggerMap.getOrElse(context,null) match {
      case el:EventLogger => Some(el)
      case null => None
    }
  /**
   * Print informational message to the log.
   * @param msg The message to print.
   * @param context The message context.
   */
  override def info(msg:String, context:String = "") = context match {
    case "" | null => log match {
      case Some(l) => l.info(msg)
      case None => noLogger(msg)
    }
    case _ => resolve(context) match {
      case Some(l) => l.info(msg)
      case None => noLogger(msg)
    }
  }
  /**
   * Print warning message to the log.
   * @param msg The message to print.
   * @param context The message context.
   */
  override def warn(msg: String, context: String = "") =  context match {
    case "" | null => log match {
      case Some(l) => l.warn(msg)
      case None => noLogger(msg)
    }
    case _ => resolve(context) match {
      case Some(l) => l.warn(msg)
      case None => noLogger(msg)
    }
  }
  /**
   * Print error message to the log.
   * @param msg The message to print.
   * @param context The message context.
   */
  override def error(msg: String, context: String = "") = context match {
    case "" | null => log match {
      case Some(l) => l.error(msg)
      case None => noLogger(msg)
    }
    case _ => resolve(context) match {
      case Some(l) => l.error(msg)
      case None => noLogger(msg)
    }
  }
  /**
   * Print debug message to the log.
   * @param msg The message to print.
   * @param context An optional context.
   */
  override def debug(msg: String, context: String = ""): Unit = context match {
    case "" | null => log match {
      case Some(l) => l.debug(msg)
      case None => noLogger(msg)
    }
    case _ => resolve(context) match {
      case Some(l) => l.debug(msg)
      case None => noLogger(msg)
    }
  }

  /**
   * Print event message to the log.
   * @param m The event to print.
   * @param context The message context.
   */
  override def event(m: util.Map[String, Object], context: String = "", eventName: String = "") = {
    //setup the application context
    val newm = m match {
      case m:util.HashMap[String,Object] => m
      case _ => new util.HashMap[String,Object](m)
    }
    // this is true when someone calls event directly instead of through withLogging,
    // so all the parts need the new prefix.
    if (!newm.containsKey("app.app_data.event_name")) {
      val badentries = m.asScala.filter(!_._1.startsWith("app.app_data.event_data"))
      for ((k, v) <- badentries) {
        newm.remove(k)
        newm.put(s"app.app_data.event_data.${k}", v)
      }
      newm.put("app.app_data.event_name", eventName)
    }
    newm.put("app.app_name", ApixioLoggable.appName.getOrElse("unknown"))
    newm.put("app.app_version", ApixioLoggable.appVersion.getOrElse("unknown"))

    context match {
      case "" | null => log match {
        case Some(l) => l.event(newm)
        case None => noLogger("<event data>")
      }
      case _ => resolve(context) match {
        case Some(l) => l.event(newm)
        case None => noLogger("<event data>")
      }
    }
  }
  /**
   * This is a default message when no logger has been set up.
   * @param message The message.
   */
  private def noLogger(message:String) =
    println("No logger (missing setupLog() call?) for message: )" + message)
}

object ApixioLoggable {
  /**
   * This is used to stash loggers.
   */
  private val loggerMap = new HashMap[String, EventLogger]()
  /**
   * These are some app global pieces of information.
   */
  private var appName:Option[String] = None
  private var appVersion:Option[String] = None
  /**
   * Set up logging.  Takes the place of a log4j.xml file, and allows easy
   * setup from yml files and such.
   * @note Assumes the definition of the following parameters: graphiteHost,
   * fluentHost, prefix, enableConsole
   * @param config A callback method to map configuration names to configuration
   * strings.
   */
  def initialize(config:PartialFunction[String,Option[String]]):Unit = initializeLogging(config)
  /**
   * Set up logging.  Takes the place of a log4j.xml file, and allows easy
   * setup from yml files and such.
   * @note Assumes the definition of the following parameters: graphiteHost,
   * fluentHost, prefix, enableConsole
   * @param config A callback method to map configuration names to configuration
   * strings.
   */
  def initializeLogging(config:(String)=>Option[String]):Unit = config match {
    case c:((String)=>Option[String]) => initializeLogging(Some(c))
    case null => initializeLogging(None)
  }
  /**
   * Set up logging.  Takes the place of a log4j.xml file, and allows easy
   * setup from yml files and such.
   * @note Assumes the definition of the following parameters: graphiteHost,
   * fluentHost, prefix, enableConsole
   * @param config A callback method to map configuration names to configuration
   * strings.
   */
  def initializeLogging(config:Option[(String)=>Option[String]]):Unit = {
    // Define some quickie functions to get fields:
    val getCfg = (n:String) => config.flatMap(_(n))
    val getCfgWithDefault = (n:String, d:String) => config.flatMap(_(n)).getOrElse(d)

    // Setup app global information
    appName = getCfg("prefix").map(_.split("\\.").last)
    if (appName.isEmpty)
      appName = Option(getClass.getPackage.getImplementationTitle)
    appVersion = Option(getClass.getPackage.getImplementationVersion)

    // Configure Graphite under async: non-blocking, large buffer
    val graphiteAppender = new GraphiteAppender
    getCfg("graphiteHost") match {
      case Some(graphiteHost) =>
        val graphiteAsync = new AsyncAppender
        graphiteAsync.setBlocking(false)
        graphiteAsync.setBufferSize(1000)
        graphiteAsync.setName("graphiteAsync")
        graphiteAppender.setName("graphite")
        graphiteAppender.setHost(graphiteHost)
        graphiteAppender.setPrefix(getCfgWithDefault("prefix","default"))
        graphiteAppender.activateOptions()
        graphiteAsync.addAppender(graphiteAppender)
        Logger.getRootLogger.addAppender(graphiteAsync)
      case None => // No graphite.  Skip initialization.
    }
    // Configure fluent under async: blocking, large buffer
    getCfg("fluentHost") match {
      case Some(fluentHost) =>
        val fluentAsync = new AsyncAppender
        fluentAsync.setBlocking(true)
        fluentAsync.setBufferSize(1000)
        fluentAsync.setName("fluentAsync")
        val fluentAppender = new FluentAppender
        fluentAppender.setName("fluent")
        fluentAppender.setHost(fluentHost)
        fluentAppender.setPrefix(getCfgWithDefault("prefix","default"))
        fluentAppender.setTag(getCfgWithDefault("prefix","default"))
        fluentAppender.setLabel(getCfgWithDefault("prefix","default"))
        fluentAppender.setLevel(getCfgWithDefault("level","INFO"))  // new option- allow trimming INFO junk
        fluentAppender.activateOptions()
        fluentAsync.addAppender(fluentAppender)
        Logger.getRootLogger.addAppender(fluentAsync)
        graphiteAppender.addAppender(fluentAppender);
      case None => // No fluent.  Skip initialization.
    }

  }

  def getName() : String = appName.getOrElse("unknown")
  def getVersion() : String = appVersion.getOrElse("unknown")
}
