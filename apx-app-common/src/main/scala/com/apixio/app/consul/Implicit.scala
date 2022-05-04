package com.apixio.app.consul

import akka.actor.ActorContext
import akka.event.LoggingAdapter
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.apixio.app.commons.utils.ConfigUtils._
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object Implicit {
  private val default = ConfigFactory.parseString(
    """
      | consul {
      |    enable_discovery = false
      |    server = "localhost"
      |    port = 8500
      |    dc = "stg"
      |    token = "uuid"
      |    startup_endpoints = {
      |    }
      |  }
      |  system_endpoints = [] //deprecated.
    """.stripMargin)

  implicit class ConsulConfig(config: Config) {
    def addStartupEndpoints()(implicit mat: ActorMaterializer,
                              ec: ExecutionContext,
                              serviceDiscovery: (ConsulEndpoint, String) => Future[List[ServiceEndpoint]] = null, //current used for unit test.
                              timeout: Timeout = Timeout(30 seconds), log: LoggingAdapter = akka.event.NoLogging): Config = {
      Try(discoverSystemEndpointsFromMap(config.withFallback(default), "consul.startup_endpoints", if (serviceDiscovery == null) ConsulUtils.getServiceEndpoints else serviceDiscovery)) match {
        case Success(result) => result.withFallback(config).resolve()
        case Failure(failure) =>
          log.error(failure, "Error occurred while discovering system endpoints")
          config
      }
    }

    def consulEndpointDiscoveryService(implicit context: ActorContext, mat: ActorMaterializer): Unit = {
      context.actorOf(ConsulActor.props(config, context.self), ConsulActor.actorName)
    }
  }

  private def discoverSystemEndpointsFromMap(config: Config, endpointName: String, getServiceEndpoints: (ConsulEndpoint, String) => Future[List[ServiceEndpoint]])(implicit mat: ActorMaterializer, ec: ExecutionContext, timeout: Timeout, log: LoggingAdapter): Config = {
    val empty = ConfigFactory.parseString("")
    if (!config.getBoolean("consul.enable_discovery")) {
      log.info("Skipping discovering system endpoint from consul.")
      return empty
    }
    log.info("Discovering system endpoints from consul...")
    val consulServer = ConsulEndpoint(config)
    val endpointNames = config.getConfig(endpointName)
    val endPoints = endpointNames.getChildren.filter { c => endpointNames.getValue(c).valueType().name() == "OBJECT"}.map{e => e -> endpointNames.getConfig(e)}.toMap

    val futures = endPoints.map { case (k, eConfig) =>
      EndpointResolver(eConfig.getStringOpt("name").getOrElse(k), eConfig.getStringOpt("node"), eConfig.getStringOpt("server_path"), eConfig.getStringOpt("port_path"), eConfig.getStringOpt("pattern_path"))
    }.map { resolver =>
      getServiceEndpoints(consulServer, resolver.endpointName).map { consulEndpoints =>
        val filtered = resolver.node match {
          case Some(n) => consulEndpoints.filter(_.nodeName == n)
          case _ => consulEndpoints
        }
        filtered.headOption match {
          case Some(se) => Some(resolver.resolve(config, se.server, se.port.toString))
          case _ => Option.empty
        }
      }
    }

    val result = Future.sequence(futures).map { entries =>
      ConfigFactory.parseString(entries.flatten.mkString("\n"))
    }.recoverWith{ case _ => Future(empty)}

    Await.result(result, timeout.duration)
  }
}