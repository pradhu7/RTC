package com.apixio.dao.elastic

import com.apixio.model.profiler.{Code, WorkItem}
import com.sksamuel.elastic4s.ElasticDsl
import com.sksamuel.elastic4s.mappings.{DynamicMapping, MappingDefinition}
import com.sksamuel.elastic4s.mappings.FieldType._
import com.sksamuel.elastic4s.ElasticDsl._
import org.elasticsearch.search.SearchHit
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._

/**
 * This is a wrapped work item object that can interface with elastic search.
 * @note This class wrapper allows elastic4s to translate the wrapped document to JSON using
 * our own defined idiom.
 * @author slydon@apixio.com
 */
case class WorkItemDocument(wi:WorkItem) extends TypedDocumentSource {
  /**
   * This is a value to use for an indexing key for the wrapped document.
   * @return A key to use, or None to simply use the elastic search default index value.
   */
  override def key() = Some(wi.key)
  /**
   * The type of the document.
   */
  override def documentType() = WorkItemDocument.typeName
  /**
   * Get this document as JSON.
   * @return This document as JSON.
   */
  override def json() = wi.asJson()(MappingSource.mapper)
  /**
   * Delete this document
   * @return Success/Failure
   */
  def delete()(implicit mgr: IndexManager) = mgr.deleteThis(this)
  /**
   * Update this old document with the fields that differ from the newWI. Do not call if findings are different.
   * When findings are different, we need to be locking this work item.
   * @param newWI to compare to find which fields to update
   * @param fields to possibly update if they differ
   * @return Success/Failure
   */
  def update(newWI: WorkItem, fields: Set[String])(implicit mgr: IndexManager) : Boolean =
    mgr.updateThis(this, wi.updateFields(newWI, fields))
}

/**
 * Static fields and methods for the OpportunityDocument class.
 * @author rbelcinski@apixio.com
 */
object WorkItemDocument extends MappingSource[WorkItem] {
  val logger = LoggerFactory.getILoggerFactory.getLogger(getClass.getCanonicalName)
  /**
   * The type of the document.
   */
  override val typeName: String = "workitem"
  /**
   * A mapping definition for this type.  Used to override any type mappings that
   * are not treated correctly by elastic search.
   * @return A mapping definition for this type.
   */
  override def mapping: MappingDefinition = ElasticDsl.mapping(s"$typeName") fields (
    "patient" typed StringType index NotAnalyzed,
    "code" nested(
      "c" typed StringType index NotAnalyzed fields (
        field name "raw" typed FloatType ignoreMalformed true
      ),
      "s" typed StringType index NotAnalyzed
    ),
    "codes" nested(
      "c" typed StringType index NotAnalyzed,
      "s" typed StringType index NotAnalyzed
    ),
    "claimedCodes" nested(
      "c" typed StringType index NotAnalyzed,
      "s" typed StringType index NotAnalyzed
    ),
    "project" typed StringType index NotAnalyzed,
    "pass" typed IntegerType,
    "scores" typed DoubleType,
    "score" typed DoubleType,
    "dynamic" typed DoubleType,
    "haf" typed DoubleType,
    "state" typed StringType index NotAnalyzed,
    "phase" typed StringType index NotAnalyzed,
    "bundle" typed StringType index NotAnalyzed,
    "comment" typed StringType index NotAnalyzed,
    "isClaimed" typed BooleanType,
    "isProblem" typed BooleanType,
    "isReportable" typed BooleanType,
    "users" typed StringType,                       //want this analyzed since it is a list
    "findings" nested(
      "document" typed StringType index NotAnalyzed,
      "code" nested(
        "c" typed StringType index NotAnalyzed,
        "s" typed StringType index NotAnalyzed
      ),
      "pages" typed IntegerType,                    //list
      "startDos" typed DateType,
      "endDos" typed DateType,
      "predictedDos" typed DateType,
      "score" typed DoubleType,
      "tags" typed StringType,                      //want this analyzed since it is a list
      "state" typed StringType index NotAnalyzed,
      "ineligible" typed LongType,
      "claimed" typed DateType,
      "annotations" nested(
        "uuid" typed StringType index NotAnalyzed,
        "user" typed StringType index NotAnalyzed,
        "org" typed StringType index NotAnalyzed,
        "project" typed StringType index NotAnalyzed,
        "confirmed" typed BooleanType,
        "code" nested(
          "c" typed StringType index NotAnalyzed,
          "s" typed StringType index NotAnalyzed
        ),
        "dos" typed DateType,
        "dosStart" typed DateType,
        "dosEnd" typed DateType,
        "provider" nested(
          "fn" typed StringType index NotAnalyzed,
          "ln" typed StringType index NotAnalyzed,
          "mn" typed StringType index NotAnalyzed,
          "npi" typed StringType index NotAnalyzed
        ),
        "encounterType" typed StringType index NotAnalyzed,
        "reviewFlag" typed BooleanType,
        "comment" typed StringType index NotAnalyzed,
        "timestamp" typed DateType,
        "phase" typed StringType index NotAnalyzed,
        "pages" typed IntegerType,                  //list
        "winner" typed BooleanType,
        "last" typed BooleanType,
        "rejectReason" typed StringType index NotAnalyzed
      ),
      "coverage" nested(
        "s" typed StringType index NotAnalyzed,
        "e" typed StringType index NotAnalyzed
      )
    ),
    "metadata" nested(
      "whitelist" nested(
        "code" nested(
          "c" typed StringType index NotAnalyzed,
          "s" typed StringType index NotAnalyzed
        )
      ),
      "whitelistCount" typed IntegerType,
      "lastAnnotated" typed DateType,
      "firstAnnotated" typed DateType,
      "findingCount" typed IntegerType,
      "hccType" typed StringType index NotAnalyzed
    )
  ) dynamic DynamicMapping.False // Stuff not in this definition is ignored...
  // TODO: only allow metadata field to be dynamic. Which is not supported by version 2.4.x

  /**
   * Translate a hit object to something represented as a domain object.
   * @param h The hit to translate.
   * @return The translated object.
   */
  override def translate(h:SearchHit) : Option[Result] = {
    // Only do something if the returned object type is something that we would
    // understand
    if (h.getType == typeName)
      // Make a result and return:
      Some(Result(h.index,h.getId,h.score,WorkItemDocument(convertJson(h.getSourceAsString))))
    else
      None
  }
  /**
   * Convert the indicated object into an object of the indicated type
   * @param s The JSON to convert.
   * @return An option containing the converted object.
   */
  override def convertJson(s:String) : WorkItem = WorkItem.fromJson(s)(MappingSource.mapper)
  /**
   * A method for getting all of the work items related to an opportunity.
   * @param patient The patient
   * @param code The code
   * @param (implicit) The mgr already established for the index to query.
   * @return An array of the matching WorkItems.
   */
  def workItems(patient: String, code: Code, limit: Int = 1000)(implicit mgr: IndexManager): List[WorkItem] = {
    val raw = search in mgr.name / typeName query {
      if (code == null)
        termQuery("patient", patient)
      else
        bool {
          must(
            termQuery("patient", patient),
            nestedQuery("code") query {
              bool {
                must (
                  termQuery("code.c", code.code),
                  termQuery("code.s", code.system)
                )
              }
            }
          )
        }
    } size limit
    mgr.rawSearch(raw).get.getHits.getHits.flatMap(translate(_)).map(_.entity.asInstanceOf[WorkItemDocument].wi).toList
  }

  /**
   * A method for getting a particular work item.
   * @param key The work item key
   * @param (implicit) The mgr already established for the index to query.
   * @return An array of the matching WorkItems.
   */
  def workItem(key: String)(implicit mgr: IndexManager): WorkItem =
    convertJson(mgr.rawGet(get id key from mgr.name / typeName).get.sourceAsString)

  /**
   * A method to delete all work items not matching the bundle id.
   * @param bundleId The bundle id to match for not being deleted.
   * @param persisted The set of keys that were persisted during bundling.
   * @param f The partial function for calling back to app to delete (true) or not (false) this item (and side-effect logging).
   * @param (implicit) The mgr already established for the index to query.
   * @return The number of deleted work items.
   */
  def deleteWork(bundleId: String, persisted: Set[String])(f: WorkItemDocument => Boolean)(implicit mgr: IndexManager) : Unit = {
    var count : Int = 0
    mgr.scroll(5, search in mgr.name / typeName query { bool { must(termsQuery("state", "routable", "deleted")) not(termQuery("bundle", bundleId)) } }) { wis =>
      wis.foreach(_ match {
        case wid:WorkItemDocument if !persisted.contains(wid.wi.key) && f(wid) =>
          wid.delete
        case _ =>
          false
      })
    }
  }
}
