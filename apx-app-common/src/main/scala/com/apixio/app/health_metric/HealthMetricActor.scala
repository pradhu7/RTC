package com.apixio.app.health_metric

import akka.actor.{FSM, Props}
import com.typesafe.config.Config
import kamon.Kamon
import com.apixio.app.commons.utils.ConfigUtils._

case class Bucket(name: String, minOpt: Option[Long], maxOpt: Option[Long]) {
  def isInRange(value: Long): Boolean = {
    (minOpt, maxOpt) match {
      case (Some(min), Some(max)) if value >= min && value <= max => true
      case (Some(min), None) if value >= min => true
      case (None, Some(max)) if value <= max => true
      case _ => false
    }
  }
}

object Bucket {
  def fromConfig(name: String, entry: Config): Option[Bucket] = {
    val minOpt = if (entry.hasPath("min")) Some(entry.getLong("min")) else Option.empty[Long]
    val maxOpt = if (entry.hasPath("max")) Some(entry.getLong("max")) else Option.empty[Long]
    if (minOpt.isEmpty && maxOpt.isEmpty) Option.empty else Some(Bucket(name, minOpt, maxOpt))
  }
}
class HealthMetricActor(appConfig: Config) extends FSM[Unit, HealthMetricRegistry] {
  private val bucketsMap: Map[String, List[Bucket]] = {
    val metricConfig: Config = appConfig.getConfig("metric")
    metricConfig.getChildren.map { child =>
      val config = metricConfig.getConfig(child)
      child -> config.getChildren.toList.flatMap { c => Bucket.fromConfig(c, config.getConfig(c))}
    }.toMap
  }

  private lazy val kamonPrefix = appConfig.getStringOpt("kamon-prefix").getOrElse("kamon-unknown")
  private lazy val kamonPublish = appConfig.getBooleanOpt("publish-using-kamon").getOrElse(false)

  val UNKNOWN = "uninitialized"

  import HealthMetricActor._
  startWith(Unit, HealthMetricRegistry.empty)

  when(Unit) {
    case Event(AddMetric(names, value), stateData) =>
      if (kamonPublish) {
        names.foreach { name =>
          Kamon.histogram(s"$kamonPrefix.$name").record(value)
        }
      }
      stay() using stateData.addMetrics(names, value)

    case Event(GetMetric(name), stateData) =>
      sender() ! LoadAvg(name, stateData.getLoadAvg(name))
      stay()

    case Event(GetMetricByBucket(name), stateData) =>
      val l = stateData.getLoadAvg(name)
      if (l.isEmpty) {
        sender() ! LoadAvgBucket(name, UNKNOWN)
      } else {
        val status = bucketsMap.get(name).flatMap(b => b.find(_.isInRange(l.head.toLong)).map(_.name)).getOrElse(UNKNOWN)
        sender() ! LoadAvgBucket(name, status)
      }
      stay()
  }
}


object HealthMetricActor {
  def props(appConfig: Config): Props = {
    if (appConfig.hasChildren("metric"))
      Props(new HealthMetricActor(appConfig))
    else Props.empty
  }
  val actorName = "health_metrics"

  case class AddMetric(metricNames: List[String], value: Long)
  case class GetMetric(metricName: String)
  case class GetMetricByBucket(metricName: String)
  case class LoadAvg(metricName: String, loadAvg: List[Double])
  case class LoadAvgBucket(metricName: String, status: String)
}
