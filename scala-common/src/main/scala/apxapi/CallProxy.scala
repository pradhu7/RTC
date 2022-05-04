package com.apixio.scala.apxapi

import com.apixio.scala.logging.ApixioLoggable
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scalaj.http.HttpResponse

trait CallProxyTrait {
  def callService(function: () => HttpResponse[Array[Byte]]): HttpResponse[Array[Byte]]
}

class CallProxy(timeout: Duration ) extends ApixioLoggable with CallProxyTrait {
  this.setupLog(this.getClass.getName)

  def callService(function: () => HttpResponse[Array[Byte]]): HttpResponse[Array[Byte]] = {
    Await.result(
      Future.successful(function.apply()),
      timeout
    )
  }
}
