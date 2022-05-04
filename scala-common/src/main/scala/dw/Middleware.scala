package com.apixio.scala.dw

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import java.util
import javax.servlet.{DispatcherType, ServletRequest, ServletResponse}

import com.apixio.scala.apxapi.ApxApi
import org.eclipse.jetty.server.Request
import io.dropwizard.setup.Environment

object Middleware {
  private val cookieName = "ApxToken"

  implicit def servletRequestToRequest(req: ServletRequest): Request = req.asInstanceOf[Request]
  implicit def servletResponseToHttpServletResponse(res: ServletResponse): HttpServletResponse = res.asInstanceOf[HttpServletResponse]
  // authRequiredList will let all request through without authentication if regex is not set, hence "$^" == match nothing
  lazy val authRequiredList = ApxServices.configuration.application.getOrElse("authRequiredList", "$^").asInstanceOf[String]

  def registerFilters(environment: Environment, filters: List[(String, (ServletRequest, ServletResponse) => Unit)]): Unit =
    filters.foreach(f =>
      environment.servlets()
        .addFilter(f._1, new AppFilter(f._2))
        .addMappingForUrlPatterns(util.EnumSet.of(DispatcherType.REQUEST), true, "/*"))

  def addHSTSHeader(req: ServletRequest, res: ServletResponse): Unit =
    res.setHeader("Strict-Transport-Security", "max-age=7777000; includeSubDomains")

  def addXFrameHeader(req: ServletRequest, res: ServletResponse): Unit =
    res.setHeader("X-Frame-Options", "DENY")

  /**
    * Redirect HTTP traffic to HTTPS
    *
    * Our apps are configured via iptables to forward HTTP traffic on port 80 to HTTP port specified in the
    * application config file
    * This filter will intercept HTTP traffic, and redirect it to HTTPS
    * Inspired by https://github.com/dropwizard-bundles/dropwizard-redirect-bundle/blob/master/src/main/java/io/dropwizard/bundles/redirect/
    */
  def httpToHttps(req: ServletRequest, res: ServletResponse): Unit =
    if (req.getScheme == "http") {
      val serverName = req.getServerName
      val uri = req.getRequestURI
      val query = Option(req.getQueryString) match {
        case Some(q) => s"?$q"
        case _ => ""
      }
      val reqUri = "https://" + serverName + uri + query

      res.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY)
      res.setHeader("Location", reqUri)
    }

  def addApxSession(req: ServletRequest, res: ServletResponse): Unit = {
    // TODO: these two url exceptions need to be moved to the config as "unauthed enddpoints"
    if(!req.getRequestURI.contains("/api/logout") && !req.getRequestURI.equals("/api/log")) {
      getToken(req) match {
        case eToken: Some[String] =>
          val apxapi = new ApxApi(ApxServices.configuration, external_token = eToken)
          req.setAttribute("apxSession", apxapi)
        // Ajax requests should return a 401 if we can't get a token, while regular requests should continue the filter chain
        case _ if req.getHeader("X-REQUESTED-WITH") == "XMLHttpRequest" => res.sendError(HttpServletResponse.SC_UNAUTHORIZED)
        case _ if req.getRequestURI.matches(authRequiredList) => res.sendError(HttpServletResponse.SC_UNAUTHORIZED)
        case _ => None
      }
    }
  }

  def getToken(req: Any): Option[String] = req match {
    case r: ServletRequest => Option(r.getCookies).map(_.toList).flatMap {
      _.find(_.getName == cookieName).map(_.getValue)
    }
    case _ => None
  }
}
