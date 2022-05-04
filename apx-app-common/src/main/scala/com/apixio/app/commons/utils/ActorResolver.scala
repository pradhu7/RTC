package com.apixio.app.commons.utils

import akka.actor.{ActorContext, ActorRef}
import akka.util.Timeout

import scala.concurrent.{ExecutionContext, Future}

case class ActorResolver(resolver: String => Option[ActorRef] ) {
  def resolve(actorName: String): Option[ActorRef] = resolver(actorName)
  @deprecated
  def resolveOrEmpty(actorName: String): ActorRef = resolve(actorName).getOrElse(ActorRef.noSender)
}

object ActorResolver {
  val empty: ActorResolver = ActorResolver{ _ => Option.empty  }

  def fromContext(context: ActorContext): ActorResolver = {
    val resolver: String => Option[ActorRef] = {actorName: String => context.child(actorName)}
    ActorResolver(resolver)
  }
}

object Implicit {
  implicit class RichActorContext(val context: ActorContext) extends AnyVal {
    def createActorResolver(): ActorResolver = ActorResolver.fromContext(context)
  }

  implicit class RichOptionResolver(val value: Option[ActorRef]) extends AnyVal {
    def `!`(message: Any): Unit = {
      value match {
        case Some(ref) => ref ! message
        case _ => //ignore
      }
    }

    def `?`(message: Any)(implicit timeout: Timeout, ec: ExecutionContext): Future[Any] = {
      value match {
        case Some(ref) =>
          import akka.pattern._
          ref ? message
        case _ => Future(throw new MatchError("Unresolved sender"))//ignore
      }
    }
  }
}