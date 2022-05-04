package com.apixio.model.patientdataset

import com.fasterxml.jackson.annotation.JsonProperty

case class EngineVariantMetaData(@JsonProperty("engine")        engine: String,
                            @JsonProperty("variant")       variant: String,
                            @JsonProperty("mcid")       mcid: String)