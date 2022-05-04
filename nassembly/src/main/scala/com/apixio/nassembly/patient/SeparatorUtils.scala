package com.apixio.nassembly.patient

import com.apixio.datacatalog.ExtractedTextOuterClass.{ExtractedText, RawExtractedText, TextType}
import com.apixio.datacatalog.PatientProto
import com.apixio.datacatalog.SummaryObjects._
import com.apixio.nassembly.clinicalactor.ClinicalActorUtils
import com.apixio.nassembly.demographics.{DemographicsUtils, EmptyDemographicUtil}
import com.apixio.util.nassembly.SummaryUtils

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConverters._

object SeparatorUtils {

  /**
   * Take all clinical actors and create Summary Object
   *
   * @param patient Patient Proto
   * @return
   */
  def separateActors(patient: PatientProto.Patient): Iterable[ClinicalActorSummary] = {
    val codedBase = getCodedBase(patient)
    codedBase.getClinicalActorsList.filter(ClinicalActorUtils.notEmpty)
      .map(actor => {
        SummaryUtils.createClinicalActorSummary(actor, codedBase.getParsingDetailsList,
          codedBase.getSourcesList)
      })
  }

  /**
   * Take all clinical actors and create Summary Object
   *
   * @param patient Patient Proto
   * @return
   */
  def separatePatientClinicalActors(patient: PatientProto.Patient): Iterable[PatientClinicalActorSummary] = {
    val codedBase = getCodedBase(patient)
    codedBase.getClinicalActorsList.filter(ClinicalActorUtils.notEmpty)
      .map(actor => {
        SummaryUtils.createPatientClinicalActorSummary(actor, codedBase)
      })
  }

  /**
   * Flatten each procedure by diagnosis codes and create Summary Object
   *
   * @param patient Patient Proto
   * @return
   */
  def separateProcedures(patient: PatientProto.Patient): Iterable[Seq[ProcedureSummary]] = {
    val codedBase = getCodedBase(patient)
    patient.getProceduresList
      .toIterable
      .map(proc => {
        SummaryUtils.createProcedureSummaries(proc, codedBase.getPatientMeta,
          codedBase.getParsingDetailsList, codedBase.getSourcesList,
          codedBase.getClinicalActorsList, codedBase.getEncountersList).asScala
      })
  }

  def separateFfsClaims(patient: PatientProto.Patient): Iterable[Seq[FfsClaimSummary]] = {
    val codedBase = getCodedBase(patient)
    patient.getFfsClaimsList
      .toIterable
      .map(ffs => {
        SummaryUtils.createFfsClaimSummaries(ffs, codedBase.getPatientMeta,
          codedBase.getParsingDetailsList, codedBase.getSourcesList,
          codedBase.getClinicalActorsList, codedBase.getEncountersList).asScala
      })
  }

  def separateRaClaims(patient: PatientProto.Patient): Iterable[RaClaimSummary] = {
    val codedBase = getCodedBase(patient)
    patient.getRaClaimsList
      .toIterable
      .filter(_.getSerializedSize > 0)
      .map(raClaim => {
        SummaryUtils.createRaClaimSummary(raClaim, codedBase.getPatientMeta,
          codedBase.getParsingDetailsList, codedBase.getSourcesList,
          codedBase.getClinicalActorsList, codedBase.getEncountersList)
      })
  }

  def separateProblems(patient: PatientProto.Patient): Iterable[ProblemSummary] = {
    val codedBase = getCodedBase(patient)
    patient.getProblemsList
      .toIterable
      .filter(_.getSerializedSize > 0)
      .map(problem => {
        SummaryUtils.createProblemSummary(problem, codedBase.getPatientMeta,
          codedBase.getParsingDetailsList, codedBase.getSourcesList,
          codedBase.getClinicalActorsList, codedBase.getEncountersList)
      })
  }

  def separateMao004s(patient: PatientProto.Patient): Iterable[Mao004Summary] = {
    val codedBase = getCodedBase(patient)
    patient.getMao004SList
      .toIterable
      .filter(_.getSerializedSize > 0)
      .map(problem => {
        SummaryUtils.createMao004Summary(problem, codedBase.getPatientMeta,
          codedBase.getParsingDetailsList, codedBase.getSourcesList,
          codedBase.getClinicalActorsList, codedBase.getEncountersList)
      })
  }

  def separateDocuments(patient: PatientProto.Patient): Iterable[DocumentMetaSummary] = {
    val codedBase = getCodedBase(patient)
    patient.getDocumentsList
      .toIterable
      .map(doc => {
        SummaryUtils.createDocumentMetaSummary(doc, codedBase.getPatientMeta,
          codedBase.getParsingDetailsList, codedBase.getSourcesList,
          codedBase.getClinicalActorsList, codedBase.getEncountersList)
      })
  }

  def separateStringContent(patient: PatientProto.Patient): Iterable[ExtractedText] = {
    patient.getExtractedTextList.filter(_.getTextType == TextType.STRING_CONTENT)
      .map(et => hydrateRawExtractedText(patient, et))
  }

  def separateExtractedText(patient: PatientProto.Patient): Iterable[ExtractedText] = {
    patient.getExtractedTextList.filter(_.getTextType == TextType.TEXT_EXTRACTED)
      .map(et => hydrateRawExtractedText(patient, et))
  }

  def separateHocrText(patient: PatientProto.Patient): Iterable[ExtractedText] = {
    patient.getExtractedTextList.filter(_.getTextType == TextType.HOCR)
      .map(et => hydrateRawExtractedText(patient, et))
  }

  /**
   * Convert Raw Extracted Text to Extract Text by adding source and patientMeta
   * @param patient Patient Proto
   * @param rawText Raw Extracted Text Proto
   * @return
   */
  private def hydrateRawExtractedText(patient: PatientProto.Patient, rawText: RawExtractedText): ExtractedText = {
    val builder = ExtractedText.newBuilder()
    builder.setPatientMeta(patient.getBase.getPatientMeta).build()
    builder.setDocumentId(rawText.getDocumentId)
    builder.setTextType(rawText.getTextType)
    builder.setStringContent(rawText.getStringContent)
    patient.getBase.getSourcesList.find(s => s.getInternalId == rawText.getSourceId).foreach(builder.setSource)

    builder.build()
  }

  private def getCodedBase(patient: PatientProto.Patient): PatientProto.CodedBasePatient = {
    patient.getBase
  }

  def separateEncounters(patient: PatientProto.Patient): Iterable[EncounterSummary] = {
    val codedBase = getCodedBase(patient)
    codedBase.getEncountersList
      .toIterable
      .filter(_.getSerializedSize > 0)
      .map(encounter => {
        SummaryUtils.createEncounterSummary(encounter, codedBase.getPatientMeta,
          codedBase.getParsingDetailsList, codedBase.getSourcesList,
          codedBase.getClinicalActorsList)
      })
  }

  def separateAllergies(patient: PatientProto.Patient): Iterable[AllergySummary] = {
    val codedBase = getCodedBase(patient)
    patient.getAllergiesList
      .toIterable
      .filter(_.getSerializedSize > 0)
      .map(allergy => {
        SummaryUtils.createAllergySummary(allergy, codedBase.getPatientMeta,
          codedBase.getParsingDetailsList, codedBase.getSourcesList,
          codedBase.getClinicalActorsList, codedBase.getEncountersList)
      })
  }

  def separateCoverage(patient: PatientProto.Patient): Iterable[CoverageSummary] = {
    val codedBase = getCodedBase(patient)
    val allCoverages = patient.getCoverageList.toIterable

    allCoverages
      .filter(_.getSerializedSize > 0)
      .filterNot(_.getCoverageInfo.getHealthPlanName.isEmpty) // health plan name is mandatory
      .filter(c => {
        c.getCoverageInfo.hasEndDate || c.getCoverageInfo.hasDciEndDate // for bad legacy data
      })
      .map(coverage => {
        SummaryUtils.createCoverageSummary(coverage, codedBase.getPatientMeta,
          codedBase.getParsingDetailsList, codedBase.getSourcesList)
      })
  }

  def separateContactDetails(patient: PatientProto.Patient): Iterable[ContactDetailsSummary] = {
    val codedBase = getCodedBase(patient)
    val allContactDetails = Seq(patient.getPrimaryContactDetails) ++ patient.getAlternateContactDetailsList.asScala
    allContactDetails
      .filter(_.getSerializedSize > 0)
      .map(contactDetails => {
        SummaryUtils.createContactDetailsSummary(contactDetails, codedBase.getPatientMeta,
          codedBase.getParsingDetailsList, codedBase.getSourcesList)
      })
  }

  def separateBiometrics(patient: PatientProto.Patient): Iterable[BiometricValueSummary] = {
    val codedBase = getCodedBase(patient)
    patient.getBiometricValuesList
      .toIterable
      .filter(_.getSerializedSize > 0)
      .map(bm => {
        SummaryUtils.createBiometricSummary(bm, codedBase.getPatientMeta,
          codedBase.getParsingDetailsList, codedBase.getSourcesList,
          codedBase.getClinicalActorsList, codedBase.getEncountersList)
      })
  }

  def separateSocialHistories(patient: PatientProto.Patient): Iterable[SocialHistorySummary] = {
    val codedBase = getCodedBase(patient)
    patient.getSocialHistoriesList
      .toIterable
      .filter(_.getSerializedSize > 0)
      .map(socialHistory => {
        SummaryUtils.createSocialHistorySummary(socialHistory, codedBase.getPatientMeta,
          codedBase.getParsingDetailsList, codedBase.getSourcesList,
          codedBase.getClinicalActorsList, codedBase.getEncountersList)
      })
  }

  def separateDemographics(patient: PatientProto.Patient): Iterable[DemographicsSummary] = {
    val codedBase = getCodedBase(patient)
    val allDemographics = (Seq(patient.getPrimaryDemographics) ++ patient.getAlternateDemographicsList.asScala)
      .filter(_.getSerializedSize > 0)
    if (allDemographics.nonEmpty) {
      allDemographics
        .map(demo => {
          SummaryUtils.createDemographicsSummary(demo, codedBase.getPatientMeta,
            codedBase.getParsingDetailsList,
            codedBase.getSourcesList)
        })
    }
    else if (!EmptyDemographicUtil.isEmpty(patient.getBase.getPatientMeta)) {
      val summaryOption = DemographicsUtils.createEmpty(patient.getBase)
      Iterable(summaryOption).flatten
    }
    else {
      Iterable.empty[DemographicsSummary]
    }

  }

  def separateLabResults(patient: PatientProto.Patient): Iterable[LabResultSummary] = {
    val codedBase = getCodedBase(patient)
    patient.getLabResultsList
      .toIterable
      .filter(_.getSerializedSize > 0)
      .map(labResult => {
        SummaryUtils.createLabResultSummary(labResult, codedBase.getPatientMeta,
          codedBase.getParsingDetailsList,
          codedBase.getSourcesList,
          codedBase.getClinicalActorsList, codedBase.getEncountersList)
      })
  }

  def separatePrescriptions(patient: PatientProto.Patient): Iterable[PrescriptionSummary] = {
    val codedBase = getCodedBase(patient)
    patient.getPrescriptionsList
      .toIterable
      .filter(_.getSerializedSize > 0)
      .map(prescriptionCBO => {
        SummaryUtils.createPrescriptionSummary(prescriptionCBO, codedBase.getPatientMeta,
          codedBase.getParsingDetailsList,
          codedBase.getSourcesList,
          codedBase.getClinicalActorsList, codedBase.getEncountersList)
      })
  }

  def separateFamilyHistories(patient: PatientProto.Patient): Iterable[FamilyHistorySummary] = {
    val codedBasePatient = patient.getBase
    patient.getFamilyHistoriesList
      .toIterable
      .filter(_.getSerializedSize > 0)
      .map(familyHistoryCBO => {
        SummaryUtils.createFamilyHistorySummary(familyHistoryCBO, codedBasePatient)
      })
  }

  def separateImmunization(patient: PatientProto.Patient): Iterable[ImmunizationSummary] = {
    val codedBasePatient = patient.getBase
    patient.getImmunizationsList
      .toIterable
      .filter(_.getSerializedSize > 0)
      .map(immunizationCBO => {
        SummaryUtils.createImmunizationSummary(immunizationCBO, codedBasePatient)
      })
  }

}