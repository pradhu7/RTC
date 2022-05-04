package com.apixio.model.patientdataset.reports

import com.fasterxml.jackson.annotation.JsonProperty

case class ProgressReport(@JsonProperty("projectDataSetUuid")  projectDataSetUuid: String,
                          @JsonProperty("projectDataSetName")  projectDataSetName: String,
                          @JsonProperty("documentProgresses")  documentProgresses: List[DocumentProgress]
                          )

case class DocumentProgress(@JsonProperty("documentUuid")         documentUuid: String,
                            @JsonProperty("patientUuid")          patientUuid: String,
                            @JsonProperty("documentSignalsReady") documentSignalsReady: String,
                            @JsonProperty("patientSignalsReady")  patientSignalsReady: String,
                            @JsonProperty("allSignalsReady")      allSignalsReady: String,
                            @JsonProperty("predictionsReady")     predictionsReady: String,
                            @JsonProperty("predictionsCount")     predictionsCount: String,
                            @JsonProperty("predictionsSentAt")    predictionsSentAt: String
                           )

case class ProgressReportCsvRow(projectDataSetUuid: String,
                                projectDataSetName: String,
                                documentUuid: String,
                                patientUuid: String,
                                documentSignalsReady: String,
                                patientSignalsReady: String,
                                allSignalsReady: String,
                                predictionsReady: String,
                                predictionsCount: String,
                                predictionsSentAt: String
                               )
