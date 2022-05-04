package com.apixio.scala.dw

import com.apixio.scala.logging.ApixioLoggable

import javax.ws.rs.core.{Context, MediaType, UriInfo}
import javax.ws.rs.{Produces, GET, Path}
import javax.servlet.http.HttpServletRequest

/**
 * These are the common apixio endpoints
 */
@Path("/")
@Produces(Array(MediaType.APPLICATION_JSON))
class ApxCommonResource(conf: ApxConfiguration) extends ApixioLoggable {
  setupLog(getClass.getCanonicalName)

  /**
   * Get the results of the healthchecks
   * @return the health check map
   */
  @GET @Path("healthcheck")
  def healthcheck(@Context req: HttpServletRequest, @Context uri: UriInfo) =
    withAPILogging("healthcheck", req, uri) {
      val results = ApxServicesHealthCheck.checkServices(conf)
      val res = (results.isEmpty) || (results.filter(!_._2._1).size == 0)
      Utility.toJsonResponse(results ++ Map("result" -> res))
    }

  /**
   * Get the version
   * @return the version of this application
   */
  @GET @Path("version")
  def version(@Context req: HttpServletRequest, @Context uri: UriInfo) =
    withAPILogging("version", req, uri) {
      Utility.toJsonResponse(ApixioLoggable.getVersion)
    }
}
