package com.apixio.dao.elastic

import java.net.NoRouteToHostException
import java.util
import java.util.concurrent.TimeUnit

import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.ElasticDsl._

import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.util.concurrent.EsRejectedExecutionException
import org.elasticsearch.search.SearchHit
import org.elasticsearch.client.transport.NoNodeAvailableException
import org.elasticsearch.transport.{ReceiveTimeoutTransportException, RemoteTransportException}

import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

/**
 * A class that may be used to interact with an elastic search index.
 * @param timeoutValue The timeout value for interactions with the server.
 * @param name The index name for interactions.
 * @param client This is the client object used to interact with the REST API of elastic search.
 * @param indexableTypes These are the types that are used to provide object mappings to the schema.
 * @author rbelcinski@apixio.com
 */
case class IndexManager(
  var timeoutValue:Duration = Duration(60, unit = TimeUnit.SECONDS),
  var name:String = "default",
  var backingStore:String = "default-1",
  var client:ElasticClient = null,
  var indexableTypes:Array[MappingSource[Object]] = Array[MappingSource[Object]]()) extends Retry {
  /**
   * Default constructor.
   * @return A new empty instance.
   */
  def this() = this(client = null)
  /**
   * These are the last known settings used to forge an ES connection.
   */
  var settings:java.util.Map[String,Object] = null
  /**
   * This is the recovery function.
   * @return true if we want to continue.
   */
  def connectionFailureRecovery = (ex:Throwable) => ex match {
    case t:RemoteTransportException =>
      // Message:
      logger.warn(s"${t.getMessage}")
      // Try again:
      true
    case t:ReceiveTimeoutTransportException =>
      // Message:
      logger.warn(s"${t.getMessage}")
      // Try again:
      true
    case t:NoNodeAvailableException =>
      // Message:
      logger.warn(s"${t.getMessage}")
      // Make sure we're closed:
      if (Try({close();true}).getOrElse(false))
        logger.warn("Could not close previous connection.")
      // Re-build connection:
      reBuild()
      // Try again:
      true
    case r:EsRejectedExecutionException =>
      // Message:
      logger.warn(s"${r.getMessage}")
      // Make sure we're closed:
      if (Try({close();true}).getOrElse(false))
        logger.warn("Could not close previous connection.")
      // Re-build connection:
      reBuild()
      // Throttle an additional second:
      Thread.sleep(1000)
      // Try again:
      true
    case r:NoRouteToHostException =>
      // Message:
      logger.warn(s"${r.getMessage}")
      // Make sure we're closed:
      if (Try({close();true}).getOrElse(false))
        logger.warn("Could not close previous connection.")
      // Re-build connection:
      reBuild()
      // Throttle an additional second:
      Thread.sleep(1000)
      // Try again:
      true
    case ex:Throwable =>
      // Message:
      logger.error(exceptionFormatter(ex))
      // Don't try again:
      false
  }
  /**
   * For recovery messaging.
   * @return A function to emit a message during recovery semantics.
   */
  def emitRecoveryMsg = Some((s:String) => logger.info(s))
  /**
   * This is an exception formatter.
   */
  var exceptionFormatter:(Throwable)=>String =
    (e:Throwable) => {
      val tr = e.getStackTrace.mkString("\n   ")
      s"${e.getClass.getCanonicalName}: ${e.getMessage} at\n   $tr"
    }
  /**
   * Is this connection manager closed?
   * @return true if closed.
   */
  def notClosed = IndexManager.connectorMap.synchronized(IndexManager.connectorMap.values.contains(client))
  /**
   * This is used to forge the connection to the server.
   * @param cfg A configuration object to build from.
   * @return This class instance.
   */
  def build(cfg:java.util.Map[String,Object]) = {
    // Logging:
    logger.info(s"Building IndexManager instance from configuration...")
    // Stash:
    settings = cfg
    // Convert to scala map:
    val m = cfg.toMap
    // Create the client object:
    client = IndexManager.configureConnector(cfg)
    // Configure the mapping types:
    m.get("indexedTypes") match {
      case Some(obj) => obj match {
        case s:String =>
          // Tokenize:
          val tokens = s.split(",").map(_.trim).filter(_.nonEmpty).distinct
          // Get classes:
          indexableTypes = tokens
            .map(MappingSource.knownMappingSources.get)
            .filter(_.isDefined)
            .map(_.get)
        case sa:util.List[String @unchecked] =>
          // Read the list and get the classes:
          indexableTypes = sa.asInstanceOf[util.List[String]].map(MappingSource.knownMappingSources.get)
            .filter(_.isDefined)
            .map(_.get)
            .toArray
        case _ => // Don't know what this is...
          logger.warn(s"Don't know how to interpret $obj when setting indexable types.")
      }
      case None => // No type wrappers...
    }
    // Set the index name if we have one:
    m.get("index") match {
      case Some(str) => name = str.asInstanceOf[String]
      case None => // Do nothing...
    }
    // Set the backing store name if we have one:
    m.get("backingStore") match {
      case Some(str) => backingStore = str.asInstanceOf[String]
      case None => // Do nothing...
    }
    // Done:
    this
  }
  /**
   * This is used to (re)forge the connection to the server.
   * @return This class instance.
   */
  def reBuild() = {
    // Logging:
    logger.info(s"Rebuilding IndexManager instance from stashed configuration...")
    // Convert to scala map:
    val m = settings.toMap
    // Create the client object:
    client = IndexManager.configureConnector(settings, overwrite = true)
    // Configure the mapping types:
    m.get("indexedTypes") match {
      case Some(obj) => obj match {
        case s:String =>
          // Tokenize:
          val tokens = s.split(",").map(_.trim).filter(_.nonEmpty).distinct
          // Get classes:
          indexableTypes = tokens
            .map(MappingSource.knownMappingSources.get)
            .filter(_.isDefined)
            .map(_.get)
        case sa:util.List[String @unchecked] =>
          // Read the list and get the classes:
          indexableTypes = sa.asInstanceOf[util.List[String]].map(MappingSource.knownMappingSources.get)
            .filter(_.isDefined)
            .map(_.get)
            .toArray
        case _ => // Don't know what this is...
          logger.warn(s"Don't know how to interpret $obj when setting indexable types.")
      }
      case None => // No type wrappers...
    }
    // Set the index name if we have one:
    m.get("index") match {
      case Some(str) => name = str.asInstanceOf[String]
      case None => // Do nothing...
    }
    // Set the backing store name if we have one:
    m.get("backingStore") match {
      case Some(str) => backingStore = str.asInstanceOf[String]
      case None => // Do nothing...
    }
    // Done:
    this
  }
  /**
   * Create an index on the elastic search cluster described by the client object.
   * @param nShards The number of shards.
   * @param nReplicas The number of replicas.
   * @return True if the the action worked.
   */
  def createIndex(nShards:Int = 5, nReplicas:Int = 1) = {
    // Logging:
    logger.info(s"Creating index... name = $name ($backingStore), shards = $nShards, replicas = $nReplicas.")
    // Create a recovery context:
    val continue = withRecovery(delay = 1000, recoveryOutput = emitRecoveryMsg) {
      // Execute the DSL and await the response:
      Some(client.execute {
        create index backingStore shards nShards replicas nReplicas
      }.await(timeoutValue))
    }{
      // Attempt connection recovery:
      connectionFailureRecovery
    } match {
      case Some(r) => r.isAcknowledged
      case None => false
    }
    // Create the alias if the last thing was successful:
    if (continue) {
      // Create recovery context:
      withRecovery(delay = 1000, recoveryOutput = emitRecoveryMsg) {
        // Execute alias creation DSL:
        Some(client.execute(add alias name on backingStore).await(timeoutValue))
      }{
        // Attempt connection recovery:
        connectionFailureRecovery
      } match {
        case Some(r) => r.isAcknowledged
        case None => false
      }
    } else false
    // Done...
  }
  /**
   * Create an index on the elastic search cluster described by the client object.
   * @param nShards The number of shards.
   * @param nReplicas The number of replicas.
   * @return True if the the action worked.
   */
  def createTypedIndex(nShards:Int = 5, nReplicas:Int = 1) = {
    // Logging:
    logger.info(s"Creating typed index... name = $name ($backingStore), shards = $nShards, replicas = $nReplicas.")
    // Create a recovery context:
    val continue = withRecovery(delay = 1000, recoveryOutput = emitRecoveryMsg) {
      // Execute the DSL and await the response:
      Some(client.execute {
        create index backingStore shards nShards replicas nReplicas mappings (
          indexableTypes.map(_.mapping).toSeq: _* )
      }.await(timeoutValue))
    }{
      // Attempt connection recovery:
      connectionFailureRecovery
    } match {
      case Some(r) => r.isAcknowledged
      case None => false
    }
    // Create the alias if the last thing was successful:
    if (continue) {
      // Create recovery context:
      withRecovery(delay = 1000, recoveryOutput = emitRecoveryMsg) {
        // Execute alias creation DSL:
        Some(client.execute(add alias name on backingStore).await(timeoutValue))
      }{
        // Attempt connection recovery:
        connectionFailureRecovery
      } match {
        case Some(r) => r.isAcknowledged
        case None => false
      }
    } else false
    // Done...
  }
  /**
   * Determine if the index exists.
   * @return True if the index exists.
   */
  def indexExists(): Boolean = {
    // Logging:
    logger.info(s"Checking index existence... name = $name ($backingStore).")
    // Place some executable code inside a recovery loop:
    withRecovery(delay = 1000, recoveryOutput = emitRecoveryMsg) {
      // Execute DSL and await response:
      Some(client.execute(index exists backingStore).await(timeoutValue))
    }{
      // Attempt connection recovery:
      connectionFailureRecovery
    } match {
      case Some(r) => r.isExists
      case None => false
    }
  }
  /**
   * Delete an index on the elastic search cluster described by the client object.
   * @return True if the the action worked.
   */
  def deleteIndex(): Boolean = {
    // Logging:
    logger.info(s"Deleting index... name = $backingStore.")
    // Place some executable code inside a recovery loop:
    withRecovery(delay = 1000, recoveryOutput = emitRecoveryMsg) {
      // Execute the DSL and await the response:
      Some(client.execute(delete index backingStore).await)
    }{
      // Attempt connection recovery:
      connectionFailureRecovery
    } match {
      case Some(r) => r.isAcknowledged
      case None => false
    }
  }
  /**
   * Insert the indicated document into the indicated index.
   * @param ds The document wrapper.
   * @return A tuple containing the status, creation status, and the result ID.
   */
  def indexThis(ds:TypedDocumentSource): (Boolean,Boolean,String) = {
    // Logging:
    logger.debug(s"Shoving document $ds into index name = $name, type = ${ds.documentType}.")
    // Create an insertion expression based on the ID value:
    val indexExpression = ds.key match {
      case Some(s) =>
        // Index using our own computed id:
        index into s"$name/${ds.documentType}" doc ds id s
      case None =>
        // Index using default ID:
        index into s"$name/${ds.documentType}" doc ds
    }
    // Create a recovery context:
    withRecovery(delay = 1000, recoveryOutput = emitRecoveryMsg) {
      // Execute the DSL and await the response:
      Some(client.execute(indexExpression).await(timeoutValue))
    }{
      // Attempt connection recovery:
      connectionFailureRecovery
    } match {
      case Some(r) => (true,r.isCreated,r.getId)
      case None => (false,false,"")
    }
  }
  /**
   * Insert the indicated document(s) into the indicated index.
   * @param ds The document set to index.
   * @return true if the operation succeeded.
   */
  def indexThese(ds:Iterable[TypedDocumentSource]): Boolean = {
    // Logging:
    logger.debug(s"Shoving documents into index name = $name.")
    // Count the items:
    ds.count(items => true) match {
      case 0 => true // Nothing to index...
      case _ =>
        // Create a recovery context:
        withRecovery(delay = 1000, recoveryOutput = emitRecoveryMsg) {
          // Execute the DSL and await the response:
          Some(
            client.execute(
              bulk {
                for { d <- ds } yield d.key match {
                  case Some(s) =>
                    // Index using our own computed id:
                    index into s"$name/${d.documentType}" doc d id s
                  case None =>
                    // Index using default ID:
                    index into s"$name/${d.documentType}" doc d
                }
              }
            ).await(timeoutValue)
          )
        }{
          // Attempt connection recovery:
          connectionFailureRecovery
        } match {
          case Some(r) => !r.hasFailures
          case None => false
        }
    }
  }
  /**
   * Delete this.
   * @param d The Document to remove
   * @return true if the operation succeeded.
   */
  def deleteThis(d:TypedDocumentSource): Boolean = {
    logger.debug(s"Deleting document from index name = $name.")
    withRecovery(delay = 1000, recoveryOutput = emitRecoveryMsg) {
      Some(client.execute(delete id d.key.get from name / d.documentType).await(timeoutValue))
    }{
      connectionFailureRecovery
    } match {
      case Some(r) => r.isFound
      case None => false
    }
  }
  /**
   * Delete a bunch of items from the index.
   * @param ds The document set to remove.
   * @return true if the operation succeeded.
   */
  def deleteThese(ds:Iterable[TypedDocumentSource]): Boolean = {
    logger.debug(s"Deleting documents from index name = $name.")
    ds.isEmpty match {
      case true => false
      case false =>
        withRecovery(delay = 1000, recoveryOutput = emitRecoveryMsg) {
          Some(client.execute(
            bulk { for { d <- ds } yield delete id d.key.get from name / d.documentType }
          ).await(timeoutValue))
        }{
          // Attempt connection recovery:
          connectionFailureRecovery
        } match {
          case Some(r) => !r.hasFailures
          case None => false
        }
    }
  }
  /**
   * Apply a function over a scoll
   * @param size The group size
   * @param sd The search query
   * @param dur (default=1m) The scroll keep-alive duration.
   * @param f The method to apply to each element
   */
  def scroll(size: Int, sd: SearchDefinition, dur: String = "1m")(f: Iterable[TypedDocumentSource] => Unit): Unit = {
    var resp = client.execute { sd scroll dur limit size }.await(timeoutValue)
    while (resp.getHits.getHits.size > 0) {
      f(resp.getHits.getHits.flatMap(x => indexableTypes.find(_.typeName == x.getType).get.translate(x)).map(_.entity))
      resp = client.execute { searchScroll(resp.getScrollId).keepAlive(dur) }.await(timeoutValue)
    }
  }
  /**
   * Get the source document using the indicated id and type.
   * @param item The ID of the item.  Not called "id" because of Elastic4s DSL.
   * @param itemType The type of the item.  This is needed for the "table" name to search in.
   * @return An option containing the object if successful.
   */
  def getDocumentWithTranslation(item:String,itemType:String) = {
    // Logging:
    logger.debug(s"Grabbing $item from index name = $name/$itemType.")
    // Create a recovery context:
    withRecovery(delay = 1000, recoveryOutput = emitRecoveryMsg) {
      // Execute the DSL and await the response:
      Some(client.execute { get id item from s"$name/$itemType" }.await(timeoutValue))
    }{
      // Attempt connection recovery:
      connectionFailureRecovery
    } match {
      case Some(r) =>
        if (r.isExists) {
          indexableTypes.find(_.typeName == itemType) match {
            case Some(m) => m.convertJson(r.sourceAsString)
            case None =>
              logger.warn(s"No mapper available for $itemType")
              None
          }
        } else None
      case None => None
    }
  }
  /**
   * Pull a series of documents using multiget semantics.
   * @param items The IDs of the items to get.
   * @param itemType The type of items to map to.
   * @return An option containing an array of (id,Option[Any]).
   */
  def getDocumentsWithTranslation(items:Array[String],itemType:String) = {
    // Logging:
    logger.debug(s"Grabbing ${items.length} items from index name = $name/$itemType.")
    // Check array length:
    items.length match {
      case 0 => None // Nothing to get...
      case _ =>
        // Create a recovery context:
        withRecovery(delay = 1000, recoveryOutput = emitRecoveryMsg) {
          // Execute the DSL and await the response:
          Some(client.execute {
            multiget { for { i <- items } yield get id s"$i" from s"$name/$itemType" }
          }.await(timeoutValue))
        }{
          // Attempt connection recovery:
          connectionFailureRecovery
        } match {
          case Some(r) =>
            if (r.responses.isEmpty) {
              // Got nothing back:
              logger.warn(s"Got NOTHING from index name = $name/$itemType.")
              None
            } else {
              Some(r.responses.map(mr =>
                if (mr.isFailed)
                  (mr.getId,None)
                else {
                  indexableTypes.find(_.typeName == itemType) match {
                    case Some(m) => Try(mr.getResponse.getSourceAsString) match {
                      case Success(src) =>
                        if (src == null || src.isEmpty)
                          (mr.getId,None)
                        else
                          (mr.getId,m.convertJson(src))
                      case Failure(_) =>
                        (mr.getId,None)
                    }
                    case None =>
                      logger.warn(s"No mapper available for $itemType")
                      (mr.getId,None)
                  }
                }))
            }
          case None => None
        }
    }
  }
  /**
   * Execute a search on the indicated index.
   * @param f A function that takes the index name and returns a search definition.
   * @return An array of results, which might be empty.
   */
  def search(f:(String) => SearchDefinition): Array[SearchHit] = {
    // Logging:
    logger.debug(s"Performing search in index name = $name.")
    // Create the query object:
    val q = f(name)
    // Debug: display the builder:
    logger.debug(s"builder = ${q._builder}")
    // Place some executable code inside a recovery loop:
    withRecovery(delay = 1000, recoveryOutput = emitRecoveryMsg) {
      // Execute the DSL and await the response:
      Some(client.execute(q).await(timeoutValue))
    }{
      // Attempt connection recovery:
      connectionFailureRecovery
    } match {
      case Some(r) => r.getHits.getTotalHits match {
        case 0 => Array[SearchHit]()
        case _ => r.getHits.getHits
      }
      case None => Array[SearchHit]()
    }
  }
  /**
   * Execute a search definition using the wrapped ES client.
   * @param sd The search definition to execute.
   * @param throwOnRetryFail Should we throw on retry fail?
   * @return An option wrapping the search response, or None.
   */
  def rawSearch(sd:SearchDefinition, throwOnRetryFail:Boolean = false) = {
    // Place some executable code inside a recovery loop:
    withRecovery(delay = 1000, rethrow = throwOnRetryFail, recoveryOutput = emitRecoveryMsg) {
      // Execute the DSL and await the response:
      Some(client.execute(sd).await(timeoutValue))
    }{
      // Attempt connection recovery:
      connectionFailureRecovery
    }
  }
  /**
   * Execute a get definition using the wrapped ES client.
   * @param gd The get definition to execute.
   * @param throwOnRetryFail Should we throw on retry fail?
   * @return An option wrapping the search response, or None.
   */
  def rawGet(gd:GetDefinition, throwOnRetryFail:Boolean = false) = {
    // Place some executable code inside a recovery loop:
    withRecovery(delay = 1000, rethrow = throwOnRetryFail, recoveryOutput = emitRecoveryMsg) {
      // Execute the DSL and await the response:
      Some(client.execute(gd).await(timeoutValue))
    }{
      // Attempt connection recovery:
      connectionFailureRecovery
    }
  }
  /**
   * Execute a search on the indicated index and perform result translation to domain
   * objects.
   * @param f A function that takes the index name and returns a search definition.
   * @return An array of results, which might be empty.
   */
  def searchWithTranslation(f:(String) => SearchDefinition) = {
    // Attempt translation of the indicated hit:
    def translate(sh:SearchHit) = {
      // This is the return:
      var result:Option[Result] = None
      // Loop through until we get something...
      indexableTypes.find(t => {
        result = Try(t.translate(sh)).getOrElse(None)
        result.isDefined
      })
      // Done...
      result
    }
    // Logging:
    logger.debug(s"Performing translated search in index name = $name.")
    // Create the query object:
    val q = f(name)
    // Debug: display the builder:
    logger.debug(s"builder = ${q._builder}")
    // Place some executable code inside a recovery loop:
    withRecovery(delay = 1000, recoveryOutput = emitRecoveryMsg) {
      // Execute the DSL and await the response:
      Some(client.execute(q).await(timeoutValue))
    }{
      // Attempt connection recovery:
      connectionFailureRecovery
    } match {
      case Some(r) => r.getHits.getTotalHits match {
        case 0 => Array[Result]()
        case _ => r.getHits.getHits.map(translate).filter(_.isDefined).map(_.get)
      }
      case None => Array[Result]()
    }
  }
  /**
   * Perform item counts using standard query mechanics which can allow filtering.
   * @param f A function that takes the index name and returns a search definition.
   * @return The total number of hits.
   */
  def complexCount(f:(String) => SearchDefinition) = {
    // Logging:
    logger.debug(s"Performing complex count in index name = $name.")
    // Create the query object:
    val q = f(name)
    // Debug: display the builder:
    logger.debug(s"builder = ${q._builder}")
    // Place some executable code inside a recovery loop:
    withRecovery(delay = 1000, recoveryOutput = emitRecoveryMsg) {
      // Execute the DSL and await the response:
      Some(client.execute(q).await(timeoutValue))
    }{
      // Attempt connection recovery:
      connectionFailureRecovery
    } match {
      case Some(r) => r.getHits.getTotalHits
      case None => 0L
    }
  }
  /**
   * Perform updates on the indeicated document.
   * @param d The document to update.
   * @param df The fields to update in the document.
   * @return true if the update "took."
   */
  def updateThis(d:TypedDocumentSource,df:Map[String,Any]) : Boolean = {
    logger.debug(s"Performing update of item $id in index name = $name.")
    withRecovery(delay = 1000, recoveryOutput = emitRecoveryMsg) {
      df.isEmpty match {
        case true => None
        case false =>
          Some(client.execute(update(d.key.get).in(s"${name}/${d.documentType}").doc(df).retryOnConflict(3)).await(timeoutValue))
      }
    }{
      connectionFailureRecovery
    } match {
      case Some(r) => !r.isCreated
      case None => false
    }
  }
  /**
   * Perform updates on the indicated document(s).
   * @param updates The collection of document to update fields.
   * @return true if the updates "took."
   */
  def updateThese(updates:Iterable[(TypedDocumentSource,Map[String,Any])]) : (Boolean, List[Map[String,Any]]) = {
    logger.debug(s"Performing updates of items in index name = $name.")
    withRecovery(delay = 1000, recoveryOutput = emitRecoveryMsg) {
      Some(
        client.execute(
          bulk {
            updates.filter(_._2.nonEmpty).map(up => update(up._1.key.get).in(s"${name}/${up._1.documentType}").doc(up._2).retryOnConflict(3))
          }
       ).await(timeoutValue)
     )
    }{
      connectionFailureRecovery
    } match {
      case Some(r) => (!r.hasFailures, r.original.getItems.map(x => Map(
        "id" -> x.getId,
        "failed" -> x.isFailed,
        "status" -> Option(x.getFailure).map(_.getStatus)
      )).toList)
      case None => (false, List.empty)
    }
  }
  /**
   * Perform a bulk transaction on the index.
   * @param b The transaction to execute.
   * @return true if everything worked.
   */
  def bulkTransaction(b: => BulkDefinition) = {
    // Logging:
    logger.debug(s"Performing bulk execution in index name = $name.")
    // Place some executable code inside a recovery loop:
    withRecovery(delay = 1000, recoveryOutput = emitRecoveryMsg) {
      // Execute the DSL and await the response:
      Some(client.execute(b).await(timeoutValue))
    }{
      // Attempt connection recovery:
      connectionFailureRecovery
    } match {
      case Some(r) => !r.hasFailures
      case None => false
    }
  }
  /**
   * Perform a count on the index given the indicated query block.
   * @param typ The type in the index.
   * @param qry The query to execute a count for.
   * @return The result of the count.
   */
  def countThings(typ:String = "")(qry: => QueryDefinition) = {
    // Place some executable code inside a recovery loop:
    withRecovery(delay = 1000, recoveryOutput = emitRecoveryMsg) {
      // Execute the DSL and await the response:
      typ match {
        case null | "" =>
          logger.debug(s"Performing count of items in index name = $name.")
          Some(client.execute { count from s"$name" query qry }.await(timeoutValue))
        case s:String =>
          logger.debug(s"Performing count of items of type $s in index name = $name.")
          Some(client.execute { count from s"$name/$s" query qry }.await(timeoutValue))
      }
    }{
      // Attempt connection recovery:
      connectionFailureRecovery
    } match {
      case Some(r) => r.getCount
      case None => 0L
    }
  }
  /**
   * Shut down the client.
   */
  def close() = {
    // Logging:
    logger.info("Closing connection to elastic search.")
    // Do close:
    IndexManager.connectorMap.synchronized {
      IndexManager.connectorMap.find(kvp => kvp._2 == client) match {
        case Some(v) => IndexManager.connectorMap.remove(v._1)
        case None =>
      }
      client.close()
    }
  }
  /**
   * Execute a code block and close the connection after.
   * @param f A function to execute.
   * @tparam T The type that the passed expression produces.
   * @return
   */
  def withConnectionClose[T](f: => T) = try { f } finally { close() }
}

/**
 * Static fields and methods for the IndexManager class.
 * @author rbelcinski@apixio.com
 */
object IndexManager {
  /**
   * The logger.
   */
  val logger = LoggerFactory.getILoggerFactory.getLogger(getClass.getCanonicalName)
  /**
   * Create a copy of the indicated connection manager, minus the client.
   * @param that The manager to copy from.
   * @return The new connection manager.
   */
  def apply(that:IndexManager):IndexManager = {
    // Make return:
    val ret = IndexManager(timeoutValue = that.timeoutValue, name = that.name, backingStore = that.backingStore,
      indexableTypes = that.indexableTypes)
    // Stash connection parameters:
    ret.settings = that.settings
    // Done:
    ret
  }
  /**
   * Create an elastic search connector using the indicated configuration.
   * @note This part is broken off from the instance class because client connections can
   * be shared between index managers.
   * @param cfg The configuration to use.
   * @param noStash Don't use the connection stash?
   * @param overwrite Overwrite the value of the connection stash?
   * @return The connector object.
   */
  def configureConnector(cfg:java.util.Map[String,Object], noStash:Boolean = false, overwrite:Boolean = false) = {
    // Logging:
    logger.info(s"Creating elastic search connector...")
    // Convert to scala map:
    val m = cfg.toMap
    // Get the cluster name:
    val cn = m.get("cluster") match {
      case Some(str) => str.toString
      case None => "Local"
    }
    // Form connection URI:
    val url = m.get("hosts") match {
      case Some(lh) => s"elasticsearch://$lh"
      case None =>
        // If host list not specified, then look for independent specification of
        // host address and port:
        val h = m.get("address") match {
          case Some(str) => str.toString
          case None => "localhost"
        }
        val p = m.get("port") match {
          case Some(str) => str.toString
          case None => "9300"
        }
        // Form result:
        s"elasticsearch://$h:$p"
    }
    // Form the connection object, or get existing:
    if (noStash) {
      // Make some settings:
      val s = Settings.settingsBuilder().put("cluster.name",cn).build()
      // Make a corresponding URI object:
      val uri = ElasticsearchClientUri(url)
      // Create the client:
      val rem = ElasticClient.remote(s,uri)
      // Done:
      rem
    } else if (overwrite) {
      // Make some settings:
      val s = Settings.settingsBuilder().put("cluster.name",cn).build()
      // Make a corresponding URI object:
      val uri = ElasticsearchClientUri(url)
      // Create the client:
      val rem = ElasticClient.remote(s,uri)
      // Stash it:
      connectorMap.synchronized(connectorMap.put(url,rem))
      // Done:
      rem
    } else {
      connectorMap.synchronized {
        connectorMap.get(url) match {
          case Some(cc) => cc
          case None =>
            // Make some settings:
            val s = Settings.settingsBuilder().put("cluster.name",cn).build()
            // Make a corresponding URI object:
            val uri = ElasticsearchClientUri(url)
            // Create the client:
            val rem = ElasticClient.remote(s,uri)
            // Stash it:
            connectorMap.put(url,rem)
            // Done:
            rem
        }
      }
    }
  }
  /**
   * Clear the connection map safely.
   */
  def clearConnectionMap() = connectorMap.synchronized(connectorMap.clear())
  /**
   * This is used to stash ElasticClient objects.
   */
  val connectorMap = new mutable.HashMap[String,ElasticClient]
  /**
   * Catalog all known indices and aliases.
   * @note This will only return those indices for which there are aliases defined.
   * @param cfg The connection properties.
   * @return An option containing index names and their aliases, or None.
   */
  def catalog(cfg:java.util.Map[String,Object]) = {
    // Make a connector:
    val cn = configureConnector(cfg, noStash = true)
    // Do stuff:
    try {
      // Get all aliases:
      val c = cn.execute(get alias "*" on "*").await.getAliases
      // Were there entries?
      c.size() match {
        case 0 => None
        case _ =>
          // Get all keys (indices):
          val keys = c.keysIt()
          // Make a collection of (index,alias) pairs:
          val items = keys.flatMap(k => c.get(k).map(i => (k,i.alias())))
          // Done:
          Some(items.toList)
      }
    } finally {
      // Close the connection no matter what:
      cn.close()
    }
  }
  /**
   * Get the cluster health.
   */
  def clusterHealth(client: ElasticClient) =
    client.execute { get cluster health }.await(Duration(5, unit = TimeUnit.SECONDS)).getStatus
}
