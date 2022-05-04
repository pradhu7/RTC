package com.apixio.app.health_metric

import java.time.{Duration, LocalDateTime}

import scala.concurrent.duration.FiniteDuration

case class CountSum(count: Long, sum: Long, min: Long, max: Long) {
  lazy val avg: Double = sum.toDouble / count

  def +(v: Long): CountSum = {
    val newMin = if (v < min) v else min
    val newMax = if (v > max) v else max
    copy(count = count + 1, sum = sum + v, min = newMin, max = newMax)
  }
  def +(other: CountSum):CountSum = {
    if (other == CountSum.zero) return this
    val newMin = if (other.min < min) other.min else min
    val newMax = if (other.max > max) other.max else max
    copy(count = count + other.count, sum = sum + other.sum, min = newMin, max = newMax)
  }
}

object CountSum {
  val zero: CountSum = CountSum(0, 0, Int.MaxValue, Int.MinValue)
}

case class SlidingWindow(window: FiniteDuration, private val dataPoints: Vector[CountSum], private val lastEntry: LocalDateTime) {
  private lazy val sum = dataPoints.reduce(_+_)

  def add(value: Long): SlidingWindow = {
    require(LocalDateTime.now().withSecond(0).withNano(0) == lastEntry)
    val updatedLast = dataPoints.last + value
    val updatedDataPoints = dataPoints.dropRight(1) :+ updatedLast
    copy(dataPoints = updatedDataPoints)
  }

  def refresh(): SlidingWindow = {
    val now = LocalDateTime.now().withSecond(0).withNano(0)
    val duration = Duration.between(lastEntry, now)
    val mins = duration.toMinutes.toInt
    val updatedDataPoints = dataPoints.drop(mins) ++ Vector.fill[CountSum](mins){CountSum.zero}
    copy(lastEntry = now, dataPoints = updatedDataPoints)
  }

  def getAvg: Double = {
    require(LocalDateTime.now().withSecond(0).withNano(0) == lastEntry)
    if (sum == CountSum.zero) 0 else sum.avg
  }

  def getMin: Double = {
    require(LocalDateTime.now().withSecond(0).withNano(0) == lastEntry)
    if (sum == CountSum.zero) return 0
    sum.min
  }

  def getMax: Double = {
    require(LocalDateTime.now().withSecond(0).withNano(0) == lastEntry)
    if (sum == CountSum.zero) return 0
    sum.max
  }
}

object SlidingWindow {
  def apply(window: FiniteDuration): SlidingWindow = {
    val dataPoints = Vector.fill[CountSum](window.toMinutes.toInt){ CountSum.zero }
    val lastEntry: LocalDateTime = LocalDateTime.now().withSecond(0).withNano(0)
    SlidingWindow(window, dataPoints, lastEntry)
  }
}