package com.apixio.model.patientdataset

import java.time.LocalDateTime

import com.apixio.XUUID
import com.fasterxml.jackson.annotation.JsonProperty

case class ProjectDataSetDocument(@JsonProperty("pdsUuid")                pdsUuid: XUUID,
                                  @JsonProperty("projectDataSetUuid")     projectDataSetUuid: XUUID,
                                  @JsonProperty("projectDataSetName")     projectDataSetName: String,
                                  @JsonProperty("documentUuid")           documentUuid: XUUID,
                                  @JsonProperty("patientUuid")            patientUuid: String,
                                  @JsonProperty("hasError")               hasError: DbBoolean,
                                  @JsonProperty("hasDemographic")         hasDemographic: DbBoolean,
                                  @JsonProperty("hasEligibility")         hasEligibility: DbBoolean,
                                  @JsonProperty("documentSignalsReady")   documentSignalsReady: DbBoolean,
                                  @JsonProperty("patientSignalsReady")    patientSignalsReady: DbBoolean,
                                  @JsonProperty("allSignalsReady")        allSignalsReady: DbBoolean,
                                  @JsonProperty("predictionsReady")       predictionsReady: DbBoolean,
                                  @JsonProperty("predictionsCount")       predictionsCount: Int,
                                  @JsonProperty("shouldSendPredictions")  shouldSendPredictions: DbBoolean,
                                  @JsonProperty("predictionsSent")        predictionsSent: DbBoolean,
                                  @JsonProperty("predictionsSentAt")      predictionsSentAt: LocalDateTime
                                 )
