package com.apixio.nassembly.patientfamilyhistory

import com.apixio.model.nassembly.CPersistable

class PatientFamilyHistoryCPersistable extends CPersistable[PatientFamilyHistoryExchange] {

  override def getDataTypeName: String = {
    PatientFamilyHistoryExchange.dataTypeName
  }

}
