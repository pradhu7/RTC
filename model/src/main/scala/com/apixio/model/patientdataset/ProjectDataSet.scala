package com.apixio.model.patientdataset

import java.time.{LocalDate, LocalDateTime}

import com.apixio.XUUID
import com.fasterxml.jackson.annotation.JsonProperty

case class ProjectDataSet(@JsonProperty("dbId")                     dbId: Long,
                          @JsonProperty("projectDataSetUuid")       projectDataSetUuid: XUUID,
                          @JsonProperty("projectDataSetName")       projectDataSetName: String,
                          @JsonProperty("projectUuid")              projectUuid: XUUID,
                          @JsonProperty("state")                    state: ProjectDataSetState,
                          @JsonProperty("pdsUuid")                  pdsUuid: XUUID,
                          @JsonProperty("dataType")                 dataType: String,
                          @JsonProperty("predictionEngine")         predictionEngine: String,
                          @JsonProperty("predictionEngineVariant")  predictionEngineVariant: Option[String],
                          @JsonProperty("numberOfItems")            numberOfItems: Int,
                          @JsonProperty("documentDateOverride")     documentDateOverride: LocalDate,
                          @JsonProperty("createdBy")                createdBy: String,
                          @JsonProperty("createdAt")                createdAt: LocalDateTime,
                          @JsonProperty("requireEligibility")       requireEligibility: Boolean,
                          @JsonProperty("projectDataSetType")       projectDataSetType: ProjectDataSetType,
                          @JsonProperty("eligibility")              eligibility: Eligibility = null,
                          @JsonProperty("documentUuids")            documentUuids: List[String] = null,
                          @JsonProperty("patientUuids")             patientUuids: List[String] = null
                         )
