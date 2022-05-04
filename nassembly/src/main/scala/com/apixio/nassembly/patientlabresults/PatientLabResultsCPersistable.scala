package com.apixio.nassembly.patientlabresults

import com.apixio.model.nassembly.CPersistable

class PatientLabResultsCPersistable extends CPersistable[PatientLabResultsExchange] {
  override def getDataTypeName: String = {
    PatientLabResultsExchange.dataTypeName
  }
}
