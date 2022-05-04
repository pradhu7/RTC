package com.apixio.app.fluent

import akka.actor.{Actor, ActorLogging, Props}
import com.apixio.app.fluent.FluentLoggerActor.{JsonMessage, MapMessage}
import com.apixio.app.fluent.JsonUtils.JObjectHelper
import com.typesafe.config.{Config, ConfigFactory}
import org.fluentd.logger.FluentLogger
import org.json4s.JsonAST.{JObject, JValue}

class FluentLoggerActor(host: String, port: Int, tagPrefix: String) extends Actor with ActorLogging {
  private val fluentLogger = FluentLogger.getLogger(null, host, port)

  override def receive: Receive = {
    case JsonMessage(message: JValue) =>
      message match {
        case jobj:JObject =>
          val map = (JObject() +++ JVMInfo() +++ jobj).toJavaMap
          map.put("level", "EVENT")
          fluentLogger.log(tagPrefix, map, currentTimestamp)

        case _ =>
          val map = (JObject() +++ JVMInfo() +++ JObject("app" -> message)).toJavaMap
          map.put("level", "EVENT")
          fluentLogger.log(tagPrefix, map, currentTimestamp)
      }

    case MapMessage(map: Map[String, String]) =>
      import collection.JavaConversions._
      fluentLogger.log(tagPrefix, Map("level" -> "EVENT") ++ map, currentTimestamp)
  }

  override def postStop(): Unit = {
    fluentLogger.close()
    super.postStop()
  }

  private def currentTimestamp = {
    System.currentTimeMillis() / 1000
  }
}

object FluentLoggerActor {

  val default: Config = ConfigFactory.parseString("""
      |fluent_logging {
      |    enabled = false
      |    tag-prefix = "placeholder"
      |    server = "localhost"
      |    port = 24224
      |}
    """.stripMargin)

  def props(appConfig: Config): Props = {
    val resolved = appConfig.withFallback(default)
    if (resolved.getBoolean("fluent_logging.enabled"))
      Props(new FluentLoggerActor(resolved.getString("fluent_logging.server"), resolved.getInt("fluent_logging.port"), resolved.getString("fluent_logging.tag-prefix")))
    else
      Props.empty
  }

  def props(host: String, port: Int, tagPrefix: String): Props = {
    if (host.isEmpty || tagPrefix.isEmpty)
      Props.empty
    else
      Props(new FluentLoggerActor(host, port, tagPrefix))
  }

  def actorName = "fluent_logger"

  case class JsonMessage(message: JValue)
  case class MapMessage(map: Map[String, String])
}
