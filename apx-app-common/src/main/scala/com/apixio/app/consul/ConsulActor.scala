package com.apixio.app.consul

import akka.actor.{ActorLogging, ActorRef, ActorSystem, FSM, Props}
import akka.pattern._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.apixio.app.consul.utils.URLUtils
import com.typesafe.config.Config

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.concurrent.duration._

case class ConsulEndpoint(token: String, server: String, port: Int, dc: String, tag: String) {
  private val urlBuilder = URLUtils.buildURI("")(Map("dc" -> dc, "token" -> token, "tag" -> tag))_

  def toServicesURI: String = {
    urlBuilder(Seq("/v1/catalog/services"))
  }

  def toServiceURI(serviceName: String): String = {
    urlBuilder(Seq(s"/v1/catalog/service/$serviceName"))
  }
}

object ConsulEndpoint {
  def optionalString(config: Config, path: String, default: String): String = if (config.hasPath(path)) {
    config.getString(path)
  } else {
    default
  }
  def apply(config: Config): ConsulEndpoint = {
    val consulConfig = config.getConfig("consul")
    val consul_tag = optionalString(config, "tag", "ec2")
    ConsulEndpoint(consulConfig.getString("token"), consulConfig.getString("server"), consulConfig.getInt("port"), consulConfig.getString("dc"), consul_tag)
  }
}

case class Services(serviceNames: Set[String])

case class ConsulData(consul: ConsulEndpoint, services: Set[String], endpoints: Map[String, ServiceEndpoint])

class ConsulActor(config: Config, parent: ActorRef)(implicit mat: ActorMaterializer) extends FSM[ConsulActor.State, ConsulData] with ActorLogging {
  private val consulServer = ConsulEndpoint(config)

  import ConsulActor._
  import context.dispatcher
  implicit val system: ActorSystem = context.system

  override def preStart(): Unit = {
    super.preStart()
    val appEntry = config.getStringList("application_endpoints").toList
    val entries = if (appEntry.isEmpty) {
      implicit val timeout = Timeout(30 seconds)
      ConsulUtils.getServices(consulServer)
    } else Future{appEntry}

    entries.map { entries =>
      ServiceEntries(entries)

    }.pipeTo(self)
  }

  startWith(ConsulActor.InitState, ConsulData(consulServer, Set.empty, Map.empty))

  when(ConsulActor.InitState) {
    case Event(entries: ServiceEntries, data) =>
      self ! ConsulActor.RefreshEndpoints
      goto(ConsulActor.ReadyState) using data.copy(services = entries.values.toSet)
  }

  when(ConsulActor.ReadyState) {
    case Event(ConsulActor.RefreshEndpoints, data) =>
      val futures = data.services.map{ endpoint =>
        Future(endpoint) zip ConsulUtils.getServiceEndpoints(consulServer, endpoint)
      }
      Future.sequence(futures).pipeTo(self)
      stay()

    case Event(values: Set[(String, List[ServiceEndpoint])], data) =>
      val endpointMap = values.collect { case (name, endpoint) if endpoint.nonEmpty => name -> endpoint.head
      }.toMap
      system.scheduler.scheduleOnce(5 minutes, self, ConsulActor.RefreshEndpoints)
      goto(ConsulActor.ReadyState) using data.copy(endpoints = endpointMap)
  }
}


object ConsulActor {
  sealed trait State
  case object InitState extends State
  case object ReadyState extends State
  case object RefreshEndpoints


  def props(config: Config, parent: ActorRef)(implicit mat: ActorMaterializer): Props = Props(new ConsulActor(config, parent))
  val actorName = "consul_actor"
  case class ResolvedServiceEndpoint(list: List[ServiceEndpoint]) {
    def getServerPort(name: String): Option[(String, Int)] = {
      list.find(_.nodeName == name).map(v=> (v.server, v.port) )
    }
  }
  case class ServerEndpoint(serviceName: String, node: String, serverIp: String, serverPort: Int)

  case class ServiceEntries(values: Seq[String])
}