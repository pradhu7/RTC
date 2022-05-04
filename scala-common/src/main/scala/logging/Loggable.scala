package com.apixio.scala.logging

import com.apixio.restbase.RestUtil
import com.apixio.scala.dw.Utility
import com.apixio.scala.apxapi.{ApxAuthException, ApxCodeException, ApxForbiddenException}

import java.util
import java.net.InetAddress
import javax.ws.rs.core.{Response, UriInfo}
import javax.servlet.http.HttpServletRequest
import apxapi.ResourceTooBusyException
import org.apache.log4j.Level
import org.joda.time.DateTime

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

/**
 * This is used for tracking current position of method calls just in case we have
 * inherited objects deep in the call stack
 * @param items A collection of event items for the context.
 * @param start The start time for the context.
 * @param methodName The method name for the context.
 * @author rbelcinski@apixio.com
 */
case class MethodStackItem(items:util.Map[String,Object], start:Long, methodName:String)

/**
 * This is a throwable object that contains an alternate return code.
 * @param msg A message for an assertion.
 * @param o An alternate return code.
 */
case class LogThrowable[T](msg:String, o:Option[T]) extends AssertionError(msg)

/**
 * Classes that have this trait can log things to the standard logging facility.
 * @note This version is really basic.  Other services should override this to add in things like fluent
 * logging and so forth.  This specific trait emits nothing.
 * @author rbelcinski@apixio.com
 */
trait Loggable {
  /**
   * This is used to stash event maps for the current context.
   */
  var items = new ThreadLocal[Option[util.Map[String,Object]]]()
  /**
   * A starting item for logging execution time.
   */
  var start = new ThreadLocal[Option[Long]]()
  /**
   * This is the name of the current method, if any.
   */
  var methodName = new ThreadLocal[Option[String]]()
  /**
   * This is a stack of tracking contexts.
   */
  var contexts = new ThreadLocal[mutable.ArrayStack[MethodStackItem]]()
  /**
   * A special assertion function that contains an alternate return.
   * @param condition A condition to evaluate.
   * @param message A message in case of failure.
   * @param o A return value in case of failure.
   */
  def logAssert[T](condition:Boolean, message: => String, o: => Option[T]) = {
    if (!condition)
      throw new LogThrowable[T](message,o)
  }
  /**
   * Perform all needed facility setups.
   * @param name A name for the logger.
   */
  def setupLog(name:String) = {}
  /**
   * Make sure we have some thread local variables for this loggable object.
   */
  def initializeThreadLocals() = {
    // The event map:
    items.get() match {
      case null => items.set(None)
      case _ => // Leave this as it is...
    }
    // The start code:
    start.get() match {
      case null => start.set(None)
      case _ => // Leave this as it is...
    }
    // The method name:
    methodName.get() match {
      case null => methodName.set(None)
      case _ => // Leave this as it is...
    }
    // The context stack:
    contexts.get() match {
      case null => contexts.set(new mutable.ArrayStack[MethodStackItem])
      case _ => // Leave this as it is...
    }
  }
  /**
   * Fill a map with JVM metrics
   * @param m A map to fill.
   */
  def addJvmMetricsToEventMap(m:util.Map[String,Object]) =
    Map(
      "jvm.memory.total.bytes" -> Runtime.getRuntime.totalMemory.toString,
      "jvm.memory.free.bytes" -> Runtime.getRuntime.freeMemory.toString,
      "jvm.memory.max.bytes" -> Runtime.getRuntime.maxMemory.toString,
      "jvm.processors.count" -> Runtime.getRuntime.availableProcessors.toString,
      "jvm.thread" -> Thread.currentThread.getId.toString,
      "jvm.threads.count" -> Thread.activeCount.toString,
      "hostname" -> s"ip-${InetAddress.getLocalHost.getHostAddress.replace('.', '-')}",
      "time" -> System.currentTimeMillis().toString,
      "datestamp" -> DateTime.now().toString
    ).foreach(kvp => pushMetric(kvp._1,kvp._2))

  /**
   * Fill the initial app container
   * @param m A map to fill.
   */
  def addAppContainerToEventMap(m:util.Map[String,Object]) =
    Map(
      "app.app_name" -> "unknown",
      "app.app_version" -> "unknown"
    ).foreach(kvp => pushMetric(kvp._1,kvp._2))

  /**
   * Add a metric to our set.
   * @param n A name for the item.
   * @param v A value for the item.
   */
  def pushMetric(n:String, v:Object) = items.get() match {
    case Some(m) =>
      m.put(s"app.app_data.event_data.$n", v)
    case None =>
      warn(s"pushMetric() has no item set to fill for key and value called $n=$v")
    case _ =>
      warn(s"pushMetric() has non-initialized ThreadLocal to fill for key and value called $n=$v")
  }
  /**
   * Add a piece of user info to our set.
   * @param n A name for the item.
   * @param v A value for the item.
   */
  def pushUserInfo(n:String, v:Object) = items.get() match {
    case Some(m) =>
      m.put(s"app.app_user_info.$n", v)
    case None =>
      warn(s"pushUserInfo() has no item set to fill for key and value called $n=$v")
    case _ =>
      warn(s"pushUserInfo() has non-initialized ThreadLocal to fill for key and value called $n=$v")
  }
  /**
   * Add a piece of api info to our set.
   * @param n A name for the item.
   * @param v A value for the item.
   */
  def pushAPIInfo(n:String, v:Object) = items.get() match {
    case Some(m) =>
      m.put(s"app.app_data.event_data.api.$n", v)
    case None =>
      warn(s"pushAPIInfo() has no item set to fill for key and value called $n=$v")
    case _ =>
      warn(s"pushAPIInfo() has non-initialized ThreadLocal to fill for key and value called $n=$v")
  }
  /**
   * Print informational message to the log.
   * @param msg The message to print.
   * @param context An optional context.
   */
  def info(msg:String, context:String = "")
  /**
   * Print warning message to the log.
   * @param msg The message to print.
   * @param context An optional context.
   */
  def warn(msg:String, context:String = "")
  /**
   * Print error message to the log.
   * @param msg The message to print.
   * @param context An optional context.
   */
  def error(msg:String, context:String = "")
  /**
   * Print debug message to the log.
   * @param msg The message to print.
   * @param context An optional context.
   */
  def debug(msg:String, context:String = "")
  /**
   * Print event message to the log.
   * @param m The event to print.
   * @param context An optional context.
   */
  def event(m:util.Map[String,Object], context:String = "", eventName: String = "")

  private def logByLevel(msg: String, context: String = "", level: Level): Unit = level match {
    case Level.DEBUG => debug(msg, context)
    case Level.INFO | _ => info(msg, context) // for now don't want to support other levels
  }

  /**
   * Start logging the indicated method.
   * @param name The name of the method to log.
   * @param context An optional context.
   */
  def startMethod(name:String, context:String = "", level: Level = Level.INFO): Unit = {
    // Make sure the current thread has been set up:
    initializeThreadLocals()
    // Allocate map of items:
    items.set(Some(new util.HashMap[String,Object]()))
    // Fill with JVM metrics:
    addJvmMetricsToEventMap(items.get().get)
    // Tally start time:
    start.set(Some(System.currentTimeMillis()))
    // Stash method name:
    methodName.set(Some(name))
    // Setup the event name
    items.get().get.put("app.app_data.event_name", name)
    // Push to stack:
    contexts.get().push(
      MethodStackItem(items.get().get, start.get().get, methodName.get().get))
    // Log start:

    logByLevel(s"Starting: $name", context, level)
  }
  /**
   * Finish logging the indicated method.
   * @param context An optional context.
   */
  def endMethod(context:String = "", level: Level = Level.INFO): Unit = {
    // Make sure the current thread has been set up:
    initializeThreadLocals()
    // All of these things should be defined:
    if (
      items.get().isDefined &&
        start.get().isDefined &&
        methodName.get().isDefined) {
      // Calculate time:
      val t = System.currentTimeMillis - start.get().get
      // Pull item map:
      val im = items.get().get
      // Pull name:
      val n = methodName.get().get
      // Stash items and so forth to log:
      pushMetric("millis", t.asInstanceOf[Object])
      //im.put(s"$n.millis", t.asInstanceOf[Object])
      // Push event data:
      event(im, context)
      // Log end of method:
      logByLevel(s"Ending: $n", context, level)
      // Pop the stack:
      val stk = contexts.get()
      stk.pop()
      // Clear for next time:
      if (stk.nonEmpty) {
        val c = stk.pop()
        items.set(Some(c.items))
        start.set(Some(c.start))
        methodName.set(Some(c.methodName))
        stk.push(c)
      } else {
        items.set(None)
        start.set(None)
        methodName.set(None)
      }
    }
    else
      warn("Unpaired endMethod() call...", context)
  }
  /**
   * Wrap a function context with some logging boiler-plate.
   * @param name The name of the context.
   * @param args A map of arguments to a function that we'd want to log.
   * @param userinfo A map of user info for a web accessed function that we'd want to log.
   * @param noiseLevel log level to use for intermediary logging like "Starting: event.name"
   * @param f A function to execute.
   * @param t A transform to execute if the function F worked.
   * @param default The error default value to return from the function if everything fails.
   * @tparam T The input type.
   * @tparam R The output type.
   * @return The return value of type R.
   */
  def withLogging[T,R](name:String, args:Map[String,Object] = null, userinfo:Map[String,Object] = null, noiseLevel: Level = Level.INFO)
    (f: => Option[T])(t: => T => R)(implicit default:R = null): R = {
    // Start logging:
    startMethod(name, level = noiseLevel)
    // Attach user info if provided
    Option(userinfo).map(_.foreach(el => pushUserInfo(el._1,el._2)))
    // Log parameters to the function:
    Option(args).map(_.foreach(el => pushMetric(el._1,el._2)))

    // This is used to track errors:
    var err:String = "success"
    // Evaluate block to get the initial data item of interest:
    val ret = try{
      f
    } catch {
      case e:LogThrowable[T] =>
        warn(s"${e.getMessage}")
        err = "error"
        pushMetric("error", e.getMessage)
        e.o // Return the alternate value encoded in the assertion.
      case e:AssertionError =>
        warn(s"${e.getMessage}")
        err = "error"
        pushMetric("error", e.getMessage)
        None
      case NonFatal(ex) =>
        error(Loggable.exceptionFormatter(ex))
        err = "error"
        pushMetric("error", Loggable.flatExceptionFormatter(ex))
        None
      case ex => // Fatal
        error(Loggable.exceptionFormatter(ex))
        throw ex
    }

    // Execute transformer on the data item to final return value.  Log the
    // item size of "String" or "Response" type:
    val transformed = ret match {
      case None =>
        err = "error"
        default
      case Some(o) => Try(t(o)) match {
        case Success(r) =>
          r match {
            case v:String =>
              // Log the string length:
              pushMetric("bytes", v.length.asInstanceOf[Object])
              // Return the transformed object:
              r
            case v:Response =>
              // Assume that the response contains serialized text already, so pull
              // the entity and log length:
              val ent = v.getEntity
              if (ent != null)
                pushMetric("bytes",
                  Try(ent.toString.length.asInstanceOf[Object])
                    .getOrElse(0.asInstanceOf[Object]))
              // Log response code:
              pushMetric("code", v.getStatus.asInstanceOf[Object])
              // Return the transformed object:
              r
            case _ =>
              // Something else... don't store bytes and just return object:
              r
          }
        case Failure(ex) => ex match {
          case e:AssertionError =>
            warn(s"${e.getMessage}")
            err = "error"
            pushMetric("error", e.getMessage)
            default
          case _ =>
            error(Loggable.exceptionFormatter(ex))
            err = "error"
            pushMetric("error", Loggable.flatExceptionFormatter(ex))
            default
        }
      }
    }
    pushMetric("status", err)
    // Terminate the logging context:
    endMethod(level = noiseLevel)
    // Done... just shove the return value out:
    transformed
  }

  def prepareAPIInfo(req: HttpServletRequest, uri: UriInfo) : Map[String,String]= {
    val paths = uri.getMatchedURIs.asScala.map(x => s"/${x}").toList
    val pathparams = uri.getPathParameters.asScala.map(x => s"{${x._1}}" -> x._2.asScala.head).toMap
    val queryparams = uri.getQueryParameters.asScala.map(x => s"{${x._1}}" -> x._2.asScala.head).toMap
    val prefix = if (paths.size > 1) paths.last else ""
    var template : String = paths.head
    pathparams.foreach { x => template = template.replace(x._2, x._1) }
    Map("prefix" -> prefix, "uri" -> paths.head, "endpoint" -> template,
        "method" -> req.getMethod, "requestId" -> RestUtil.getRequestId) ++
      pathparams.map(x => s"params.${x._1.slice(1,x._1.size-1)}" -> x._2).toMap ++
      queryparams.map(x => s"params.${x._1.slice(1,x._1.size-1)}" -> x._2).toMap
  }

  private def errorInternal(msg: String) : Boolean = {
    error(msg)
    pushMetric("error", msg)
    false
  }

  /**
   * Wrap the internals of an apixio API REST resource with some logging boiler-plate.
   * @param name The event name for the api log.
   * @param req The request context.
   * @param uri The uriinfo context.
   * @param noiseLevel log level to use for intermediary logging like "Starting: event.name"
   * @return The return value, most likely a response, but could be anything.
   */
  def withAPILogging(name: String, req: HttpServletRequest, uri: UriInfo, suppressErrors: Iterable[Int] = Set(), noiseLevel: Level = Level.INFO)(f: => Any): Any = {
    startMethod(name, level = noiseLevel)
    // Attach user and API info
    prepareAPIInfo(req, uri).foreach(el => pushAPIInfo(el._1, el._2))
    RestUtil.getCommonResourceInfo(req).asScala.toMap.foreach(el => pushUserInfo(el._1, el._2))

    var success:Boolean = true
    var code:Int = -1
    val ret = Try(f) match {
      case Success(r) => r match {
        case v:String =>
          pushMetric("bytes", v.length.asInstanceOf[Object])
          code = 200
          Response.ok(v).build
        case v:Response =>
          if (v.getStatus >= 400 && !suppressErrors.toSet.contains(v.getStatus)) success = false
          pushMetric("bytes", Try(Option(v.getEntity).map(_.toString.length).getOrElse(0)).getOrElse(0).asInstanceOf[Object])
          code = v.getStatus
          v
        case _ => //unlikely, but possible, so do nothing
          r
      }
      case Failure(ex) => ex match {
        case e @ (_:LogThrowable[_] | _:AssertionError) =>
          success = errorInternal(e.getMessage)
          code = 400
          Utility.errorResponse(400, e.getMessage)
        case e:ApxAuthException =>
          success = errorInternal(e.msg)
          code = 401
          Utility.errorResponse(401, e.msg)
        case e:ApxForbiddenException =>
          success = errorInternal(e.msg)
          code = 403
          Utility.errorResponse(403, e.msg)
        case e:ApxCodeException =>
          success = errorInternal(e.msg)
          code = e.code
          Utility.errorResponse(e.code, e.msg)
        case e:ResourceTooBusyException =>
          success = errorInternal(e.getMessage)
          code = 502
          Utility.errorResponse(502, e.getMessage)
        case _ =>
          success = errorInternal(Loggable.flatExceptionFormatter(ex))
          code = 500
          Utility.errorResponse(500, Loggable.exceptionFormatter(ex))
      }
    }
    pushMetric("status", if (success) "success" else "error")
    if (code > -1) pushMetric("code",  code.asInstanceOf[Object])
    endMethod(level = noiseLevel)
    ret
  }
}

/**
 * Classes that have this trait can log things to the standard logging facility.
 * @author rbelcinski@apixio.com
 */
object Loggable {
  /**
   * Turn on logging?
   */
  var enableLogging = true
  /**
   *  This is a formatter for use in "withTracks"
   */
  var exceptionFormatter:(Throwable)=>String =
    (e:Throwable) => s"${e.getClass.getCanonicalName}: ${e.getMessage} at\n   ${e.getStackTrace.mkString("\n   ")}"

  var flatExceptionFormatter:(Throwable)=>String =
    (e:Throwable) => s"${e.getClass.getCanonicalName}: ${e.getMessage} at:   ${e.getStackTrace.mkString(",   ")}"
}

/**
 * Some standard functions to plug into transformer blocks in withLogging.
 * @author rbelcinski@apixio.com
 */
object Transform {
  /**
   * Transform an object of type T to a string.
   * @param v The value to transform.
   * @tparam T The type parameter.
   * @return A string representation of T.
   */
  def string[T](v:T) = v.toString
  /**
   * Transform an object of type T to itself.
   * @param v The value to transform.
   * @tparam T The type parameter.
   * @return An object of type T.
   */
  def identity[T](v:T) = v
  /**
   * Transform an object of type T to json.
   * @param v The value to transform.
   * @tparam T The type parameter.
   * @return A string representation of T as JSON.
   */
  def json[T](v:T) = jsonTransformer(v)
  /**
   * This is a callback to transform a value to JSON.  Plug in a specific
   * transformer here.
   */
  var jsonTransformer:(Any)=>String = (v:Any) => "{}"
}
