package com.apixio.app.health_metric

import scala.concurrent.duration._

case class HealthMetricDataPoints(entries: List[SlidingWindow]) {
  def addValue(v: Long): HealthMetricDataPoints = {
    val updated = entries.map { e =>
      e.refresh().add(v)
    }
    copy(updated)
  }
}

case class HealthMetricRegistry(map: Map[String, HealthMetricDataPoints]) {
  def addMetric(metricName: String, value: Long): HealthMetricRegistry = {
    val dp = map.getOrElse(metricName, HealthMetricDataPoints(List(SlidingWindow(1 minute), SlidingWindow(5 minutes), SlidingWindow(15 minutes)))).addValue(value)
    copy(map = map + (metricName -> dp))
  }

  def addMetrics(metricNames: List[String], value: Long): HealthMetricRegistry = {
    metricNames.foldLeft(this) { case (acc, each) =>
      acc.addMetric(each, value)
    }
  }

  def getLoadAvg(metricName: String): List[Double] = {
    map.get(metricName).map(_.entries.map(_.refresh().getAvg)).getOrElse(List.empty)
  }
}

object HealthMetricRegistry {
  def empty = HealthMetricRegistry(Map.empty)
}