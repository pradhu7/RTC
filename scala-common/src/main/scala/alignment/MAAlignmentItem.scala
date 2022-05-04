package com.apixio.scala.utility.alignment

import com.apixio.model.profiler.{Annotation, Code, EventTypeX}
import com.apixio.scala.subtraction.RapsCluster
import org.joda.time.{DateTime, DateTimeZone, DurationFieldType}

/**
  * Model to repesent an item in the white list for ma alignment
  *
  * @param dosStart start DOS
  * @param dosEnd end DOS
  * @param encounterType encounter type code (01, 02, 10, or 20)
  */
@deprecated("Legacy")
case class MAAlignmentItem(dosStart: DateTime, dosEnd: DateTime, encounterType: Code) {
  /**
    * given a list of auto-aligned items (with encounter date + provider type)
    * the alignment item can be considered aligned if:
    *   - provider type match
    *   - at least 1 auto-aligned item has encounter date in the dos time range of the alignment item
    * @param autoAligneds list of auto-aligned items
    * @return True if this can be cosidered as aligned
    */
  def isMAAutoAligned(autoAligneds: List[MAAlignmentItem], tolerance: Int = 0): Boolean =
    autoAligneds.exists(_.isMAAutoAlignedWith(this, tolerance))

  def isMAAutoAlignedWith(that: MAAlignmentItem, tolerance: Int = 0): Boolean =
    MAAlignmentItem.isMatchingProviderType(this.encounterType, that.encounterType) &&
      !that.dosEnd.withFieldAdded(DurationFieldType.days(), tolerance).isBefore(dosStart) &&
      !that.dosStart.isAfter(dosEnd.withFieldAdded(DurationFieldType.days(), tolerance)
      )

  /**
    * given a list of partial-auto-aligned items (with encounter date)
    * the alignment item can be considered partial aligned if:
    *   - at least 1 auto-aligned item has encounter date in the dos time range of the alignment item
    * This will be used to narrowed down the list of items to be aligned
    * @param autoAligneds list of auto-aligned items
    * @param tolerance number days that can be used to expand matching range. In some case, the encounter date might be
    *                  off by several days.
    *                  Ideally this should be 0
    *                  a negative tolerance means that the document encounter date is absolutely not trustworthy
    * @return True if this can be cosidered as aligned
    */
  def isMAPartialAutoAligned(autoAligneds: List[MAAlignmentItem], tolerance: Int = 0): Boolean =
    tolerance < 0 || autoAligneds.exists(_.isMAPartialAutoAlignedWith(this, tolerance))

  def isMAPartialAutoAlignedWith(that: MAAlignmentItem, tolerance: Int = 0): Boolean = {
    tolerance < 0 || (
      !that.dosEnd.withFieldAdded(DurationFieldType.days(), tolerance).isBefore(dosStart) &&
        !that.dosStart.isAfter(dosEnd.withFieldAdded(DurationFieldType.days(), tolerance))
      )
  }

  /**
    * Check if an alignment item has been aligned
    * @param alignedItems list of aligned items from coder
    * @return true if both providerType, dosStart, dosEnd match at at least 1 item in the aligned list
    */
  def isMAAligned(alignedItems: List[MAAlignmentItem]): Boolean =
    alignedItems.exists(this.isMAAlignedWith)

  def isMAAlignedWith(that: MAAlignmentItem): Boolean =
    that.dosStart.isEqual(this.dosStart) &&
      that.dosEnd.isEqual(this.dosEnd) &&
      MAAlignmentItem.isMatchingProviderType(this.encounterType, that.encounterType)
}

object MAAlignmentItem {
  // instantiate ma alignment item from raps cluster
  def apply(cluster: RapsCluster): MAAlignmentItem =
    MAAlignmentItem(
      cluster.dosStart.toDateTime(DateTimeZone.UTC),
      cluster.dosEnd.toDateTime(DateTimeZone.UTC),
      cluster.providerType.orNull
    )

  // instantiate ma alignment item from EventTypeX
  def apply(e: EventTypeX): MAAlignmentItem =
    MAAlignmentItem(
      e.fact.time.start.toDateTime(DateTimeZone.UTC),
      e.fact.time.end.toDateTime(DateTimeZone.UTC),
      Code(e.evidence.attributes.getOrElse("encounterType", ""), Code.RAPSPROV) // TODO: encode provider type system
    )

  // instantiate ma alignment item from EventTypeX
  def apply(a: Annotation): MAAlignmentItem =
    MAAlignmentItem(
      a.dosStart.toDateTime(DateTimeZone.UTC),
      a.dosEnd.toDateTime(DateTimeZone.UTC),
      Code(a.encounterType, Code.RAPSPROV) // TODO: encode provider type system
    )

  // HARDCODE AND SHOULD BE AVOIDED
  val equivalentProviderType = Map(
    "P" -> "20",
    "20" -> "P",
    "O" -> "10",
    "10" -> "O"
  )

  def isMatchingProviderType(code1: Code, code2: Code): Boolean = {
    (code1 == null, code2 == null) match {
      case (true, true) => true
      case (true, false) => false
      case (false, true) => false
      case (false, false) =>
        code1.code == code2.code ||
          equivalentProviderType.get(code1.code).contains(code2.code) // ideally we should not check equivalent
    }
  }
}
