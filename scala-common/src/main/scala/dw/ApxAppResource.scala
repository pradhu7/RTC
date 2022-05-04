package com.apixio.scala.dw

import com.apixio.scala.logging.ApixioLoggable
import javax.ws.rs.{GET, Path, Produces}
import javax.ws.rs.core.{Context, MediaType, Response, UriInfo}
import javax.servlet.http.HttpServletRequest

import com.apixio.scala.apxapi.{ApxApi, ApxAuthException}

@Path("/")
@Produces(Array(MediaType.APPLICATION_JSON))
class ApxAppResource extends ApixioLoggable {
  import ApxAppResource._
  setupLog(getClass.getCanonicalName)

  @GET @Path("me")
  def users(@Context req: HttpServletRequest, @Context uri: UriInfo, @Context apxapi: ApxApi) =
    withAPILogging("users/me", req, uri, Set(401)) {
      formatResponse(Option(apxapi).map(_.useraccounts.getUser("me")))
    }

  @GET @Path("me/roles")
  def roles(@Context req: HttpServletRequest, @Context uri: UriInfo, @Context apxapi: ApxApi) =
    withAPILogging("me/roles", req, uri, Set(401)) {
      formatResponse(Option(apxapi).map(_.useraccounts.getUserRoles("me")))
    }
}

object ApxAppResource {
  def safeGetSession(req: HttpServletRequest): Option[ApxApi] =
    Option(req.getAttribute("apxSession"))
    .map(_.asInstanceOf[ApxApi])

  def formatResponse(obj: Option[Any]): Response =
    obj match {
      case None => Utility.errorResponse(401, "No apxSession found")
      case Some(d) => Utility.toJsonResponse(d)
      case _ => throw new ApxAuthException("Unexpected response format")
    }
}
