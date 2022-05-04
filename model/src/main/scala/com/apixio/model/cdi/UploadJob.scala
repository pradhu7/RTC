package com.apixio.model.cdi

import com.fasterxml.jackson.module.scala.JsonScalaEnumeration
import com.fasterxml.jackson.annotation.JsonProperty

case class UploadJob(@JsonProperty("jobId") jobId: Long,
                     @JsonProperty("uploadbatch_id") uploadBatchId: Long,
                     @JsonProperty("pdsId") pdsId: String,
                     @JsonProperty("batchName") batchName: String,
                     @JsonScalaEnumeration(classOf[JobTypeType]) jobType: JobType.JobType,
                     @JsonScalaEnumeration(classOf[JobStatusType]) jobStatus: JobStatus.JobStatus,
                     @JsonProperty("userName") userName: String,
                     @JsonProperty("jvmMaxMemSize") jvmMaxMemSize: Option[Int] = None,
                     @JsonProperty("sequenceFileBatchSize") sequenceFileBatchSize: Option[Int] = None,
                     @JsonProperty("hostName") hostName: Option[String] = None,
                     @JsonProperty("hostIp") hostIp: Option[String] = None,
                     @JsonProperty("pubsubPort") pubsubPort: Option[Int] = None,
                     @JsonProperty("adminPort") adminPort: Option[Int] = None,
                     @JsonProperty("startedAt") startedAt: Option[String] = None,
                     @JsonProperty("completedAt") completedAt: Option[String] = None,
                     @JsonProperty("retryCount") retryCount: Int = 0,
                     @JsonScalaEnumeration(classOf[EnvType]) envType: EnvType.EnvType
                    )
