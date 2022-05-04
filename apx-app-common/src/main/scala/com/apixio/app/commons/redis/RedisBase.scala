package com.apixio.app.commons.redis

import com.apixio.app.health_metric.MetricCapture

trait RedisBase extends MetricCapture {
  def prefix: String
  val NASSOC: String = "nassoc-"
  val ASSOC_USER_PROJECT: String = "userproj"
  val NASSOC_USER_PROJECT_BY_USER = "nassoc-userproj-us:"

  def makeKey(str: String): String = prefix + str
}