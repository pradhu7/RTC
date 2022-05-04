package com.apixio.app.documentsearch

import com.apixio.scala.apxapi.ApxApiFactory
import com.apixio.scala.dw._
import com.apixio.scala.logging.ApixioLoggable
import io.dropwizard.Application
import io.dropwizard.configuration.{EnvironmentVariableSubstitutor, SubstitutingSourceProvider}
import io.dropwizard.setup.{Bootstrap, Environment}
import io.dropwizard.views.ViewBundle
import scalikejdbc.ConnectionPool



/**
  * Top level lifetime management object for the router service.
  */
class DocumentSearchService extends Application[ApxConfiguration] with ApixioLoggable {

  override def initialize(bootstrap: Bootstrap[ApxConfiguration]) {
    super.initialize(bootstrap)

    ApxServices.setupObjectMapper(bootstrap.getObjectMapper)
    bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider,
      new EnvironmentVariableSubstitutor(false)))
    // FOR VIEW
    bootstrap.addBundle(new ViewBundle[ApxConfiguration])
  }

  override def run(cfg: ApxConfiguration, env: Environment) {
    ApxServices.init(cfg)
    ApxServices.setupApxLogging()
    setupLog(getClass.getCanonicalName)
    ApxServices.setupApiAcls("Document Search Filter", env)

    env.jersey.register(ApxApiFactory.buildBinder)
    env.jersey.register(new ApxAppResource())

    // hook in your application's resources here
    env.jersey.register(new DocumentSearchResource())

    // Set up the datasource
    setupDatasource(ApxServices.configuration)

    // common resources/healthcheck
    env.jersey.register(new ApxCommonResource(cfg))
    env.healthChecks.register("apxservices", new ApxServicesHealthCheck(cfg))

    info("Done setting up service!")
  }

  def setupDatasource(config: ApxConfiguration): Unit = {
    val sql = config.application("sql").asInstanceOf[Map[String,String]]
    Class.forName("org.sqlite.JDBC")
    ConnectionPool.singleton(sql("host").toString, null, null)
  }
}

/**
  * Container for static fields and methods for BundlerApplication.
  */
object DocumentSearchService {
  /**
    * Main entry point.
    *
    * @param args Command line arguments.
    */
  def main(args: Array[String]): Unit = new DocumentSearchService().run(args: _*)
}