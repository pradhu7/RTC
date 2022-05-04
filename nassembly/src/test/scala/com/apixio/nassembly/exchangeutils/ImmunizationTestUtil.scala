package com.apixio.nassembly.exchangeutils

import com.apixio.datacatalog.ImmunizationProto.ImmunizationInfo
import com.apixio.datacatalog.PatientProto
import com.apixio.model.patient._
import com.apixio.nassembly.exchangeutils.MockApoUtil._
import com.apixio.nassembly.exchangeutils.TestVerificationUtil._
import com.apixio.nassembly.patient.SeparatorUtils
import org.joda.time.DateTime

object ImmunizationTestUtil {

  var srcAdminImmu: Administration = null

  def generateAdminImmuTestData: Administration = {
    val adminImmu = new Administration
    adminImmu.setAdminDate(new DateTime())
    adminImmu.setAmount(generateAlphaNumericString)
    adminImmu.setDosage(generateAlphaNumericString)
    adminImmu.setEndDate(getDateAfterNumOfDays(10))
    adminImmu.setMedication(PrescriptionTestUtil.populateTestDataForMedication)
    adminImmu.setMedicationSeriesNumber(23)
    adminImmu.setQuantity(2.0)
    adminImmu.setStartDate(getDateBeforeNumOfDays(5))
    setMockCodedBaseObject(adminImmu)
    srcAdminImmu = adminImmu
    adminImmu
  }

  def assertAdminImmu(patientProto: PatientProto.Patient): Unit = {
    val summaries = SeparatorUtils.separateImmunization(patientProto)
    assert(summaries.size == 1)
    val adminImmuInfo = summaries.head.getImmunizationInfo
    verifyAdminImmu(adminImmuInfo)
  }

  private def verifyAdminImmu(immunizationInfo: ImmunizationInfo): Unit = {
    verifyDates(immunizationInfo.getAdminDate, srcAdminImmu.getAdminDate)
    verifyStrings(immunizationInfo.getAmount, srcAdminImmu.getAmount)
    verifyStrings(immunizationInfo.getDosage, srcAdminImmu.getDosage)
    verifyDates(immunizationInfo.getEndDate, srcAdminImmu.getEndDate)
    PrescriptionTestUtil.verifyMedication(immunizationInfo.getMedicationInfo, srcAdminImmu.getMedication)
    verifyStrings(immunizationInfo.getMedicationSeriesNumber.getValue.toString, srcAdminImmu.getMedicationSeriesNumber.toString)
    verifyStrings(immunizationInfo.getQuantity.getValue.toString, srcAdminImmu.getQuantity.toString)
    verifyDates(immunizationInfo.getStartDate, srcAdminImmu.getStartDate)
  }

}