package com.apixio.scala.dw


import io.dropwizard.ConfiguredBundle
import io.dropwizard.setup.{Bootstrap, Environment}
import Middleware._

class ConfiguredFilterBundle extends ConfiguredBundle[ApxConfiguration] {
  override def initialize(bootstrap: Bootstrap[_]) = {}

  override def run(configuration: ApxConfiguration, environment: Environment): Unit = {
    // wire up all filters to all routes
    registerFilters(environment, List(
      ("httpToHttps", httpToHttps _),
      ("addApxSession", addApxSession _),
      ("addHSTSHeader", addHSTSHeader _),
      ("addXFrameHeader", addXFrameHeader _)
    ))
  }
}
