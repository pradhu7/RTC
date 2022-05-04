package com.apixio.scala.subtraction

import com.apixio.model.profiler._
import com.apixio.model.utility.Conversions.dateTimeOrdering
import com.apixio.scala.apxapi.Project
import com.apixio.scala.seqstore.SeqStore
import org.joda.time.{DateTime, DateTimeZone}

@deprecated("Legacy")
class CommercialRiskSubtraction(proj: Project, pat: String) {
  val indexKey = "source.type"
  val indexKeyValue = "FeeForServiceInsuranceClaim"
  val source = "commercial"

  def events(): List[SubtractionEvent] =
    process(filter(SeqStore.getSequence(pat, Some(indexKey), Some(indexKeyValue), Some(proj.start.getMillis), Some(proj.end.getMillis), Some(proj.pds))))

  def filter(events: List[EventTypeX]) : List[EventTypeX] = {
    events.filter(e => (proj.claimsbatches.isEmpty || proj.claimsbatches.contains(e.attributes.getOrElse("$batchId", ""))) &&
                       e.fact.time.end.compareTo(proj.start) >= 0 && e.fact.time.end.compareTo(proj.end) <= 0 &&
                       CommercialRiskSubtraction.getVersion(e) == CommercialRiskSubtraction.version)
  }

  def process(events: List[EventTypeX]) : List[SubtractionEvent] = {
    val cptVersion: String = proj.properties.getOrElse("gen", Map.empty).get("cptversion")
      .map(_.asInstanceOf[String])
      .getOrElse(proj.paymentYear)
    val billTypeVersion: String = proj.properties.getOrElse("gen", Map.empty).get("billtypeversion")
      .map(_.asInstanceOf[String])
      .getOrElse(proj.paymentYear)

    def isValidCPT: (EventTypeX => Boolean) = e => {
      CptModel.isValid(cptVersion, e.fact.code)
    }

    def isValidBillType(providerType: String = ""): (EventTypeX => Boolean) = e => {
      BillTypeModel.isValid(billTypeVersion, e.fact.code)
    }

    //group transactions
    val transactions: List[(String,SubtractionEvent)] = events.groupBy(e => e.evidence.source.uri).toList.flatMap(ebyt => {
      val providerCodes = ebyt._2.filter(_.fact.code.isProviderType).map(_.fact.code).distinct
      assert(providerCodes.size < 2)
      //we must have at least 1 provider type information to know how to handle this claim
      val icds = providerCodes.headOption match {
        // for professional encounters and there exists a valid cpt code, we don't check billtype
        case Some(providerType) if providerType.isProfessional && ebyt._2.exists(isValidCPT) =>
            ebyt._2.filter(_.fact.code.isIcd)
        // for inpatient encounter, we check billtype but does not require valid cpt code
        case Some(providerType) if providerType.isInpatient && ebyt._2.exists(isValidBillType(providerType.code)) =>
            ebyt._2.filter(_.fact.code.isIcd)
        //for outpatient encounters, we make sure we have at least one valid bill type for the type of encounter
        case Some(providerType) if providerType.isOutpatient && ebyt._2.exists(isValidBillType(providerType.code))
          && ebyt._2.exists(e => CptModel.isValid(cptVersion, e.fact.code)) =>
            ebyt._2.filter(_.fact.code.isIcd)
        case _ => List.empty
      }
      val res = icds.map(e => (e.source.uri, CommercialRiskSubtraction.toSubtractionEvent(e, providerCodes.head, new MonthMap(proj.start, proj.end, true), source)))
      //if there exists a single DELETE for this transaction, remove the entire transaction.
      if (res.exists(!_._2.add)) List.empty else res
    })

    //group clusters by code
    transactions.groupBy(t => (t._1, t._2.code)).map(_._2.map(_._2).sortBy(_.transaction).last).filter(x => x.add && x.ineligible.toLong > 0).toList
  }
}

object CommercialRiskSubtraction {
  val version = "0.0.1"
  val unknown = "unknown"

  def getVersion(event: EventTypeX): String = event.evidence.attributes.getOrElse("version", unknown)

  def toSubtractionEvent(event: EventTypeX, providerType: Code, mm: MonthMap, source: String) : SubtractionEvent =
    SubtractionEvent(event.fact.code,
                     event.fact.time.end,
                     DateTime.parse(event.evidence.attributes("transactionDate")).toDateTime(DateTimeZone.UTC),
                     providerType,
                     event.evidence.attributes.get("transactionType").filter(_ == "DELETE").isEmpty,
                     mm,
                     source,
                     DateTime.parse(event.evidence.attributes("processingDate")).toDateTime(DateTimeZone.UTC))
}
