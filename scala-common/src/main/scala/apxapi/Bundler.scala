package com.apixio.scala.apxapi

class Bundler (connection :ServiceConnection, tm: ApxSession, options: Map[String,String] = Map()) extends BaseService(connection, tm, options) {

  def bundle(projectId:String):Array[Array[String]] = {
    put[Array[Array[String]]](s"/bundler/bundle/" + projectId + "?persist=true&delete=true&sync=true")
  }

}
