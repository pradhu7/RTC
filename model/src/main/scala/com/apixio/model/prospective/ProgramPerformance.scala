package com.apixio.model.prospective

import com.fasterxml.jackson.annotation.JsonProperty

case class ProgramPerformance(
            @JsonProperty("total_count")               total_count: Long = 0,
            @JsonProperty("claimed_count")             claimed_count: Long = 0,
            @JsonProperty("claimRaf_count")            claimRaf_count: Float = 0,
            @JsonProperty("openRaf_count")             openRaf_count: Float = 0,
            @JsonProperty("delivered_count")           delivered_count: Long = 0,
            @JsonProperty("accepted_count")            accepted_count: Long = 0,
            @JsonProperty("rejected_count")            rejected_count: Long = 0,
            @JsonProperty("snoozed_count")             snoozed_count: Long = 0,
            @JsonProperty("accepted_on_claimed_count") accepted_on_claimed_count: Long = 0
                             )
{
  @JsonProperty("open_opp_count")
  val open_opp_count = total_count - claimed_count

  @JsonProperty("non_delivered_count")
  val non_delivered_count = total_count - delivered_count

  @JsonProperty("response_count")
  val response_count = accepted_count + rejected_count

  @JsonProperty("non_response_count")
  val non_response_count = total_count - response_count - snoozed_count

  @JsonProperty("accept_not_on_claimed_count")
  val accept_not_on_claimed_count = accepted_count - accepted_on_claimed_count

}
