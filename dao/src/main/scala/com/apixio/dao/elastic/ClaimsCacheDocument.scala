package com.apixio.dao.elastic

import com.apixio.model.profiler.{Claim, ClaimsCache}

import com.sksamuel.elastic4s.ElasticDsl
import com.sksamuel.elastic4s.mappings.{DynamicMapping, MappingDefinition, DynamicTemplateDefinition}
import com.sksamuel.elastic4s.mappings.FieldType._
import com.sksamuel.elastic4s.ElasticDsl._
import org.elasticsearch.search.SearchHit
import scala.collection.mutable.ListBuffer

case class ClaimsCacheDocument(claimsCache: ClaimsCache) extends TypedDocumentSource {
  override def key() = Some(claimsCache.patient)

  override def documentType: String = ClaimsCacheDocument.typeName

  override def json: String = claimsCache.asJson()(MappingSource.mapper)
}

object ClaimsCacheDocument extends MappingSource[ClaimsCache] {

  override val typeName: String = "claim"

  override def translate(h: SearchHit): Option[Result] = {
    if (h.getType == typeName)
      Some(Result(h.index,h.getId,h.score, ClaimsCacheDocument(convertJson(h.getSourceAsString))))
    else
      None
  }

  override def convertJson(s: String): ClaimsCache = MappingSource.mapper.readValue[ClaimsCache](s)

  override def mapping: MappingDefinition = new MappingDefinition(s"$typeName") fields (
    field("patient") typed StringType index NotAnalyzed,
    field("job") typed StringType index NotAnalyzed,
    field("updated") typed DateType index NotAnalyzed,
    field("claims") nested (
      field("monthmap") typed IntegerType,
      field("code") nested (
        field("c") typed StringType index NotAnalyzed,
        field("s") typed StringType index NotAnalyzed
      )
    )
  ) templates DynamicTemplateDefinition(
    name = "string_template",
    mapping = field typed StringType index NotAnalyzed,
    _match_mapping_type = Some(StringType.elastic)
  )

  /**
   * Get claim cache by patient
   * @param pat patient UUID
   * @param mgr (implicit) The mgr already established for the index to query.
   * @return A ClaimsCache object
   */
  def patientClaims(pat: String)(implicit mgr: IndexManager): Option[ClaimsCache] = {
    mgr.rawGet(get id pat from mgr.name / typeName).map(_.sourceAsString)
      .flatMap(Option(_))
      .map(convertJson)
  }

  /**
    * Get claim cache for whole project
    * @param mgr (implicit) The mgr already established for the index to query.
    * @return A ClaimsCache object
    */
  def allClaims()(implicit mgr: IndexManager): List[ClaimsCache] = {
    val claims = ListBuffer[ClaimsCache]()
    mgr.scroll(1000, search in mgr.name / typeName )(r => {
      claims ++= r.map(d => d.asInstanceOf[ClaimsCacheDocument].claimsCache)
    })
    claims.toList
  }

  def projectClaimsIndex(project: String) =
    s"${project.toLowerCase}-claims"
}
