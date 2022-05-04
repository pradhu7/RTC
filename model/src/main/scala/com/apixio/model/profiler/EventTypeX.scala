package com.apixio.model.profiler

import com.apixio.model.event.{AttributesType,FactType,EventType,EvidenceType,ReferenceType,TimeRangeType}
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.ScalaObjectMapper
import org.joda.time.{DateTime,DateTimeZone}
import scala.collection.JavaConverters._

case class TimeRangeTypeX(start: DateTime, end: DateTime)
case class ReferenceTypeX(`type`: String, uri: String)
case class FactTypeX(code: Code, time: TimeRangeTypeX, values: Map[String,String])
case class EvidenceTypeX(attributes: Map[String,String], inferred: Boolean, source: ReferenceTypeX)
case class EventTypeX(attributes: Map[String,String], evidence: EvidenceTypeX, fact: FactTypeX, source: ReferenceTypeX, subject: ReferenceTypeX) {
  @JsonIgnore def isClaim() : Boolean = attributes.get("sourceType").map(_ == "CMS_KNOWN").getOrElse(false)
  @JsonIgnore def isNarrative() : Boolean = attributes.get("sourceType").map(_ == "NARRATIVE").getOrElse(false)
  @JsonIgnore def isAnnotation() : Boolean = attributes.get("sourceType").map(_ == "USER_ANNOTATION").getOrElse(false)
  @JsonIgnore def isLynty() : Boolean = attributes.get("sourceType").map(_ == "LYNTY").getOrElse(false)
  @JsonIgnore def isInEligible() : Boolean = attributes.get("sourceType").map(_ == "INELIGIBLE").getOrElse(false)
  @JsonIgnore def isSkip() : Boolean = isAnnotation && fact.values.get("result").map(_ == "skipped").getOrElse(false)
  @JsonIgnore def isHccBundlable() : Boolean = isNarrative && evidence.attributes.get("pageNumber").nonEmpty && source.`type` == "document" && subject.`type` == "patient"
  @JsonIgnore def isManualBundlable() : Boolean = isNarrative && fact.code.isCodeAll
  def toHcc(v: String) : List[EventTypeX] = fact.code.toHcc(v).map(x => copy(fact = fact.copy(code = x))).toList
  def asJson()(implicit mapper: ObjectMapper) = mapper.writeValueAsString(this)
  def extractTags() : List[String] =
    List(evidence.attributes.get("face2face").find(_ == "true").map(_ => "f2f"),
         evidence.attributes.get("hasMention").find(_ == "YES").map(_ => "mention"),
         attributes.get("bucketType").find(_.startsWith("maxentModel")).map(_ => "v5"),
         attributes.get("bucketType").find(_.startsWith("dictionary")).map(_ => "dict"),
         evidence.attributes.get("extractionType"),
         evidence.attributes.get("tag")
        ).flatten
}

object EventTypeX {
  implicit def attributesType2Map(al: AttributesType) : Map[String,String] =
    Option(al).flatMap(x => Option(x.getAttribute)).map(_.asScala).getOrElse(List()).map(x => x.getName -> x.getValue).toMap
  implicit def referenceType2ReferenceTypeX(r: ReferenceType) : ReferenceTypeX = if (r != null) ReferenceTypeX(r.getType, r.getUri) else null
  implicit def timeRangeType2TimeRangeTypeX(t: TimeRangeType) : TimeRangeTypeX =
    TimeRangeTypeX((new DateTime(t.getStartTime)).toDateTime(DateTimeZone.UTC), (new DateTime(t.getEndTime)).toDateTime(DateTimeZone.UTC))
  implicit def factType2FactTypeX(f: FactType) : FactTypeX = FactTypeX(Code(f.getCode), f.getTime, f.getValues)
  implicit def evidenceType2EvidenceTypeX(e: EvidenceType) : EvidenceTypeX = EvidenceTypeX(e.getAttributes, e.isInferred, e.getSource)
  def apply(e: EventType) : EventTypeX = {
    var ex : EventTypeX = EventTypeX(e.getAttributes, e.getEvidence, e.getFact, e.getSource, e.getSubject)
    //TODO: This fixup is temporary and should be removed post-simplification release
    if (ex.isAnnotation && !ex.evidence.attributes.contains("presentedTag") && ex.evidence.attributes.contains("presentedCode"))
      ex = ex.copy(
        fact = ex.fact.copy(code = Code(ex.evidence.attributes.getOrElse("presentedCode",""), ex.evidence.attributes.getOrElse("presentedCodeSystem",""))),
        evidence = ex.evidence.copy(attributes = ex.evidence.attributes.filter(x => x._1 != "presentedCode" && x._1 != "presentedCodeSystem") ++
          (if (ex.fact.values.getOrElse("result", "") == "accept") Map("acceptedCode" -> ex.fact.code.code, "acceptedCodeSystem" -> ex.fact.code.system) else Map())))
    ex
  }
  def fromJson(obj: String)(implicit mapper: ObjectMapper with ScalaObjectMapper) : EventTypeX = mapper.readValue[EventTypeX](obj)
  def makeLynty(c: Code, s: DateTime, e: DateTime, p: String) : EventTypeX =
    EventTypeX(
      Map("sourceType" -> "LYNTY"),
      EvidenceTypeX(Map(), false, null),
      FactTypeX(c, TimeRangeTypeX(s, e), Map()),
      null,
      ReferenceTypeX("patient", p)
    )
  def makeInEligible(c: Code, s: DateTime, e: DateTime, o: DateTime, m: Long, p: String, srcs: Option[(String,String)] = None) : EventTypeX =
    EventTypeX(
      Map("sourceType" -> "INELIGIBLE"),
      EvidenceTypeX(Map("originTimestamp" -> o.toString, "monthMap" -> m.toString), false, (if (srcs.nonEmpty) ReferenceTypeX(srcs.get._1, srcs.get._2) else null)),
      FactTypeX(c, TimeRangeTypeX(s, e), Map()),
      null,
      ReferenceTypeX("patient", p)
    )
}
