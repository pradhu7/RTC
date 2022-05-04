package com.apixio.model.cdi

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.scala.JsonScalaEnumeration

case class UploadJobStatus(@JsonProperty("jobId") jobId: Long,
                           @JsonScalaEnumeration(classOf[JobStatusType]) jobStatus: JobStatus.JobStatus,
                           @JsonScalaEnumeration(classOf[JobTypeType]) jobType: JobType.JobType
                          )
