package com.apixio.nassembly.exchangeutils

import com.apixio.datacatalog.CoverageInfoOuterClass.CoverageInfo
import com.apixio.datacatalog.PatientProto
import com.apixio.model.patient._
import com.apixio.nassembly.exchangeutils.MockApoUtil._
import com.apixio.nassembly.exchangeutils.TestVerificationUtil._
import com.apixio.nassembly.patient.SeparatorUtils

object CoverageTestUtil {

  var srcCoverage: Coverage = null

  def generateCoverageTestData: Coverage = {
    val coverage = new Coverage
    coverage.setBeneficiaryID(generateExternalOriginalId)
    coverage.setEndDate(getDateAfterNumOfDays(2).toLocalDate)
    coverage.setGroupNumber(generateExternalOriginalId)
    coverage.setHealthPlanName(generateAlphaNumericString)
    coverage.setMemberNumber(generateExternalOriginalId)
    coverage.setSequenceNumber(generateRandomInt)
    coverage.setStartDate(getDateBeforeNumOfDays(2).toLocalDate)
    coverage.setSubscriberID(generateExternalOriginalId)
    coverage.setType(CoverageType.HMO)
    setMockCodedBaseObject(coverage)
    srcCoverage = coverage
    coverage
  }

  def assertCoverage(patientProto: PatientProto.Patient): Unit = {
    val summaries = SeparatorUtils.separateCoverage(patientProto)
    assert(summaries.size == 1)
    val coverageInfo = summaries.head.getCoverageInfo
    verifyCoverage(coverageInfo)
  }

  private def verifyCoverage(coverageInfo: CoverageInfo): Unit = {
    verifyExternalOriginalId(coverageInfo.getBeneficiaryId, srcCoverage.getBeneficiaryID)
    verifyLocalDates(coverageInfo.getEndDate, srcCoverage.getEndDate)
    verifyExternalOriginalId(coverageInfo.getGroupNumber, srcCoverage.getGroupNumber)
    verifyStrings(coverageInfo.getHealthPlanName, srcCoverage.getHealthPlanName)
    verifyStrings(coverageInfo.getSequenceNumber.toString, srcCoverage.getSequenceNumber.toString)
    verifyLocalDates(coverageInfo.getStartDate, srcCoverage.getStartDate)
    verifyExternalOriginalId(coverageInfo.getSubscriberNumber, srcCoverage.getSubscriberID)
    verifyStrings(coverageInfo.getCoverageType, srcCoverage.getType.name())
  }

}