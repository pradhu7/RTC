package com.apixio.model.cdi

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.scala.JsonScalaEnumeration

case class UploadJobStatusDetails(@JsonProperty("jobId") jobId: Long,
                                  @JsonProperty("batchStatus") batchStatus: BatchStatus,
                                  @JsonScalaEnumeration(classOf[JobTypeType]) jobType: JobType.JobType,
                                  @JsonProperty("progress") progress: Map[String, Int],
                                  @JsonProperty("logs") logs: List[Any],
                                  @JsonProperty("states") states: List[Any],
                                  @JsonProperty("errors") errors: List[Any]
                                 )
