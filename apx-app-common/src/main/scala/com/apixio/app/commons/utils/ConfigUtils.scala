package com.apixio.app.commons.utils

import com.typesafe.config.Config

object ConfigUtils {

  implicit class ConfigOption(config: Config) {
    def toConfigOption(paths: String*): Option[Config] = {
      paths.foldLeft(Some(config): Option[Config]) {
        case (acc, each) =>
          if (acc.exists(_.hasPath(each))) acc.map(_.getConfig(each)) else Option.empty
      }
    }

    def hasChildren(names: String*): Boolean = {
      names.forall { config.hasPath }
    }

    def getBooleanOpt(name: String): Option[Boolean] = if (config.hasPath(name)) Some(config.getBoolean(name)) else Option.empty
    def getStringOpt(name: String): Option[String]   = if (config.hasPath(name)) Some(config.getString(name)) else Option.empty
    def getIntOpt(name: String): Option[Int]         = if (config.hasPath(name)) Some(config.getInt(name)) else Option.empty

    def getChildren: Set[String] = {
      import scala.collection.JavaConverters._
      config.root().unwrapped().keySet().asScala.toSet
    }

  }
}
