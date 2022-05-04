package com.apixio.nassembly.exchangeutils

import com.apixio.dao.patient2.PatientUtility
import com.apixio.datacatalog.AnatomyOuterClass.Anatomy
import com.apixio.datacatalog.BillingInfoOuterClass.BillingInfo
import com.apixio.datacatalog.ClinicalCodeOuterClass.ClinicalCode
import com.apixio.datacatalog.CodedBaseObjects.{CodedBase, FfsClaimCBO, ProcedureCBO}
import com.apixio.datacatalog.CptModifierOuterClass.CptModifier
import com.apixio.datacatalog.FfsClaimOuterClass.FfsClaim
import com.apixio.datacatalog.ProcedureProto.ProcedureInfo
import com.apixio.model.external.{AxmCodeOrName, AxmConstants, CaretParser}
import com.apixio.model.patient.{CodedBaseObject, Patient, Procedure}
import com.apixio.nassembly.apo.converterutils.CommonUtils.parseDateString
import com.apixio.nassembly.exchangeutils.ApoToProtoConverter._
import com.apixio.nassembly.procedure.ProcedureMetadataKeys
import com.apixio.util.nassembly.AxmToProtoConverters.convertAxmClinicalCode
import com.apixio.util.nassembly.DataCatalogProtoUtils
import com.google.common.base.Strings.nullToEmpty

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}


object APOToProcedureProtoUtils {

  //  val logger: Logger = LoggerFactory.getLogger(getClass)

  def convertToProcedureProto(pdsId: String, apo: Patient, procedure: Procedure, apoConverter: ApoToProtoConverter =
  new ApoToProtoConverter()): ProcedureCBO = {
    val builder = ProcedureCBO.newBuilder()

    val codedBase: CodedBase = createCodedBase(
      pdsId, apo, procedure, apoConverter)

    //info builder
    val info = convertProcedureInfo(procedure)
    builder.setProcedureInfo(info)


    //add supporting diagnosis
    builder.addAllSupportingDiagnosisCodes(
      procedure.getSupportingDiagnosis.asScala.map(convertClinicalCode(_, validateCode = true))
        .filter(_ != null)
        .sortBy(c => c.getCode)
        .distinct
        .asJava
    )


    builder.setBase(codedBase)
    builder.build()
  }

  def convertToFfsClaimProto(pdsId: String, apo: Patient, procedure: Procedure, apoConverter: ApoToProtoConverter =
  new ApoToProtoConverter()): FfsClaimCBO = {
    val builder = FfsClaimCBO.newBuilder()

    val codedBase: CodedBase = createCodedBase(
      pdsId, apo, procedure, apoConverter)

    //ffs claim builder
    val procedureInfo = convertProcedureInfo(procedure)
    val billingInfo = getBilling(procedure)
    val ffsClaim = FfsClaim.newBuilder()
      .setProcedureInfo(procedureInfo)
      .setBillingInfo(billingInfo)
      .build()


    //add supporting diagnosis
    builder.addAllSupportingDiagnosisCodes(
      procedure.getSupportingDiagnosis.asScala.flatMap(c => Option(convertClinicalCode(c, validateCode = true)))
        .sortBy(c => c.getCode)
        .distinct
        .asJava
    )


    builder.setFfsClaim(ffsClaim)
    builder.setBase(codedBase)
    builder.build()
  }

  private def createCodedBase(pdsId: String,
                              apo: Patient,
                              procedure: CodedBaseObject,
                              apoConverter: ApoToProtoConverter): CodedBase = {
    // start with parsing details so we can pass through internalId
    val parsingDetailsId = apoConverter.getNewParsingDetailsId(apo, procedure)

    // then do source so we can pass through internalId
    val sourceId = apoConverter.getNewSourceId(pdsId, apo, procedure)

    // Primary Actor
    val primaryActorId = apoConverter.getNewPrimaryActorId(pdsId, apo, procedure)

    // Supplementary Actors
    val supplementaryActorIds = apoConverter.getNewSupplementaryActorIds(pdsId, apo, procedure)

    //add encounter
    val encounter = apoConverter.convertEncounterById(pdsId, apo, procedure.getSourceEncounter)

    //meta
    val oid = PatientUtility.getSourceFileArchiveUUID(apo).toString
    val dataCatalogMeta = convertDataCatalogMeta(pdsId, procedure, oid)

    val codedBase = buildCodedBase(parsingDetailsId, sourceId, primaryActorId, supplementaryActorIds, encounter,
      dataCatalogMeta)
    codedBase
  }

  def convertProcedureInfo(procedure: Procedure): ProcedureInfo = {
    val infoBuilder = ProcedureInfo.newBuilder()
    if (procedure.getPerformedOn != null) infoBuilder.setPerformedOn(DataCatalogProtoUtils.fromDateTime(procedure.getPerformedOn))
    if (procedure.getEndDate != null) infoBuilder.setEndDate(DataCatalogProtoUtils.fromDateTime(procedure.getEndDate))
    if (nullToEmpty(procedure.getProcedureName).trim.nonEmpty) infoBuilder.setProcedureName(procedure.getProcedureName)
    // Set code if procedure code is non null and it's valid
    Option(procedure.getCode)
      .flatMap(c => Option(convertClinicalCode(c, validateCode = true, trimModifiers = true)))
      .foreach(infoBuilder.setCode)

    // CptModifiers
    val cptModifier = convertCptModifiers(procedure.getMetadata)
    infoBuilder.setCptModifier(cptModifier)

    val codeTranslations = convertCodeTranslations(procedure)
    infoBuilder.addAllCodeTranslations(codeTranslations.asJava)

    if (nullToEmpty(procedure.getInterpretation).trim.nonEmpty)
      infoBuilder.setInterpretation(procedure.getInterpretation)

    val anatomy = getBodySite(procedure)
    if (anatomy != null) infoBuilder.setBodySite(anatomy)
    infoBuilder.setDeleteIndicator(getDeleteIndicator(procedure))

    infoBuilder.build()
  }

  private def convertCptModifiers(metadata: java.util.Map[String, String]): CptModifier = {
    val builder = CptModifier.newBuilder()
    builder.setModifier1(metadata.getOrDefault("MODIFIER1", ""))
    builder.setModifier2(metadata.getOrDefault("MODIFIER2", ""))
    builder.setModifier3(metadata.getOrDefault("MODIFIER3", ""))
    builder.setModifier4(metadata.getOrDefault("MODIFIER4", ""))
    builder.build()
  }

  private def getBilling(procedure: Procedure): BillingInfo = {
    val metadata = procedure.getMetadata

    def metaDataToClinicalCode(key: String): Option[ClinicalCode] = {
      def tryToConvert(raw: String): Try[Option[ClinicalCode]] = Try {
        val code: AxmCodeOrName = CaretParser.toAxmCodeOrName(raw)
        if (code.isCode) {
          Some(convertAxmClinicalCode(code.getCode))
        }
        else {
          None
        }
      }
      val raw = metadata.getOrDefault(key, "")
      tryToConvert(raw)
      match {
        case Success(codeOpt) =>
          codeOpt
        case Failure(exception) =>
          // Found "bad" data loaded with missing 5th caret
          tryToConvert(raw + "^") match {
            case Success(codeOpt) =>
              codeOpt
            case Failure(exception) =>
              None
          }
      }
    }

    val builder = BillingInfo.newBuilder()


    metaDataToClinicalCode(ProcedureMetadataKeys.BILL_TYPE.toString)
      .foreach(builder.setBillType)

    metaDataToClinicalCode(ProcedureMetadataKeys.PROVIDER_TYPE.toString)
      .foreach(builder.setProviderType)

    builder.setPlaceOfService(metadata.getOrDefault(ProcedureMetadataKeys.PLACE_OF_SERVICE.toString, ""))

    //Set ProviderId
    val rawProviderId = metadata.get(ProcedureMetadataKeys.BILLING_PROVIDER_ID.toString)
    if (nullToEmpty(rawProviderId).trim.nonEmpty) {
      val eid = CaretParser.toExternalID(rawProviderId)
      builder.setProviderId(convertExternalId(eid))
    }

    if (nullToEmpty(metadata.get(ProcedureMetadataKeys.BILLING_PROVIDER_NAME.toString)).trim.nonEmpty)
      builder.setProviderName(metadata.get(ProcedureMetadataKeys.BILLING_PROVIDER_NAME.toString))

    if (nullToEmpty(metadata.get(ProcedureMetadataKeys.TRANSACTION_DATE.toString)).trim.nonEmpty &&
      parseDateString(metadata.get(ProcedureMetadataKeys.TRANSACTION_DATE.toString)).nonEmpty)
      builder.setTransactionDate(parseDateString(metadata.get(ProcedureMetadataKeys.TRANSACTION_DATE.toString)).get)

    builder.build()
  }

  private def getBodySite(procedure: Procedure): Anatomy = {
    convertAnatomy(procedure.getBodySite)
  }

  def isBaseProcedure(procedure: Procedure): Boolean = {
    getClaimType(procedure).isEmpty
  }

  def isFfsClaim(procedure: Procedure): Boolean = {
    getClaimType(procedure) == AxmConstants.FEE_FOR_SERVICE_CLAIM
  }




  private def getDeleteIndicator(procedure: Procedure): Boolean = {
    procedure.getMetadata.getOrDefault("DELETE_INDICATOR", "false").toLowerCase() match {
      case "true" =>
        true
      case _ =>
        false
    }
  }

}
