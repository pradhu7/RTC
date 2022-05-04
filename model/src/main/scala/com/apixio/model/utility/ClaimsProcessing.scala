package com.apixio.model.utility

import com.apixio.model.profiler.{Code,EventTypeX,MonthMap}
import com.apixio.model.patient.Problem
import com.apixio.model.utility.Conversions.dateTimeOrdering
import scala.collection.JavaConverters._
import org.joda.time.{DateTime,DateTimeZone}

object ClaimsProcessing {
  def lyntyByEvents(start: DateTime, end: DateTime, icdMapping: String, patient: String, events: Seq[EventTypeX]) : List[EventTypeX] = {
    events.filter(x => x.fact.time.end.compareTo(start) < 0 || x.fact.time.end.compareTo(end) > 0)
      .flatMap(e => e.fact.code.toHcc(icdMapping)) //don't want hierarchy for lynty signal...
      .distinct
      .map(x => EventTypeX.makeLynty(x, start, end, patient))
      .toList
  }

  private def reduceLynty(probs: Seq[Problem]) : Boolean =
    !probs.map(_.getMetadata.asScala.filter(x => x._1 == "DELETE_INDICATOR" && x._2 == "true").map(_._2).headOption.getOrElse("false").toBoolean)
      .reduceLeft(_||_)

  def lyntyByProblems(start: DateTime, end: DateTime, icdMapping: String, patient: String, problems: Seq[Problem]) : List[EventTypeX] = {
    clusterProblems(problems.filter(x => x.getMetadata != null && x.getMetadata.get("TRANSACTION_DATE") != null)
        .filter(x => x.getEndDate.compareTo(start) < 0 || x.getEndDate.compareTo(end) > 0))
      .map(x => (x._1, reduceLynty(x._2)))
      .filter(_._2)
      .flatMap(x => x._1._1.toHcc(icdMapping))
      .toList.distinct
      .map(x => EventTypeX.makeLynty(x, start, end, patient))
      .toList
  }

  def claimsByEvents(start: DateTime, end: DateTime, icdMapping: String, patient: String, events: Seq[EventTypeX], andChildren: Boolean = true) : List[EventTypeX] = {
    val now = DateTime.now()
    val editTime = (e: EventTypeX) => (e.attributes.get("editTimestamp").map(x => DateTime.parse(x)).getOrElse(now))
    events.filter(x => x.fact.time.end.compareTo(start) >= 0 && x.fact.time.end.compareTo(end) <= 0)
      .sortBy(x => editTime(x)).reverse
      .flatMap(e => e.fact.code.toHcc(icdMapping, andChildren).map(c => (c -> (editTime(e), new MonthMap(start, end, true)))))
      .toMap
      .map(x => EventTypeX.makeInEligible(x._1, start, end, x._2._1, x._2._2.toLong, patient))
      .toList
  }

  private val knownErrorCodes = Set[String]("301", "302", "303", "304", "305", "306", "307", "308", "309", "310", "311", "313", "314", "315", "316", "317", "318", "319", "350", "353", "354", "400", "401", "402", "403", "404", "405", "406", "407", "408", "409", "410", "411", "412", "413", "414", "415", "416", "417", "418", "419", "420", "421", "422", "423", "424", "425", "450", "451", "453", "454", "455", "460", "490", "491", "492", "500", "502")

  //problems are already filtered, grouped, and sorted
  private def reduceClaims(start: DateTime, end: DateTime, problems: Seq[Problem]) : (DateTime, MonthMap) = {
    val mm = new MonthMap(start, end)
    var ts : DateTime = null
    problems.takeWhile { p =>
      val meta = p.getMetadata.asScala
      //Deletes don't count for claims
      if (!meta.filter(x => x._1 == "DELETE_INDICATOR" && x._2 == "true").map(_._2).headOption.getOrElse("false").toBoolean) {
        ts = p.getLastEditDateTime
        mm.setAll()
      }
      !mm.all
    }
    (ts, mm)
  }

  private def clusterProblems(probs: Seq[Problem]) : Map[(Code,DateTime,DateTime),Seq[Problem]] = {
    probs.groupBy(p => (Code(p.getCode.getCode, p.getCode.getCodingSystemOID), p.getStartDate, p.getEndDate))
      .map(x => (x._1, x._2.groupBy(_.getMetadata.asScala.getOrElse("TRANSACTION_DATE", "")).filter(_._1.nonEmpty).toSeq.sortBy(x => DateTime.parse(x._1)).last._2.sortBy(_.getLastEditDateTime)))
  }

  private def mergeMaps(maps: List[(DateTime,MonthMap)]) : (DateTime, MonthMap) = {
    var mm : MonthMap = null
    var ts : DateTime = null
    maps.sortBy(_._1).takeWhile { m =>
      ts = m._1
      if (mm == null) mm = m._2
      mm.or(m._2)
      !mm.all
    }
    (ts, mm)
  }

  private def hasError(patient: String, prob: Problem): Boolean = {
    val metadata = prob.getMetadata.asScala
    val errorsFound = metadata
      .filter(x => {
        val (key, value) = x
        key.contains("_ERROR")
      })
      .map(x => {
        val (key, errorCode) = x
        errorCode
      }).toSet

    errorsFound.size > 0 match {
      case true => {
        val knownErrors = knownErrorCodes & errorsFound
        assert(errorsFound.size == knownErrors.size, s"Unknown error code ${errorsFound.mkString(",")} for problem ${patient}")
        true
      }
      case _ => false
    }
  }

  def claimsByProblems(start: DateTime, end: DateTime, icdMapping: String, patient: String, problems: Seq[Problem], andChildren: Boolean = true) : List[EventTypeX] = {

    val problemsToKeep = problems
      .filter(x => x.getMetadata != null && x.getMetadata.get("TRANSACTION_DATE") != null)
      .filter(x => x.getEndDate.compareTo(start) >= 0 && x.getEndDate.compareTo(end) <= 0)
      .filterNot(hasError(patient, _))

    clusterProblems(problemsToKeep)
      .map(x => (x._1, reduceClaims(start, end, x._2)))
      .filter(_._2._2.toLong > 0).toList      //the toList is critical here otherwise the map will overwrite the earlier entries
      .flatMap(x => x._1._1.toHcc(icdMapping, andChildren).map(c => (c, x._2))).toList
      .groupBy(_._1)
      .map(x => (x._1, mergeMaps(x._2.map(_._2))))
      .map(x => EventTypeX.makeInEligible(x._1, start, end, x._2._1, x._2._2.toLong, patient))
      .toList
  }
}
