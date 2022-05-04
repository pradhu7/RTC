package com.apixio.app.consul

import com.typesafe.config.Config

case class EndpointResolver(endpointName: String, node: Option[String], serverPath: Option[String], portPath: Option[String], patternPath: Option[String]) {
  def resolve(config: Config, server: String, port: String): String = {
    Seq(resolvePath(config, serverPath, server),
        resolvePath(config, portPath, port),
        resolvePattern(config, patternPath, server, port))
      .flatten.mkString("\n")
  }

  private def resolvePath(config: Config, pathOpt: Option[String], value: String): Option[String] = {
    pathOpt match {
      case Some(path) if config.hasPath(path) =>
        config.getValue(path).valueType().name() match {
          case "NUMBER" => value
          case _ => "\"" + value + "\""
        }
        Some(s"$path = $value")

      case _ => Option.empty
    }
  }

  private def resolvePattern(config: Config, pathOpt: Option[String], server: String , port: String): Option[String] = {
    pathOpt match {
      case Some(sourcePath) if config.hasPath(sourcePath) =>
        val path = config.getString(sourcePath)
        val resolved = resolve(path, Map("server" -> server, "port" -> port))
        Some(sourcePath + " = " + "\"" + resolved + "\"")

      case _ => Option.empty
    }
  }

  private def findAllVars(text: String): Set[String] = {
    val pattern = "\\$\\{(.*?)\\}".r
    val matcher = pattern.findAllMatchIn(text).toList
    matcher.map{v => if (v.groupCount > 0) v.group(1) else ""}.toSet.filterNot(_ == "")
  }

  private def resolve(text: String, map: Map[String, String]) : String = {
    val vars = findAllVars(text)
    val resolvedMap = map.filter{ case (k, _) => vars.contains(k) }.map { case (k, v) => "${"+k+"}" -> v }
    resolvedMap.foldLeft(text) { case (acc, (k, v)) =>
      acc.replaceAllLiterally(k, v)
    }
  }
}
