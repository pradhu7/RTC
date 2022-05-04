package com.apixio.nassembly.patientprescriptions

import com.apixio.model.nassembly.CPersistable

class PatientPrescriptionCPersistable extends CPersistable[PatientPrescriptionExchange] {
  override def getDataTypeName: String = {
    PatientPrescriptionExchange.dataTypeName
  }

}
