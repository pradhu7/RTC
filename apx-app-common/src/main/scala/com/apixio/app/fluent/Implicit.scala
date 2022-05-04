package com.apixio.app.fluent

import akka.actor.{ActorContext, ActorRef, Props}
import akka.event.LoggingAdapter
import com.apixio.app.commons.utils.ActorResolver
import com.typesafe.config.Config
import org.json4s.JsonAST.JValue

object Implicit {
  implicit class FluentConfig(appConfig: Config) {
    def addFluentLoggingService()(implicit context: ActorContext): ActorRef = {
      createFluentActorRef(context, FluentLoggerActor.props(appConfig))
    }
  }

  implicit def addFluentLoggingService(host: String, port: Int, tagPrefix: String)(implicit context: ActorContext): ActorRef = {
    createFluentActorRef(context, FluentLoggerActor.props(host, port, tagPrefix))
  }

  private def createFluentActorRef(context: ActorContext, props: Props) = {
    if (props != Props.empty)
      context.actorOf(props, FluentLoggerActor.actorName)
    else ActorRef.noSender
  }

  implicit class FluentActorResolver(actorResolver: ActorResolver) {
    def sendFluentEvent(value: JValue)(implicit log: LoggingAdapter): Unit = {
      val fActorRef: Option[ActorRef] = getFluentEventActorRef(log)
      fActorRef.foreach { ref => ref ! FluentLoggerActor.JsonMessage(value)  }
    }

    def sendFluentEvent(map: Map[String, String])(implicit log: LoggingAdapter): Unit = {
      val fActorRef: Option[ActorRef] = getFluentEventActorRef(log)
      fActorRef.foreach { ref => ref ! FluentLoggerActor.MapMessage(map)  }
    }

    private def getFluentEventActorRef(log: LoggingAdapter) = {
      val fActorRef = actorResolver.resolve(FluentLoggerActor.actorName)
      if (fActorRef.isEmpty) {
        log.warning("Unable to send message. Reason: FluentLoggerActor is not mapped to current ActorResolver.")
      }
      fActorRef
    }
  }
}
