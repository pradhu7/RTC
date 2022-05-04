package com.apixio.scala.apxapi

import com.apixio.model.profiler.Provider


class Npi(connection :ServiceConnection, tm: ApxSession, options: Map[String,String] = Map()) extends BaseService(connection, tm, options) {
  def getProvider(npi: String) : Option[Provider] = {
    val providerResponse: Option[Map[String, Any]] = try {
      get[List[Map[String, Any]]](s"/providers/search/npi/${npi}", auth=false).headOption
    } catch {
      case _: Throwable => None
    }
    providerResponse.map(p => {
      val name = p("name").asInstanceOf[Map[String, String]]
      val npi = p("npi").toString
      Provider(fn = name("first"), ln = name("last"), mn = name("middle"), npi = npi)
    })
  }

  def searchProvider(projectId: String)(query: String) = {
    get[String](s"/providers/project/search/$query?projId=$projectId")
  }
}
