package com.apixio.nassembly.exchangeutils

import com.apixio.datacatalog.DemographicsInfoProto.DemographicsInfo
import com.apixio.datacatalog.{NameOuterClass, PatientProto}
import com.apixio.model.patient._
import com.apixio.nassembly.exchangeutils.MockApoUtil._
import com.apixio.nassembly.exchangeutils.TestVerificationUtil.{verifyClinicalCodeByConverting, verifyDates, verifyStrings}
import com.apixio.nassembly.patient.SeparatorUtils
import org.joda.time.DateTime

object DemographicsTestUtil {

  var srcDemographics: Demographics = null

  def generateDemographicsTestData = {
    val demographics = new Demographics
    demographics.setDateOfBirth(DateTime.now.minusYears(30))
    demographics.setDateOfDeath(new DateTime())
    demographics.setEthnicity(generateClinicalCode)
    demographics.setGender(Gender.MALE)
    demographics.setLanguages(generateSingularListOfAlphaNumericString)
    demographics.setMaritalStatus(MaritalStatus.UNMARRIED)

    val patientName: Name = populateTestDataForName
    demographics.setName(patientName)

    demographics.setRace(generateClinicalCode)
    demographics.setReligiousAffiliation(generateAlphaNumericString)

    srcDemographics = demographics
    demographics
  }

  def populateTestDataForName = {
    val patientName = new Name
    patientName.setFamilyNames(generateSingularListOfAlphaNumericString)
    patientName.setGivenNames(generateSingularListOfAlphaNumericString)
    patientName.setNameType(NameType.ANONYMOUS)
    patientName.setPrefixes(generateSingularListOfAlphaNumericString)
    patientName.setSuffixes(generateSingularListOfAlphaNumericString)
    patientName
  }

  def assertDemographics(patientProto: PatientProto.Patient) = {
    val summaries = SeparatorUtils.separateDemographics(patientProto)
    assert(summaries.size == 1)
    val demographicsInfo = summaries.head.getDemographicsInfo
    verifyDemographics(demographicsInfo)
  }

  private def verifyDemographics(demographicsInfo: DemographicsInfo) = {
    verifyDates(demographicsInfo.getDob, srcDemographics.getDateOfBirth())
    verifyDates(demographicsInfo.getDod, srcDemographics.getDateOfDeath())
    verifyClinicalCodeByConverting(demographicsInfo.getEthnicity, srcDemographics.getEthnicity())
    verifyStrings(demographicsInfo.getGender.name(), srcDemographics.getGender().name())
    verifyStrings(demographicsInfo.getLanguages(0), srcDemographics.getLanguages().get(0))
    verifyStrings(demographicsInfo.getMartialStatus.name(), srcDemographics.getMaritalStatus().name())
    verifyName(demographicsInfo.getName, srcDemographics.getName())
    verifyClinicalCodeByConverting(demographicsInfo.getRace, srcDemographics.getRace())
    verifyStrings(demographicsInfo.getReligiousAffiliation, srcDemographics.getReligiousAffiliation())
  }

  def verifyName(protoName: NameOuterClass.Name, modelName: Name) = {
    verifyStrings(protoName.getFamilyNames(0), modelName.getFamilyNames.get(0))
    verifyStrings(protoName.getGivenNames(0), modelName.getGivenNames.get(0))
    verifyStrings(protoName.getNameType, modelName.getNameType.name())
    verifyStrings(protoName.getPrefixes(0), modelName.getPrefixes.get(0))
    verifyStrings(protoName.getSuffixes(0), modelName.getSuffixes.get(0))
  }

}