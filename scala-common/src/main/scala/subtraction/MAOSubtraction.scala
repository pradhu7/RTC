package com.apixio.scala.subtraction

import com.apixio.model.profiler.{Code, EventTypeX, MonthMap, TimeRangeTypeX}
import com.apixio.scala.apxapi.Project
import com.apixio.scala.dw.ApxServices
import com.apixio.scala.seqstore.SeqStore
import org.joda.time.DateTime

import scala.collection.JavaConverters._

@deprecated("Legacy")
class MAOSubtraction(project: Project, patient: String) {
  val start: DateTime = project.start
  val end  : DateTime = project.end

  def getEvents: List[EventTypeX] =
    SeqStore.getSequence(patient, Some("source.type"), Some("RiskAdjustmentInsuranceClaim"), Some(start.getMillis), Some(end.getMillis), Some(project.pds))
      .filter(e => Option(e.evidence.source).map(_.`type`).contains(MAOSubtraction.maoType) &&
                   e.fact.time.end.compareTo(start) >= 0 && e.fact.time.end.compareTo(end) <= 0 &&
                   (project.claimsbatches.isEmpty || project.claimsbatches.contains(e.attributes.getOrElse("$batchId", ""))))

  def events(): List[SubtractionEvent] = MAOSubtraction.claimsByMAOs(start, end, getEvents)
}

case class MAOTransaction(start: DateTime, end: DateTime, events: List[EventTypeX]) {
  val date: DateTime = events.map(_.evidence.attributes.get("transactionDate")).distinct match {
    case Some(d)::Nil => DateTime.parse(d)
    case None::Nil => new DateTime(0L)
    case _ => throw new IllegalArgumentException("Events in one MAO transaction must have the same transaction Date")
  }
  val loaded: DateTime = events.flatMap(_.evidence.attributes.get("processingDate"))
    .map(DateTime.parse).sortBy(_.getMillis).lastOption.getOrElse(new DateTime(0))

  val codes: List[SubtractionEvent] = events.filter(e => e.fact.code != null && e.fact.time.end.isBefore(end) && e.fact.time.start.isAfter(start))
    .map(x => SubtractionEvent(x.fact.code, x.fact.time.end, date, null, x.evidence.attributes.get("transactionType").contains("ADD"), new MonthMap(start, end, true), MAOSubtraction.maoType, loaded))

  def eventEncounterTypeSwitch(e: EventTypeX): String = e.evidence.attributes.getOrElse("encounterTypeSwitch", "1")

  // IF ENCOUNTER TYPE SWITCH IS NOT PRESENT, DEFAULT TO 1
  val encounterTypeSwitch: Int = events.map(eventEncounterTypeSwitch).filter(_.nonEmpty).distinct match {
    case _::_::_ => throw new IllegalArgumentException("All in events in 1 MAO transaction must have the same encounter type switch")
    case h::Nil => h.toInt match {
      case e if 1 <= e && e <= 9 => e
      case _ => throw new IllegalArgumentException(s"Invalid value for encounter type switch: $h")
    }
    case _ => 1 // Per Vishnu if no encounter Type Switch is found, set to 1
  }

  val parentId: Option[String] = events.filter(x => Seq("4","5","6").contains(eventEncounterTypeSwitch(x)))
    .flatMap(_.evidence.attributes.get("parentSourceId")).filter(_.nonEmpty).distinct match {
    case Nil => None
    case h::Nil => Option(h)
    case _ => throw new IllegalArgumentException("MAO transaction should not specify multiple parent source Id")
  }

}

case class MAO(start: DateTime, end: DateTime, ICN: String, events: List[EventTypeX]) {
  val transactions: List[MAOTransaction] = events.groupBy(_.evidence.source.uri)
    .mapValues(x => MAOTransaction(start, end, x)).values.toList.sortBy(_.date.getMillis)
  val dos: TimeRangeTypeX = events.map(_.fact.time).distinct match {
    case h::Nil => h
    case _ => throw new IllegalArgumentException("Events in one MAO must have the same date of service.")
  }
  val loadedDate: DateTime = transactions.map(_.loaded).sortBy(_.getMillis).last // should be the same as transaction date for MAO
  val codes: List[SubtractionEvent] = transactions.foldLeft(List[SubtractionEvent]())((c, tx) => tx.encounterTypeSwitch match {
    case 3 | 6 | 9 => tx.codes  // REPLACEMENT
    case 2 | 5 | 8 => List()    // VOID
    case 1 | 4 | 7 => MAOSubtraction.reduce(c ++ tx.codes) // ORIGINAL
    case _ => throw new IllegalArgumentException(s"Invalid value for encounter type switch: ${tx.encounterTypeSwitch}")
  })
  val parentId: Option[String] = transactions.flatMap(_.parentId).filter(_.nonEmpty).distinct match {
      case h::Nil => Some(h)
      case Nil => None
      case _ => throw new IllegalArgumentException("MAO claim should not specify more than 1 parent source Id")
    }

  val children: scala.collection.mutable.ListBuffer[MAO] = scala.collection.mutable.ListBuffer.empty
}

object MAOSubtraction {
  val maoType: String = "EDPS"

  def reduce(c: List[SubtractionEvent]): List[SubtractionEvent] = c.groupBy(_.code)
    .mapValues(_.sortBy(x => x.transaction.getMillis).reduce(_+_)).values.filterNot(_.ineligible.toLong == 0).toList

  def collapseTree(mao: MAO): (Long,List[SubtractionEvent]) = {
    mao.children.map(collapseTree).sortBy(_._1)  // SORT BY LOADED DATE
      .foldLeft((mao.loadedDate.getMillis, mao.codes)) {
        (dc1, dc2) => (math.max(dc1._1, dc2._1), reduce(dc1._2 ++ dc2._2))
      }
  }

  def claimsByMAOs(start: DateTime, end: DateTime, events: List[EventTypeX]): List[SubtractionEvent] = {
    // GROUP EVENTS BY ENCOUNTER ID AND CREATE MAO CLAIMS
    val claims = events.filterNot(x => x.fact.time.end.isBefore(start) || x.fact.time.start.isAfter(end))
      .groupBy(_.source.uri).map(x => x._1 -> MAO(start, end, x._1, x._2))

    // LINK CHILDREN TO PARENT CLAIM
    val roots = claims.values.filter(_.parentId.nonEmpty).flatMap(x => claims.get(x.parentId.get) match {
      case Some(p) =>
        p.children += x
        None
      case None => Some(x) // CANNOT FIND MY PARENT, I'M A ROOT
    }) ++ claims.values.filter(_.parentId.isEmpty)

    // COLLAPSE TREE TO PRODUCE LIST OF SUBTRACTION EVENTS
    reduce(roots.flatMap(x => MAOSubtraction.collapseTree(x)._2 ).toList)
  }

  implicit class ImprovedSubtractionEvent(e: SubtractionEvent) {
    // Assumption: same dos, dosStart and dosEnd
    def +(that: SubtractionEvent): SubtractionEvent = {
      val mm = new MonthMap(that.ineligible.startRaw, that.ineligible.endRaw)
      (this.e.add, that.add) match {
        case (true, false) => mm.load(this.e.ineligible.toLong() & ~this.e.ineligible.toLong)
        case (false, true) => mm.and(that.ineligible) // THIS CASE SHOULD NEVER HAPPEN
        case _ => mm.or(this.e.ineligible); mm.or(that.ineligible)
      }

      that.copy(
        ineligible = mm,
        dos = if (this.e.dos.isBefore(that.dos)) that.dos else this.e.dos,
        transaction = if (this.e.transaction.isBefore(that.transaction)) that.transaction else this.e.transaction,
        loaded = if (this.e.loaded.isBefore(that.loaded)) that.loaded else this.e.loaded
      )
    }
  }
}
