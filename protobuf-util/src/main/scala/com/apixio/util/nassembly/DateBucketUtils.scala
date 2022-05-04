package com.apixio.util.nassembly

import com.apixio.datacatalog.ExternalIdOuterClass.ExternalId
import org.joda.time.{DateTime, Period, PeriodType}

object DateBucketUtils {

  def getYearlyQuarter(dt: DateTime): String = {
    Option(dt).map(_ => {
      val quarter = Math.ceil(dt.monthOfYear().get() / 3.0).toInt
      s"${dt.getYear}.$quarter"
    }).getOrElse(EMPTY_YEAR_QUARTER)
  }

  def getAllYearlyQuarter(st: DateTime, et: DateTime): Set[String] = {
    0.until(new Period(st, et.plusMonths(1), PeriodType.months()).getMonths).map(e => getYearlyQuarter(st.plusMonths(e))).toSet
  }

  def getYearlyHalf(dt: DateTime): String = {
    val half = Math.ceil(dt.monthOfYear().get() / 6.0).toInt
    s"${dt.getYear}.$half"
  }

  def getAllYearlyHalf(st: DateTime, et: DateTime): Set[String] = {
    0.until(new Period(st, et.plusMonths(1), PeriodType.months()).getMonths).map(e => getYearlyHalf(st.plusMonths(e))).toSet
  }

  val EMPTY_YEAR_QUARTER = "0.0"

  /**
   * Used for creating original Id of a patient wrapper
   * @param dataTypeName datatype grouped by date bucket
   * @param dateBucket date bucket string
   * @return
   */
  def dateBucketId(dataTypeName: String, dateBucket: String): ExternalId = {
    ExternalId.newBuilder().setId(dateBucket).setAssignAuthority(s"${dataTypeName}_DateBucket").build()
  }

}
