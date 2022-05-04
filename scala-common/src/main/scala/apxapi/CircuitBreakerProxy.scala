package com.apixio.scala.apxapi

import akka.actor.ActorSystem
import akka.pattern.CircuitBreaker
import com.apixio.scala.logging.ApixioLoggable

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scalaj.http.HttpResponse

case class CircuitBreakerConfig(name: String, callTimeout: Int, resetTimeout: Int, maxFailures: Int)

class CircuitBreakerProxy(config: CircuitBreakerConfig)( implicit val system: ActorSystem) extends ApixioLoggable with CallProxyTrait {
  this.setupLog(this.getClass.getName)

  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  def callService(function: () =>  HttpResponse[Array[Byte]]): HttpResponse[Array[Byte]] = {
    circuitBreaker.withSyncCircuitBreaker(function.apply())
  }

  private val circuitBreaker = CircuitBreaker((system).scheduler,
    maxFailures = config.maxFailures,
    callTimeout = config.callTimeout seconds,
    resetTimeout = config.resetTimeout seconds)
    .onOpen(onOpen())
    .onHalfOpen(onHalfOpen())
    .onClose(onClose())
    .onCallTimeout(onCallTimeout)
    .onCallFailure(onCallFailure)


  def onOpen(): Unit = {
    error(config.name + " circuit breaker open")

  }

  def onHalfOpen(): Unit = {
    error(config.name + " circuit breaker half open")
  }

  def onClose(): Unit = {
    info(config.name + " circuit breaker closed")
  }

  def onCallTimeout(nanoseconds: Long): Unit = {
    error(config.name + " circuit breaker timeout")

  }

  def onCallFailure(nanoseconds: Long): Unit = {
    error(config.name + " circuit breaker failure")
  }
}
