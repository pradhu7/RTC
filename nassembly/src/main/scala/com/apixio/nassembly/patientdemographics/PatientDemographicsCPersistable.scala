package com.apixio.nassembly.patientdemographics

import com.apixio.model.nassembly.CPersistable

class PatientDemographicsCPersistable extends CPersistable[PatientDemographicsExchange] {

  override def getDataTypeName: String = PatientDemographicsExchange.dataTypeName
}
