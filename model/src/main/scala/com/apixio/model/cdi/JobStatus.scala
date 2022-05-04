package com.apixio.model.cdi

import com.fasterxml.jackson.core.`type`.TypeReference

object JobStatus extends Enumeration {
  type JobStatus = Value

  val QUEUED = Value("QUEUED")
  val RUNNING = Value("RUNNING")
  val COMPLETED = Value("COMPLETED")
  val INCOMPLETE = Value("INCOMPLETE")

  val USER_KILLED = Value("USER_KILLED")
  val AUTO_KILLED = Value("AUTO_KILLED")
  val REMOTE_PROCESS_DIED = Value("REMOTE_PROCESS_DIED")
}

class JobStatusType extends TypeReference[JobStatus.type]