package com.apixio.model.patientdataset

import java.time.{LocalDate, LocalDateTime}

import com.apixio.XUUID
import com.fasterxml.jackson.annotation.JsonProperty

case class ProjectDataSetV2(@JsonProperty("dbId")                     dbId: Long,
                          @JsonProperty("projectDataSetUuid")       projectDataSetUuid: XUUID,
                          @JsonProperty("projectDataSetName")       projectDataSetName: String,
                          @JsonProperty("projectUuid")              projectUuid: XUUID,
                          @JsonProperty("state")                    state: ProjectDataSetState,
                          @JsonProperty("pdsUuid")                  pdsUuid: XUUID,
                          @JsonProperty("dataType")                 dataType: String,
                          @JsonProperty("models")                   models: List[EngineVariantMetaData],
                          @JsonProperty("numberOfItems")            numberOfItems: Int,
                          @JsonProperty("documentDateOverride")     documentDateOverride: LocalDate,
                          @JsonProperty("createdBy")                createdBy: String,
                          @JsonProperty("createdAt")                createdAt: LocalDateTime,
                          @JsonProperty("requireEligibility")       requireEligibility: Boolean,
                          @JsonProperty("eligibilityDates")         eligibilityDates: List[EligibilityDateRange],
                          @JsonProperty("projectDataSetType")       projectDataSetType: ProjectDataSetType,
                          @JsonProperty("documentUuids")            documentUuids: List[String] = null,
                          @JsonProperty("patientUuids")             patientUuids: List[String] = null,
                          @JsonProperty("docPatMap")                docPatMap: Map[String,String] = null
                         )
