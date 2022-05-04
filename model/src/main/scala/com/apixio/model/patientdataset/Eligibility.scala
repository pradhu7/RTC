package com.apixio.model.patientdataset

import java.time.LocalDate

import com.fasterxml.jackson.annotation.JsonProperty
import org.joda.time.DateTime

case class Eligibility(@JsonProperty("eligibilityType") eligibilityType: EligibilityType,
                       @JsonProperty("startDate")       startDate: LocalDate,
                       @JsonProperty("endDate")         endDate: LocalDate,
                       @JsonProperty("paymentYear")     paymentYear: Int)

case class EligibilityDateRange( @JsonProperty("startDate")       startDate: DateTime,
                                 @JsonProperty("endDate")         endDate: DateTime)