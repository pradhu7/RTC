package com.apixio.scala.seqstore

import com.apixio.dao.seqstore.SeqStoreDAO
import com.apixio.dao.seqstore.utility.{Criteria, Range}
import com.apixio.model.profiler.EventTypeX
import com.apixio.model.event.ReferenceType
import com.apixio.scala.dw.ApxServices
import scala.collection.JavaConverters._

@deprecated("SeqStore no longer used. See PatientService")
object SeqStore {
  val Annotation = "Annotation"
  val Narrative = "Narrative"
  val Structured = "Structured"

  def getSequence(patient: String, path: Option[String] = None, value: Option[String] = None, start: Option[Long] = None,
                  end: Option[Long] = None, pds: Option[String] = None, tags: List[String] = List(), tagType : Option[String] = None) : List[EventTypeX] = {
    val ref = new ReferenceType
    ref.setUri(patient)
    ref.setType("patient")

    var builder = new Criteria.TagTypeCriteria.Builder()
    builder = builder.setSubject(ref)
    builder = if (path.nonEmpty) builder.setPath(path.get) else builder
    builder = if (value.nonEmpty) builder.setValue(value.get) else builder
    builder = if (pds.nonEmpty) builder.setOrgId(pds.get) else builder
    //ignoring tags for now

    // default to tagType.all
    builder.setTagType(SeqStoreDAO.TagType.all)
    if(tagType.nonEmpty) {
      if (tagType.get.equals(Annotation)) builder.setTagType(SeqStoreDAO.TagType.Annotation)
      else if (tagType.get.equals(Narrative)) builder.setTagType(SeqStoreDAO.TagType.Inferred)
      else if (tagType.get.equals(Structured)) builder.setTagType(SeqStoreDAO.TagType.NonInferred)
    }

    builder = if (start.nonEmpty || end.nonEmpty) builder.setRange({
      var rangeBuilder = new Range.RangeBuilder()
      rangeBuilder = if (start.nonEmpty) rangeBuilder.setStart(start.get) else rangeBuilder
      rangeBuilder = if (end.nonEmpty) rangeBuilder.setEnd(end.get) else rangeBuilder
      rangeBuilder.build()
    }) else builder

    ApxServices.seqStoreDAO.getIteratorSequence(builder.build()).asScala.flatMap(_.asScala.map(EventTypeX(_))).toList
  }

  def getAnnotationSequence(patient: String, start: Option[Long] = None, end: Option[Long] = None, pds: Option[String] = None, tags: List[String] = List()) : List[EventTypeX] = {
    getSequence(patient, None, None, start, end, pds, tags, Option(Annotation))
  }

  def getNarrativeSequence(patient: String, start: Option[Long] = None, end: Option[Long] = None, pds: Option[String] = None, path: Option[String] = None, value: Option[String] = None, tags: List[String] = List()) : List[EventTypeX] = {
    getSequence(patient, path, value, start, end, pds, tags, Option(Narrative))
        .filter( _.attributes.getOrElse("sourceType", "UNKNOWN") == "NARRATIVE") // Historical table will return all
                                                                                 // annotations, so we have to filter
  }


  def getStructuredSequence(patient: String, start: Option[Long] = None, end: Option[Long] = None, pds: Option[String] = None, path: Option[String] = None, value: Option[String] = None) : List[EventTypeX] = {
    getSequence(patient, path, value, start, end, pds, List(), Option(Structured))
  }
}
