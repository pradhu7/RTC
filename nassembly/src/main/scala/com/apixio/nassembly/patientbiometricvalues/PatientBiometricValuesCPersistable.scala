package com.apixio.nassembly.patientbiometricvalues

import com.apixio.model.nassembly.CPersistable

class PatientBiometricValuesCPersistable extends CPersistable[PatientBiometricValuesExchange] {
  override def getDataTypeName: String = {
    PatientBiometricValuesExchange.dataTypeName
  }
}
