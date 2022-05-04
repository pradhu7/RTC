package com.apixio.model.cdi

import com.fasterxml.jackson.annotation.JsonProperty
import org.joda.time.DateTime

case class UploadBatch(customerUuid: String,
                       pdsId: String,
                       batchName: String,
                       required: String,
                       @JsonProperty("batchStatus") batchStatus: BatchStatus,
                       createdAt: DateTime,
                       userName: String,
                       modifiedAt: DateTime,
                       @JsonProperty("priority") priority: BatchPriorityType
                      )
