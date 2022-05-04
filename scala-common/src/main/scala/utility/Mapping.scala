package com.apixio.scala.utility

import java.io.InputStream
import java.net.{HttpURLConnection, URL}

import com.apixio.scala.apxapi.ApxApi
import com.apixio.scala.logging.ApixioLoggable


object Mapping extends ApixioLoggable {

  setupLog(this.getClass.getCanonicalName)

  private val MAPPING = "mapping"
  private val TIMEOUT = 2000
  private val NUM_RETRIES = 5

  // Than you Google https://stackoverflow.com/questions/7930814/whats-the-scala-way-to-implement-a-retry-able-call-like-this-one
  private def retry[T](n: Int)(fn: => T): T = {
    try {
      fn
    } catch {
      case e: Throwable => {
        error(s"${e.getMessage} - ${e.printStackTrace()}")
        if (n > 0) {
          val tryNumber = NUM_RETRIES - n
          val waitTime = TIMEOUT * tryNumber
          info(s"Retrying ${tryNumber + 1} after $waitTime ms ...")
          Thread.sleep(waitTime)
          retry(n - 1)(fn)
        } else {
          val msg = s"Giving up fetching mappings, after $NUM_RETRIES retries"
          error(msg)
          throw new RuntimeException(msg)
        }
      }
    }
  }

  private def get(path:String): InputStream = {
    val url = try {
      ApxApi.service(ApxApi.MAPPING).url
    } catch {
      case e: NoSuchElementException =>
        error(s"No mapping URL specified. Error=${e.getMessage}")
    }

    retry(NUM_RETRIES) {
      val fullpath = s"${url}${path}"
      info(s"fetching mappings for $fullpath")
      val connection = (new URL(fullpath).openConnection.asInstanceOf[HttpURLConnection])
      connection.setConnectTimeout(5000)
      connection.setReadTimeout(20000)
      connection.setRequestMethod("GET")
      connection.getInputStream
    }
  }

  def getCodeMapping(): InputStream = get("/mapping/v0/code")
  def getIcdModel(): InputStream = get(s"/mapping/v0/icd")
  def getHccModel(): InputStream = get(s"/mapping/v0/hcc")
  def getCptModel(): InputStream = get(s"/mapping/v0/cpt")
  def getBilltypeModel(): InputStream = get(s"/mapping/v0/billtype")
  def getMeasureModel(): InputStream = get(s"/mapping/v0/measure")
  def getFactModel(): InputStream = get(s"/mapping/v0/fact")
  def getConjectureModel(): InputStream = get(s"/mapping/v0/conjecture")
  def getRiskGroupModel(): InputStream = get(s"/mapping/v0/riskgroups")

  def getCodeMappingV2(): InputStream = get("/mapping/v2/code")
  def getIcdModelV2(): InputStream = get(s"/mapping/v2/icd")
  def getHccModelV2(): InputStream = get(s"/mapping/v2/hcc")
  def getCptModelV2(): InputStream = get(s"/mapping/v2/cpt")
  def getSnomedModel(): InputStream = get(s"/mapping/v2/snomed")
}
