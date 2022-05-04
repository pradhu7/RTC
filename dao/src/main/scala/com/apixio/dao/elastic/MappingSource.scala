package com.apixio.dao.elastic

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.ScalaObjectMapper
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{SearchDefinition, ScoreDefinition, QueryDefinition}
import com.sksamuel.elastic4s.mappings.{DynamicMapping, MappingDefinition}

import org.elasticsearch.search.SearchHit

import scala.util.{Failure, Try, Success}

/**
 * Types that implement this trait can create mapping definitions.
 * @author rbelcinski@apixio.com
 */
trait MappingSource[+T] {
  /**
   * The type of the document.
   */
  val typeName:String
  /**
   * A mapping definition for this type.  Used to override any type mappings that
   * are not treated correctly by elastic search.
   * @return A mapping definition for this type.
   */
  def mapping:MappingDefinition
  /**
   * Translate a hit object to something represented as a domain object.
   * @param h The hit to translate.
   * @return The translated object, or None.
   */
  def translate(h:SearchHit):Option[Result]
  /**
   * Convert the indicated object into an object of the indicated type
   * @param s The JSON to convert.
   * @return An option containing the converted object.
   */
  def convertJson(s:String):T
  /**
   * Search the index for this type using the indicated query.
   * @param startIndex The starting index for the result set. (0)
   * @param number The number of entities in the result set. (10)
   * @param q The query to execute.
   * @param mgr The index manager to use.
   * @return The collection of things.
   */
  def searchIndex(startIndex:Int = 0, number:Int = 10)(q: => QueryDefinition)(implicit mgr:IndexManager) =
    mgr.searchWithTranslation { indexName =>
      search in s"$indexName/$typeName" from startIndex limit number query q
    }.toList.map(_.asInstanceOf[Result])
  /**
   * Get a bunch of ids from the index for the wrapped type.
   * @param startIndex The starting index for the result set. (0)
   * @param number The number of entities in the result set. (10)
   * @param mgr The index manager to use.
   * @return An array of string IDs, which might be empty.
   */
  def indexIds(startIndex:Int = 0, number:Int = 10)(implicit mgr:IndexManager) =
    mgr.search { indexName =>
      search in s"$indexName/$typeName" from startIndex limit number query matchAllQuery fields ""
    }.toList.map(_.getId)
  /**
   * Get a bunch of ids from the index for the wrapped type.
   * @param startIndex The starting index for the result set. (0)
   * @param number The number of entities in the result set. (10)
   * @param q A query to execute.
   * @param mgr The index manager to use.
   * @return An array of string IDs, which might be empty.
   */
  def indexIdsWithQuery(startIndex:Int = 0, number:Int = 10)(q:QueryDefinition)(implicit mgr:IndexManager) =
    mgr.search { indexName =>
      search in s"$indexName/$typeName" from startIndex limit number query q fields ""
    }.toList.map(_.getId)
  /**
   * Search the index for this type using the indicated query and scoring using the scorers array.
   * @param startIndex The starting index for the result set. (0)
   * @param number The number of entities in the result set. (10)
   * @param q The query to execute.
   * @param sc The array of scoring objects.
   * @param mgr The index manager to use.
   * @return The collection of things.
   */
  def scoredSearchIndex(startIndex:Int = 0, number:Int = 10)(q: => QueryDefinition)(sc: => Array[ScoreDefinition[_]])(implicit mgr:IndexManager) = {
    mgr.searchWithTranslation { indexName =>
      search in s"$indexName/$typeName" from startIndex limit number query functionScoreQuery(q).scorers(sc:_*)
    }.toList.map(_.asInstanceOf[Result])
  }
  /**
   * Return a count of things that satisfy the indicated query.
   * @param q The query to execute.
   * @param mgr The index manager to use.
   * @return The count result.
   */
  def count(q: => QueryDefinition)(implicit mgr:IndexManager) = mgr.countThings(typeName)(q)
  /**
   * Get an entity if it exists.
   * @param id The ID of the entity.
   * @param mgr The manager to use to get the entity.
   * @return An option containing the entity or None.
   */
  def getEntity(id:String)(implicit mgr:IndexManager) =
    mgr.getDocumentWithTranslation(id,typeName).asInstanceOf[T]
  /**
   * Get a collection of entities given the indicated IDs.
   * @param ids The IDs to look for.
   * @param mgr The manager to use to get the entities.
   * @return An option containing a List[(id,Option[T])]
   */
  def getEntities(ids:Array[String])(implicit mgr:IndexManager) =
    mgr.getDocumentsWithTranslation(ids,typeName) match {
      case Some(a) => Some(a.toList.map(t => (t._1,t._2.asInstanceOf[T])))
      case None => None
    }
  /**
   * Insert a mapping for this type into the indicated manager.
   * @param mgr The manager to use to inject the mapping.
   */
  def putMapping(implicit mgr:IndexManager) = {
    // Execute the PUT mapping:
    val r = Try(mgr.client.execute(
      put mapping s"${mgr.name}" / typeName as this.mapping._fields
    ).await(mgr.timeoutValue))
    // Unpack the response:
    r match {
      case Success(s) => s.isAcknowledged
      case Failure(_) => false
    }
  }
  /**
   * Insert a mapping for this type into the indicated manager.
   * @param mgr The manager to use to inject the mapping.
   */
  def putNonDynamicMapping(implicit mgr:IndexManager) = {
    // Execute the PUT mapping:
    val r = Try(mgr.client.execute(
      put mapping s"${mgr.name}" / typeName as this.mapping._fields dynamic DynamicMapping.False
    ).await(mgr.timeoutValue))
    // Unpack the response:
    r match {
      case Success(s) => s.isAcknowledged
      case Failure(_) => false
    }
  }
}

/**
 * This is the set of supported mappings.
 * @note Probably would be better to do this with reflection.
 * @author rbelcinski@apixio.com
 */
object MappingSource {
  /**
   * The object mapper.
   */
  var mapper:ObjectMapper with ScalaObjectMapper = null
  /**
   * Setup the json object mapper that should be used.
   * @params m The object mapper with scala object mapper
   */
  def setMapper(m:ObjectMapper with ScalaObjectMapper) : Unit = {
    mapper = m
  }
  /**
   * This is the collection of known mapping sources.
   */
  val knownMappingSources = Map(
    "WorkItemDocument" -> WorkItemDocument
  )
}
