package com.apixio.scala.apxapi

case class HeaderOrData(columnName: Option[String], columnType: Option[Int], pos: Option[Int],
                        data: Option[Array[Any]])

class Metrics(connection: ServiceConnection, tm: ApxSession, options: Map[String,String] = Map()) extends BaseService(connection, tm, options) {
  def version() = {
    get[String](s"/version", auth = false)
  }

  def executeQuery(queryString: String) = {
    post[Array[Array[HeaderOrData]]](s"/metrics/query/execute", data = queryString.getBytes, headers = jsonContent)
  }

  def executeSavedQuery(queryName: String, params: Map[String, Any]) = {
    post[Array[Array[HeaderOrData]]](s"/metrics/query/${queryName}/execute", data = asJsonBytes(params), headers = jsonContent)
  }
}
