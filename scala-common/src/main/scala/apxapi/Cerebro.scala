package apxapi

import com.apixio.scala.apxapi.{ApxSession, BaseService, ServiceConnection}

class Cerebro(connection :ServiceConnection, tm: ApxSession, options: Map[String,String] = Map()) extends BaseService(connection, tm, options) {
  def getMcidStatus(projectId: String, mcid: String, startTime: String, endTime: String ) : Map[String, Any] = {
    get[Map[String, Any]](s"/projects/$projectId/mcids/$mcid/progressReport?startTime=$startTime&endTime=$endTime")
  }
}
