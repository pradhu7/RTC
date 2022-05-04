package com.apixio.scala.apxapi

import org.glassfish.hk2.api.Factory
import javax.inject.Inject
import javax.ws.rs.container.ContainerRequestContext
import org.glassfish.jersey.process.internal.RequestScoped
import org.glassfish.hk2.utilities.binding.AbstractBinder

class ApxApiFactory @Inject()(ctr: ContainerRequestContext) extends Factory[ApxApi] {
  private final val context: ContainerRequestContext = ctr

  // we only perform the cast here rather than the entire ApxApi setup because Filters/Middleware can return
  // the appropriate 401 status and injected services can not
  override def provide(): ApxApi = context.getProperty("apxSession").asInstanceOf[ApxApi]

  override def dispose(apxapi: ApxApi): Unit = { }
}

object ApxApiFactory {
  def buildBinder() =
    new AbstractBinder {
      override def configure(): Unit = {
        bindFactory(classOf[ApxApiFactory])
          .to(classOf[ApxApi])
          .in(classOf[RequestScoped])
      }
    }
}
