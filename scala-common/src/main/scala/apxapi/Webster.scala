package apxapi

import com.apixio.scala.apxapi.{ApxSession, BaseService, ServiceConnection}

class Webster (connection :ServiceConnection, tm: ApxSession, options: Map[String,String] = Map())
  extends BaseService(connection, tm, options) {

  def getLatestSchema(fullSchema: Boolean = false): String = {
    get[String](s"/api/schema?fullSchema=$fullSchema", auth = false)
  }

  def getSchema(id: Int, fullSchema: Boolean = false): String = {
    get[String](s"/api/schema/$id?fullSchema=$fullSchema", auth = false)
  }

}
