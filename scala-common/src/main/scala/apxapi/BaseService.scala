package com.apixio.scala.apxapi

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException

import akka.pattern.CircuitBreakerOpenException
import apxapi.ResourceTooBusyException
import com.apixio.scala.dw.ApxServices
import com.apixio.scala.logging.ApixioLoggable
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import scala.collection.JavaConverters._
import scala.reflect.runtime.universe.typeOf
import scalaj.http.{Http, HttpResponse}


/**
 * Base class for any apxapi object to extend.
 * Exceptions are raised on issues observed, and the return type T exists on success.  This assumes
 * users don't want/need to check non-error status codes.
 * This class also handles retry and token swap on some failures.
 */
case class ServiceConnection(var url: String, var callProxy: CallProxyTrait)
abstract class BaseService(connection :ServiceConnection, tm: ApxSession, options: Map[String,String] = Map()) extends ApixioLoggable {
  setupLog(getClass.getCanonicalName)

  val microservices = Option(ApxServices.configuration.microservices).getOrElse(Map())
  val jsonContent : Map[String,String] = Map("content-type" -> "application/json")
  val connTimeoutMs = microservices.getOrElse("connTimeoutMs", "1000").toInt
  val readTimeoutMs = microservices.getOrElse("readTimeoutMs", "10000").toInt

  def asJsonBytes(o: Any) : Array[Byte] = ApxServices.mapper.writeValueAsBytes(o)

  def authHeader(token: String) : Map[String,String] = Map("Authorization" -> s"Apixio ${token}")

  def get[T: Manifest](
      path: String,
      params: Map[String,String] = Map(),
      headers: Map[String,String] = Map(),
      data: Array[Byte] = Array(),
      auth: Boolean = true,
      rawRequest: HttpServletRequest = null,
      rawResponse: HttpServletResponse = null) : T = execute[T](s"${connection.url}${path}", "GET", params, headers, data, auth, rawRequest, rawResponse)

  def post[T: Manifest](
      path: String,
      params: Map[String,String] = Map(),
      headers: Map[String,String] = Map(),
      data: Array[Byte] = Array(),
      auth: Boolean = true,
      rawRequest: HttpServletRequest = null,
      rawResponse: HttpServletResponse = null) : T = execute[T](s"${connection.url}${path}", "POST", params, headers, data, auth, rawRequest, rawResponse)

  def put[T: Manifest](
      path: String,
      params: Map[String,String] = Map(),
      headers: Map[String,String] = Map(),
      data: Array[Byte] = Array(),
      auth: Boolean = true,
      rawRequest: HttpServletRequest = null,
      rawResponse: HttpServletResponse = null) : T = execute[T](s"${connection.url}${path}", "PUT", params, headers, data, auth, rawRequest, rawResponse)

  def delete[T: Manifest](
      path: String,
      params: Map[String,String] = Map(),
      headers: Map[String,String] = Map(),
      data: Array[Byte] = Array(),
      auth: Boolean = true,
      rawRequest: HttpServletRequest = null,
      rawResponse: HttpServletResponse = null) : T = execute[T](s"${connection.url}${path}", "DELETE", params, headers, data, auth, rawRequest, rawResponse)


  def execute[T: Manifest](
      uri: String,
      method: String,
      params: Map[String,String],
      headers: Map[String,String],
      data: Array[Byte],
      auth: Boolean,
      rawRequest: HttpServletRequest,
      rawResponse: HttpServletResponse,
      tries: Int = 0): T = {
    if (auth && tm.internal_token.isEmpty)
        tm.swap_token()

    //GET with data is unsupported in scalaj and javax
    if ((method == "GET" || method == "DELETE") && data.nonEmpty) throw new UnsupportedOperationException(s"${method}s with body data do not work with this library.")

    val mergedHeaders = rawRequest match {
      case null => headers
      case _ => rawRequest
          .getHeaderNames
          .asScala
          .foldLeft(headers) { case (headers, k: String) =>
            if (headers.contains(k))
              headers
            else
              headers + (k -> rawRequest.getHeader(k))
          }
    }

    def req = {
      var req = Http(uri)
        .timeout(connTimeoutMs = connTimeoutMs, readTimeoutMs = readTimeoutMs)
        .params(params)

      if (data.nonEmpty)
        req = req.postData(data)

      // It is necessary to set the method *after* potentially setting
      // the payload, as the underlying library used will override the
      // method to `POST`. This manifests as a bug when making an HTTP
      // request with a request body, but not the `POST` verb.
      req = req.method(method)

      if (auth)
        req = req.headers(mergedHeaders ++ authHeader(tm.internal_token.get))
      else
        req = req.headers(mergedHeaders)

      req
    }

    try {

      debug(s"req: ${req.toString}")
      var res = connection.callProxy.callService(() => {req.asBytes})
      debug(s"res: ${connection.url} [1] => (${res.code}, ${res.body})")

      //try one more time with a token swap if you get a 401
      if (res.code == 401 && auth) {
        tm.swap_token()
        res = connection.callProxy.callService(() => { req.asBytes })
        debug(s"res: ${connection.url} [2] => (${res.code}, ${res.body})")
      }

      if (res.code == 401) throw new ApxAuthException(new String(res.body))
      if (res.code == 403) throw new ApxForbiddenException(new String(res.body))
      if (res.code >= 400) throw new ApxCodeException(res.code, new String(res.body))

      ApxApi.updateMicroservicesStat(connection.url, ApxApi.SUCCESS, System.currentTimeMillis)

      if (rawResponse != null)
        res.headers
          .foreach { header =>
            header match {
              case (k, vs) => vs.foreach(rawResponse.addHeader(k, _))
            }
          }

      typeOf[T] match {
        case t if t =:= typeOf[Array[Byte]] => res.body.asInstanceOf[T]
        case t if t =:= typeOf[HttpResponse[Array[Byte]]] => res.asInstanceOf[T]
        case t if t <:< typeOf[String] => (new String(res.body)).asInstanceOf[T]
        case _ => ApxServices.mapper.readValue[T](new String(res.body))
      }


    } catch {
      case e:CircuitBreakerOpenException =>
        ApxApi.updateMicroservicesStat(connection.url, ApxApi.FAIL, System.currentTimeMillis)
        throw new ResourceTooBusyException
      case e:TimeoutException =>
        ApxApi.updateMicroservicesStat(connection.url, ApxApi.FAIL, System.currentTimeMillis)
        throw new ResourceTooBusyException
      case e: Exception if e.isInstanceOf[UnknownHostException] ||
          e.isInstanceOf[SocketTimeoutException] ||
          e.isInstanceOf[ConnectException] =>
        if (tries < 2) {
          execute(uri, method, params,headers, data, auth, rawRequest, rawResponse, tries+1)
        }
        else {
          ApxApi.updateMicroservicesStat(connection.url, ApxApi.FAIL, System.currentTimeMillis)
          debug(s"Couldn't connect to ${connection.url}")
          throw e
        }
      case e:IOException =>
        ApxApi.updateMicroservicesStat(connection.url, ApxApi.FAIL, System.currentTimeMillis)
        throw e
    }
  }
}
