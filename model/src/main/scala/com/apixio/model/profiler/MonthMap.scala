package com.apixio.model.profiler

import org.joda.time.{DateTime,DateTimeZone}

//TODO: This structure only supports 63 months (just over 5 years)
class MonthMap(val startRaw: DateTime, val endRaw: DateTime, allSet: Boolean = false) {
  assert(startRaw.isBefore(endRaw) || startRaw.isEqual(endRaw))

  private val start = startRaw.toDateTime(DateTimeZone.UTC)
  private val end = endRaw.toDateTime(DateTimeZone.UTC)
  private var vector : Long = 0L
  private val completeVector : Long = (1L << (offset(end) + 1)) - 1L

  if (allSet) vector = completeVector

  private def offset(d: DateTime) : Int =
    (d.getYear - start.getYear) * 12 + (d.getMonthOfYear - start.getMonthOfYear)

  def load(mapping: Long) : Unit = vector = mapping
  def toLong() : Long = vector
  def setAll() : Unit = vector = completeVector
  def all() : Boolean = vector == completeVector
  def set(date: DateTime) : Unit = {
    val d = date.toDateTime(DateTimeZone.UTC)
    if ((d.isBefore(end) && d.isAfter(start)) || d == start || d == end) {
      vector |= (1 << offset(d))
    }
  }
  def isSet(date: DateTime) : Boolean = {
    val d = date.toDateTime(DateTimeZone.UTC)
    ((d.isBefore(end) && d.isAfter(start)) || d == start || d == end) && (vector & (1 << offset(d))) > 0
  }
  def or(m: MonthMap) : Unit = {
    assert(m.startRaw.compareTo(startRaw) == 0 && m.endRaw.compareTo(endRaw) == 0)
    vector |= m.toLong
  }
  def and(m: MonthMap) : Unit = {
    assert(m.startRaw.compareTo(startRaw) == 0 && m.endRaw.compareTo(endRaw) == 0)
    vector &= m.toLong
  }
  def xor(m: MonthMap) : Unit = {
    assert(m.startRaw.compareTo(startRaw) == 0 && m.endRaw.compareTo(endRaw) == 0)
    vector ^= m.toLong
  }
}
