package com.apixio.model.patientdataset.reports

import com.apixio.model.patientdataset.ProjectDataSetState
import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonProperty}

@JsonIgnoreProperties(ignoreUnknown = true)
case class SummaryReport(@JsonProperty("projectDataSetUuid")              projectDataSetUuid: String,
                         @JsonProperty("projectDataSetName")              projectDataSetName: String,
                         @JsonProperty("projectDataSetState")             projectDataSetState: ProjectDataSetState,
                         @JsonProperty("projectUuid")                     projectUuid: String,
                         @JsonProperty("pdsUuid")                         pdsUuid: String,
                         @JsonProperty("dataType")                        dataType: String,
                         @JsonProperty("predictionEngine")                predictionEngine: String,
                         @JsonProperty("predictionEngineVariant")         predictionEngineVariant: Option[String],
                         @JsonProperty("numberOfDocs")                    numberOfDocs: Int,
                         @JsonProperty("docsWithCompleteDocSignals")      docsWithCompleteDocSignals: Int,
                         @JsonProperty("docsWithCompletePatientSignals")  docsWithCompletePatientSignals: Int,
                         @JsonProperty("docsWithSignalErrors")            docsWithSignalErrors: Int,
                         @JsonProperty("docsWithoutPatientDemo")          docsWithoutPatientDemo: Int,
                         @JsonProperty("docsWithoutPatientEligibility")   docsWithoutPatientEligibility: Int,
                         @JsonProperty("docsWithZeroPredictionsMade")     docsWithZeroPredictionsMade: Int,
                         @JsonProperty("docsWithPredictionsMade")         docsWithPredictions: Int,
                         @JsonProperty("docsWithPredictionsSent")         docsWithPredictionsSent: Int,
                         @JsonProperty("totalPredictions")                totalPredictions: Int,
                         @JsonProperty("numberOfUniquePatients")          numberOfUniquePatients: Int,
                         @JsonProperty("docsWithNullPatients")            docsWithNullPatients: Int,
                         @JsonProperty("patsWithPredictions")             patsWithPredictions: Int,
                         @JsonProperty("patsWithoutPredictions")          patsWithoutPredictions: Int
                        )
