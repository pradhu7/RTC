package com.apixio.model.profiler

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonIgnoreProperties, JsonProperty}
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.fasterxml.jackson.module.scala.ScalaObjectMapper
import org.joda.time.LocalDate

import scala.math.Ordered.orderingToOrdered

/**
  * Convert CoverageRange to the following format, during serialization to JSON
  * {
  *   s: "2015-1-1"
  *   e: "2015-2-28"
  * }
  */
class CoverageRangeSerializer extends JsonSerializer[CoverageRange] {
  override def serialize(value: CoverageRange, gen: JsonGenerator, provider: SerializerProvider): Unit = {
    val start : String = Option(value.start) match {
      case Some(start) => s"${start.getYear}-${start.getMonthOfYear}-${start.getDayOfMonth}"
      case _ => ""
    }

    val end : String = Option(value.end) match {
      case Some(end) => s"${end.getYear}-${end.getMonthOfYear}-${end.getDayOfMonth}"
      case _ => ""
    }


    if (start.nonEmpty && end.nonEmpty) {
      gen.writeStartObject
      gen.writeStringField("s", start)
      gen.writeStringField("e", end)
      gen.writeEndObject
    }
  }
}

/**
  * Convert the following format to CoverageRange object, during dserialization from JSON
  * {
  *   s: "2015-1-1"
  *   e: "2015-2-28"
  * }
  */
class CoverageRangeDeserializer extends JsonDeserializer[CoverageRange] {
  override def deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): CoverageRange = {
    val oc = jsonParser.getCodec()
    val node: JsonNode = oc.readTree(jsonParser)
    val startDate: LocalDate = Option(node.get("s")) match {
      case Some(start) => {
        val s = start.asText.split("-")
        new LocalDate(s(0).toInt, s(1).toInt, s(2).toInt)
      }
      case _ => throw new Error("No start date in coverage range")
    }
    val endDate = Option(node.get("e")) match {
      case Some(end) => {
        val e = end.asText.split("-")
        new LocalDate(e(0).toInt, e(1).toInt, e(2).toInt)
      }
      case _ => throw new Error("No end date in coverage range")
    }
    CoverageRange(startDate, endDate)
  }
}

/**
  * Wrapper for Coverage Range
  * @param start - Start Date
  * @param end - End Date
  */
@JsonSerialize(using = classOf[CoverageRangeSerializer])
@JsonDeserialize(using = classOf[CoverageRangeDeserializer])
case class CoverageRange(start: LocalDate, end: LocalDate)

@JsonIgnoreProperties(ignoreUnknown=true)
case class WorkItem(
    @JsonProperty("patient") patient: String = "",
    @JsonProperty("code") code: Code = Code(),
    @JsonProperty("project") project: String = "",
    @JsonProperty("pass") pass: Int = 1,
    @JsonProperty("scores") scores: List[Double] = List(0.0, 0.0, 0.0), //score, dynamic, haf
    @JsonProperty("state") state: String = "",
    @JsonProperty("phase") phase: String = "",
    @JsonProperty("bundle") bundle: String = "",
    @JsonProperty("comment") comment: String = "",
    @JsonProperty("users") users: List[String] = List(),
    @JsonProperty("coverage") coverage: List[CoverageRange] = List(),
    @JsonProperty("metadata") metadata: Map[String, Any] = Map.empty,
    @JsonProperty("isClaimed") isClaimed: Option[Boolean] = None,
    @JsonProperty("isProblem") isProblem: Option[Boolean] = None,
    @JsonProperty("isReportable") isReportable: Option[Boolean] = Some(false),
    @JsonProperty("claimedCodes") claimedCodes: List[Code] = List.empty,
    @JsonProperty("codes") codes: List[Code] = List.empty,
    @JsonProperty("findings") findings: List[Finding] = List()) extends Ordered[WorkItem] {

  def key() : String = List(project, patient, code.key, pass.toString).mkString(",")

  def getScore() : Double = scores(0)
  def getDynamic() : Double = scores(1)
  def getHaf() : Double = scores(2)

  def score(s:Double) : WorkItem = copy(scores = List(s, getDynamic, getHaf))
  def dynamic(s:Double) : WorkItem = copy(scores = List(getScore, s, getHaf))
  def haf(s:Double) : WorkItem = copy(scores = List(getScore, getDynamic, s))

  def pack() : WorkItem = {
    var f = findings.map(_.pack)
    val states = f.map(_.state).distinct.sorted
    val s = if (state == WorkItem.DELETED) state
            else if (states.contains(WorkItem.CLAIMED)) WorkItem.CLAIMED
            else if (states.contains(WorkItem.ACCEPTED)) WorkItem.ACCEPTED
            else if (states.contains(WorkItem.REJECTED)) WorkItem.REJECTED
            else WorkItem.ROUTABLE
    f = f.filterNot(x => s == WorkItem.CLAIMED && !x.isWorked)
    val p = if (phase == "problem") phase else f.flatMap(_.annotations.map(_.phase)).sorted.lastOption match {
      case Some("code") => "qa1"
      case Some("qa1") => "qa2"
      case Some("qa2") => "qa3"
      case Some("qa3") => "complete"
      case _ => "code"
    }
    val u = f.flatMap(_.annotations.map(_.user)).distinct.sorted
    val annotations = f.flatMap(_.annotations).sortBy(_.timestamp.getMillis)
    val meta = Map(
      "lastAnnotated" -> annotations.lastOption.map(_.timestamp),
      "firstAnnotated" -> annotations.headOption.map(_.timestamp),
      "findingCount" -> findings.size
    )
    copy(state = s, phase = p, users = u, findings = f, metadata = Option(metadata).getOrElse(Map.empty) ++ meta)
  }

  @JsonIgnore def isWorked() : Boolean = findings.map(_.annotations.size).sum > 0

  def updateFields(wi: WorkItem, fields: Set[String]): Map[String,Any] = {
    assert(patient == wi.patient && code == wi.code && project == wi.project)
    Map(
      "scores" -> (if (fields.contains("scores") && scores != wi.scores) Some(wi.scores) else None),
      "state" -> (if (fields.contains("state") && state != wi.state) Some(wi.state) else None),
      "phase" -> (if (fields.contains("phase") && phase != wi.phase) Some(wi.phase) else None),
      "bundle" -> (if (fields.contains("bundle") && bundle != wi.bundle) Some(wi.bundle) else None),
      "comment" -> (if (fields.contains("comment") && comment != wi.comment) Some(wi.comment) else None),
      "users" -> (if (fields.contains("users") && users != wi.users) Some(wi.users) else None),
      "isClaimed" -> (if (fields.contains("isClaimed") && isClaimed != wi.isClaimed) wi.isClaimed else None),
      "isProblem" -> (if (fields.contains("isProblem") && isProblem != wi.isProblem) wi.isProblem else None),
      "isReportable" -> (if (fields.contains("isReportable") && isReportable != wi.isReportable) wi.isReportable else None),
      "metadata" -> (if (fields.contains("metadata") && metadata != wi.metadata) Some(wi.metadata) else None),
      "coverage" -> (if (fields.contains("coverage") && coverage != wi.coverage) Some(wi.coverage) else None)
    ).filter(_._2.nonEmpty).mapValues(_.get)
  }

  def asJson()(implicit mapper: ObjectMapper) = mapper.writeValueAsString(this)

  def compare(that: WorkItem) : Int = (scores.sum, key) compare (that.scores.sum, that.key)
}

object WorkItem {
  val DELETED = "deleted"
  val CLAIMED = Finding.CLAIMED
  val ACCEPTED = Finding.ACCEPTED
  val ROUTABLE = Finding.ROUTABLE
  val REJECTED = Finding.REJECTED

  def fromJson(obj: String)(implicit mapper: ObjectMapper with ScalaObjectMapper) : WorkItem = {
    val wi = mapper.readValue[WorkItem](obj)
    wi.copy(findings = wi.findings.map(f => f.normalizeDates))
  }
}
