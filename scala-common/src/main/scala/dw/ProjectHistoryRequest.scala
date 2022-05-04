package com.apixio.scala.dw

import java.time.Instant
import java.time.temporal.ChronoUnit

import com.apixio.scala.utility.Constraints.ValidationMethod
import com.datasift.dropwizard.scala.validation.constraints.{Size, Valid}
import com.fasterxml.jackson.annotation.JsonIgnore

case class ProjectHistoryRequest(@Valid @Size(max = 2) timeFilters: Array[TimeBoundFilters], page: Option[Int], pageSize: Option[Int]) {
  @ValidationMethod(message = "You cannot have more than 1 `current` timeFilter")
  @JsonIgnore
  def isThereOnlyOneCurrentTBF: Boolean =
    timeFilters.toList.count(_.currentStatus.getOrElse(false)) <= 1

  @ValidationMethod(message = "You cannot have more than 1 historical timeFilter")
  @JsonIgnore
  def isThereOnlyOneHistoricalTBF: Boolean =
    timeFilters.toList
      .filterNot(_.currentStatus.getOrElse(false))
      .length <= 1

  lazy val firstTimeBoundFilter: TimeBoundFilters = timeFilters
    .find { tbf =>
      !tbf.currentStatus.getOrElse(false)
    }.getOrElse(TimeBoundFilters.empty)

  lazy val currentFilter: TimeBoundFilters = timeFilters
    .find { tbf =>
      tbf.currentStatus.getOrElse(false)
    }.getOrElse(TimeBoundFilters.empty)

  def pageOrDefault: Int = page.getOrElse(ProjectHistoryRequest.defaultPage)

  def pageSizeOrDefault: Int = pageSize.getOrElse(ProjectHistoryRequest.defaultPageSize)
}

object ProjectHistoryRequest {
  val defaultPage = 0
  val defaultPageSize = 10
}

case class TimeBoundFilters(startDate: Option[Long], endDate: Option[Long], currentStatus: Option[Boolean], @Valid featureFilters: Filters) {
  @ValidationMethod(message = "Cannot specify both `currentStatus=true` and a timestamp range")
  @JsonIgnore
  def isEitherCurrentOrTimestamp: Boolean = (startDate, endDate, currentStatus) match {
    case (None, None, Some(true)) =>
      true
    case (_, _, Some(true)) =>
      false
    case _ =>
      true
  }

  @ValidationMethod(message = "Start Date must be before End Date")
  @JsonIgnore
  def isStartBeforeEnd: Boolean = (startDate, endDate) match {
    case (Some(startDate), Some(endDate)) =>
      startDate < endDate
    case _ =>
      true
  }
}

object TimeBoundFilters {
  def empty = {
    val endDate: Instant = Instant.now
    val startDate: Instant = endDate.minus(7, ChronoUnit.DAYS)
    TimeBoundFilters(Some(startDate.toEpochMilli), Some(endDate.toEpochMilli), None, Filters(None, None, None, None, None, None, None, None, None, None, None, None, None, None, None))
  }
}

case class Filters(phasesCompleted: Option[Set[String]],
                   nodesCompleted: Option[Set[String]],
                   oppDecisions: Option[Set[String]],
                   documentDecisions: Option[Set[String]],
                   overturns: Option[Set[String]],
                   hccs: Option[Set[String]],
                   conditions: Option[Set[String]],
                   documentIDs: Option[Set[String]],
                   patientNames: Option[Set[String]],
                   patientDOBs: Option[Set[String]],
                   patientIDs: Option[Set[String]],
                   reviewers: Option[Set[String]],
                   reviewersOrgIDs: Option[Set[String]],
                   rejectReasons: Option[Set[String]],
                   result: Option[Set[String]]) {
  // Not doing phase/node validation here cuz that's some crazy
  // bizlogic shit that'll need a copy of the `workflow`. This is
  // "just" the DTO.

  @ValidationMethod(message = "oppDecision must be `accept` or `reject`")
  @JsonIgnore
  def isOppDecisionEnum: Boolean = oppDecisions.fold(true)(_.subsetOf(Filters.DECISIONS))

  @ValidationMethod(message = "documentDecision must be `accept` or `reject`")
  @JsonIgnore
  def isDocumentDecisionEnum: Boolean = documentDecisions.fold(true)(_.subsetOf(Filters.DECISIONS))

  @ValidationMethod(message = "overturn must be `agreed`, `positive`, `negative`, or `flagged`")
  @JsonIgnore
  def isOverturnEnum: Boolean = overturns.fold(true)(_.subsetOf(Filters.OVERTURNS))
}

object Filters {
  final val DECISIONS = Set("accept", "reject")
  final val OVERTURNS = Set("agreed", "positive", "negative", "flagged")
}
