package com.apixio.model.prospective

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty}

//This class is used to send data to the FE for pv/ca provider performance dashboard
case class ProviderPerformance(@JsonProperty("projId") projectId: String = "",
                               @JsonProperty("providerName") providerName: String = "",
                               @JsonProperty("providerGroup") providerGroup: String = "",
                               @JsonProperty("npi") npi: String = "",
                               @JsonProperty("total") total: Long = 0,
                               @JsonProperty("delivered") delivered: Long = 0,
                               @JsonProperty("notDelivered") notDelivered: Long = 0,
                               @JsonProperty("accepted") accepted: Long = 0,
                               @JsonProperty("rejected") rejected: Long = 0,
                               @JsonProperty("snoozed") snooze: Long = 0,
                               @JsonProperty("claimed") claimed: Long = 0,
                               @JsonProperty("openRaf") openRaf: Float = 0) {

  @JsonProperty
  val responseCount: Long = rejected + accepted //Ignore snooze

  @JsonProperty
  val responseRate: Float = if (responseCount == 0) 0 else {
    responseCount.toFloat / delivered
  }

  @JsonProperty
  val acceptRate: Float = if (responseCount == 0) 0 else {
    accepted.toFloat / responseCount
  }
}



object ProviderPerformance {

  def groupByProviders(responses: List[ProviderResponse]): List[ProviderPerformance] = {
    responses.groupBy(r => (r.npi, r.projectId)).map {
      case ((npi, projectId), responses) =>
        val providerName = responses.head.providerName
        val providerGroup = responses.head.providerGroup //Just take the first value
        val total = responses.size
        val delivered = responses.count(_.deliveryDate > 0)
        val notDelivered = responses.size - responses.count(_.deliveryDate > 0)
        val accepted = responses.count(r => r.decision.toLowerCase == "accepted" && r.hasResponse)
        val rejected = responses.count(r => r.decision.toLowerCase == "rejected" && r.hasResponse)
        val snoozed = responses.count(_.decision.toLowerCase == "snooze")
        val claimed = responses.count(_.isClaimed)
        val responseRate = (accepted + rejected) / total
        val acceptRate = accepted / (accepted + rejected)
        val openRaf = responses.filter(r => r.decision != "rejected" && !r.isClaimed && !r.hierarchyFiltered)
          .map(_.raf).sum
        ProviderPerformance(projectId,
          providerName,
          providerGroup,
          npi,
          total,
          delivered,
          notDelivered,
          accepted,
          rejected,
          snoozed,
          claimed,
          openRaf)
    }.toList
  }
}




