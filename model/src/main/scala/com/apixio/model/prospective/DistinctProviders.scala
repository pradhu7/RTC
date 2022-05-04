package com.apixio.model.prospective

import com.fasterxml.jackson.annotation.JsonProperty

case class DistinctProviders(@JsonProperty("npi") npi: String = "",
                             @JsonProperty("providerName") providerName: String = "",
                             @JsonProperty("providerGroup") providerGroup: String = "")
