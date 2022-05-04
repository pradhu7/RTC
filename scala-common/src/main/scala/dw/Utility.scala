package com.apixio.scala.dw

import com.apixio.restbase.RestUtil
import javax.ws.rs.core.{Response, UriInfo}
import javax.servlet.http.Cookie

import scala.collection.JavaConversions._

/**
 * Some utility methods
 */
object Utility {
  val okResponse = Response.ok().build

  def toJsonResponse(obj: Any): Response = Response.ok(ApxServices.mapper.writeValueAsString(obj)).build

  def toResponse(obj: String): Response = Response.ok(obj).build

  def errorResponse(status: Integer, msg: String): Response = {
    val code = Response.Status.fromStatusCode(status)
    val ent = ApxServices.mapper.writeValueAsString(Map("requestId" -> RestUtil.getRequestId,
      "reason" -> code.toString, "code" -> status.toString, "message" -> msg))
    Response.status(code).entity(ent).build
  }

  def convertQueryParams(uriInfo: UriInfo) = uriInfo.getQueryParameters.map { case (k,v) => k -> v.mkString }.toMap

  class ApxCookie(name: String, value: String, httpOnly: Boolean = false) extends Cookie(name: String, value: String) {
    // we should only be sending secure cookies
    setSecure(true)
    // prevent the browser from accessing/manipulating this cookie - this needs to be false if the cookie controls browser functionality
    setHttpOnly(httpOnly)
    // this is necessary so the cookie will update regardless of the resource path being called
    setPath("/")
  }
}

