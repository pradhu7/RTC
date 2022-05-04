package com.apixio.nassembly.exchangeutils

import com.apixio.datacatalog.SocialHistoryInfoOuterClass.SocialHistoryInfo
import com.apixio.datacatalog.PatientProto
import com.apixio.model.patient._
import com.apixio.nassembly.exchangeutils.MockApoUtil._
import com.apixio.nassembly.exchangeutils.TestVerificationUtil.{verifyClinicalCodeByConverting, verifyDates, verifyStrings}
import com.apixio.nassembly.patient.SeparatorUtils
import org.joda.time.DateTime

object SocialHistoryTestUtil {

  var srcSocialHistory: SocialHistory = null

  def generateSocialHistoryData = {
    val socialHistory = new SocialHistory
    socialHistory.setDate(new DateTime())
    socialHistory.setFieldName(generateAlphaNumericString)
    socialHistory.setType(generateClinicalCode)
    socialHistory.setValue(generateAlphaNumericString)
    srcSocialHistory = socialHistory
    socialHistory
  }

  def assertSocialHistory(patientProto: PatientProto.Patient) = {
    val summaries = SeparatorUtils.separateSocialHistories(patientProto)
    assert(summaries.size == 1)
    val socialHistoryInfo = summaries.head.getSocialHistoryInfo
    verifySocialHistory(socialHistoryInfo)
  }

  private def verifySocialHistory(socialHistoryInfo: SocialHistoryInfo) = {
    verifyDates(socialHistoryInfo.getDate, srcSocialHistory.getDate)
    verifyStrings(socialHistoryInfo.getFieldName, srcSocialHistory.getFieldName)
    verifyClinicalCodeByConverting(socialHistoryInfo.getType, srcSocialHistory.getType)
    verifyStrings(socialHistoryInfo.getValue, srcSocialHistory.getValue)
  }

}