package com.apixio.nassembly.patientallergies

import com.apixio.model.nassembly.CPersistable

class PatientAllergiesCPersistable extends CPersistable[PatientAllergiesExchange] {
  override def getDataTypeName: String = {
    PatientAllergiesExchange.dataTypeName
  }
}
