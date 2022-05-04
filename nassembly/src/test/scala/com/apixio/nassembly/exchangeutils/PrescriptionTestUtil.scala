package com.apixio.nassembly.exchangeutils

import com.apixio.datacatalog.MedicationProto.MedicationInfo
import com.apixio.datacatalog.PrescriptionProto.PrescriptionInfo
import com.apixio.datacatalog.{NameOuterClass, PatientProto}
import com.apixio.model.patient._
import com.apixio.nassembly.exchangeutils.MockApoUtil._
import com.apixio.nassembly.exchangeutils.TestVerificationUtil.{verifyClinicalCodeByConverting, verifyDates, verifyStrings}
import com.apixio.nassembly.patient.SeparatorUtils

object PrescriptionTestUtil {

  var srcPrescription: Prescription = null

  def generatePrescriptionTestData: Prescription = {
    val prescription = new Prescription
    prescription.setActivePrescription(false)
    prescription.setAmount(generateAlphaNumericString)
    prescription.setAssociatedMedication(populateTestDataForMedication)
    prescription.setDosage(generateAlphaNumericString)
    prescription.setEndDate(getDateAfterNumOfDays(5))
    prescription.setFillDate(getDateBeforeNumOfDays(1))
    prescription.setFrequency(generateAlphaNumericString)
    prescription.setPrescriptionDate(getDateBeforeNumOfDays(10))
    prescription.setQuantity(2.0)
    prescription.setRefillsRemaining(5)
    prescription.setSig(generateAlphaNumericString)
    setMockCodedBaseObject(prescription)
    srcPrescription = prescription
    prescription
  }

  def populateTestDataForMedication: Medication = {
    val medication = new Medication
    medication.setBrandName(generateAlphaNumericString)
    medication.setForm(generateAlphaNumericString)
    medication.setGenericName(generateAlphaNumericString)
    medication.setIngredients(generateSingularListOfAlphaNumericString)
    medication.setRouteOfAdministration(generateAlphaNumericString)
    medication.setStrength(generateAlphaNumericString)
    medication.setUnits(generateAlphaNumericString)
    medication
  }

  def assertPrescriptions(patientProto: PatientProto.Patient): Unit = {
    val summaries = SeparatorUtils.separatePrescriptions(patientProto)
    assert(summaries.size == 1)
    val prescriptionInfo = summaries.head.getPrescriptionInfo
    verifyPrescription(prescriptionInfo)
  }

  private def verifyPrescription(prescriptionInfo: PrescriptionInfo): Unit = {
    verifyStrings(prescriptionInfo.getAmount, srcPrescription.getAmount)
    verifyDates(prescriptionInfo.getPrescriptionDate, srcPrescription.getPrescriptionDate)
    verifyStrings(prescriptionInfo.getDirections, srcPrescription.getSig)
    verifyDates(prescriptionInfo.getEndDate, srcPrescription.getEndDate)
    verifyDates(prescriptionInfo.getFillDate, srcPrescription.getFillDate)
    verifyClinicalCodeByConverting(prescriptionInfo.getCode, srcPrescription.getCode)

    verifyMedication(prescriptionInfo.getAssociatedMedication, srcPrescription.getAssociatedMedication)
  }

  def verifyName(protoName: NameOuterClass.Name, modelName: Name): Unit = {
    verifyStrings(protoName.getFamilyNames(0), modelName.getFamilyNames.get(0))
    verifyStrings(protoName.getGivenNames(0), modelName.getGivenNames.get(0))
    verifyStrings(protoName.getNameType, modelName.getNameType.name())
    verifyStrings(protoName.getPrefixes(0), modelName.getPrefixes.get(0))
    verifyStrings(protoName.getSuffixes(0), modelName.getSuffixes.get(0))
  }

  def verifyMedication(protoMedication: MedicationInfo, modelMedication: Medication): Unit = {
    verifyStrings(protoMedication.getBrandName, modelMedication.getBrandName)
    verifyStrings(protoMedication.getForm, modelMedication.getForm)
    verifyStrings(protoMedication.getGenericName, modelMedication.getGenericName)
    verifyStrings(protoMedication.getIngredients(0), modelMedication.getIngredients.get(0))
    verifyStrings(protoMedication.getRouteOfAdministration, modelMedication.getRouteOfAdministration)
    verifyStrings(protoMedication.getStrength, modelMedication.getStrength)
    verifyStrings(protoMedication.getUnits, modelMedication.getUnits)
  }

}