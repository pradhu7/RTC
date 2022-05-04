package com.apixio.scala.apxapi

import scala.collection.mutable

class CMP(connection :ServiceConnection, tm: ApxSession, options: Map[String,String] = Map()) extends BaseService(connection, tm, options) {
  def projectMetrics(pid: String, start: Option[Long] = None, end: Option[Long] = None) = {
    val params = mutable.Map[String, String]()

    def addPairAs[T](key: String)(value: T) =
      params += (key -> value.toString)

    start.foreach(addPairAs[Long]("start"))
    end.foreach(addPairAs[Long]("end"))
    get[String](s"/cmp/v1/metric/hcc/project/$pid", params=params.toMap)
  }
}
