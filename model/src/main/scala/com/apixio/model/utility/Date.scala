package com.apixio.model.utility

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}

@JsonIgnoreProperties(Array("mappingDtf"))
trait DateModel {

  def getStartDate(): DateTime
  def getEndDate(): DateTime

  val mappingDtf: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")
}

trait DateFunctions {

  def filterDate[T <: DateModel](dateModel: T, date: Option[DateTime]): Boolean = {
    date match {
      case None => true
      case Some(targetDate) =>
        val isAfterOrEqualStart: Boolean = dateModel.getStartDate().isEqual(targetDate) || dateModel.getStartDate().isBefore(targetDate)
        val isBeforeOrEqualEnd: Boolean = dateModel.getEndDate().isEqual(targetDate) || dateModel.getEndDate().isAfter(targetDate)
        isAfterOrEqualStart && isBeforeOrEqualEnd
    }
  }

}
