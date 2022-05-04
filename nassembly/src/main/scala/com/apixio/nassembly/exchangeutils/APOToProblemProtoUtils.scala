package com.apixio.nassembly.exchangeutils

import com.apixio.datacatalog.CodedBaseObjects.{CodedBase, Mao004CBO, ProblemCBO, RaClaimCBO}
import com.apixio.datacatalog.Mao004OuterClass.Mao004
import com.apixio.datacatalog.ProblemInfoOuterClass.{ProblemInfo, ProblemOrigin}
import com.apixio.datacatalog.ResolutionStatusOuterClass
import com.apixio.datacatalog.RiskAdjustmentClaimOuterClass.RiskAdjustmentClaim
import com.apixio.model.external.AxmConstants
import com.apixio.model.patient.{Patient, Problem, ResolutionStatus, Source}
import com.apixio.nassembly.apo.converterutils.ArgumentUtil
import com.apixio.nassembly.apo.converterutils.CommonUtils.parseDateString
import com.apixio.nassembly.exchangeutils.ApoToProtoConverter.{convertClinicalCode, convertCodeTranslations, getClaimType}
import com.apixio.nassembly.problem.ProblemMetadataKeys
import com.apixio.util.nassembly.DataCatalogProtoUtils
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

object APOToProblemProtoUtils {

  private val logger = LoggerFactory.getLogger(getClass)


  def isBaseProblem(problem: Problem): Boolean = {
    getClaimType(problem).isEmpty
  }

  def isRaClaim(problem: Problem): Boolean = {
    getClaimType(problem) == AxmConstants.RISK_ADJUSTMENT_CLAIM
  }

  def isMao004(problem: Problem): Boolean = {
    getClaimType(problem) == AxmConstants.MAO_004_ENCOUNTER_CLAIM
  }

  def getProblemOrigin(sourceType: String): ProblemOrigin = {
    sourceType match {
      case "RAPS" => ProblemOrigin.RAPS
      case "RAPS_RETURN" => ProblemOrigin.RAPS
      case "MAO_004_REPORT" => ProblemOrigin.MAO_004
      case _ => ProblemOrigin.PROBLEM
    }
  }



  def convertToProblemProto(pdsId: String,
                            apo: Patient,
                            problem: Problem,
                            apoConverter: ApoToProtoConverter = new ApoToProtoConverter()): ProblemCBO = {
    val builder = ProblemCBO.newBuilder()

    val codedBase: CodedBase = apoConverter.createCodedBase(pdsId, apo, problem)

    //info builder
    val problemInfo = convertProblemInfo(problem, apo.getSourceById(problem.getSourceId))
    builder.setProblemInfo(problemInfo)

    builder.setBase(codedBase)
    builder.build()
  }

  def convertToRaClaimProto(pdsId: String,
                            apo: Patient,
                            problem: Problem,
                            apoConverter: ApoToProtoConverter = new ApoToProtoConverter()): RaClaimCBO = {
    val builder = RaClaimCBO.newBuilder()

    val codedBase: CodedBase = apoConverter.createCodedBase(pdsId, apo, problem)
    val raClaim = createRaClaim(problem, apo.getSourceById(problem.getSourceId))

    builder.setBase(codedBase)
    builder.setRaClaim(raClaim)
    builder.build()
  }

  def convertToMao004Proto(pdsId: String,
                            apo: Patient,
                            problem: Problem,
                            apoConverter: ApoToProtoConverter = new ApoToProtoConverter()): Mao004CBO = {
    val builder = Mao004CBO.newBuilder()

    val codedBase: CodedBase = apoConverter.createCodedBase(pdsId, apo, problem)
    val mao004 = createMao004(problem, apo.getSourceById(problem.getSourceId))

    builder.setBase(codedBase)
    builder.setMao004(mao004)
    builder.build()
  }

  def convertProblemInfo(problem: Problem, source: Source): ProblemInfo = {
    val builder = ProblemInfo.newBuilder()

    Option(problem.getStartDate).foreach(d => builder.setStartDate(DataCatalogProtoUtils.fromDateTime(d)))
    Option(problem.getEndDate).foreach(d => builder.setEndDate(DataCatalogProtoUtils.fromDateTime(d)))
    Option(problem.getDiagnosisDate).foreach(d => builder.setDiagnosisDate(DataCatalogProtoUtils.fromDateTime(d)))
    Option(problem.getProblemName).foreach(builder.setName)
    Option(problem.getSeverity).foreach(builder.setSeverity)
    Option(problem.getTemporalStatus).foreach(builder.setTemporalStatus)
    Option(problem.getResolutionStatus).foreach(r => builder.setResolutionStatus(convertResolutionStatus(r)))

    Option(problem.getMetaTag("DELETE_INDICATOR")).foreach(b => builder.setDeleteIndicator(ArgumentUtil.toBoolean(b)))
    // legacy data
    Option(problem.getMetaTag("DELETE__INDICATOR")).foreach(b => builder.setDeleteIndicator(ArgumentUtil.toBoolean(b)))
    Option(problem.getMetaTag("DELETE")).foreach(b => builder.setDeleteIndicator(ArgumentUtil.toBoolean(b)))

    // Add_or_Delete flag = delete indicator
    Option(problem.getMetaTag(ProblemMetadataKeys.ADD_OR_DELETE_FLAG.toString)).map(_.toLowerCase) match {
      case Some("delete") | Some("d") => builder.setDeleteIndicator(true)
      case _ => ()
    }

    Option(problem.getCode)
      .flatMap(c => Option(convertClinicalCode(c, validateCode = true)))
      .foreach(builder.setCode)

    val codeTranslations = convertCodeTranslations(problem)
    builder.addAllCodeTranslations(codeTranslations.asJava)

    builder.setOrigin(getDataOrigin(problem, source))

    builder.build()
  }

  def convertResolutionStatus(status: ResolutionStatus): ResolutionStatusOuterClass.ResolutionStatus = {
    status.toString match {
      case "ACTIVE" => ResolutionStatusOuterClass.ResolutionStatus.STILL_ACTIVE
      case "INACTIVE" => ResolutionStatusOuterClass.ResolutionStatus.INACTIVE
      case "RESOLVED" => ResolutionStatusOuterClass.ResolutionStatus.RESOLVED_
      case "RECURRENCE" => ResolutionStatusOuterClass.ResolutionStatus.RECURRENCE
      case "RELAPSE" => ResolutionStatusOuterClass.ResolutionStatus.RELAPSE
      case "REMISSION" => ResolutionStatusOuterClass.ResolutionStatus.REMISSION
      case _ => ResolutionStatusOuterClass.ResolutionStatus.UNRECOGNIZED
    }
  }

  def getDataOrigin(problem: Problem, source: Source): ProblemOrigin = {
    Option(problem.getMetaTag(AxmConstants.CLAIM_TYPE)) match {
      case Some(AxmConstants.MAO_004_ENCOUNTER_CLAIM) => ProblemOrigin.MAO_004
      case Some(AxmConstants.RISK_ADJUSTMENT_CLAIM) =>
        source.getSourceSystem match {
          case "RAPS_RETURN" => ProblemOrigin.RAPS
          case "RAPS" => ProblemOrigin.RA_CLAIMS
          case s =>
            logger.warn(s"unrecognized risk adjustment source system $s") //TODO: "PDF" is also a source system. Legacy?
            ProblemOrigin.RA_CLAIMS
        }
      case Some(ct) =>
        logger.warn(s"unrecognized claim type $ct")
        ProblemOrigin.RA_CLAIMS
      case None =>
        ProblemOrigin.PROBLEM
    }
  }

  def createRaClaim(problem: Problem, source: Source): RiskAdjustmentClaim = {
    val builder = RiskAdjustmentClaim.newBuilder()
    builder.setProblemInfo(convertProblemInfo(problem, source))

    val metadata = problem.getMetadata.asScala
    metadata.get(ProblemMetadataKeys.TRANSACTION_DATE.toString).foreach(d => if (parseDateString(d).nonEmpty)
      builder.setTransactionDate(parseDateString(d).get))
    metadata.get(ProblemMetadataKeys.FILE_MODE.toString).foreach(builder.setFileMode)
    metadata.get(ProblemMetadataKeys.PLAN_NUMBER.toString).foreach(builder.setPlanNumber)
    metadata.get(ProblemMetadataKeys.OVERPAYMENT_ID.toString).foreach(builder.setOverpaymentId)
    metadata.get(ProblemMetadataKeys.OVERPAYMENT_ID_ERROR_CODE.toString).foreach(builder.setOverpaymentIdErrorCode)
    metadata.get(ProblemMetadataKeys.PAYMENT_YEAR.toString).foreach(builder.setPaymentYear)
    metadata.get(ProblemMetadataKeys.PAYMENT_YEAR_ERROR.toString).foreach(builder.setPaymentYearError)
    metadata.get(ProblemMetadataKeys.DETAIL_NUMBER_ERROR.toString).foreach(builder.setDetailErrorNumber)
    metadata.get(ProblemMetadataKeys.PATIENT_CONTROL_NUMBER.toString).foreach(builder.setPatientControlNumber)
    metadata.get(ProblemMetadataKeys.PATIENT_HIC_NUMBER_ERROR.toString).foreach(builder.setPatientHicNumberError)
    metadata.get(ProblemMetadataKeys.CORRECTED_HIC_NUMBER.toString).foreach(builder.setCorrectedHicNumber)
    metadata.get(ProblemMetadataKeys.PATIENT_DATE_OF_BIRTH.toString).foreach(d => if (parseDateString(d).nonEmpty)
      builder.setPatientDob(parseDateString(d).get))
    metadata.get(ProblemMetadataKeys.PATIENT_DATE_OF_BIRTH_ERROR.toString).foreach(builder.setPatientDobError)
    metadata.get(ProblemMetadataKeys.PROVIDER_TYPE.toString).foreach(builder.setProviderType)
    metadata.get(ProblemMetadataKeys.DIAGNOSIS_CLUSTER_ERROR1.toString).foreach(builder.setDiagnosisClusterError1)
    metadata.get(ProblemMetadataKeys.DIAGNOSIS_CLUSTER_ERROR2.toString).foreach(builder.setDiagnosisClusterError2)
    metadata.get(ProblemMetadataKeys.RISK_ASSESSMENT_CODE_CLUSTERS.toString)
      .map(cs => cs.split(",").filter(_.nonEmpty).map(_.trim).toIterable.asJava) // split on comma, then sanitize
      .foreach(builder.addAllRiskAssessmentCodeClusters)

    builder.build()
  }

  def createMao004(problem: Problem, source: Source): Mao004 = {
    val builder = Mao004.newBuilder()
    builder.setProblemInfo(convertProblemInfo(problem, source))

    val metadata = problem.getMetadata.asScala

    metadata.get(ProblemMetadataKeys.PLAN_SUBMISSION_DATE.toString).foreach(d => if (parseDateString(d).nonEmpty)
      builder.setPlanSubmissionDate(parseDateString(d).get))
    metadata.get(ProblemMetadataKeys.ENCOUNTER_CLAIM_TYPE.toString).foreach(builder.setEncounterClaimType)
    metadata.get(ProblemMetadataKeys.MA_CONTRACT_ID.toString).foreach(builder.setMaContractId)
    metadata.get(ProblemMetadataKeys.ORIGINAL_ENCOUNTER_ICN.toString).foreach(builder.setOriginalEncounterIcn)
    metadata.get(ProblemMetadataKeys.ENCOUNTER_TYPE_SWITCH.toString).foreach(builder.setEncounterTypeSwitch)
    metadata.get(ProblemMetadataKeys.REPLACEMENT_ENCOUNTER_SWITCH.toString).foreach(builder.setReplacementEncounterSwitch)
    metadata.get(ProblemMetadataKeys.ENCOUNTER_ICN.toString).foreach(builder.setEncounterIcn)
    metadata.get(ProblemMetadataKeys.SUBMISSION_FILE_TYPE.toString).foreach(builder.setSubmissionFileType)
    metadata.get(ProblemMetadataKeys.REPORT_DATE.toString).foreach(d => if (parseDateString(d).nonEmpty)
      builder.setReportDate(parseDateString(d).get))
    metadata.get(ProblemMetadataKeys.HICN.toString).foreach(builder.setHicn)
    metadata.get(ProblemMetadataKeys.PROCESSING_DATE.toString).foreach(d => if (parseDateString(d).nonEmpty)
      builder.setProcessingDate(parseDateString(d).get))
    metadata.get(ProblemMetadataKeys.SUBMISSION_INTERCHANGE_NUMBER.toString).foreach(builder.setSubmissionInterchangeNumber)

    builder.build()
  }
}
