package com.apixio.app.documentsearch

import com.apixio.scala.dw.ApxServices
import com.apixio.scala.logging.ApixioLoggable
import org.sqlite.{SQLiteConfig, SQLiteOpenMode}
import scalikejdbc._

import scala.util.{Failure, Success, Try}


class DocumentSearchManager extends ApixioLoggable {
  setupLog(this.getClass.getCanonicalName)
  val dbDirectory = ApxServices.configuration.getApplication().get("encryptedDatabasePath").getOrElse("db")

  // Map of versions to parameters to initialize database. currently unused.
  //  val cipherVersions = Map(
  //    "v1" -> Map(
  //      "legacy" -> 1,
  //      "legacy_page_size" -> 1024,
  //      "kdf_iter" -> 4000,
  //      "fast_kdf_iter" -> 2,
  //      "hmac_use" -> 0,
  //      "kdf_algorithm" -> 0,
  //      "hmac_algorithm" -> 0
  //    ),
  //    "v2" -> Map(
  //      "legacy" -> 2,
  //      "legacy_page_size" -> 1024,
  //      "kdf_iter" -> 4000,
  //      "fast_kdf_iter" -> 2,
  //      "hmac_use" -> 1,
  //      "hmac_pgno" -> 1,
  //      "hmac_salt_mask" -> 0x3a,
  //      "kdf_algorithm" -> 0,
  //      "hmac_algorithm" -> 0
  //    ),
  //    "v3" -> Map(
  //      "legacy" -> 3,
  //      "legacy_page_size" -> 1024,
  //      "kdf_iter" -> 64000,
  //      "fast_kdf_iter" -> 2,
  //      "hmac_use" -> 1,
  //      "hmac_pgno" -> 1,
  //      "hmac_salt_mask" -> 0x3a,
  //      "kdf_algorithm" -> 0,
  //      "hmac_algorithm" -> 0
  //    ),
  //    "v4" -> Map(
  //      "legacy" -> 4,
  //      "legacy_page_size" -> 4096,
  //      "kdf_iter" -> 256000,
  //      "fast_kdf_iter" -> 2,
  //      "hmac_use" -> 1,
  //      "hmac_pgno" -> 1,
  //      "hmac_salt_mask" -> 0x3a,
  //      "kdf_algorithm" -> 2,
  //      "hmac_algorithm" -> 2,
  //      "plaintext_header_size" -> 0
  //    )
  //  )

  def decryptDatabase(implicit session: DBSession, key: String) = {
    // This is a separate way to initialize the database if not using URI method
    //    sql"SELECT wxsqlite3_config('cipher', 'sqlcipher');".execute().apply()
    //    cipherVersions("v4").keys.foreach(key => {
    //      val value = cipherVersions("v4")(key)
    //      sql"""SELECT wxsqlite3_config('sqlcipher', $key, $value);""".execute().apply()
    //    })
    SQL(s"PRAGMA key='$key'").execute().apply()
  }

  def getPatientDB(patient:String) = {
    Class.forName("org.sqlite.JDBC").newInstance()
    val conf = new SQLiteConfig()
    conf.setOpenMode(SQLiteOpenMode.NOMUTEX);
    val dbUrl = s"jdbc:sqlite:file:$dbDirectory/$patient.db?cipher=sqlcipher&legacy=4"
    conf.createConnection(dbUrl)
  }

  def searchPatientSnippets(patient: String, termList: String) = {
    Try {
      getSnippetsForTermList(patient,termList)
    } match {
      case Success(snippets) => {
        snippets.groupBy(_("id")).mapValues(_.groupBy(_("page")).mapValues(_.map(_("snippets").toString.replaceAll("[^\\x00-\\x7F]", ""))))
      }
      case Failure(e) => {
        error(s"${e.getMessage} - ${e.printStackTrace()}")
        throw e
      }
    }
  }

  def searchPatientOverlays(patient: String, termList: String) = {
    Try {
      getOverlaysForTermList(patient,termList)
    } match {
      case Success(overlays) => {
        overlays.groupBy(_("id")).mapValues(_.groupBy(_("page")))
      }
      case Failure(e) => {
        error(s"${e.getMessage} - ${e.printStackTrace()}")
        throw e
      }
    }
  }

  def getGroupedTermValues(termList: String) = {
    termList.split(',').flatMap(term => {
      val termParts = term.split('|')
      val searchTerm = termParts(0)
      termParts.length match {
        case 1 => {
          List(Map("term" -> searchTerm, "filter" -> None))
        }
        case 2 => {
          val id = s""""${termParts(1)}""""
          List(Map("term" -> searchTerm, "filter" -> (id)))
        }
        case 3 => {
          val id = s""""${termParts(1)}""""
          val page = termParts(2)
          val pageString = s""""${termParts(2)}""""
          List(Map("term" -> searchTerm, "filter" -> (id, page)),Map("term" -> searchTerm, "filter" -> (id, pageString)))
        }
      }
    }).groupBy(_("term")).mapValues(_.map(_("filter")).toList)
  }

  def getSnippetsForTermList(patient: String, termList: String) = {
    val prefix = "<span class=\"highlight\">"
    val suffix = "</span>"
    using(DB(getPatientDB(patient))) { db =>
      db localTx {
        implicit session =>
          decryptDatabase(implicitly, patient)
          getGroupedTermValues(termList).flatMap {case (term, filters) => {
            val wildcardTerm = s""""$term*""""
            val sql = filters(0) match {
              case None => {
                sql"""select id, page, snippet(document_text,2,$prefix,$suffix,'...',10) as snippets
                  from document_text where (document_text MATCH $wildcardTerm)"""
              }
              case filter: String => {
                val filterString = SQLSyntax.createUnsafely(filters.mkString(","))
                sql"""select id, page, snippet(document_text,2,$prefix,$suffix,'...',10) as snippets
                  from document_text where (document_text MATCH $wildcardTerm) and id IN ($filterString)"""
              }
              case filter: Tuple2[String, Any] => {
                val filterString = SQLSyntax.createUnsafely(filters.mkString(","))
                sql"""select id, page, snippet(document_text,2,$prefix,$suffix,'...',10) as snippets
                  from document_text where (document_text MATCH $wildcardTerm) and (id, page) IN (VALUES $filterString)"""
              }
            }
            sql.map(_.toMap)
              .list
              .apply()
          }
        }
      }
    }
  }

  def getOverlaysForTermList(patient: String, termList: String) = {
    using(DB(getPatientDB(patient))) { db =>
      db localTx {
        implicit session => {
          decryptDatabase(implicitly, patient)
          getGroupedTermValues(termList).flatMap {case (term, filters) => {
            val overlayQuery = getOverlayQuery(term.toString)
            val sql = filters(0) match {
              case None => {
                sql"""$overlayQuery"""
              }
              case filter: String => {
                val filterString = SQLSyntax.createUnsafely(filters.mkString(","))
                sql"""$overlayQuery and d1.id IN ($filterString)"""
              }
              case filter: Tuple2[String,Any] => {
                val filterString = SQLSyntax.createUnsafely(filters.mkString(","))
                sql"""$overlayQuery and (d1.id, d1.page) IN (VALUES $filterString)"""
              }
            }
            sql.map(_.toMap)
              .list
              .apply()
          }
        }
      }}
    }
  }

  def getOverlayQuery(term: String) = {
    val termParts = term.split(" ")
    val value = (1 to termParts.length).map(i => s"d$i.value").mkString(" || ' ' || ")
    val minX = if (termParts.length == 1) "d1.left" else s"min(${(1 to termParts.length).map(i => s"d$i.left").mkString(",")})"
    val maxX = s"max(${(1 to termParts.length).map(i => s"d$i.left + d$i.width").mkString(", ")})"
    val minY = if (termParts.length == 1) "d1.top" else s"min(${(1 to termParts.length).map(i => s"d$i.top").mkString(",")})"
    val maxY = s"max(${(1 to termParts.length).map(i => s"d$i.top + d$i.height").mkString(", ")})"
    val width = if (termParts.length == 1) "d1.width" else s"$maxX - $minX"
    val height = if (termParts.length == 1) "d1.height" else s"$maxY - $minY"
    val joins = termParts.length > 1 match {
      case false => ""
      case true => (2 to termParts.length).map(i =>
        s"""join document_overlay d$i on d$i.id = d${i - 1}.id and
         d$i.page = d${i - 1}.page and d$i.item = d${i - 1}.item + 1""").mkString(" ")
    }
    val where = (1 to termParts.length).map(i => {
      // Replace any apostrophes with two apostrophes to escape them
      var termPartValue = termParts(i-1).replace("'","''")
      // Make the last part of the query a wildcard
      if (i == termParts.length) {
        termPartValue += "%%"
      }
      s"d$i.value like '$termPartValue'"
    }).mkString(" and ")

    val query = s"""select
        $value as value,
        d1.id as id,
        d1.page as page,
        $minX as left,
        $minY as top,
        $width as width,
        $height as height
        from document_overlay d1
        $joins
        where $where"""
    SQLSyntax.createUnsafely(query)
  }

  //  Previous attempt at utilizing a connection pool.
  //  var loadedPatients = mutable.Map[String,Any]()
  //
  //  // TODO: Securely retrieve patient DB if it exists, reusing any connections
  //  def loadPatientDB(patient:String) = {
  //    loadedPatients.getOrElseUpdate(patient, {
  //      // TODO: Check Completeness Database for patient - if missing or needs update, queue update
  //      //      using(DB(ConnectionPool.borrow())) { db =>
  //      //        db.localTx {
  //      //          implicit session =>
  //      //            val sql =
  //      //              sql"""SELECT * from patients where patient_id = $patient"""
  //      //            sql.map(_.toMap)
  //      //              .list
  //      //              .apply()
  //      //        }
  //      //      }
  //
  //      // Add this named database to the connection pool
  //      Class.forName("org.sqlite.JDBC").newInstance()
  //      val dbUrl = s"jdbc:sqlite:$dbDirectory/$patient.db"
  ////      val settings = new ConnectionPoolSettings(initialSize = 1, maxSize = 1, connectionTimeoutMillis = 30000)
  ////      ConnectionPool.add(patient, dbUrl, null, null, settings)
  //
  //      val conf = new SQLiteConfig()
  ////      conf.setReadOnly(true)
  //      conf.setOpenMode(SQLiteOpenMode.NOMUTEX)
  //      conf.setSharedCache(true)
  ////      conf.setJournalMode(SQLiteConfig.JournalMode.)
  ////      conf.setTempStore(SQLiteConfig.TempStore.MEMORY)
  ////      conf.setSynchronous(SQLiteConfig.SynchronousMode.OFF)
  //      val source = new SQLiteDataSource(conf)
  //      source.setUrl(dbUrl)
  ////      source.setReadOnly(true)
  //      val dataSourceConnectionPool = new DataSourceConnectionPool(source)
  //      Try {
  //        ConnectionPool.add(patient, dataSourceConnectionPool)
  //      } match {
  //        case Success(overlays) => {
  //          println(s"Added $patient to Connection Pool")
  //        }
  //        case Failure(e) => {
  //          error(s"Failed to add $patient to Connection Pool: ${e.getMessage} - ${e.printStackTrace()}")
  //          throw e
  //        }
  //      }
  //// Test Code to brute force detect sqlcipher encryption version
  ////      cipherVersions.keys.foreach(version => {
  ////        Try {
  ////          using(DB(ConnectionPool(patient).borrow())) { db =>
  ////            db.localTx {
  ////              implicit session => {
  ////                sql"SELECT wxsqlite3_config('cipher', 'sqlcipher');".map(_.toMap).first().apply()
  ////                cipherVersions(version).keys.foreach(key => {
  ////                  val value = cipherVersions(version)(key)
  ////                  sql"""SELECT wxsqlite3_config('sqlcipher', $key, $value);""".execute().apply()
  ////                })
  ////                sql"PRAGMA key='1';".map(_.toMap).first().apply()
  ////                sql"SELECT * from document_text limit 1;".map(_.toMap).list().apply()
  ////              }
  ////            }
  ////          }
  ////        } match {
  ////          case Success(results) => {
  ////            println(results)
  ////          }
  ////          case Failure(e) => {
  ////            println(s"Failed to decrypt database with version $version")
  ////          }
  ////        }
  ////      })
  //    })
  //  }
  //
  //  def removePatientFromDB(patient: String) = {
  //    loadedPatients.remove(patient)
  ////    ConnectionPool.close(patient)
  //  }
}