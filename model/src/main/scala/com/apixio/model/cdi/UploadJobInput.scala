package com.apixio.model.cdi

import com.fasterxml.jackson.annotation.JsonProperty

case class UploadJobInput( @JsonProperty("userName") userName: String,
                           @JsonProperty("postValidate") postValidate: Option[Boolean] = None,
                           @JsonProperty("jvmMaxMemSize") jvmMaxMemSize: Option[Int] = None,
                           @JsonProperty("sequenceFileBatchSize") sequenceFileBatchSize: Option[Int] = None
                         )
