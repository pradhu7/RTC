package com.apixio.app.consul.utils

object URLUtils {
  def buildURI(startURL: String)(params: Map[String, String])(pathFragments: String*): String = {
    val basePath = if (startURL.endsWith("/")) startURL.dropRight(1) else startURL
    val paramFrag = params.map { case (k, v) => s"$k=$v"}.mkString("?", "&", "")
    val pathFrag = pathFragments.map(entry => if (entry.startsWith("/")) entry.drop(1) else entry)
      .map(entry => if (entry.endsWith("/")) entry.dropRight(1) else entry).mkString("/", "/", "")
    s"$basePath$pathFrag$paramFrag"
  }
}
