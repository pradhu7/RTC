package com.apixio.scala.dw

import java.io.File
import javax.validation.Validation

import com.apixio.scala.logging.ApixioLoggable
import com.apixio.scala.utility.ApxConfigurationWatcher
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.introspect.{AnnotationIntrospectorPair, JacksonAnnotationIntrospector}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.introspect.ScalaAnnotationIntrospector
import io.dropwizard.Configuration
import io.dropwizard.configuration.DefaultConfigurationFactoryFactory
import io.dropwizard.jackson.Jackson

import scala.annotation.meta.field
import scala.beans.BeanProperty

/**
 * The super set configuration object for all scala dropwizard Apixio services.
 */
case class ApxConfiguration (
    @BeanProperty @(JsonProperty @field) var application:Map[String,Object] = Map[String,Object](),
    @BeanProperty @(JsonProperty @field) var acl:Map[String,String] = Map[String,String](),
    @BeanProperty @(JsonProperty @field) var apxLogging:Map[String,String] = Map[String,String](),
    @BeanProperty @(JsonProperty @field) var cql:Map[String,String] = Map[String,String](),
    @BeanProperty @(JsonProperty @field) var cqlInternal:Map[String,String] = Map[String,String](),
    @BeanProperty @(JsonProperty @field) var cqlApplication:Map[String,String] = Map[String,String](),
    @BeanProperty @(JsonProperty @field) var cqlSignal:Map[String,String] = Map[String,String](),
    @BeanProperty @(JsonProperty @field) var elastic:Map[String,Object] = Map[String,Object](),
    @BeanProperty @(JsonProperty @field) var searchChartSpace:Map[String,String] = Map[String,String](),
    @BeanProperty @(JsonProperty @field) var kafka:Map[String,String] = Map[String,String](),
    @BeanProperty @(JsonProperty @field) var mysql:Map[String,Object] = Map[String,Object](),
    @BeanProperty @(JsonProperty @field) var redis:Map[String,String] = Map[String,String](),
    @BeanProperty @(JsonProperty @field) var jdbc_ddl:Map[String,Object] = Map[String,Object](),
    @BeanProperty @(JsonProperty @field) var jdbc_dml:Map[String,Object] = Map[String,Object](),
    @BeanProperty @(JsonProperty @field) var kafka_metric:Map[String,Object] = Map[String,Object](),
    @BeanProperty @(JsonProperty @field) var s3:Map[String,String] = Map[String,String](),
    @BeanProperty @(JsonProperty @field) var microservices:Map[String,String] = Map[String,String](),
    @BeanProperty @(JsonProperty @field) var security:Map[String,String] = Map[String,String](),
    @BeanProperty @(JsonProperty @field) var seqstore:Map[String,String] = Map[String,String](),
    @BeanProperty @(JsonProperty @field) var path:Map[String,String] = Map[String,String](),
    @BeanProperty @(JsonProperty @field) var consul:Map[String,String] = Map[String,String](),
    @BeanProperty @(JsonProperty @field) var kafkaSignal:Map[String,Object] = Map[String,Object](),
    @BeanProperty @(JsonProperty @field) var jdbcSignalControl:Map[String,String] = Map[String,String](),
    @BeanProperty @(JsonProperty @field) var jdbcSignalDocmeta:Map[String,String] = Map[String,String](),
    @BeanProperty @(JsonProperty @field) var jdbcProvider:Map[String,String] = Map[String,String](),
    @BeanProperty @(JsonProperty @field) var propertyHelperConfig:Map[String,String] = Map[String,String](),
    @BeanProperty @(JsonProperty @field) var jdbcApxDataDao:Map[String,String] = Map[String,String](),
    @BeanProperty @(JsonProperty @field) var filterConfig:Map[String,Object] = Map[String,Object](),
    @BeanProperty @(JsonProperty @field) var featureFlagsConfig:Map[String,Object] = Map[String,Object](),
    @BeanProperty @(JsonProperty @field) var microServiceConfig:Map[String,Object] = Map[String,Object](),
    @BeanProperty @(JsonProperty @field) var uploadBatchConfig:Map[String,Integer] = Map[String,Integer]()

) extends Configuration {
  /**
   * Default constructor for deserialization.
   * @return A new instance of ElasticbundlerConfiguration.
   */
  def this() = this(application = Map[String,Object]())
}

/**
 * Static fields and methods for the ElasticbundlerConfiguration class.
 */
object ApxConfiguration extends ApixioLoggable {
  setupLog(getClass.getCanonicalName)

  /**
   * The currently loaded configuration object.
   */
  var configuration:Option[ApxConfiguration] = None
  /**
   * Read the file and set up the configuration.
   * @param path The path to read from.
   */
  def initializeFromFile(path:String) = {
    val v = Validation.buildDefaultValidatorFactory.getValidator
    val m = Jackson.newObjectMapper
    m.registerModule(DefaultScalaModule)
     .setAnnotationIntrospector(new AnnotationIntrospectorPair(ScalaAnnotationIntrospector, new JacksonAnnotationIntrospector()))
    val fac = new DefaultConfigurationFactoryFactory[ApxConfiguration]()

    val file = new File(path)
    val cfg = fac.create(classOf[ApxConfiguration],v,m,"dw").build(file)
    ApxConfiguration.configuration = Some(cfg)
  }

  def monitorConfigurationChanges(file:File) : Unit = {
    val watcher = new ApxConfigurationWatcher(file)
    watcher start()
    debug("ApxConfiguration :: start watching [" + file.getAbsolutePath + "] for changes")
  }


}
