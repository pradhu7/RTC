package com.apixio.model.cdi

import com.fasterxml.jackson.core.`type`.TypeReference

object EnvType extends Enumeration {
  type EnvType = Value

  val UAT = Value("UAT")
  val DEFAULT = Value("DEFAULT")

}

class EnvType extends TypeReference[EnvType.type]