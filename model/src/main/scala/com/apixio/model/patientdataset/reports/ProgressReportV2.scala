package com.apixio.model.patientdataset.reports

import com.fasterxml.jackson.annotation.JsonProperty

case class ProgressReportV2(@JsonProperty("projectDataSetUuid")  projectDataSetUuid: String,
                          @JsonProperty("projectDataSetName")  projectDataSetName: String,
                          @JsonProperty("predictionEngine")                predictionEngine: String,
                          @JsonProperty("predictionEngineVariant")         predictionEngineVariant: Option[String],
                          @JsonProperty("predictionMCID")      predictionMCID: String,
                          @JsonProperty("documentProgresses")  documentProgresses: List[DocumentProgressV2]
                          )

case class DocumentProgressV2(@JsonProperty("documentUuid")         documentUuid: String,
                            @JsonProperty("patientUuid")          patientUuid: String,
                            @JsonProperty("documentSignalsReady") documentSignalsReady: String,
                            @JsonProperty("patientSignalsReady")  patientSignalsReady: String,
                            @JsonProperty("allSignalsReady")      allSignalsReady: String,
                            @JsonProperty("predictionsReady")     predictionsReady: String,
                            @JsonProperty("predictionsCount")     predictionsCount: String,
                            @JsonProperty("predictionsSentAt")    predictionsSentAt: String
                           )

case class ProgressReportCsvRowV2(projectDataSetUuid: String,
                                projectDataSetName: String,
                                predictionEngine: String,
                                predictionEngineVariant: Option[String],
                                predictionMCID: String,
                                documentUuid: String,
                                patientUuid: String,
                                documentSignalsReady: String,
                                patientSignalsReady: String,
                                allSignalsReady: String,
                                predictionsReady: String,
                                predictionsCount: String,
                                predictionsSentAt: String
                               )
