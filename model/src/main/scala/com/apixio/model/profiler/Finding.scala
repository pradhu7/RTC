package com.apixio.model.profiler

import com.apixio.model.event.{EventType, AttributesType}
import com.apixio.model.utility.Conversions.dateTimeOrdering
import com.fasterxml.jackson.annotation.{JsonIgnore,JsonProperty}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.ScalaObjectMapper
import scala.collection.JavaConverters._
import org.joda.time.{DateTime,DateTimeZone}

case class Finding(
    @JsonProperty("document") document: String = "",
    @JsonProperty("code") code: Code = Code(),
    @JsonProperty("pages") pages: List[Int] = List(),
    @JsonProperty("startDos") startDos: DateTime = null,
    @JsonProperty("endDos") endDos: DateTime = null,
    @JsonProperty("predictedDos") predictedDos: DateTime = null,
    @JsonProperty("score") score: Double = 0.0,
    @JsonProperty("tags") tags: List[String] = List(),
    @JsonProperty("state") state: String = "",
    @JsonProperty("ineligible") ineligible: Long = 0L,
    @JsonProperty("claimed") claimed: DateTime = null,
    @JsonProperty("annotations") annotations: List[Annotation] = List()) {

  def key() : String = List(document, code.key).mkString("_")

  def applyAnnotation(a: Annotation) : Finding =
    copy(annotations = (annotations ++ List(a)).distinct.sortBy(_.timestamp))

  def pack() : Finding = {
    val mm = new MonthMap(startDos, endDos)
    mm.load(ineligible)
    val s = if (mm.all) Finding.CLAIMED
            else if (annotations.isEmpty) Finding.ROUTABLE
            else if (annotations.last.confirmed) Finding.ACCEPTED
            else Finding.REJECTED
    val lastByCoders = annotations.groupBy(_.user).mapValues(_.sortBy(_.timestamp.getMillis).last).toMap
    val annots = annotations match {
      case Nil => annotations
      case annots =>
        val w = annots.sortBy(_.timestamp.getMillis).last
        annots.map(a => a.copy(winner = (a == w), last = (a == lastByCoders(a.user))))
    }
    copy(state = s, annotations = annots)
  }

  def normalizeDates() : Finding = {
    copy(startDos = Option(startDos).map(_.toDateTime(DateTimeZone.UTC)).getOrElse(null),
         endDos = Option(endDos).map(_.toDateTime(DateTimeZone.UTC)).getOrElse(null),
         predictedDos = Option(predictedDos).map(_.toDateTime(DateTimeZone.UTC)).getOrElse(null),
         claimed = Option(claimed).map(_.toDateTime(DateTimeZone.UTC)).getOrElse(null),
         annotations = annotations.map(a => a.normalizeDates)
        )
  }

  @JsonIgnore def isWorked() : Boolean = annotations.size > 0

  def merge(f: Finding) : Finding = {
    assert(key == f.key)
    isWorked match {
      case true => this.copy(ineligible = f.ineligible, claimed = f.claimed).pack
      case false => f.pack
    }
  }

  def asJson()(implicit mapper: ObjectMapper) = mapper.writeValueAsString(this)
}

object Finding {
  val CLAIMED = "claimed"
  val ACCEPTED = "accepted"
  val ROUTABLE = "routable"
  val REJECTED = "rejected"

  private val categories : List[Set[String]] = List(
    Set("v5", "f2f", "plainText"), Set("v5", "f2f", "ocrText"), Set("v5", "plainText"), Set("v5", "ocrText"),
    Set("dict", "f2f", "plainText"), Set("dict", "f2f", "ocrText"), Set("dict", "plainText"), Set("dict", "ocrText"))
  private val extraTags : Set[String] = Set("mention", "lynty")
  private val maxPages = 30

  private def suggestedPages(events: List[EventTypeX]) : List[Int] = {
    val tagmap = events.map(x => (x, Set(x.extractTags:_*) -- extraTags)).groupBy(_._2).toMap
    val pages = categories.filter(tagmap.contains(_)).headOption match {
      case Some(key) => tagmap(key).flatMap(_._1.evidence.attributes.get("pageNumber")).map(_.toInt).distinct.sorted
      case None => events.flatMap(_.evidence.attributes.get("pageNumber")).map(_.toInt).distinct.sorted
    }
    (pages.size > maxPages match {
      case true =>
        var reducedPages : List[Int] = List()
        var usedPages : Set[Int] = Set()
        pages.foreach { p =>
          if (!usedPages.contains(p)) {
            reducedPages ++= List(p)
            usedPages ++= Set(p - 2, p - 1, p, p + 1, p + 2)
          }
        }
        reducedPages.distinct.sorted
      case false => pages
    }).take(maxPages)
  }

  private def extractTags(events: List[EventTypeX]) : List[String] = events.flatMap(_.extractTags).distinct.sorted

  def hcc(code: Code, bundlable: List[EventTypeX], start: DateTime, end: DateTime, annots: List[EventTypeX], ineligible: (Long,DateTime), withLynty: Boolean, pageFilter: List[EventTypeX] => List[Int] = suggestedPages) : Finding = {
    assert(bundlable.map(_.source.uri).toSet.size == 1)
    var finding = Finding(bundlable.head.source.uri,
                          code,
                          pageFilter(bundlable),
                          start.toDateTime(DateTimeZone.UTC),
                          end.toDateTime(DateTimeZone.UTC),
                          bundlable.map(_.fact.time.start).min.toDateTime(DateTimeZone.UTC), //TODO: this the best date to use?
                          0.0,
                          extractTags(bundlable) ++ (if (withLynty) List("lynty") else List()),
                          Finding.ROUTABLE,
                          ineligible._1, ineligible._2)
    finding = annots.map(Annotation(_)).foldLeft(finding) { (f, a) => f.applyAnnotation(a) }
    finding.pack
  }

  def manual(bundlable: List[EventTypeX], start: DateTime, end: DateTime, annots: List[EventTypeX]) : Finding = {
    assert(bundlable.map(_.source.uri).toSet.size == 1)
    var finding = Finding(bundlable.head.source.uri,
                          bundlable.head.fact.code,
                          List(),
                          start,
                          end,
                          bundlable.map(_.fact.time.start).min.toDateTime(DateTimeZone.UTC), //TODO: this the best date to use?
                          0.0,
                          extractTags(bundlable),
                          Finding.ROUTABLE)
    finding = annots.map(Annotation(_)).foldLeft(finding) { (f, a) => f.applyAnnotation(a) }
    finding.pack
  }


  def fromJson(obj: String)(implicit mapper: ObjectMapper with ScalaObjectMapper) : Finding = mapper.readValue[Finding](obj)
}
