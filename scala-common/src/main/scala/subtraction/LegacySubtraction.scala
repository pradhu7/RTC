package com.apixio.scala.subtraction

import com.apixio.model.profiler.EventTypeX
import com.apixio.scala.apxapi.Project

@deprecated("Legacy")
class LegacySubtraction(proj: Project, pat: String) extends RAPSSubtraction(proj, pat) {
  override val source = "legacy"

  override def filter(events: List[EventTypeX]) : List[EventTypeX] =
    events.filter(e => (proj.claimsbatches.isEmpty || proj.claimsbatches.contains(e.attributes.getOrElse("$batchId", ""))) &&
                       e.fact.time.end.compareTo(proj.start) >= 0 && e.fact.time.end.compareTo(proj.end) <= 0)

  override def clusterId(events: List[EventTypeX]) : String = {
    val ids = events.map(e => RAPSSubtraction.getEvidenceSource(e).split(",").toList.slice(0, 3).mkString(",")).toSet
    assert(ids.size == 1)
    ids.head
  }
}
