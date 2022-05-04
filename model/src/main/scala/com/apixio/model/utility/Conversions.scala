package com.apixio.model.utility

import org.joda.time.DateTime

package object Conversions {
  implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)
}
