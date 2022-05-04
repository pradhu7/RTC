package com.apixio.app.health_metric

import com.apixio.app.commons.utils.ActorResolver
import com.apixio.app.health_metric.HealthMetricActor.AddMetric

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import com.apixio.app.commons.utils.Implicit._

trait MetricCapture {
  def metricNames: List[String]

  def asyncMetric[T](operation: Future[T])(implicit actorResolver: ActorResolver, ec: ExecutionContext): Future[T] = {
    captureAsyncMetric(operation)
  }

  def captureAsyncMetric[T](operation: Future[T])(implicit actorResolver: ActorResolver, ec: ExecutionContext): Future[T] = {
    val startTime = System.currentTimeMillis()
    operation.onComplete {
      case Success(o) if actorResolver.resolve(HealthMetricActor.actorName).nonEmpty =>
        actorResolver.resolve(HealthMetricActor.actorName) ! AddMetric(metricNames, System.currentTimeMillis() - startTime)

      case Failure(o)  if actorResolver.resolve(HealthMetricActor.actorName).nonEmpty =>
        actorResolver.resolve(HealthMetricActor.actorName) ! AddMetric(metricNames, System.currentTimeMillis() - startTime)

      case _ =>
    }
    operation
  }

  def captureSyncMetric[T](operation: T)(implicit actorResolver: ActorResolver): T = {
    val startTime = System.currentTimeMillis()
    Try(operation) match {
      case Success(t) if actorResolver.resolve(HealthMetricActor.actorName).nonEmpty =>
        actorResolver.resolve(HealthMetricActor.actorName) ! AddMetric(metricNames, System.currentTimeMillis() - startTime)
        t

      case Success(t) => t

      case Failure(t) if actorResolver.resolve(HealthMetricActor.actorName).nonEmpty =>
        actorResolver.resolve(HealthMetricActor.actorName) ! AddMetric(metricNames, System.currentTimeMillis() - startTime)
        throw t

      case Failure(t) => throw t
    }
  }
}