package com.apixio.nassembly.exchangeutils

import com.apixio.datacatalog.FamilyHistoryProto.FamilyHistoryInfo
import com.apixio.datacatalog.PatientProto
import com.apixio.model.patient._
import com.apixio.nassembly.exchangeutils.MockApoUtil._
import com.apixio.nassembly.exchangeutils.TestVerificationUtil._
import com.apixio.nassembly.patient.SeparatorUtils

object FamilyHistoryTestUtil {

  var srcFH: FamilyHistory = null

  def generateFamilyHistoryTestData = {
    val familyHistory = new FamilyHistory
    familyHistory.setFamilyHistory(generateAlphaNumericString)
    setMockCodedBaseObject(familyHistory)
    srcFH = familyHistory
    familyHistory
  }

  def assertFamilyHistory(patientProto: PatientProto.Patient) = {
    val summaries = SeparatorUtils.separateFamilyHistories(patientProto)
    assert(summaries.size == 1)
    val familyHistoryInfo = summaries.head.getFamilyHistoryInfo
    verifyFamilyHistory(familyHistoryInfo)
  }

  private def verifyFamilyHistory(familyHistoryInfo: FamilyHistoryInfo) = {
    verifyStrings(familyHistoryInfo.getFamilyHistory, srcFH.getFamilyHistory)
  }

}