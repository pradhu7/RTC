package com.apixio.model.profiler

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty}
import com.fasterxml.jackson.databind.ObjectMapper
import org.joda.time.DateTime

case class Claim(code: Code, start: Long, end: Long, bitMask: Long) {
  @JsonIgnore lazy val monthMap: MonthMap = {
    val mm = new MonthMap(new DateTime(start), new DateTime(end))
    mm.load(bitMask)
    mm
  }
}

case class ClaimsCache(
    patient: String,
    job: String,
    updated: Long,
    claims: List[Claim]
) {
  def asJson()(implicit mapper: ObjectMapper): String = mapper.writeValueAsString(this)
}