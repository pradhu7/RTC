package com.apixio.scala.apxapi

import akka.actor.{Actor, ActorSystem, Props, Timers}
import com.apixio.scala.dw.ApxServices
import com.apixio.scala.logging.{ApixioLoggable, Transform}
import com.orbitz.consul.Consul
import com.orbitz.consul.model.health.ServiceHealth
import com.orbitz.consul.option.ImmutableQueryOptions

import scala.collection.JavaConverters._
import scala.collection.immutable.Map
import scala.concurrent.duration._
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

//TODO currenlty cache refresh implemented with timer but later it needs to listen consul watches
@deprecated("ConsulDao has been deprecated. Use ApxApi directly instead of calling this. Will be removed in future release", "3.0.0")
class ConsulDao extends Actor with Timers {
  import ConsulDao._

  def receive = {

    case TICK =>
      updateCache()
  }
}

@deprecated("ConsulDao has been deprecated. Use ApxApi directly instead of calling this. Will be removed in future release", "3.0.0")
object ConsulDao extends ApixioLoggable {
  setupLog(this.getClass.getName)

  val PASSING = "passing"
  val TICK = "tick"
  val system = ActorSystem("CongigurationRefreshTimer")
  val tickActor = system.actorOf(Props(classOf[ConsulDao]))
  var refreshCallback:() => Unit = null
  val connTimeoutMs = 5000
  val readTimeoutMs = 5000
  import system.dispatcher

  lazy val config = ApxServices.configuration.consul

  lazy val address: String = config match {
    case null => null
    case _ => config.getOrElse("address", "").replace("/v1", "")
  }
  lazy val token: String = config match {
    case null => null
    case _ => config.getOrElse("token", "")
  }
  lazy val dc: String = config match {
    case null => null
    case _ => config.getOrElse("dc", "")
  }
  lazy val tag: String = config match {
    case null => null
    case _ => config.getOrElse("tag", "ec2")
  }
  lazy val urlTemplate: String = config match {
    case null => null
    case _ => config.getOrElse("urlTemplate", "")
  }
  lazy val refreshInterval: FiniteDuration = config match {
    case null => null
    case _ => Duration(config.getOrElse("refreshInterval", "10").toInt, MINUTES)
  }

  val consul: Consul = {
    withLogging("consul.agent",
                Map("address" -> address,
                    "connTimeoutMs" -> connTimeoutMs.toString(),
                    "readTimeoutMs" -> readTimeoutMs.toString(),
                    "dc" -> dc,
                    "tag" -> tag)) {
      val consul: Consul = Consul
        .builder()
        .withPing(true)
        .withUrl(address)
        .withTokenAuth(token)
        .withConnectTimeoutMillis(connTimeoutMs)
        .withReadTimeoutMillis(readTimeoutMs)
        .build()

      val dcs = consul
        .catalogClient()
        .getDatacenters

      if (!dcs.contains(dc))
        throw new Error(f"""Consul Datacenter "$dc" not in list of available Datacenters: $dcs""")

      Option(consul)
    } (Transform.identity)
  }

  val consulQueryOptions = ImmutableQueryOptions
    .builder()
    .token(token)
    .datacenter(dc)
    .addTag(tag)
    .build()

  private var configuration: Map[String, String] = loadConfig()

  var services: Map[String, List[String]] = loadServices()

  private def getQueryParams = Map("tag" -> tag, "dc" -> dc, "token" -> token)

  def updateCache() = {

    services = loadServices()
    debug("services cache updated " + services.mkString(","))
    configuration = loadConfig()
    debug("configuration cache updated")
    if (refreshCallback!= null) refreshCallback.apply()
  }

  private def getServicesNames(): Set[String] =
    withLogging("consul.getServices") {
      val serviceListResponse = Option(consul)
        .map { _.catalogClient()
            .getServices(consulQueryOptions)
        }

      val serviceNames = serviceListResponse
        .map { _.getResponse
            .keySet()
            .asScala
            .toSet
        }

      if (serviceNames.isDefined)
        serviceNames
      else {
        warn("Consul return empty service names!")
        None
      }

    } (Transform.identity)

  def getConfigurationValue(name:String, autoRefresh:Boolean = false): String ={
    configuration.get(name) match {
      case None => null
      case Some(value) => {value}
    }
  }

  def getServices(callback:()=> Unit): Map[String, List[String]] = {
    refreshCallback = callback
    services
  }

  def getServices(): Map[String, List[String]] = {
    services
  }

  def loadConfig(): Map[String, String] = {
    getConsulConfigKeys().flatMap( key => Option(key -> getConsulValue(key))).toMap
  }

  //overrides config from consul with config from yaml
  def loadServices(): Map[String, List[String]] = {
    var result = getPassingServices()
    // TODO: Cache this. YAML configs aren't changing all the time.
    if (ApxServices.configuration != null && ApxServices.configuration.microservices != null) {
      for (microservice <- ApxServices.configuration.microservices) {
        result += microservice._1 -> List(microservice._2)
      }
    }
    result
  }

  def getPassingServices(): Map[String, List[String]] = {
    withLogging("consul.getPassingServices") {
      Try {
        val serviceNames = getServicesNames()
        serviceNames.foldLeft(Map[String, List[String]]()) {
          case (passes, serviceName) =>
            val serviceData = getServiceInfo(serviceName)
            if (serviceData.isEmpty) {
              // TODO: Check for override in yaml `microservices` block before emitting `warn`.
//              info(s"No healthy services found for $serviceName")
              passes
            } else {
              val addresses = serviceData.map { serviceInfo =>
                constructUrl(serviceInfo.getNode.getNode, serviceInfo.getService.getPort.toString)
              }.toList
              passes + (serviceName -> addresses)
            }
        }
      } match {
        case Success(s) => Some(s)
        case Failure(f) =>
//          warn(s"Using last successful service list as I am unable to fetch for services from Consul. Error=${f.getMessage}")
          Some(services)
      }
    }(Transform.identity)
  }

  private def constructUrl(node: String, port: String, baseApi: String = "") = {
    urlTemplate.format(node,port,baseApi)
  }

  def getServiceInfo(serviceName: String) = {
    val healthyServices = Option(consul).map { c => c.healthClient().getHealthyServiceInstances(serviceName, consulQueryOptions) }

    val ret: Option[List[ServiceHealth]] = healthyServices.map(_.getResponse.asScala.toList)
    ret.foreach { shl: List[ServiceHealth] =>
      pushMetric("healthy_count", shl.length.toString)
    }
    ret.getOrElse(Seq())
  }

  def getConsulValue(key: String): String = {
    val valueResponse = Option(consul)
      .flatMap { consul =>
        Option(consul
          .keyValueClient()
          .getConsulResponseWithValue(key, consulQueryOptions)
          .orElse(null))
      }
    valueResponse
      .flatMap { value =>
        Option(value
          .getResponse
          .getValueAsString
          .orElse(null))
      }.orNull
  }

  def getConsulConfigKeys():Seq[String] = {
    Option(consul)
      .map { consul =>
        consul
          .keyValueClient()
          .getKeys("config", consulQueryOptions)
          .asScala
      }.orNull
  }

  /**
    * Create CircuitBreakerConfig
    *
    * Will try to get values for callTimeout, readTimeout and maxFailures from
    * consul via key formatted as: config/services/gateway/cb/{service-name}
    * in format: callTimeoutInMilliseconds,readTimeoutInMilliseconds,maxFailuresAsInt
    * i.e. 10000,3000,5
    *
    * If config is not available will fall back to default values
    *
    * @param configName
    * @param circuitBreakerName
    * @return CircuitBreakerConfig
    */
  def getCircuitBreakerConfig(configName: String, circuitBreakerName: String): CircuitBreakerConfig = {
    val key = s"config/services/gateway/cb/$configName"
    val defaults = (10, 3, 5)

    val (callTimeout, resetTimeout, maxFailures) = configuration.get(key) match {
      case Some(v) => {
        try {
          val d = v.toString.split(",")
          (d(0).toInt / 1000, d(1).toInt / 1000, d(2).toInt)
        } catch {
          case NonFatal(e) => {
            error(e.getMessage, e.getStackTrace.toString)
            defaults
          }
        }
      }
      case _ => defaults
    }
    CircuitBreakerConfig(circuitBreakerName, callTimeout, resetTimeout, maxFailures)
  }


}
