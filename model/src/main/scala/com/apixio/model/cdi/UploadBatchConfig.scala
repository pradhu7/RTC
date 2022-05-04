package com.apixio.model.cdi

import com.fasterxml.jackson.annotation.JsonProperty

case class UploadBatchConfig( @JsonProperty("batchName") batchName: String,
                              @JsonProperty("userName") userName: String,
                              @JsonProperty("configType") configType: BatchConfigType,
                              @JsonProperty("pathUrl") pathUrl: String,
                              @JsonProperty("sourceSystem") sourceSystem: String,
                              @JsonProperty("sourceType") sourceType: String,
                              @JsonProperty("sourceDate") sourceDate: String,
                              @JsonProperty("maxFileSize") maxFileSize: Int,
                              @JsonProperty("dciStartDate") dciStartDate: String,
                              @JsonProperty("dciEndDate") dciEndDate: String
                            )
