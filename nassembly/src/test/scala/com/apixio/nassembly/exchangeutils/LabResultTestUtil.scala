package com.apixio.nassembly.exchangeutils

import com.apixio.datacatalog.{LabResultProto, PatientProto}
import com.apixio.model.patient.{LabFlags, LabResult}
import com.apixio.nassembly.patient.SeparatorUtils
import com.apixio.nassembly.util.DataConstants
import org.joda.time.DateTime
import MockApoUtil._
import TestVerificationUtil._

import scala.collection.JavaConverters._

object LabResultTestUtil {

  var srcLabResult: LabResult = null

  def generateLabResult: LabResult = {
    val labResult = new LabResult()
    labResult.setFlag(LabFlags.SENSITIVE)
    labResult.setLabName("test lab name" + DataConstants.SEP_UNDERSCORE + generateAlphaNumericString)
    labResult.setLabNote("test lab note" + DataConstants.SEP_UNDERSCORE + generateAlphaNumericString)
    labResult.setOtherDates(List(generateTypedDate, generateTypedDate).asJava)
    labResult.setPanel(generateClinicalCode)
    labResult.setRange(generateAlphaNumericString)
    labResult.setSampleDate(new DateTime())
    labResult.setCode(generateClinicalCode)
    labResult.setFlag(LabFlags.ABNORMAL)
    labResult.setSpecimen(generateClinicalCode)
    labResult.setSuperPanel(generateClinicalCode)
    labResult.setSequenceNumber(444)
    labResult.setUnits(generateAlphaNumericString)
    labResult.setValue(generateEitherStringOrNum)
    setMockCodedBaseObject(labResult)
    srcLabResult = labResult
    labResult
  }


  def assertLabResult(patientProto: PatientProto.Patient): Unit = {
    val labResultSummaries = SeparatorUtils.separateLabResults(patientProto)

    assert(labResultSummaries.size == 1)
    val labResultSummary = labResultSummaries.head
    val labResultProto = labResultSummary.getLabResultInfo

    verifyLabResult(labResultProto, srcLabResult)

  }

  def verifyLabResult(labResultProto: LabResultProto.LabResultInfo,
                      sourceLabResult: LabResult): Unit = {
    verifyClinicalCodeByConverting(labResultProto.getCode, sourceLabResult.getCode)
    verifyStrings(sourceLabResult.getValue.toString, labResultProto.getValue.getValue)
    assert(labResultProto.getFlag.name().equals(sourceLabResult.getFlag.name()))
    verifyStrings(sourceLabResult.getRange, labResultProto.getRange)
    assert(labResultProto.getCodeTranslationsCount.equals(sourceLabResult.getCodeTranslations.size()))
    verifyCodeTranslations(sourceLabResult.getCodeTranslations, labResultProto.getCodeTranslationsList)

    assert(labResultProto.getFlag.toString.equals(sourceLabResult.getFlag.toString))
    verifyStrings(sourceLabResult.getLabName, labResultProto.getName)

    verifyClinicalCodeByConverting(labResultProto.getSpecimen, sourceLabResult.getSpecimen)
    verifyClinicalCodeByConverting(labResultProto.getPanel, sourceLabResult.getPanel)
    verifyClinicalCodeByConverting(labResultProto.getSuperPanel, sourceLabResult.getSuperPanel)

    verifyDates(labResultProto.getSampleDate, sourceLabResult.getSampleDate)
    assert(labResultProto.getSequenceNumber.getValue == sourceLabResult.getSequenceNumber)
    verifyStrings(labResultProto.getLabNote, sourceLabResult.getLabNote)
    verifyStrings(sourceLabResult.getUnits, labResultProto.getUnits)

  }
}
