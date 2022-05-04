package com.apixio.model.cdi

import com.fasterxml.jackson.core.`type`.TypeReference

object JobType extends Enumeration {
  type JobType = Value

  val VALIDATE = Value("VALIDATE")
  val UPLOAD = Value("UPLOAD")
}

class JobTypeType extends TypeReference[JobType.type]