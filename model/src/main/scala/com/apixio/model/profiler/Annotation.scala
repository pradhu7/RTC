package com.apixio.model.profiler

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.ScalaObjectMapper
import scala.collection.JavaConverters._
import org.joda.time.{DateTime,DateTimeZone}

case class Annotation(
    @JsonProperty("uuid") uuid: String = "",
    @JsonProperty("user") user: String = "",
    @JsonProperty("org") org: String = "",
    @JsonProperty("project") project: String = "",
    @JsonProperty("confirmed") confirmed: Boolean = false,
    @JsonProperty("code") code: Code = Code(),
    @JsonProperty("dos") dos: DateTime = null,
    @JsonProperty("dosStart") dosStart: DateTime = null,
    @JsonProperty("dosEnd") dosEnd: DateTime = null,
    @JsonProperty("provider") provider: Provider = Provider(),
    @JsonProperty("encounterType") encounterType: String = "",
    @JsonProperty("reviewFlag") reviewFlag: Boolean = false,
    @JsonProperty("comment") comment: String = "",
    @JsonProperty("timestamp") timestamp: DateTime = null,
    @JsonProperty("phase") phase: String = "",
    @JsonProperty("pages") pages: List[Int] = List(),
    @JsonProperty("winner") winner: Boolean = false,
    @JsonProperty("last") last: Boolean = false,
    @JsonProperty("rejectReason") rejectReason: String = "") {

  def asJson()(implicit mapper: ObjectMapper) = mapper.writeValueAsString(this)

  def normalizeDates() : Annotation = {
    copy(dos = Option(dos).map(_.toDateTime(DateTimeZone.UTC)).getOrElse(null),
         dosStart = Option(dosStart).map(_.toDateTime(DateTimeZone.UTC)).getOrElse(null),
         dosEnd = Option(dosEnd).map(_.toDateTime(DateTimeZone.UTC)).getOrElse(null),
         timestamp = Option(timestamp).map(_.toDateTime(DateTimeZone.UTC)).getOrElse(null)
        )
  }
}

object Annotation {
  def apply(e: EventTypeX) : Annotation = e.evidence.attributes.get("presentedTag") match {
    case Some(_) => //this is the old type
      Annotation(e.attributes.getOrElse("annotationUUID", ""),
                 e.evidence.source.uri,
                 e.evidence.attributes.getOrElse("codingOrganization", ""),
                 e.evidence.attributes.getOrElse("project", ""),
                 e.fact.values.getOrElse("result", "") == "accept",
                 e.fact.code,
                 e.fact.time.end.toDateTime(DateTimeZone.UTC),
                 e.fact.time.start.toDateTime(DateTimeZone.UTC),
                 e.fact.time.end.toDateTime(DateTimeZone.UTC),
                 Provider.fromAnnotationEvent(e),
                 e.evidence.attributes.getOrElse("encounterType", ""),
                 e.evidence.attributes.getOrElse("reviewFlag", "false") == "true",
                 e.evidence.attributes.getOrElse("comment", ""),
                 e.evidence.attributes.get("timestamp").map(DateTime.parse(_).toDateTime(DateTimeZone.UTC)).getOrElse(null),
                 Option(e.evidence.attributes.getOrElse("presentedTag","")).filter(_.trim.nonEmpty).getOrElse("code").toLowerCase,
                 e.evidence.attributes.getOrElse("pageNumber","").replace(" ", ",").split(",").map(_.trim).filter(_.nonEmpty).map(_.toInt).toList,
                 false,
                 false,
                 e.fact.values.getOrElse("rejectReason","")
                )
    case None => //this is the new type
      Annotation(e.attributes.getOrElse("annotationUUID", ""),
                 e.evidence.source.uri,
                 e.evidence.attributes.getOrElse("codingOrganization", ""),
                 e.evidence.attributes.getOrElse("project", ""),
                 e.fact.values.getOrElse("result", "") == "accept",
                 e.fact.values.getOrElse("result", "") == "accept" match {
                   case true =>
                     Code(e.evidence.attributes.getOrElse("acceptedCode", ""), e.evidence.attributes.getOrElse("acceptedCodeSystem", ""))
                   case false =>
                     e.fact.code
                 },
                 e.fact.time.end.toDateTime(DateTimeZone.UTC),
                 e.fact.time.start.toDateTime(DateTimeZone.UTC),
                 e.fact.time.end.toDateTime(DateTimeZone.UTC),
                 Provider.fromAnnotationEvent(e),
                 e.evidence.attributes.getOrElse("encounterType", ""),
                 e.evidence.attributes.getOrElse("reviewFlag", "false") == "true",
                 e.evidence.attributes.getOrElse("comment", ""),
                 e.evidence.attributes.get("timestamp").map(DateTime.parse(_).toDateTime(DateTimeZone.UTC)).getOrElse(null),
                 e.evidence.attributes.getOrElse("presentedPhase", ""),
                 e.evidence.attributes.getOrElse("pageNumber","").replace(" ", ",").split(",").map(_.trim).filter(_.nonEmpty).map(_.toInt).toList,
                 false,
                 false,
                 e.fact.values.getOrElse("rejectReason","")
                )
  }

  def fromJson(obj: String)(implicit mapper: ObjectMapper with ScalaObjectMapper) : Annotation = mapper.readValue[Annotation](obj)
}
