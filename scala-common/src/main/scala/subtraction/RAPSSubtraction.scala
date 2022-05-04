package com.apixio.scala.subtraction

import com.apixio.model.profiler.{Code, MonthMap, EventTypeX}
import com.apixio.model.utility.Conversions.dateTimeOrdering
import com.apixio.scala.apxapi.Project
import com.apixio.scala.dw.ApxServices
import com.apixio.scala.seqstore.SeqStore
import org.joda.time.{DateTime,DateTimeZone}
import scala.collection.JavaConverters._

@deprecated("Legacy: see com.apixio.app.subtraction.RapsManager")
class RAPSSubtraction(proj: Project, pat: String) {
  val indexKey = "source.type"
  val indexKeyValue = "RiskAdjustmentInsuranceClaim"
  val source = "raps"

  def events(): List[SubtractionEvent] =
    process(filter(SeqStore.getSequence(pat, Some(indexKey), Some(indexKeyValue), Some(proj.start.getMillis), Some(proj.end.getMillis), Some(proj.pds))))

  def filter(events: List[EventTypeX]) : List[EventTypeX] = {

    def isValid(e: EventTypeX) : Boolean = {
      val batchMatch = proj.claimsbatches.isEmpty || proj.claimsbatches.contains(e.attributes.getOrElse("$batchId", ""));
      val rangeMatch = e.fact.time.end.compareTo(proj.start) >= 0 && e.fact.time.end.compareTo(proj.end) <= 0
      val versionMatch = RAPSSubtraction.getVersion(e) == RAPSSubtraction.version
      val evidenceTypeMatch = RAPSSubtraction.getEvidenceType(e) == RAPSSubtraction.rapsType
      return batchMatch && rangeMatch && versionMatch && evidenceTypeMatch
    }

    def hasErrors(e: EventTypeX) : Boolean = {
      // Error codes can be a comma separated list of codes, i.e. "408, 409"
      val errors = e.evidence.attributes.get("transactionStatusCode")
        .map(_.split(",").map(_.trim).filter(_.nonEmpty).toList)
        .getOrElse(List.empty).toSet

      val errorsFound = errors.size > 0
      val known: Set[String] = RAPSSubtraction.knownErrors & errors
      val allErrorsAreKnown = known.size == errors.size

      assert(!errorsFound || allErrorsAreKnown, s"Unknown error code ${errors.mkString(",")} for patinent ${pat}")

      return errorsFound
    }
    events
      .filter(isValid)
      .filterNot(hasErrors)
  }

  def clusterId(events: List[EventTypeX]) : String = {
    val ids = events.map(_.source.uri).toSet
    assert(ids.size == 1, s"${events.head.subject.uri} has more than one clusterId for a transactionId")
    ids.head
  }

  def process(events: List[EventTypeX]) : List[SubtractionEvent] = {
    //group transactions
    val transactions: List[(String,SubtractionEvent)] = events.groupBy(e => RAPSSubtraction.getEvidenceSource(e)).toList.map(ebyt => {
      val providerCodes = ebyt._2.filter(_.fact.code.isProviderType).map(_.fact.code).distinct
      assert(providerCodes.size < 2, s"${events.head.subject.uri} has more than one provider")
      val providerType : Code = providerCodes.headOption.getOrElse(null)
      (clusterId(ebyt._2),
       ebyt._2.filter(_.fact.code.isIcd).map(e =>
         RAPSSubtraction.toSubtractionEvent(e, providerType, new MonthMap(proj.start, proj.end), source)
       )
      )
    }).filter(_._2.nonEmpty)
    .map(ebyt => (ebyt._1, ebyt._2.reduce(_ or _))) //TODO: probably will fail for DELETEs if they coincide with an ADD on the same transaction (not sure what to do when/if that happens)
    //group clusters
    /*
    this approach assumes we merge all transactions after a delete
    transactions.groupBy(_._1).flatMap(x => x._2.values.flatten.map(sevents => {
      val ddate = sevents.filterNot(_.add).sortBy(_.transaction).last
      sevents.filter(x => x.transaction > ddate && x.add).reduce(_ or _)
    })).filter(x => x.add && x.ineligible.toLong > 0)
    */
    //going with approach of taking last transaction in cluster
    transactions.groupBy(_._1).map(_._2.map(_._2).sortBy(_.transaction).last).filter(x => x.add && x.ineligible.toLong > 0).toList
  }
}

object RAPSSubtraction {
  val rapsType = "RAPS_RETURN"
  val version = "0.0.1"
  val unknown = "unknown"
  val defaultDate = new DateTime(1900, 1, 1, 0, 0, DateTimeZone.UTC)
  val knownErrors = Set[String]("301", "302", "303", "304", "305", "306", "307", "308", "309", "310", "311", "313", "314", "315", "316", "317", "318", "319", "350", "353", "354", "400", "401", "402", "403", "404", "405", "406", "407", "408", "409", "410", "411", "412", "413", "414", "415", "416", "417", "418", "419", "420", "421", "422", "423", "424", "425", "450", "451", "453", "454", "455", "460", "490", "491", "492", "500", "502")

  def getVersion(event: EventTypeX) = event.evidence.attributes.getOrElse("version", unknown)

  def getEvidenceType(event: EventTypeX) = Option(event.evidence.source).map(_.`type`).getOrElse(unknown)

  def getTransactionDate(event: EventTypeX) =
    event.evidence.attributes.get("transactionDate").map(d => DateTime.parse(d).toDateTime(DateTimeZone.UTC)).getOrElse(defaultDate)

  def getEvidenceSource(event: EventTypeX) = Option(event.evidence.source).map(_.uri).getOrElse(
    List(event.fact.code.key,
         event.fact.time.start.toDateTime(DateTimeZone.UTC).toString,
         event.fact.time.end.toDateTime(DateTimeZone.UTC).toString,
         getTransactionDate(event).toString
    ).mkString(","))

  def toSubtractionEvent(event: EventTypeX, providerType: Code, mm: MonthMap, source: String) : SubtractionEvent = {
    mm.setAll() // TODO playing it safe for now, but MonthMap will need to be removed completely
    SubtractionEvent(event.fact.code,
                     event.fact.time.end,
                     getTransactionDate(event),
                     providerType,
                     event.evidence.attributes.get("transactionType").filter(_ == "DELETE").isEmpty,
                     mm,
                     source,
                     event.evidence.attributes.get("processingDate").map(d => DateTime.parse(d).toDateTime(DateTimeZone.UTC)).getOrElse(defaultDate))
  }
}
