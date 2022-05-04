package apxapi

import com.apixio.scala.apxapi.{ApxSession, BaseService, ServiceConnection}

class ModelCatalog (connection: ServiceConnection, tm: ApxSession, options: Map[String,String] = Map()) extends BaseService(connection, tm, options) {
  def version() = {
    get[String](s"/version", auth = false)
  }

  def getMCMeta(mcid: String) = {
    get[Map[String, Any]](s"/mcs/models/$mcid/metadata")
  }

  def getSearchMeta(mcid: String) = {
    get[Map[String, Any]](s"/mcs/models/$mcid/metadata/search")
  }

  def predictionMCIDToLogicalId(predMCID: String) = {
    get[Seq[String]](s"/mcs/models/${predMCID}/owns")
  }

  def getMCIDs(mcid: String) = {
    val path = s"/mcs/models/${mcid}/deps/owners"
    get[Seq[Map[String, Any]]](path)
  }

  def logicalIdMeta(logicalId: String): Map[String,Any] = {
    val path = s"/mcs/logids/${logicalId.replaceAll("/","%2F")}/owner"
    get[Map[String, Any]](path)
  }

  def logicalIdToMcid(logicalId: String): String = {
    logicalIdMeta(logicalId).getOrElse("id", "").asInstanceOf[String]
  }

  def getMCIDsByEngineVariantState(engine:String, variant:String, state: String) = {
    val path = s"/mcs/models?search.engine=${engine}&search.variant=${variant}&state=${state}"
    get [Seq[Map[String,Any]]](path)
  }

  def getMCIDsByEngineVariantStateWithPds(engine:String, variant:String, state: String, pdsId: String) = {
    val path = s"/mcs/models?search.engine=${engine}&search.variant=${variant}&state=${state}&search.pds=${pdsId}"
    get [Seq[Map[String,Any]]](path)
  }

  def getMCIDsByTagAndState(tag: String, state: String) = {
    val path = s"/mcs/models?search.tags=contains($tag)&state=$state&latest=creation"
    get[Seq[Map[String, Any]]](path)
  }
}



