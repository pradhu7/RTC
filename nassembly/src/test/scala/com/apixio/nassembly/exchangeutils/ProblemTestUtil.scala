package com.apixio.nassembly.exchangeutils

import com.apixio.datacatalog.ProblemInfoOuterClass.ProblemInfo
import com.apixio.datacatalog.{Mao004OuterClass, PatientProto, RiskAdjustmentClaimOuterClass}
import com.apixio.model.external.AxmConstants
import com.apixio.model.patient.{Problem, ResolutionStatus, Source}
import com.apixio.nassembly.patient.SeparatorUtils
import com.apixio.nassembly.problem.ProblemMetadataKeys
import com.apixio.util.nassembly.DataCatalogProtoUtils
import org.joda.time.DateTime
import MockApoUtil._
import TestVerificationUtil._

import java.util

object ProblemTestUtil {

  var srcProblem: Problem = null
  var srcRaClaimProblem: Problem = null
  var srcRaClaimMeta: util.HashMap[String, String] = null
  var srcMao004Problem: Problem = null
  var srcMao004Meta: util.HashMap[String, String] = null

  def generateMao004Problem(source: Source): Problem = {
    val problem = generateCoreProblem(source)
    problem.setMetadata(generateMao004Meta)
    srcMao004Problem = problem
    problem
  }

  def generateRaClaimProblem(source: Source): Problem = {
    val problem = generateCoreProblem(source)
    problem.setMetadata(generateRaClaimMeta)
    srcRaClaimProblem = problem
    problem
  }

  def generateProblem(source: Source): Problem = {
    val problem: Problem = generateCoreProblem(source)
    srcProblem = problem
    problem
  }

  private def generateCoreProblem(source: Source) = {
    val problem = new Problem
    problem.setSourceId(source.getSourceId)
    problem.setDiagnosisDate(new DateTime())
    problem.setEndDate(new DateTime())
    problem.setProblemName(generateAlphaNumericString)
    problem.setResolutionStatus(ResolutionStatus.ACTIVE)
    problem.setSeverity(generateAlphaNumericString)
    problem.setStartDate(DateTime.now.minusDays(2))
    problem.setTemporalStatus(generateAlphaNumericString)
    problem
  }

  def generateMao004Meta = {
    val mao004Meta = new util.HashMap[String, String]()
    mao004Meta.put(AxmConstants.CLAIM_TYPE, AxmConstants.MAO_004_ENCOUNTER_CLAIM)
    mao004Meta.put(ProblemMetadataKeys.HICN.toString, generateAlphaNumericString)
    mao004Meta.put(ProblemMetadataKeys.PLAN_SUBMISSION_DATE.toString, "2012-03-22")
    mao004Meta.put(ProblemMetadataKeys.ENCOUNTER_CLAIM_TYPE.toString, generateAlphaNumericString)
    mao004Meta.put(ProblemMetadataKeys.MA_CONTRACT_ID.toString, generateAlphaNumericString)
    mao004Meta.put(ProblemMetadataKeys.ORIGINAL_ENCOUNTER_ICN.toString, generateAlphaNumericString)
    mao004Meta.put(ProblemMetadataKeys.ENCOUNTER_TYPE_SWITCH.toString, generateAlphaNumericString)
    mao004Meta.put(ProblemMetadataKeys.REPLACEMENT_ENCOUNTER_SWITCH.toString, generateAlphaNumericString)
    mao004Meta.put(ProblemMetadataKeys.ENCOUNTER_ICN.toString, generateAlphaNumericString)
    mao004Meta.put(ProblemMetadataKeys.SUBMISSION_FILE_TYPE.toString, generateAlphaNumericString)
    mao004Meta.put(ProblemMetadataKeys.REPORT_DATE.toString, "2013-03-04")
    mao004Meta.put(ProblemMetadataKeys.PROCESSING_DATE.toString, "2019-04-05")
    mao004Meta.put(ProblemMetadataKeys.SUBMISSION_INTERCHANGE_NUMBER.toString, generateAlphaNumericString)
    srcMao004Meta = mao004Meta
    mao004Meta
  }

  def generateRaClaimMeta = {
    val raClaimMeta = new util.HashMap[String, String]()
    raClaimMeta.put(AxmConstants.CLAIM_TYPE, AxmConstants.RISK_ADJUSTMENT_CLAIM)
    raClaimMeta.put(ProblemMetadataKeys.TRANSACTION_DATE.toString, "2017-03-22")
    raClaimMeta.put(ProblemMetadataKeys.FILE_MODE.toString, generateAlphaNumericString)
    raClaimMeta.put(ProblemMetadataKeys.PLAN_NUMBER.toString, generateAlphaNumericString)
    raClaimMeta.put(ProblemMetadataKeys.OVERPAYMENT_ID.toString, generateAlphaNumericString)
    raClaimMeta.put(ProblemMetadataKeys.OVERPAYMENT_ID_ERROR_CODE.toString,
      generateAlphaNumericString)
    raClaimMeta.put(ProblemMetadataKeys.PAYMENT_YEAR.toString, generateAlphaNumericString)
    raClaimMeta.put(ProblemMetadataKeys.PAYMENT_YEAR_ERROR.toString, generateAlphaNumericString)
    raClaimMeta.put(ProblemMetadataKeys.DETAIL_NUMBER_ERROR.toString, generateAlphaNumericString)
    raClaimMeta.put(ProblemMetadataKeys.PATIENT_CONTROL_NUMBER.toString, generateAlphaNumericString)
    raClaimMeta.put(ProblemMetadataKeys.PATIENT_HIC_NUMBER_ERROR.toString, generateAlphaNumericString)
    raClaimMeta.put(ProblemMetadataKeys.CORRECTED_HIC_NUMBER.toString, generateAlphaNumericString)
    raClaimMeta.put(ProblemMetadataKeys.PATIENT_DATE_OF_BIRTH.toString,
      "1984-03-22")
    raClaimMeta.put(ProblemMetadataKeys.PATIENT_DATE_OF_BIRTH_ERROR.toString, generateAlphaNumericString)
    raClaimMeta.put(ProblemMetadataKeys.PROVIDER_TYPE.toString, generateAlphaNumericString)
    raClaimMeta.put(ProblemMetadataKeys.DIAGNOSIS_CLUSTER_ERROR1.toString, generateAlphaNumericString)
    raClaimMeta.put(ProblemMetadataKeys.DIAGNOSIS_CLUSTER_ERROR2.toString, generateAlphaNumericString)
    raClaimMeta.put(ProblemMetadataKeys.RISK_ASSESSMENT_CODE_CLUSTERS.toString,
      generateAlphaNumericString)
    srcRaClaimMeta = raClaimMeta
    raClaimMeta
  }

  def assertRaClaimProblem(patientProto: PatientProto.Patient): Unit = {
    val summaries = SeparatorUtils.separateRaClaims(patientProto)
    assert(summaries.size == 1)
    val raClaim = summaries.head.getRaClaim
    verifyProblems(raClaim.getProblemInfo, srcRaClaimProblem)
    verifyRaClaimMeta(raClaim)
  }

  def assertMao004Problem(patientProto: PatientProto.Patient): Unit = {
    val summaries = SeparatorUtils.separateMao004s(patientProto)
    assert(summaries.size == 1)
    val mao004Claim = summaries.head.getMao004
    verifyProblems(mao004Claim.getProblemInfo, srcMao004Problem)
    verifyMao004Meta(mao004Claim)
  }

  def assertProblem(patientProto: PatientProto.Patient): Unit = {
    val summaries = SeparatorUtils.separateProblems(patientProto)
    assert(summaries.size == 1)
    val summary = summaries.head
    verifyProblems(summary.getProblemInfo, srcProblem)
  }

  def verifyProblems(problemInfo: ProblemInfo,
                     srcProblem: Problem) = {

    verifyStrings(srcProblem.getProblemName, problemInfo.getName)
    verifyStrings(srcProblem.getResolutionStatus.name(), DataCatalogProtoUtils.convertResolutionStatus(problemInfo
      .getResolutionStatus).name())
    verifyDates(problemInfo.getStartDate, srcProblem.getStartDate)
    verifyDates(problemInfo.getDiagnosisDate, srcProblem.getDiagnosisDate)
    verifyDates(problemInfo.getEndDate, srcProblem.getEndDate)
    verifyStrings(srcProblem.getSeverity, problemInfo.getSeverity)
    verifyStrings(srcProblem.getTemporalStatus, problemInfo.getTemporalStatus)
  }


  def verifyMao004Meta(mao004Claim: Mao004OuterClass.Mao004): Unit = {
    verifyStrings(mao004Claim.getHicn, srcMao004Meta.get(ProblemMetadataKeys.HICN.toString))
    verifyDates(mao004Claim.getPlanSubmissionDate, srcMao004Meta.get(ProblemMetadataKeys.PLAN_SUBMISSION_DATE
      .toString))
    verifyStrings(mao004Claim.getEncounterClaimType,
      srcMao004Meta.get(ProblemMetadataKeys.ENCOUNTER_CLAIM_TYPE.toString))
    verifyStrings(mao004Claim.getMaContractId, srcMao004Meta.get(ProblemMetadataKeys.MA_CONTRACT_ID.toString))
    verifyStrings(mao004Claim.getOriginalEncounterIcn,
      srcMao004Meta.get(ProblemMetadataKeys.ORIGINAL_ENCOUNTER_ICN.toString))
    verifyStrings(mao004Claim.getEncounterTypeSwitch,
      srcMao004Meta.get(ProblemMetadataKeys.ENCOUNTER_TYPE_SWITCH.toString))
    verifyStrings(mao004Claim.getReplacementEncounterSwitch,
      srcMao004Meta.get(ProblemMetadataKeys.REPLACEMENT_ENCOUNTER_SWITCH.toString))
    verifyStrings(mao004Claim.getEncounterIcn, srcMao004Meta.get(ProblemMetadataKeys.ENCOUNTER_ICN.toString))
    verifyStrings(mao004Claim.getSubmissionFileType,
      srcMao004Meta.get(ProblemMetadataKeys.SUBMISSION_FILE_TYPE.toString))
    verifyDates(mao004Claim.getReportDate, srcMao004Meta.get(ProblemMetadataKeys.REPORT_DATE.toString))
    verifyDates(mao004Claim.getProcessingDate, srcMao004Meta.get(ProblemMetadataKeys.PROCESSING_DATE.toString))
    verifyStrings(mao004Claim.getSubmissionInterchangeNumber,
      srcMao004Meta.get(ProblemMetadataKeys.SUBMISSION_INTERCHANGE_NUMBER.toString))
  }

  def verifyRaClaimMeta(raClaim: RiskAdjustmentClaimOuterClass.RiskAdjustmentClaim): Unit = {
    verifyDates(raClaim.getTransactionDate, srcRaClaimMeta.get(ProblemMetadataKeys.TRANSACTION_DATE.toString))
    verifyStrings(raClaim.getFileMode, srcRaClaimMeta.get(ProblemMetadataKeys.FILE_MODE.toString))
    verifyStrings(raClaim.getPlanNumber, srcRaClaimMeta.get(ProblemMetadataKeys.PLAN_NUMBER.toString))
    verifyStrings(raClaim.getOverpaymentId, srcRaClaimMeta.get(ProblemMetadataKeys.OVERPAYMENT_ID.toString))
    verifyStrings(raClaim.getOverpaymentIdErrorCode,
      srcRaClaimMeta.get(ProblemMetadataKeys.OVERPAYMENT_ID_ERROR_CODE
        .toString))
    verifyStrings(raClaim.getPaymentYear, srcRaClaimMeta.get(ProblemMetadataKeys.PAYMENT_YEAR.toString))
    verifyStrings(raClaim.getPaymentYearError,
      srcRaClaimMeta.get(ProblemMetadataKeys.PAYMENT_YEAR_ERROR.toString))
    verifyStrings(raClaim.getDetailErrorNumber,
      srcRaClaimMeta.get(ProblemMetadataKeys.DETAIL_NUMBER_ERROR.toString))
    verifyStrings(raClaim.getPatientControlNumber,
      srcRaClaimMeta.get(ProblemMetadataKeys.PATIENT_CONTROL_NUMBER.toString))
    verifyStrings(raClaim.getPatientHicNumberError,
      srcRaClaimMeta.get(ProblemMetadataKeys.PATIENT_HIC_NUMBER_ERROR.toString))
    verifyStrings(raClaim.getCorrectedHicNumber,
      srcRaClaimMeta.get(ProblemMetadataKeys.CORRECTED_HIC_NUMBER.toString))
    verifyDates(raClaim.getPatientDob,
      srcRaClaimMeta.get(ProblemMetadataKeys.PATIENT_DATE_OF_BIRTH.toString))
    verifyStrings(raClaim.getPatientDobError,
      srcRaClaimMeta.get(ProblemMetadataKeys.PATIENT_DATE_OF_BIRTH_ERROR.toString))
    verifyStrings(raClaim.getProviderType, srcRaClaimMeta.get(ProblemMetadataKeys.PROVIDER_TYPE.toString))
    verifyStrings(raClaim.getDiagnosisClusterError1, srcRaClaimMeta.get(ProblemMetadataKeys
      .DIAGNOSIS_CLUSTER_ERROR1.toString))
    verifyStrings(raClaim.getDiagnosisClusterError2, srcRaClaimMeta.get(ProblemMetadataKeys
      .DIAGNOSIS_CLUSTER_ERROR2.toString))
    verifyStrings(raClaim.getRiskAssessmentCodeClusters(0).toString, srcRaClaimMeta
      .get(ProblemMetadataKeys.RISK_ASSESSMENT_CODE_CLUSTERS
        .toString).split(",")(0))
  }


}
