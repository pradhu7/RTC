package com.apixio.nassembly.exchangeutils

import com.apixio.datacatalog.AllergyInfoOuterClass.AllergyInfo
import com.apixio.datacatalog.PatientProto
import com.apixio.model.patient.Allergy
import MockApoUtil._
import TestVerificationUtil._
import com.apixio.nassembly.patient.SeparatorUtils
import org.joda.time.DateTime

object AllergyTestUtil {

  var srcAllergy: Allergy = null;

  def generateAllergy = {
    val allergy = new Allergy();
    allergy.setAllergen(generateAlphaNumericString)
    allergy.setDiagnosisDate(new DateTime())
    allergy.setReaction(generateClinicalCode)
    allergy.setReactionDate(new DateTime())
    allergy.setReactionSeverity(generateAlphaNumericString)
    allergy.setResolvedDate(new DateTime())
    setMockCodedBaseObject(allergy)
    srcAllergy = allergy
    allergy
  }


  def assertAllergy(patientProto: PatientProto.Patient) = {
    val summaries = SeparatorUtils.separateAllergies(patientProto)
    assert(summaries.size == 1)
    val allergySummary = summaries.head
    val allergyInfo = allergySummary.getAllergyInfo
    verifyAllergies(allergyInfo, srcAllergy)
  }

  def verifyAllergies(allergyInfo: AllergyInfo,
                      srcAllergy: Allergy) = {
    verifyStrings(srcAllergy.getAllergen, allergyInfo.getAllergen)
    verifyDates(allergyInfo.getDiagnosisDate, srcAllergy.getDiagnosisDate)
    verifyClinicalCodeByConverting(allergyInfo.getReaction, srcAllergy.getReaction)
    verifyDates(allergyInfo.getReactionDate, srcAllergy.getReactionDate)
    verifyStrings(srcAllergy.getReactionSeverity, allergyInfo.getReactionSeverity)
    verifyDates(allergyInfo.getResolvedDate, srcAllergy.getResolvedDate)
  }


}
