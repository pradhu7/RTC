package com.apixio.app.documentsearch

import com.apixio.scala.dw.{ApxServices, Utility}
import com.apixio.scala.logging.ApixioLoggable
import javax.servlet.http.HttpServletRequest
import javax.ws.rs._
import javax.ws.rs.core._

import scala.util.{Failure, Success, Try}
@Path("search")
class DocumentSearchResource extends ApixioLoggable {

  setupLog(this.getClass.getCanonicalName)

  val documentSearchManager = new DocumentSearchManager
  val realTimeDocumentSearchManager = new RealTimeDocumentSearchManager

  @POST @Path("snippets/patient/{patient}")
  def searchPatientSnippets(termList: String,
                    @PathParam("patient") patient: String,
                    @Context req: HttpServletRequest,
                    @Context uri: UriInfo): Any = {
    withAPILogging("search-patient-snippets", req, uri) {
      Try {
        documentSearchManager.searchPatientSnippets(patient,termList)
      } match {
        case Success(results) => Utility.toJsonResponse(results)
        case Failure(e) =>
          e.printStackTrace()
          Utility.errorResponse(404, ApxServices.mapper.writeValueAsString(e.getMessage))
      }
    }
  }

  @POST @Path("overlays/patient/{patient}")
  def searchPatientOverlays(termList: String,
                            @PathParam("patient") patient: String,
                            @Context req: HttpServletRequest,
                            @Context uri: UriInfo): Any  = {
    withAPILogging("search-patient-overlays", req, uri) {
      Try {
        documentSearchManager.searchPatientOverlays(patient,termList)
      } match {
        case Success(results) => Utility.toJsonResponse(results)
        case Failure(e) =>
          e.printStackTrace()
          Utility.errorResponse(404, ApxServices.mapper.writeValueAsString(e.getMessage))
      }
    }
  }

  @POST @Path("highlights/patient/{patientUuid}")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  def searchPatientHighlights(termsByPageByDocument: Map[String,Map[String,List[String]]],
                              @PathParam("patientUuid") patientUuid: String,
                              @Context req: HttpServletRequest,
                              @Context uri: UriInfo): Any  = {
    withAPILogging("search-document-highlights", req, uri) {
      Try {
        realTimeDocumentSearchManager.getPatientHighlights(patientUuid, termsByPageByDocument)
      } match {
        case Success(results) => Utility.toJsonResponse(results)
        case Failure(e) =>
          e.printStackTrace()
          Utility.errorResponse(404, ApxServices.mapper.writeValueAsString(e.getMessage))
      }
    }
  }

  @GET @Path("words/document/{document}")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  def getDocumentWords(@PathParam("document") document: String,
                               @Context req: HttpServletRequest,
                               @Context uri: UriInfo): Any  = {
    withAPILogging("search-patient-coordinates", req, uri) {
      Try {
        realTimeDocumentSearchManager.getDocumentWords(document)
      } match {
        case Success(results) => Utility.toJsonResponse(results)
        case Failure(e) =>
          e.printStackTrace()
          Utility.errorResponse(404, ApxServices.mapper.writeValueAsString(e.getMessage))
      }
    }
  }

  @POST @Path("words/document/{document}/{page}")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  def searchDocumentPageCoordinates(coords: Map[String, Double],
                              @PathParam("document") document: String,
                              @PathParam("page") page: String,
                              @Context req: HttpServletRequest,
                              @Context uri: UriInfo): Any  = {
    withAPILogging("search-patient-coordinates", req, uri) {
      Try {
        realTimeDocumentSearchManager.searchDocumentPageForCoords(document,page,coords)
      } match {
        case Success(results) => Utility.toJsonResponse(results)
        case Failure(e) =>
          e.printStackTrace()
          Utility.errorResponse(404, ApxServices.mapper.writeValueAsString(e.getMessage))
      }
    }
  }

  @GET @Path("hocr/document/{document}/{page}")
  def getDocumentPageHocr(@PathParam("document") document: String,
                          @PathParam("page") page: String,
                          @Context req: HttpServletRequest,
                          @Context uri: UriInfo): Any  = {
    withAPILogging("get-document-hocr", req, uri) {
      Try {
        realTimeDocumentSearchManager.getDocumentPageHocr(document, page)
      } match {
        case Success(results) => Utility.toJsonResponse(results)
        case Failure(e) =>
          e.printStackTrace()
          Utility.errorResponse(404, ApxServices.mapper.writeValueAsString(e.getMessage))
      }
    }
  }
}
