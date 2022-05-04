package com.apixio.nassembly.exchangeutils

import com.apixio.datacatalog.VitalSignOuterClass.VitalSign
import com.apixio.datacatalog.PatientProto
import com.apixio.model.patient._
import com.apixio.nassembly.exchangeutils.MockApoUtil._
import com.apixio.nassembly.exchangeutils.TestVerificationUtil.{verifyDates, verifyStrings}
import com.apixio.nassembly.patient.SeparatorUtils
import org.joda.time.DateTime

object BiometricTestUtil {

  var srcBiometric: BiometricValue = null

  def generateBiometricData = {
    val biometricValue = new BiometricValue
    biometricValue.setName(generateAlphaNumericString)
    biometricValue.setResultDate(new DateTime())
    biometricValue.setUnitsOfMeasure(generateAlphaNumericString)
    biometricValue.setValue(generateEitherStringOrNum)
    srcBiometric = biometricValue
    biometricValue
  }

  def assertBiometricValue(patientProto: PatientProto.Patient) = {
    val summaries = SeparatorUtils.separateBiometrics(patientProto)
    assert(summaries.size == 1)
    val biometricValueSummary = summaries.head
    verifyBiometrics(biometricValueSummary.getVitalSign)
  }

  private def verifyBiometrics(vitalSign: VitalSign) = {
    verifyStrings(vitalSign.getName, srcBiometric.getName)
    verifyDates(vitalSign.getResultDate, srcBiometric.getResultDate)
    verifyStrings(vitalSign.getUnitsOfMeasure, srcBiometric.getUnitsOfMeasure)
    verifyStrings(vitalSign.getValue.getValue, srcBiometric.getValue.toString)
  }
}