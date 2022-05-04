package com.apixio.model.patientdataset.reports

import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonProperty}

@JsonIgnoreProperties(ignoreUnknown = true)
case class DocumentAuditTrail(@JsonProperty("projectDataSetUuid") projectDataSetUuid: String,
                              @JsonProperty("projectDataSetName") projectDataSetName: String,
                              @JsonProperty("documentUuid")       documentUuid: String,
                              @JsonProperty("patientUuid")        patientUuid: String,
                              @JsonProperty("allSignalsReady")    allSignalsReady: String,
                              @JsonProperty("hasError")           hasError: String,
                              @JsonProperty("hasDemographic")     hasDemographic: String,
                              @JsonProperty("hasEligibility")     hasEligibility: String,
                              @JsonProperty("predictionsMade")    predictionsMade: String,
                              @JsonProperty("predictionsCount")   predictionsCount: String,
                              @JsonProperty("predictionsSentAt")  predictionsSentAt: String,
                              @JsonProperty("activeSiggens")      activeSiggens: List[String],
                              @JsonProperty("siggenResults")      siggenResults: Map[String, SiggenResult]
                             )

@JsonIgnoreProperties(ignoreUnknown = true)
case class SiggenResult(@JsonProperty("nameVersion")  nameVersion: String,
                        @JsonProperty("count")        count: Int,
                        @JsonProperty("errors")       errors: String)

