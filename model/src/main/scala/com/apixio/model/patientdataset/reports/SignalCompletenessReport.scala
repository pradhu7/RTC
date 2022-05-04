package com.apixio.model.patientdataset.reports

import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonProperty}

@JsonIgnoreProperties(ignoreUnknown = true)
case class SignalCompletenessReport(@JsonProperty("projectDataSetUuid")   projectDataSetUuid: String,
                                    @JsonProperty("projectDataSetName")   projectDataSetName: String,
                                    @JsonProperty("activeSiggens")        activeSiggens: List[String],
                                    @JsonProperty("documentCompleteness") documentCompleteness: List[DocumentSignalCompleteness]
                                   )

case class DocumentSignalCompleteness(@JsonProperty("documentUuid")       documentUuid: String,
                                      @JsonProperty("patientUuid")        patientUuid: String,
                                      @JsonProperty("hasDemographic")     hasDemographic: String,
                                      @JsonProperty("hasEligibility")     hasEligibility: String,
                                      @JsonProperty("allSignalsReady")    allSignalsReady: String,
                                      @JsonProperty("signalPresence")     signalPresence: Map[String, String]
                                     )

case class SignalCompletenessReportCsvRow(projectDataSetUuid: String,
                                          projectDataSetName: String,
                                          documentUuid: String,
                                          patientUuid: String,
                                          hasDemographic: String,
                                          hasEligibility: String,
                                          allSignalsReady: String,
                                          signalPresence: Map[String, String]
                                         )