package com.apixio.dao.elastic

import org.slf4j.LoggerFactory
import scala.util.{Failure, Success, Try}

/**
 * This trait is used to endow objects with simple retry semantics.
 * @note Created on 8/20/15 in com.apixio.hcc.elasticdao.utility
 * @author rbelcinski@apixio.com
 */
trait Retry {
  val logger = LoggerFactory.getILoggerFactory.getLogger(getClass.getCanonicalName)
  /**
   * Attempt a function block several times until we're told to terminate.
   * @param f The function block which produces a continuation flag and an option containing a result.
   * @tparam T The option type.
   * @return An option containing the return type, or None.
   */
  def withBooleanRetry[T](f: => (Boolean,Option[T])):Option[T] = {
    // The result:
    var result:Option[T] = None
    // The tracking flag:
    var continue = true
    // Function to generate a result:
    def genResult() = {
      // Do stuff:
      Try(f) match {
        case Success(r) => r
        case Failure(ex) =>
          logger.error(ex.getMessage)
          (false,None)
      }
    }
    // Loop:
    while (continue) {
      // Do the passed thing:
      val rr = genResult()
      // Stash results:
      continue = rr._1
      result = rr._2
    }
    // Done:
    result
  }
  /**
   * Attempt to obtain a result a number of times.
   * @param n The number of times to attempt (3)
   * @param delay The delay between attempts (0)
   * @param f A function that will be used to obtain a value T.
   * @tparam T The type of thing to get.
   * @return An option containing a T or None if unsuccessful.
   */
  def withRetry[T](n:Int = 3, delay:Int = 0)(f: => Option[T]):Option[T] = {
    // Execution counter:
    var count = 0
    // The result:
    var result:Option[T] = None
    // Function to generate a result:
    def genResult() = {
      // Some time between retries?
      if (delay > 0 && count != 0) Thread.sleep(delay)
      // Increment count:
      count += 1
      // Do stuff:
      Try(f) match {
        case Success(r) => r
        case Failure(ex) =>
          //TODO: write(ex.getMessage,"Error")
          //TODO: write("Retrying...","Info")
          None
      }
    }
    // Perform retries up to the limit:
    while (count < n && result.isEmpty) result = genResult()
    // Done...
    result
  }
  /**
   * Attempt to obtain a result a number of times.
   * @param n The number of times to attempt (3)
   * @param delay The delay between attempts (0)
   * @param f A function that will be used to obtain a value T.
   * @param default A default for the block, in case things fail.
   * @tparam T The type of thing to get.
   * @return An object of type T or "default" if unsuccessful.
   */
  def withDefaultedRetry[T](n:Int = 3, delay:Int = 0)(f: => T)(default:T = null.asInstanceOf[T]) = {
    // Execution counter:
    var count = 0
    var succeeded = false
    // The result:
    var result = default
    // Function to generate a result:
    def genResult() = {
      // Some time between retries?
      if (delay > 0 && count != 0) Thread.sleep(delay)
      // Increment count:
      count += 1
      // Do stuff:
      Try(f) match {
        case Success(r) => succeeded = true; r
        case Failure(ex) =>
          //TODO: write(ex.getMessage,"Error")
          //TODO: write("Retrying...","Info")
          default
      }
    }
    // Perform retries up to the limit:
    while (count < n && !succeeded) result = genResult()
    // Done...
    result
  }
  /**
   * Attempt to obtain a result a number of times.
   * @param n The number of times to attempt (3)
   * @param delay The delay between attempts (0)
   * @param rethrow Should the function rethrow if the recovery function returns false?
   * @param recoveryOutput A function to emit a recovery message somehow.
   * @param f A function that will be used to obtain a value T.
   * @param rec The recovery function.  Returns true if we are to continue.
   * @tparam T The type of thing to get.
   * @return An option containing a T or None if unsuccessful.
   */
  def withRecovery[T](n:Int = 10, delay:Int = 0, rethrow:Boolean = false, recoveryOutput:Option[(String)=>Unit] = None)(f: => Option[T])(rec:(Throwable) => Boolean):Option[T] = {
    // Execution counter:
    var count = 0
    // The result:
    var result:Option[T] = None
    // This is the last exception that was detected:
    var lastException:Option[Throwable] = None
    // Function to generate a result:
    def genResult() = {
      // Some time between retries?
      if (delay > 0 && count != 0) Thread.sleep(delay)
      // Increment count:
      count += 1
      // Do stuff:
      Try(f) match {
        case Success(r) => r
        case Failure(ex) =>
          // Stash the last exception:
          lastException = Some(ex)
          // Messages:
          //TODO: write(ex.getMessage,"Error")
          //TODO: write("Retrying...","Info")
          // Attempt recovery:
          if (!Try(rec(ex)).getOrElse(false)) {
            // Terminate looping if rec == false
            count = n
            // Rethrow?
            if (rethrow) throw ex
          }
          // Done...
          None
      }
    }
    // Perform retries up to the limit:
    while (count < n && result.isEmpty) result = genResult()
    // Emit recovery message if it appears as though we recovered from an error:
    if (result.isDefined && lastException.isDefined && recoveryOutput.isDefined && count > 1)
      recoveryOutput.get.apply(s"Recovered from '${lastException.get.getClass.getCanonicalName}: ${lastException.get.getMessage}' on attempt #$count.")
    // Done...
    result
  }
}
