package com.apixio.nassembly.exchangeutils

import com.apixio.datacatalog.ProcedureProto.ProcedureInfo
import com.apixio.datacatalog.{FfsClaimOuterClass, PatientProto}
import com.apixio.model.external.AxmConstants
import com.apixio.model.patient.{Actor, Anatomy, ParsingDetail, Procedure, Source}
import com.apixio.nassembly.patient.SeparatorUtils
import com.apixio.nassembly.procedure.ProcedureMetadataKeys
import org.joda.time.DateTime
import MockApoUtil._
import TestVerificationUtil._

import java.util
import scala.collection.JavaConverters._

object ProcedureTestUtil {

  var srcProcedure: Procedure = null
  var srcFfsClaimMeta: util.HashMap[String, String] = null
  var srcFfsClaimProcedure: Procedure = null

  def generateFfsClaimMeta = {

    val ffsClaimMeta = new util.HashMap[String, String]()
    ffsClaimMeta.put(AxmConstants.CLAIM_TYPE, AxmConstants.FEE_FOR_SERVICE_CLAIM)
    ffsClaimMeta.put(ProcedureMetadataKeys.BILL_TYPE.toString, "UNKNOWN^UNKNOWN^BILL_TYPE^2.25.726443312^")
    ffsClaimMeta.put(ProcedureMetadataKeys.PLACE_OF_SERVICE.toString, generateAlphaNumericString)
    ffsClaimMeta.put(ProcedureMetadataKeys.PROVIDER_TYPE.toString,
      "UNKNOWN^UNKNOWN^PROV_TYPE_FFS^2.25.986811684062365523470895812567751821389^")
    ffsClaimMeta.put(ProcedureMetadataKeys.BILLING_PROVIDER_ID.toString, generateAlphaNumericString)
    ffsClaimMeta.put(ProcedureMetadataKeys.BILLING_PROVIDER_NAME.toString, generateAlphaNumericString)
    ffsClaimMeta.put(ProcedureMetadataKeys.TRANSACTION_DATE.toString, "2022-03-05")
    srcFfsClaimMeta = ffsClaimMeta
    ffsClaimMeta
  }

  def generateFfsClaimProcedure(parsingDetails: ParsingDetail, source: Source, actor: Actor) = {
    val procedure = generateCoreProcedure
    procedure.setParsingDetailsId(parsingDetails.getParsingDetailsId)
    procedure.setSourceId(source.getSourceId)
    procedure.setPrimaryClinicalActorId(actor.getInternalUUID)
    procedure.setMetadata(generateFfsClaimMeta)
    srcFfsClaimProcedure = procedure
    procedure
  }

  def generateProcedure = {
    val procedure: Procedure = generateCoreProcedure
    srcProcedure = procedure
    procedure
  }

  private def generateCoreProcedure = {
    val procedure = new Procedure
    val anatomy = new Anatomy
    anatomy.setAnatomicalStructureName(generateAlphaNumericString)
    procedure.setBodySite(anatomy)
    procedure.setEndDate(new DateTime())
    procedure.setInterpretation(generateAlphaNumericString)
    procedure.setPerformedOn(DateTime.now.minusDays(2))
    procedure.setProcedureName(generateAlphaNumericString)
    procedure.setSupportingDiagnosis(List(generateClinicalCode).asJava) // Not implemented in proto
    setMockCodedBaseObject(procedure)
    procedure
  }

  def assertFfsClaim(patientProto: PatientProto.Patient): Unit = {
    val summariesList = SeparatorUtils.separateFfsClaims(patientProto)
    assert(summariesList.size == 1)
    val summaries = summariesList.head
    val ffsClaim = summaries.head.getFfsClaim
    verifyProcedures(ffsClaim.getProcedureInfo, srcFfsClaimProcedure)
    verifyFfsBillingInfo(ffsClaim)
  }

  def verifyFfsBillingInfo(ffsClaim: FfsClaimOuterClass.FfsClaim): Unit = {
    val billingInfo = ffsClaim.getBillingInfo

    verifyStrings(billingInfo.getProviderId.getAssignAuthority,
      srcFfsClaimMeta.get(ProcedureMetadataKeys.BILLING_PROVIDER_ID.toString))
    verifyStrings(billingInfo.getProviderName,
      srcFfsClaimMeta.get(ProcedureMetadataKeys.BILLING_PROVIDER_NAME.toString))
    verifyClinicalCodeByConverting(billingInfo.getProviderType,
      srcFfsClaimMeta.get(ProcedureMetadataKeys.PROVIDER_TYPE.toString))
    verifyClinicalCodeByConverting(billingInfo.getBillType,
      srcFfsClaimMeta.get(ProcedureMetadataKeys.BILL_TYPE.toString))
    verifyDates(billingInfo.getTransactionDate, srcFfsClaimMeta.get(ProcedureMetadataKeys.TRANSACTION_DATE
      .toString))
    verifyStrings(billingInfo.getPlaceOfService, srcFfsClaimMeta.get(ProcedureMetadataKeys.PLACE_OF_SERVICE.toString))
  }

  def assertProcedure(patientProto: PatientProto.Patient) = {
    val summariesList = SeparatorUtils.separateProcedures(patientProto)
    assert(summariesList.size == 1)
    val procedureSummaries = summariesList.head
    val procedureInfo = procedureSummaries.head.getProcedureInfo
    verifyProcedures(procedureInfo, srcProcedure)
  }

  def verifyProcedures(procedureInfo: ProcedureInfo,
                       srcProcedure: Procedure) = {

    // srcProcedure -> List<ClinicalCode> supportingDiagnosis is not mapped to Proto -> procedureInfo
    verifyStrings(srcProcedure.getBodySite.getAnatomicalStructureName,
      procedureInfo.getBodySite.getAnatomicalStructureName)
    verifyStrings(srcProcedure.getProcedureName, procedureInfo.getProcedureName)
    verifyDates(procedureInfo.getPerformedOn, srcProcedure.getPerformedOn)
    verifyDates(procedureInfo.getEndDate, srcProcedure.getEndDate)
    verifyStrings(srcProcedure.getInterpretation, procedureInfo.getInterpretation)
  }


}
