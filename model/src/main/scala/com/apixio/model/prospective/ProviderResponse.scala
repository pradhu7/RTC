package com.apixio.model.prospective

import com.apixio.model.prospective.ProviderResponse.long2DateTime
import com.fasterxml.jackson.annotation.JsonProperty
import org.joda.time.DateTime

//Case Class represents a row in our RDB backing provider performance
case class ProviderResponse(@JsonProperty("patId") patientId: String = "",
                            @JsonProperty("projId") projectId: String = "",
                            @JsonProperty("pds") pds: String = "",
                            @JsonProperty("code") code: String = "",
                            @JsonProperty("raf") raf: Float = 0,
                            @JsonProperty("dc") diseaseCategory: String = "",
                            @JsonProperty("d") decision: String = "",
                            @JsonProperty("npi") npi: String = "",
                            @JsonProperty("pg") providerGroup: String = "",
                            @JsonProperty("pn") providerName: String = "",
                            @JsonProperty("isC") isClaimed: Boolean = false,
                            @JsonProperty("rr") reportableRaf: Float = 0,
                            @JsonProperty("hf") hierarchyFiltered: Boolean = false,
                            @JsonProperty("dd") deliveryDate: Long = 0L,
                            @JsonProperty("rd") responseDate: Long = 0L,
                            @JsonProperty("lmd") lastModifiedDate: Long = 0L){

  def deliveryDateAsDateTime: Option[DateTime] = long2DateTime(deliveryDate)
  def responseDateAsDateTime: Option[DateTime] = long2DateTime(responseDate)
  def lastModifiedDateAsDateTime: Option[DateTime] = long2DateTime(lastModifiedDate)

  def hasResponse: Boolean = responseDate > 0
  def wasDelivered: Boolean = deliveryDate > 0
}

object ProviderResponse {
  def long2DateTime(l: Long): Option[DateTime] = {
    if (l == 0L) None else Some(new DateTime(l))
  }
}