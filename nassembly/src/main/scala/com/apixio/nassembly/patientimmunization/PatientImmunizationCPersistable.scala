package com.apixio.nassembly.patientimmunization

import com.apixio.model.nassembly.CPersistable

// Need review
class PatientImmunizationCPersistable extends CPersistable[PatientImmunizationExchange] {

  override def getDataTypeName: String = {
    PatientImmunizationExchange.dataTypeName
  }

}
