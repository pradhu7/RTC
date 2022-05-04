package com.apixio.scala.dw

import java.io.IOException
import java.util.UUID
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerResponseContext
import javax.ws.rs.container.ContainerResponseFilter

import com.apixio.scala.logging.ApixioLoggable
import org.slf4j.LoggerFactory
import com.google.common.base.Strings
import org.joda.time.DateTime


object RequestIdFilter extends ApixioLoggable {
  setupLog(getClass.getCanonicalName)
  private val REQUEST_ID = "X-Request-Id"
}

class RequestIdFilter(serviceId: String) extends ContainerResponseFilter {

  @throws[IOException]
  override def filter(request: ContainerRequestContext, response: ContainerResponseContext): Unit = {
    var id = request.getHeaderString(RequestIdFilter.REQUEST_ID)
    if (Strings.isNullOrEmpty(id)) id = UUID.randomUUID().toString

    request.getHeaders.putSingle(RequestIdFilter.REQUEST_ID, id)
    RequestIdFilter.info("service = {"+serviceId+"} method={"+request.getMethod+"} path={"+request.getUriInfo.getPath+"} request_id={"+ id +"} status={"+response.getStatus+"} timestamp={"+ new DateTime +"}")
    response.getHeaders.putSingle(RequestIdFilter.REQUEST_ID, id)
  }

}


