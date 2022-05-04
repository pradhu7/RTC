package com.apixio.nassembly.exchangeutils

import com.apixio.dao.patient2.PatientUtility
import com.apixio.datacatalog.AddressOuterClass.Address
import com.apixio.datacatalog.AllergyInfoOuterClass.AllergyInfo
import com.apixio.datacatalog.AnatomyOuterClass.Anatomy
import com.apixio.datacatalog.BaseObjects.{Base, ClinicalActor, ContactDetails, PatientDemographics}
import com.apixio.datacatalog.CareSiteOuterClass.CareSite
import com.apixio.datacatalog.ClinicalActorInfoOuterClass.ClinicalActorInfo
import com.apixio.datacatalog.ClinicalCodeOuterClass.{ClinicalCode => ClinicalCodeProto}
import com.apixio.datacatalog.CodedBaseObjects.{CodedBase, Encounter}
import com.apixio.datacatalog.ContactInfoOuterClass.{ContactInfo, TelephoneNumber}
import com.apixio.datacatalog.DataCatalogMetaOuterClass.DataCatalogMeta
import com.apixio.datacatalog.DocumentMetaOuterClass._
import com.apixio.datacatalog.EditTypeOuterClass.EditType
import com.apixio.datacatalog.EncounterInfoOuterClass.EncounterInfo
import com.apixio.datacatalog.ExternalIdOuterClass.ExternalId
import com.apixio.datacatalog.FamilyHistoryProto.FamilyHistoryInfo
import com.apixio.datacatalog.ImmunizationProto.ImmunizationInfo
import com.apixio.datacatalog.LabResultProto.{LabResultInfo, VendorDetails}
import com.apixio.datacatalog.MedicationProto.MedicationInfo
import com.apixio.datacatalog.NameOuterClass.Name
import com.apixio.datacatalog.OrganizationOuterClass.Organization
import com.apixio.datacatalog.ParsingDetailsOuterClass.ParsingDetails
import com.apixio.datacatalog.PatientMetaProto.PatientMeta
import com.apixio.datacatalog.PatientProto.{CodedBasePatient, Patient => PatientProto}
import com.apixio.datacatalog.PrescriptionProto.PrescriptionInfo
import com.apixio.datacatalog.SocialHistoryInfoOuterClass.SocialHistoryInfo
import com.apixio.datacatalog.SourceOuterClass.{Source => SourceProto}
import com.apixio.datacatalog.VitalSignOuterClass.VitalSign
import com.apixio.datacatalog._
import com.apixio.model.external.AxmConstants
import com.apixio.model.patient._
import com.apixio.model.{EitherStringOrNumber, patient}
import com.apixio.nassembly.apo.converterutils.ArgumentUtil
import com.apixio.nassembly.apo.converterutils.CommonUtils.parseDateString
import com.apixio.nassembly.demographics.DemographicsMetadataKeys
import com.apixio.nassembly.documentmeta.DocMetadataKeys
import com.apixio.nassembly.exchangeutils.APOToProblemProtoUtils._
import com.apixio.nassembly.exchangeutils.APOToProcedureProtoUtils._
import com.apixio.nassembly.exchangeutils.ApoToProtoConverter._
import com.apixio.nassembly.labresult.LabResultMetadataKeys
import com.apixio.nassembly.util.BaseEntityUtil
import com.apixio.util.nassembly.AssemblyConverters.{convertEditType, convertGender, convertMaritalStatus}
import com.apixio.util.nassembly.DataCatalogProtoUtils.{convertUuid, fromDateTime, fromLocalDate}
import com.apixio.util.nassembly.DefaultValueWrapperUtils.{wrapBoolean, wrapDouble, wrapInt, wrapLong}
import com.apixio.util.nassembly.{IdentityFunctions, InternalUUIDUtils}
import com.google.common.base.Strings.{isNullOrEmpty, nullToEmpty}
import com.google.protobuf.ByteString
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

import java.io.{PrintWriter, StringWriter}
import java.nio.charset.StandardCharsets
import java.util
import java.util.UUID
import scala.collection.JavaConversions.{asScalaBuffer, iterableAsScalaIterable}
import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

// Return null if APO object is null
class ApoToProtoConverter {

  private val logger = LoggerFactory.getLogger(classOf[ApoToProtoConverter])

  val converterContext: ConverterContext = ConverterContext()


  def convertAPO(apo: patient.Patient, pdsId: String): PatientProto = {
    Try {

      val patientBuilder = PatientProto.newBuilder()

      apo.getParsingDetails.iterator().asScala.foreach(pd => {
        val converted = convertParsingDetails(apo, pd)
        converterContext.addParsingDetails(pd.getParsingDetailsId, converted)
      })

      apo.getClinicalActors.iterator().asScala.foreach(ca => {
        val converted = convertClinicalActor(pdsId, apo, ca)
        converterContext.addActor(ca.getInternalUUID, converted)
      })

      apo.getSources.iterator().asScala.foreach(s => {
        val converted = convertSource(pdsId, apo, s)
        converterContext.addSource(s.getInternalUUID, converted)
      })

      apo.getEncounters.iterator().asScala.foreach(e => {
        val converted = convertEncounter(pdsId, apo, e)
        converterContext.addEncounter(e.getInternalUUID, converted)
      })

      apo.getCareSites.iterator().asScala.foreach(cs => {
        val converted = convertCareSite(cs)
        converterContext.addCaresite(cs.getInternalUUID, converted)
      })

      // Documents
      val documentMetaProtos = apo.getDocuments.iterator().asScala.map(doc => convertDocMetadata(pdsId, doc, apo))
      patientBuilder.addAllDocuments(documentMetaProtos.toSeq.asJava)

      // Clean text
      val extractedTextList = generateExtractedText(apo, pdsId)
      if (extractedTextList.nonEmpty) patientBuilder.addAllExtractedText(extractedTextList.asJava)


      // Allergies
      val allergyProtos = apo.getAllergies.iterator().asScala.map(a => convertAllergy(pdsId, a, apo))
      patientBuilder.addAllAllergies(allergyProtos.toSeq.asJava)

      // Biometric Values
      val biometricValueProtos = apo.getBiometricValues.iterator().asScala
        .map(bv => convertBiometricValue(pdsId, bv, apo))
      patientBuilder.addAllBiometricValues(biometricValueProtos.toSeq.asJava)

      // Procedures
      val procedureProtos = apo.getProcedures.iterator().asScala
        .filter(isBaseProcedure)
        .map(p => convertToProcedureProto(pdsId, apo, p, this))
        .toSeq
      patientBuilder.addAllProcedures(procedureProtos.asJava)

      // FFS Claims
      val ffsClaims = apo.getProcedures.iterator().asScala
        .filter(isFfsClaim)
        .map(p => convertToFfsClaimProto(pdsId, apo, p, this))
        .toSeq
      patientBuilder.addAllFfsClaims(ffsClaims.asJava)

      // Problems
      val problems = apo.getProblems.iterator().asScala
        .filter(isBaseProblem)
        .map(p => convertToProblemProto(pdsId, apo, p, this))
        .toSeq
      patientBuilder.addAllProblems(problems.asJava)

      // Ra Claims
      val raClaims = apo.getProblems.iterator().asScala
        .filter(isRaClaim)
        .map(p => convertToRaClaimProto(pdsId, apo, p, this))
        .toSeq
      patientBuilder.addAllRaClaims(raClaims.asJava)

      // Mao 004
      val mao004s = apo.getProblems.iterator().asScala
        .filter(isMao004)
        .map(p => convertToMao004Proto(pdsId, apo, p, this))
        .toSeq
      patientBuilder.addAllMao004S(mao004s.asJava)

      // Coverage
      val allCoverage = apo.getCoverage.iterator().asScala
        .map(c => convertCoverage(pdsId, apo, c))
        .toSeq
      patientBuilder.addAllCoverage(allCoverage.asJava)


      // Primary Contact Details
      val primaryContactDetails = convertContactDetails(pdsId, apo, apo.getPrimaryContactDetails)
      if (primaryContactDetails != null) patientBuilder.setPrimaryContactDetails(primaryContactDetails)

      // Alternative Contact Details
      val altContactDetails = apo.getAlternateContactDetails.iterator().asScala
        .map(cd => convertContactDetails(pdsId, apo, cd))
      patientBuilder.addAllAlternateContactDetails(altContactDetails.toSeq.asJava)

      // Social Histories
      val socialHistories = apo.getSocialHistories.iterator().asScala.map(sh => convertSocialHistory(pdsId, sh, apo))
      patientBuilder.addAllSocialHistories(socialHistories.toSeq.asJava)

      // Lab Results
      val labResults = apo.getLabs.iterator().asScala.map(labResults => convertLabResults(pdsId, labResults, apo))
      patientBuilder.addAllLabResults(labResults.toSeq.asJava)

      // Prescriptions
      val prescriptions = apo.getPrescriptions.iterator().asScala.map(prescriptions => convertPrescription(pdsId,
        prescriptions, apo))
      patientBuilder.addAllPrescriptions(prescriptions.toSeq.asJava)

      // FamilyHistory
      val familyHistories = apo.getFamilyHistories.iterator().asScala
        .map(familyHistories => convertFamilyHistory(pdsId,
          familyHistories, apo))
      patientBuilder.addAllFamilyHistories(familyHistories.toSeq.asJava)

      // Immunization
      val adminImmunizations = apo.getAdministrations.iterator().asScala
        .map(adminImmunizations => convertImmunization(pdsId,
          adminImmunizations, apo))
      patientBuilder.addAllImmunizations(adminImmunizations.toSeq.asJava)

      //primary demographics
      val primaryDemographics = convertDemographics(pdsId, apo, apo.getPrimaryDemographics)
      if (primaryDemographics != null) patientBuilder.setPrimaryDemographics(primaryDemographics)

      //alternate demographics
      val alternateDemographics = apo.getAlternateDemographics.iterator().asScala
        .map(demo => convertDemographics(pdsId, apo, demo))
      patientBuilder.addAllAlternateDemographics(alternateDemographics.toSeq.asJava)

      // Now add normalized data from converter context
      val parsingDetails = converterContext.getDistinctParsingDetails
      val clinicalActors = converterContext.getDistinctClinicalActors
      val sources = converterContext.getDistinctSources
      val normalizedEncounters = converterContext.getDistinctEncounters

      // Patient Meta
      val patientMeta = createPatientMeta(apo)

      // Data Catalog Meta
      val oid = PatientUtility.getSourceFileArchiveUUID(apo).toString
      val dataCatalogMeta = createDataCatalogMeta(pdsId, oid)

      val basePatientBuilder: CodedBasePatient = BaseEntityUtil.buildCodedBasePatient(patientMeta, dataCatalogMeta,
        parsingDetails, sources, clinicalActors, normalizedEncounters)

      patientBuilder.setBase(basePatientBuilder)
      patientBuilder.build()
    } match {
      case Success(protos) =>
        protos
      case Failure(exception) =>
        if (apo == null) {
          logger.error(s"APO is null for $pdsId")
          throw new Exception(s"APO is null. Reason: ${exception.getMessage}")
        }
        else {
          val patientId = Option(apo.getPatientId).map(_.toString).getOrElse("?")
          val docId = PatientUtility.getSourceFileArchiveUUID(apo)
          logger.error(s"Can't convert patient: $patientId, docId: $docId")
          val sw = new StringWriter
          exception.printStackTrace(new PrintWriter(sw))
          //throw new Exception(s"Exception in convert to APO. Reason: ${exception.getMessage}")
          throw new Exception(s"Exception in convert to APO. Reason: ${exception.getMessage}.Trace: ${sw.toString}")
        }
        null
    }
  }

  def convertSource(pdsId: String, apo: patient.Patient, apoSource: patient.Source): SourceProto = {
    Option(apoSource).map(_ => {
      val builder = SourceProto.newBuilder()

      if (nullToEmpty(apoSource.getSourceSystem).trim.nonEmpty) builder.setSystem(apoSource.getSourceSystem)
      if (nullToEmpty(apoSource.getSourceType).trim.nonEmpty) builder.setType(apoSource.getSourceType)
      if (apoSource.getCreationDate != null) builder.setCreationDate(apoSource.getCreationDate.getMillis)
      if (apoSource.getOrganization != null) builder.setOrganization(convertOrganization(apoSource.getOrganization))


      // start with parsing details so we can pass through internalId
      val parsingDetailsId = getNewParsingDetailsId(apo, apoSource)
      if (parsingDetailsId != null) builder.setParsingDetailsId(parsingDetailsId)

      // Note: This isn't codedBaseObject, it has a separate property
      val actorId = apoSource.getClinicalActorId
      if (actorId != null) {
        converterContext.actorsMap.get(apoSource.getClinicalActorId) match {
          case Some(clinicalActor) =>
            builder.setClinicalActorId(clinicalActor.getInternalId)
          case None =>
            // ClinicalActor.sourceId depends on Source internalId but Source internal Id depends on clinicalActor Internal Id
            // Note: ClinicalActor InternalId does *not* depend on sourceId
            // Sometimes source.sourceId != source.internalUUID :face_with_rolling_eyes
            val cycleDetected = List(apoSource.getSourceId, apoSource.getInternalUUID).contains(apo.getClinicalActorById(actorId).getSourceId)
            // if cycle detected, just compute the new id for clinical actor instead of optimistic conversion

            val ogClinicalActorId = apoSource.getClinicalActorId
            if (cycleDetected) {
              val id: UUIDOuterClass.UUID = getNewClinicalActorId(apo, ogClinicalActorId)
              builder.setClinicalActorId(id)
            } else {
              val clinicalActorInfo = convertClinicalActorById(pdsId, apo, ogClinicalActorId)
              if (clinicalActorInfo != null) builder.setClinicalActorId(clinicalActorInfo.getInternalId)
              converterContext.addActor(ogClinicalActorId, clinicalActorInfo)
            }
        }
      }
      InternalUUIDUtils.setInternalId(builder)
    }).orNull
  }

  def convertSourceById(pdsId: String, apo: patient.Patient, sourceId: UUID): SourceProto = {
    Option(sourceId).map(_ => {

      val apoSource = apo.getSourceById(sourceId)
      convertSource(pdsId, apo, apoSource)
    }).orNull
  }

  def convertEncounter(pdsId: String, apo: patient.Patient, apoEncounter: patient.Encounter): Encounter = {
    Option(apoEncounter).map(_ => {

      val builder = Encounter.newBuilder()

      // Datacatalog Meta
      val oid = PatientUtility.getSourceFileArchiveUUID(apo).toString
      val dataCatalogMeta = convertDataCatalogMeta(pdsId, apoEncounter, oid)
      builder.setDataCatalogMeta(dataCatalogMeta)

      // start with parsing details so we can pass through internalId
      val parsingDetailsId = getNewParsingDetailsId(apo, apoEncounter)
      if (parsingDetailsId != null) builder.addParsingDetailsIds(parsingDetailsId)

      // then do source so we can pass through internalId
      val sourceId = getNewSourceId(pdsId, apo, apoEncounter)
      if (sourceId != null) builder.addSourceIds(sourceId)

      // Primary Actor
      val primaryActorId = getNewPrimaryActorId(pdsId, apo, apoEncounter)
      if (primaryActorId != null) builder.setPrimaryActorId(primaryActorId)

      // Supplementary Actors
      val supplementaryActorIds = getNewSupplementaryActorIds(pdsId, apo, apoEncounter)
      if (supplementaryActorIds != null) builder.addAllSupplementaryActorIds(supplementaryActorIds.asJava)

      //see if we can fetch care site from top level apo.careSites
      val careSiteToUse: patient.CareSite = if (apoEncounter.getSiteOfService != null
        && apoEncounter.getSiteOfService.getCareSiteId != null
        && apo.getCareSiteById(apoEncounter.getSiteOfService.getCareSiteId) != null) {
        apo.getCareSiteById(apoEncounter.getSiteOfService.getCareSiteId)
      } else
        apoEncounter.getSiteOfService
      val convertedCareSite = convertCareSite(careSiteToUse)
      val encounterInfo = convertEncounterInfo(apoEncounter, convertedCareSite)
      if (encounterInfo != null) builder.setEncounterInfo(encounterInfo)

      InternalUUIDUtils.setInternalId(builder.build())
    }).orNull
  }

  def convertEncounterById(pdsId: String, apo: patient.Patient, encounterId: UUID): Encounter = {
    Option(encounterId).map(_ => {
      val encounter = apo.getEncounterById(encounterId)
      convertEncounter(pdsId, apo, encounter)
    }).orNull
  }

  def convertClinicalActor(pdsId: String, apo: patient.Patient, apoActor: patient.ClinicalActor): ClinicalActor = {
    val caBuilder = ClinicalActor.newBuilder()
    val baseObj: Base = createBase(pdsId, apo, apoActor)

    val actorInfo = convertClinicalActorInfo(apoActor)
    if (actorInfo != null) caBuilder.setClinicalActorInfo(actorInfo)

    caBuilder.setBase(baseObj)
    InternalUUIDUtils.setInternalId(caBuilder)
  }

  def getNewClinicalActorId(apo: patient.Patient, clinicalActorId: UUID): UUIDOuterClass.UUID = {
    converterContext.actorsMap.get(clinicalActorId) match {
      case Some(actor) => actor.getInternalId
      case None =>
        getNewClinicalActorId(apo.getClinicalActorById(clinicalActorId))
    }
  }

  def getNewClinicalActorId(clinicalActor: patient.ClinicalActor): UUIDOuterClass.UUID = {
    val clinicalActorInfo = convertClinicalActorInfo(clinicalActor)
    val id = IdentityFunctions.getIdentity(clinicalActorInfo)
    IdentityFunctions.identityToUUID(id)
  }

  // Find the APOActor and convert if it exists
  def convertClinicalActorById(pdsId: String, apo: patient.Patient, clinicalActorId: UUID): ClinicalActor = {
    Option(clinicalActorId).flatMap(_ => {
      val apoActor = apo.getClinicalActorById(clinicalActorId)
      Option(apoActor).map(_ => {
        convertClinicalActor(pdsId, apo, apoActor)
      })
    }).orNull
  }

  def convertDemographics(pdsId: String, apo: patient.Patient, demographics: Demographics): PatientDemographics = {
    Option(demographics).map(_ => {
      val builder = PatientDemographics.newBuilder()

      val demographicsInfo = DemographicsInfoProto.DemographicsInfo.newBuilder()
      Option(demographics.getName).foreach(n => demographicsInfo.setName(convertName(n)))
      Option(demographics.getGender).foreach(n => demographicsInfo.setGender(convertGender(n)))

      Option(demographics.getDateOfBirth).foreach(n => demographicsInfo.setDob(fromDateTime(n)))
      Option(demographics.getDateOfDeath).foreach(n => demographicsInfo.setDod(fromDateTime(n)))

      Option(demographics.getRace).foreach(n => demographicsInfo.setRace(convertClinicalCode(n)))
      Option(demographics.getEthnicity).foreach(n => demographicsInfo.setEthnicity(convertClinicalCode(n)))

      if (demographics.getLanguages.nonEmpty)
        demographicsInfo.addAllLanguages(demographics.getLanguages)

      Option(demographics.getReligiousAffiliation).foreach(demographicsInfo.setReligiousAffiliation)
      Option(demographics.getMaritalStatus).foreach(n => demographicsInfo.setMartialStatus(convertMaritalStatus(n)))

      // Currently loading PCP as an AXM ExternalId in demographics metadata
      Option(demographics.getMetaTag(DemographicsMetadataKeys.PRIMARY_CARE_PROVIDER.toString)).foreach(raw => {
        val parts = raw.split("\\^").filter(_.nonEmpty).take(2)
        if (parts.length == 2) {
          val pcp = ClinicalCodeOuterClass.ClinicalCode.newBuilder().setCode(parts(0)).setSystem(parts(1)).build()
          demographicsInfo.setPrimaryCareProvider(pcp)
        }
      })

      builder.setDemographicsInfo(demographicsInfo)

      val baseObj: Base = createBase(pdsId, apo, demographics)


      builder.setBase(baseObj)
      builder.build()
    }).orNull
  }

  def convertAllergy(pdsId: String,
                     allergy: Allergy,
                     apo: patient.Patient): CodedBaseObjects.Allergy = {
    Option(allergy).map(_ => {

      val allergyCboBuilder = CodedBaseObjects.Allergy.newBuilder()
      val oid = PatientUtility.getSourceFileArchiveUUID(apo).toString
      val dataCatalogMeta = convertDataCatalogMeta(pdsId, allergy, oid)

      val allergyInfo = convertAllergyInfo(allergy)
      Option(allergyInfo).foreach(allergyCboBuilder.setAllergyInfo)

      // start with parsing details so we can pass through internalId
      val parsingDetailsId = getNewParsingDetailsId(apo, allergy)

      // then do source so we can pass through internalId
      val sourceId = getNewSourceId(pdsId, apo, allergy)

      // Primary Actor
      val primaryActorId = getNewPrimaryActorId(pdsId, apo, allergy)

      // Supplementary Actors
      val supplementaryActorIds = getNewSupplementaryActorIds(pdsId, apo, allergy)

      // Encounter
      val encounter = convertEncounterById(pdsId, apo, allergy.getSourceEncounter)

      val codedBase = buildCodedBase(parsingDetailsId, sourceId, primaryActorId,
        supplementaryActorIds, encounter,
        dataCatalogMeta)

      allergyCboBuilder.setBase(codedBase)
      allergyCboBuilder.build()
    }).orNull
  }

  def convertBiometricValue(pdsId: String,
                            bv: BiometricValue,
                            apo: patient.Patient): CodedBaseObjects.BiometricValue = {
    Option(bv).map(_ => {

      val builder = CodedBaseObjects.BiometricValue.newBuilder()

      val oid = PatientUtility.getSourceFileArchiveUUID(apo).toString
      val dataCatalogMeta = convertDataCatalogMeta(pdsId, bv, oid)

      // start with parsing details so we can pass through internalId
      val parsingDetailsId = getNewParsingDetailsId(apo, bv)

      // then do source so we can pass through internalId
      val sourceId = getNewSourceId(pdsId, apo, bv)

      // Primary Actor
      val primaryActorId = getNewPrimaryActorId(pdsId, apo, bv)

      // Supplementary Actors
      val supplementaryActorIds = getNewSupplementaryActorIds(pdsId, apo, bv)

      // Encounter
      val encounter = convertEncounterById(pdsId, apo, bv.getSourceEncounter)

      val codedBase = buildCodedBase(parsingDetailsId, sourceId, primaryActorId,
        supplementaryActorIds, encounter,
        dataCatalogMeta)

      builder.setVitalSign(convertVitalSign(bv))
      builder.setBase(codedBase)
      builder.build()
    }).orNull
  }

  def convertSocialHistory(pdsId: String,
                           sh: patient.SocialHistory,
                           apo: patient.Patient): CodedBaseObjects.SocialHistory = {
    Option(sh).map(_ => {

      val builder = CodedBaseObjects.SocialHistory.newBuilder()

      val oid = PatientUtility.getSourceFileArchiveUUID(apo).toString
      val dataCatalogMeta = convertDataCatalogMeta(pdsId, sh, oid)


      // start with parsing details so we can pass through internalId
      val parsingDetailsId = getNewParsingDetailsId(apo, sh)

      // then do source so we can pass through internalId
      val sourceId = getNewSourceId(pdsId, apo, sh)

      // Primary Actor
      val primaryActorId = getNewPrimaryActorId(pdsId, apo, sh)

      // Supplementary Actors
      val supplementaryActorIds = getNewSupplementaryActorIds(pdsId, apo, sh)

      // Encounter
      val encounter = convertEncounterById(pdsId, apo, sh.getSourceEncounter)

      val codedBase = buildCodedBase(parsingDetailsId, sourceId, primaryActorId,
        supplementaryActorIds, encounter,
        dataCatalogMeta)

      builder.setSocialHistoryInfo(convertSocialHistoryInfo(sh))
      builder.setBase(codedBase)
      builder.build()
    }).orNull
  }

  def convertLabResults(pdsId: String,
                        labResult: patient.LabResult,
                        apo: patient.Patient): CodedBaseObjects.LabResultCBO = {
    Option(labResult).map(_ => {
      val builder = CodedBaseObjects.LabResultCBO.newBuilder()
      builder.setBase(createCodedBase(pdsId, labResult, apo))
      builder.setLabResultInfo(getLabResultInfo(apo, labResult))
      builder.build()
    }).orNull
  }

  def convertPrescription(pdsId: String,
                          prescription: patient.Prescription,
                          apo: patient.Patient): CodedBaseObjects.PrescriptionCBO = {
    Option(prescription).map(_ => {
      val prescriptionCboBuilder = CodedBaseObjects.PrescriptionCBO.newBuilder()
      prescriptionCboBuilder.setPrescriptionInfo(getPrescriptionInfo(prescription))
      prescriptionCboBuilder.setBase(createCodedBase(pdsId, prescription, apo))
      prescriptionCboBuilder.build()
    }).orNull
  }

  def convertFamilyHistory(pdsId: String, familyHistory: patient.FamilyHistory, apo: patient.Patient): CodedBaseObjects.FamilyHistoryCBO = {
    Option(familyHistory).map(_ => {
      val fhProtoBuilder = CodedBaseObjects.FamilyHistoryCBO.newBuilder()
      fhProtoBuilder.setFamilyHistoryInfo(getFamilyHistoryInfo(familyHistory))
      fhProtoBuilder.setBase(createCodedBase(pdsId, familyHistory, apo))
      fhProtoBuilder.build()
    }).orNull
  }

  def convertImmunization(pdsId: String, administration: patient.Administration, apo: patient.Patient): CodedBaseObjects.ImmunizationCBO = {
    Option(administration).map(_ => {
      val adminImmuProtoBuilder = CodedBaseObjects.ImmunizationCBO.newBuilder()
      adminImmuProtoBuilder.setImmunizationInfo(getImmunizationInfo(administration))
      adminImmuProtoBuilder.setBase(createCodedBase(pdsId, administration, apo))
      adminImmuProtoBuilder.build()
    }).orNull
  }

  /**
   * Create the base of a proto coded base object from CodedBaseObject
   * @param pdsId pdsId
   * @param codedBaseObject patient.CodedBaseObject
   * @param apo patient.Patient
   * @return
   */
  private def createCodedBase(pdsId: String, codedBaseObject: CodedBaseObject, apo: Patient): CodedBase = {

    val builder = CodedBaseObjects.CodedBase.newBuilder()

    val oid = PatientUtility.getSourceFileArchiveUUID(apo).toString
    builder.setDataCatalogMeta(convertDataCatalogMeta(pdsId, codedBaseObject, oid))

    // start with parsing details so we can pass through internalId
    val parsingDetailsId = getNewParsingDetailsId(apo, codedBaseObject)
    if (parsingDetailsId != null) builder.addParsingDetailsIds(parsingDetailsId)

    // then do source so we can pass through internalId
    val sourceId = getNewSourceId(pdsId, apo, codedBaseObject)
    if (sourceId != null) builder.addSourceIds(sourceId)

    // Primary Actor
    val primaryActorId = getNewPrimaryActorId(pdsId, apo, codedBaseObject)
    if (primaryActorId != null) builder.setPrimaryActorId(primaryActorId)

    // Supplementary Actors
    val supplementaryActorIds = getNewSupplementaryActorIds(pdsId, apo, codedBaseObject)
    builder.addAllSupplementaryActorIds(supplementaryActorIds.asJava)

    // Encounter
    val encounter = convertEncounterById(pdsId, apo, codedBaseObject.getSourceEncounter)
    if (encounter != null) builder.addEncounterIds(encounter.getInternalId)

    builder.build()
  }

  def convertCoverage(pdsId: String, apo: patient.Patient, coverage: patient.Coverage): BaseObjects.Coverage = {
    Option(coverage).map(_ => {
      val builder = BaseObjects.Coverage.newBuilder()

      val baseObj: Base = createBase(pdsId, apo, coverage)
      val coverageInfo = convertCoverageInfo(apo, coverage)

      Option(baseObj).foreach(builder.setBase)
      Option(coverageInfo).foreach(builder.setCoverageInfo)

      builder.build()
    }).orNull
  }

  def convertContactDetails(pdsId: String, apo: patient.Patient,
                            contactDetails: patient.ContactDetails): ContactDetails = {
    Option(contactDetails).map(_ => {
      val builder = ContactDetails.newBuilder()

      // Contact Info
      val contactInfo = convertContactDetailInfo(contactDetails)
      if (contactInfo != null) builder.setContactInfo(contactInfo)

      val baseObj: Base = createBase(pdsId, apo, contactDetails)

      builder.setBase(baseObj)
      builder.build()
    }).orNull
  }

  private def createBase(pdsId: String, apo: Patient, apoBaseObject: BaseObject): Base = {
    val oid = PatientUtility.getSourceFileArchiveUUID(apo).toString
    val meta = convertDataCatalogMeta(pdsId, apoBaseObject, oid)
    // start with parsing details so we can pass through internalId
    val parsingDetailsId = getNewParsingDetailsId(apo, apoBaseObject)

    // then do source so we can pass through internalId
    val sourceId = getNewSourceId(pdsId, apo, apoBaseObject)

    BaseEntityUtil.buildBaseObject(parsingDetailsId, sourceId, meta)
  }

  def getNewParsingDetailsId(apo: patient.Patient, baseObject: BaseObject): UUIDOuterClass.UUID = {
    // start with parsing details so we can pass through internalId
    Option(baseObject.getParsingDetailsId).map(id => {
      converterContext.parsingDetailsMap.get(id) match {
        case Some(parsingDetails) =>
          parsingDetails.getInternalId
        case None =>
          val parsingDetails = convertParsingDetailsById(apo, baseObject.getParsingDetailsId)
          if (parsingDetails != null) {
            converterContext.addParsingDetails(baseObject.getParsingDetailsId, parsingDetails)
            parsingDetails.getInternalId
          } else {
            null
          }
      }
    }).orNull
  }

  def getNewSource(pdsId: String, apo: patient.Patient, baseObject: BaseObject): SourceProto = {
    Option(baseObject.getSourceId).map(id => {
      converterContext.sourcesMap.get(id) match {
        case Some(source) =>
          source
        case None =>
          val source = convertSourceById(pdsId, apo, baseObject.getSourceId)
          Option(source).map(_ => {
            converterContext.addSource(baseObject.getSourceId, source)
            source
          }).orNull
      }
    }).orNull
  }

  def getNewSourceId(pdsId: String, apo: patient.Patient, baseObject: BaseObject): UUIDOuterClass.UUID = {
    Option(getNewSource(pdsId, apo, baseObject)).map(_.getInternalId).orNull
  }

  def getNewPrimaryActorId(pdsId: String, apo: patient.Patient,
                           codedBaseObject: CodedBaseObject): UUIDOuterClass.UUID = {
    if (codedBaseObject.getPrimaryClinicalActorId != null) {
      converterContext.actorsMap.get(codedBaseObject.getPrimaryClinicalActorId) match {
        case Some(clinicalActor) =>
          clinicalActor.getInternalId
        case None =>
          // ClinicalActor.sourceId depends on Source internalId but Source internal Id depends on clinicalActor Internal Id
          // Note: ClinicalActor InternalId does *not* depend on sourceId
          // Convert clinical actor -> get clinical actor id -> convert source -> convert clinical actor again with correct sourceId?
          // Also Note: Extreme edge case
          val clinicalActorInfo = convertClinicalActorById(pdsId, apo, codedBaseObject.getPrimaryClinicalActorId)
          if (clinicalActorInfo != null) clinicalActorInfo.getInternalId else null
      }
    } else {
      null
    }
  }

  def getNewSupplementaryActorIds(pdsId: String, apo: patient.Patient,
                                  codedBaseObject: CodedBaseObject): Seq[UUIDOuterClass.UUID] = {
    //add supplementary actors
    codedBaseObject.getSupplementaryClinicalActorIds.asScala.flatMap(cId => {
      converterContext.actorsMap.get(cId) match {
        case Some(clinicalActor) =>
          Some(clinicalActor.getInternalId)
        case None =>
          // ClinicalActor.sourceId depends on Source internalId but Source internal Id depends on clinicalActor Internal Id
          // Note: ClinicalActor InternalId does *not* depend on sourceId
          // Convert clinical actor -> get clinical actor id -> convert source -> convert clinical actor again with correct sourceId?
          // Also Note: Extreme edge case
          val clinicalActorInfo = convertClinicalActorById(pdsId, apo, cId)
          if (clinicalActorInfo != null) Some(clinicalActorInfo.getInternalId) else None
      }
    })
  }

  /**
   * Convert Document Content Meta using converter Context
   *
   * @param documentContent Document Content Meta proto
   * @return
   */
  def convertDocumentContentMeta(documentContent: DocumentContent): DocumentContentOuterClass.DocumentContent = {
    val contentMetaBuilder = DocumentContentOuterClass.DocumentContent.newBuilder()
    Option(documentContent.getLength).foreach(contentMetaBuilder.setLength)
    Option(documentContent.getHash).foreach(contentMetaBuilder.setHash)
    Option(documentContent.getMimeType).foreach(contentMetaBuilder.setMimeType)
    Option(documentContent.getUri).foreach(uri => contentMetaBuilder.setUri(uri.toASCIIString))

    // Source Id
    converterContext.sourcesMap.get(documentContent.getSourceId)
      .map(_.getInternalId)
      .foreach(contentMetaBuilder.setSourceId)
    // Parsing Details Id
    converterContext.parsingDetailsMap.get(documentContent.getParsingDetailsId)
      .map(_.getInternalId)
      .foreach(contentMetaBuilder.setParsingDetailsId)
    contentMetaBuilder.build()
  }

  def convertDocMetadata(pdsId: String, document: Document, apo: Patient): CodedBaseObjects.DocumentMetaCBO = {
    val docMetaBuilder = DocumentMeta.newBuilder()
    val metadata = document.getMetadata.asScala.toMap

    if (document.getInternalUUID != null) docMetaBuilder.setUuid(convertUuid(document.getInternalUUID))
    if (ArgumentUtil.isNotEmpty(document.getDocumentTitle)) docMetaBuilder.setDocumentTitle(document.getDocumentTitle)
    if (document.getDocumentDate != null) docMetaBuilder.setDocumentDate(fromDateTime(document.getDocumentDate))

    Option(document.getDocumentDate).foreach(
      d => docMetaBuilder.setDocumentDate(fromDateTime(d))
    )

    // code and code translations
    val code = convertCode(document)
    if (code != null) docMetaBuilder.setCode(code)

    val codeTranslations = convertCodeTranslations(document)
    docMetaBuilder.addAllCodeTranslations(codeTranslations.asJava)


    // Top level Metadata
    {
      metadata.get(DocMetadataKeys.DATE_ASSUMED.toString).foreach(da => docMetaBuilder.setDateAssumed(ArgumentUtil.toBoolean(da)))
      metadata.get(DocMetadataKeys.DOCUMENT_TYPE.toString).foreach(docMetaBuilder.setDocumentType)
    }

    // Content Meta
    document.getDocumentContents.foreach(content => {
      val convertedProto = convertDocumentContentMeta(content)
      if (convertedProto.getSerializedSize > 0)
        docMetaBuilder.addDocumentContents(convertedProto)
    })

    // Encounter Metadata
    val encounterMetadata: EncounterMetadata = convertEncounterMetadata(metadata)
    if (encounterMetadata.getSerializedSize > 0) docMetaBuilder.setEncounterMeta(encounterMetadata)

    // OCR Metadata [And legacy content extract metadata]
    val ocrMetadata: OcrMetadata = convertOcrMetadata(metadata)
    if (ocrMetadata.getSerializedSize > 0) docMetaBuilder.setOcrMetadata(ocrMetadata)

    // Content Metadata
    val contentMetadata: ContentMetadata = convertContentMetadata(metadata)
    if (contentMetadata.getSerializedSize > 0) docMetaBuilder.setContentMeta(contentMetadata)

    // Health Plan Source Metadata
    val healthPlanMetadata: HealthPlanMetadata = convertHealthPlanMetadata(metadata)
    if (healthPlanMetadata.getSerializedSize > 0) docMetaBuilder.setHealthPlanMeta(healthPlanMetadata)

    val docMeta = docMetaBuilder.build()
    val codedBase = createCodedBase(pdsId, document, apo)

    val builder = CodedBaseObjects.DocumentMetaCBO.newBuilder()
    builder.setBase(codedBase)
    builder.setDocumentMeta(docMeta)
    builder.build()
  }

  /**
   * Create a list of unstructured content protos for each document that has string content
   * @param patient apo patient object
   * @return
   */
  def generateExtractedText(patient: Patient, pdsId: String): Iterable[ExtractedTextOuterClass.RawExtractedText] = {
    patient.getDocuments.flatMap(doc => {
      fromExtractedText(pdsId, doc, patient) ++ fromStringContent(pdsId, doc, patient)
    })
  }

  /**
   * If a document has textexctracted, then create Extracted Text Proto
   * @param document document object
   * @return
   */
  def fromExtractedText(pdsId: String, document: Document, patient: Patient): Option[ExtractedTextOuterClass.RawExtractedText] = {
    Option(document.getMetaTag("textextracted"))
      .filterNot(_.isEmpty)
      .filterNot(_.toUpperCase == "NOT_IMPLEMENTED") // legacy from cv2 ocr
      .map(extractedText => {
        val converted = convertDocMetadata(pdsId, document, patient)
        val builder = ExtractedTextOuterClass.RawExtractedText.newBuilder()
        builder.setSourceId(getNewSourceId(pdsId, patient, document))
        builder.setDocumentId(converted.getDocumentMeta.getUuid)
        builder.setStringContent(ByteString.copyFrom(extractedText.getBytes(StandardCharsets.UTF_8)))
        builder.setTextType(ExtractedTextOuterClass.TextType.TEXT_EXTRACTED)
        builder.build()
      })
  }

  /**
   * If a document has string content, then create Extracted Text Proto
   * @param document document object
   * @return
   */
  def fromStringContent(pdsId: String, document: Document, patient: Patient): Option[ExtractedTextOuterClass.RawExtractedText] = {
    Option(document.getStringContent)
      .filter(_.nonEmpty)
      .map(stringContent => {
        val converted = convertDocMetadata(pdsId, document, patient)
        val builder = ExtractedTextOuterClass.RawExtractedText.newBuilder()
        builder.setSourceId(getNewSourceId(pdsId, patient, document))
        builder.setDocumentId(converted.getDocumentMeta.getUuid)
        builder.setStringContent(ByteString.copyFrom(stringContent.getBytes(StandardCharsets.UTF_8)))
        builder.setTextType(ExtractedTextOuterClass.TextType.STRING_CONTENT)
        builder.build()
      })
  }

  def createCodedBase(pdsId: String,
                      apo: Patient,
                      codedBaseObject: CodedBaseObject): CodedBase = {
    // start with parsing details so we can pass through internalId
    val parsingDetailsId = getNewParsingDetailsId(apo, codedBaseObject)

    // then do source so we can pass through internalId
    val sourceId = getNewSourceId(pdsId, apo, codedBaseObject)

    // Primary Actor
    val primaryActorId = getNewPrimaryActorId(pdsId, apo, codedBaseObject)

    // Supplementary Actors
    val supplementaryActorIds = getNewSupplementaryActorIds(pdsId, apo, codedBaseObject)

    //add encounter
    val encounter = convertEncounterById(pdsId, apo, codedBaseObject.getSourceEncounter)

    //meta
    val oid = PatientUtility.getSourceFileArchiveUUID(apo).toString
    val dataCatalogMeta = convertDataCatalogMeta(pdsId, codedBaseObject, oid)

    val codedBase = buildCodedBase(parsingDetailsId, sourceId, primaryActorId, supplementaryActorIds, encounter,
      dataCatalogMeta)
    codedBase
  }

}

// Static APO Conversions
object ApoToProtoConverter {

  // Set as active and current timestamp
  def createDataCatalogMeta(pdsId: String, oid: String): DataCatalogMeta = {
    val builder = DataCatalogMeta.newBuilder()
      .setPdsId(pdsId)
      .setEditType(EditType.ACTIVE)
      .setLastEditTime(DateTime.now().getMillis)
      .setOid(oid)
    builder.build()
  }

  def convertParsingDetailsById(apo: patient.Patient, parsingDetailsId: UUID): ParsingDetails = {
    Option(parsingDetailsId).map(_ => {
      val apoParsingDetails = apo.getParsingDetailById(parsingDetailsId)
      convertParsingDetails(apo, apoParsingDetails)
    }).orNull
  }

  def convertParsingDetails(apo: patient.Patient, apoParsingDetails: patient.ParsingDetail): ParsingDetails = {
    Option(apoParsingDetails).map(_ => {
      val builder = ParsingDetails.newBuilder()
      val sourceFileUUID = PatientUtility.getSourceFileArchiveUUID(apo)
      if (sourceFileUUID != null) builder.setSourceFileArchiveUUID(convertUuid(sourceFileUUID))

      if (nullToEmpty(apoParsingDetails.getSourceUploadBatch).trim.nonEmpty)
        builder.setUploadBatchId(apoParsingDetails.getSourceUploadBatch)

      if (nullToEmpty(apoParsingDetails.getSourceFileHash).trim.nonEmpty)
        builder.setSourceFileHash(apoParsingDetails.getSourceFileHash)

      if (nullToEmpty(apoParsingDetails.getParserVersion).trim.nonEmpty)
        builder.setVersion(apoParsingDetails.getParserVersion)

      if (apoParsingDetails.getParsingDateTime != null)
        builder.setParsingDate(apoParsingDetails.getParsingDateTime.getMillis)

      if (apoParsingDetails.getParser != null)
        builder.setParserType(apoParsingDetails.getParser.toString)

      InternalUUIDUtils.setInternalId(builder)
    }).orNull
  }

  def convertCareSite(apoCareSite: patient.CareSite): CareSite = {
    Option(apoCareSite).map(_ => {


      val builder = CareSite.newBuilder()
      if (nullToEmpty(apoCareSite.getCareSiteName).trim.nonEmpty) builder.setName(apoCareSite.getCareSiteName)
      if (apoCareSite.getCareSiteType != null) builder.setType(apoCareSite.getCareSiteType.name())
      if (apoCareSite.getAddress != null) builder.setAddress(convertAddress(apoCareSite.getAddress))

      builder.build()
    }).orNull
  }

  /**
   * Convert a clinical code from java to proto
   * @param code Clinical Code
   * @param validateCode if true, return null for invalid codes
   * @param trimModifiers If true, trim the CPT modifiers from the code [bad legacy data for ffs claims]
   * @return
   */
  def convertClinicalCode(code: patient.ClinicalCode, validateCode: Boolean = false, trimModifiers: Boolean = false): ClinicalCodeProto = {
    lazy val isInvalid: Boolean = isNullOrEmpty(code.getCode) || isNullOrEmpty(code.getCodingSystem) || isNullOrEmpty(code.getCodingSystemOID) // copied from Validator.java in Loader

    Option(code).filterNot(_ => {
      validateCode && isInvalid
    }).map(_ => {
      val builder = ClinicalCodeProto.newBuilder()
      if (nullToEmpty(code.getCode).trim.nonEmpty) {
        // EN-11284: We were not stripping the modifiers from the cpt codes for 4 months
        if (trimModifiers && code.getCodingSystem.contains("CPT")) {
          val codeArray = code.getCode.split("-")
          if (codeArray.nonEmpty) {
            builder.setCode(codeArray.head)
          }
        } else {
          builder.setCode(code.getCode)
        }
      }
      if (nullToEmpty(code.getDisplayName).trim.nonEmpty) builder.setDisplayName(code.getDisplayName.trim)
      if (nullToEmpty(code.getCodingSystem).trim.nonEmpty) builder.setSystem(code.getCodingSystem.trim)
      if (nullToEmpty(code.getCodingSystemOID).trim.nonEmpty) builder.setSystemOid(code.getCodingSystemOID.trim)
      if (nullToEmpty(code.getCodingSystemVersions).trim.nonEmpty) builder.setSystemVersion(code.getCodingSystemVersions.trim)
      builder.build()
    }).orNull
  }

  def convertExternalId(externalId: patient.ExternalID): ExternalId = {
    Option(externalId).map(_ => {
      val builder = ExternalId.newBuilder()
      if (nullToEmpty(externalId.getId).trim.nonEmpty) builder.setId(externalId.getId)
      if (nullToEmpty(externalId.getAssignAuthority).trim.nonEmpty) builder
        .setAssignAuthority(externalId.getAssignAuthority)
      builder.build()
    }).orNull
  }

  def createPatientMeta(apo: patient.Patient): PatientMeta = {
    Option(apo).map(_ => {
      val builder = PatientMeta.newBuilder()
      if (apo.getPatientId != null) builder.setPatientId(convertUuid(apo.getPatientId))

      // found bug in CCDA parser where primaryId isn't inside the externalId list
      Option(apo.getPrimaryExternalID).foreach(id => {
        builder.setPrimaryExternalId(convertExternalId(id))
        builder.addExternalIds(convertExternalId(id))
      })

      apo.getExternalIDs.map(convertExternalId).foreach(id => {
        if (!builder.getPrimaryExternalId.equals(id))
          builder.addExternalIds(id)
      })

      builder.build()
    }).orNull
  }

  def convertCoverageInfo(apo: patient.Patient, coverage: patient.Coverage): CoverageInfoOuterClass.CoverageInfo = {
    Option(coverage).map(_ => {
      val builder = CoverageInfoOuterClass.CoverageInfo.newBuilder()

      Option(coverage.getSequenceNumber).foreach(builder.setSequenceNumber)
      Option(coverage.getStartDate).foreach(d => builder.setStartDate(fromLocalDate(d)))
      Option(coverage.getEndDate).foreach(d => builder.setEndDate(fromLocalDate(d)))

      Option(coverage.getType).foreach(typ => builder.setCoverageType(typ.toString))
      Option(coverage.getHealthPlanName).foreach(builder.setHealthPlanName)

      Option(coverage.getGroupNumber).foreach(n => builder.setGroupNumber(convertExternalId(n)))
      Option(coverage.getSubscriberID).foreach(id => builder.setSubscriberNumber(convertExternalId(id)))
      Option(coverage.getBeneficiaryID).foreach(id => builder.setBeneficiaryId(convertExternalId(id)))

      Option(apo.getSourceById(coverage.getSourceId)).foreach(source => {
        Option(source.getDciStart).foreach(d => builder.setDciStartDate(fromLocalDate(d)))
        Option(source.getDciEnd).foreach(d => builder.setDciEndDate(fromLocalDate(d)))
      })

      builder.build()
    }).orNull
  }

  def convertContactDetailInfo(contactDetails: patient.ContactDetails): ContactInfo = {
    Option(contactDetails).map(_ => {
      val builder = ContactInfo.newBuilder()
      if (nullToEmpty(contactDetails.getPrimaryEmail).trim.nonEmpty) builder
        .setPrimaryEmail(contactDetails.getPrimaryEmail)
      if (ArgumentUtil.isNotEmpty(contactDetails.getAlternateEmails)) builder.addAllAlternateEmails(contactDetails
        .getAlternateEmails)
      if (contactDetails.getPrimaryAddress != null) builder.setAddress(convertAddress(contactDetails.getPrimaryAddress))
      if (ArgumentUtil.isNotEmpty(contactDetails.getAlternateAddresses)) builder
        .addAllAlternateAddresses(contactDetails.getAlternateAddresses.map(convertAddress).asJava)
      if (contactDetails.getPrimaryPhone != null) builder
        .setPrimaryPhone(convertPhoneNumber(contactDetails.getPrimaryPhone))
      if (ArgumentUtil.isNotEmpty(contactDetails.getAlternatePhones)) builder
        .addAllAlternatePhones(convertAllPhoneNumbers(contactDetails.getAlternatePhones).asJava)

      builder.build()
    }).orNull
  }


  def convertHealthPlanMetadata(metadata: Map[String, String]): HealthPlanMetadata = {
    val builder = HealthPlanMetadata.newBuilder()
    metadata.get(DocMetadataKeys.ALTERNATE_CHART_ID.toString)
      .foreach(id => builder.setAlternateChartId(AxmUtils.createExternalId(id))) // todo 2 carets?
    metadata.get(DocMetadataKeys.CHART_TYPE.toString).foreach(builder.setChartType)
    builder.build()
  }

  def convertOcrMetadata(documentMetadata: Map[String, String]): OcrMetadata = {
    val builder = OcrMetadata.newBuilder()
    documentMetadata.get(DocMetadataKeys.OCR_STATUS.toString).foreach(builder.setStatus)
    documentMetadata.get(DocMetadataKeys.OCR_RESOLUTION.toString).foreach(builder.setResolution)
    documentMetadata.get(DocMetadataKeys.OCR_CHILD_EXIT_CODE.toString).foreach(c => builder.setChildExitCode(c.toInt))
    documentMetadata.get(DocMetadataKeys.OCR_CHILD_EXIT_CODE_STR.toString).foreach(builder.setChildExitCodeStr)
    documentMetadata.get(DocMetadataKeys.OCR_TIMSTAMP.toString).foreach(ts => builder.setTimestampMs(ts.toLong))
    documentMetadata.get(DocMetadataKeys.GS_VERSION.toString).foreach(builder.setGsVersion)

    documentMetadata.get(DocMetadataKeys.TOTAL_PAGES.toString).foreach(count => builder.setTotalPages(wrapInt(count.toInt)))
    documentMetadata.get(DocMetadataKeys.AVAILABLE_PAGES.toString).foreach(count => builder.setAvailablePages(wrapInt(count.toInt)))
    builder.build()
  }

  def convertContentMetadata(documentMetadata: Map[String, String]): ContentMetadata = {
    val builder = ContentMetadata.newBuilder()

    // Content Extract
    documentMetadata.get(DocMetadataKeys.STRING_CONTENT_TS.toString).foreach(ts => builder.setStringContentMs(wrapLong(ts.toLong)))
    documentMetadata.get(DocMetadataKeys.TEXT_EXTRACTED_TS.toString).foreach(ts => builder.setTextExtractMs(wrapLong(ts.toLong)))

    // Deprecated Content Extract
    documentMetadata.get(DocMetadataKeys.DOC_CACHE_LEVEL.toString).foreach(builder.setDocCacheLevel)
    documentMetadata.get(DocMetadataKeys.DOC_CACHE_FORMAT.toString).foreach(builder.setDocCacheFormat)
    documentMetadata.get(DocMetadataKeys.DOC_CACHE_TS.toString).foreach(ts => builder.setDocCacheTsMs(wrapLong(ts.toLong)))

    documentMetadata.get(DocMetadataKeys.PIPELINE_VERSION.toString).foreach(builder.setPipelineVersion)

    builder.build()
  }

  def convertEncounterMetadata(documentMetadata: Map[String, String]): EncounterMetadata = {
    val builder = EncounterMetadata.newBuilder()
    documentMetadata.get(DocMetadataKeys.DOCUMENT_FORMAT.toString).foreach(builder.setDocumentFormat)
    documentMetadata.get(DocMetadataKeys.DOCUMENT_SIGNED_STATUS.toString).foreach(builder.setDocumentSignedStatus)
    documentMetadata.get(DocMetadataKeys.DOCUMENT_AUTHOR.toString).foreach(builder.setDocumentAuthor)
    documentMetadata.get(DocMetadataKeys.DOCUMENT_UPDATE.toString)
      .foreach(ts => if (parseDateString(ts).nonEmpty) builder.setDocumentUpdate(parseDateString(ts).get))
    documentMetadata.get(DocMetadataKeys.ENCOUNTER_DATE.toString)
      .foreach(ts => if (parseDateString(ts).nonEmpty)  builder.setEncounterDate(parseDateString(ts).get))
    documentMetadata.get(DocMetadataKeys.PROVIDER_TYPE.toString).foreach(builder.setProviderType)
    documentMetadata.get(DocMetadataKeys.PROVIDER_TITLE.toString).foreach(builder.setProviderTitle)
    documentMetadata.get(DocMetadataKeys.PROVIDER_ID.toString).foreach(builder.setProviderId)
    documentMetadata.get(DocMetadataKeys.DOCUMENT_SIGNED_DATE.toString)
      .foreach(ts => if (parseDateString(ts).nonEmpty)  builder.setDocumentSignedDate(parseDateString(ts).get))

    documentMetadata.get(DocMetadataKeys.ENCOUNTER_PROVIDER_TYPE.toString)
      .filter(_.nonEmpty)
      .flatMap(raw => AxmUtils.createCode(raw))
      .foreach(builder.setEncounterProviderType)

    documentMetadata.get(DocMetadataKeys.ENCOUNTER_DATE_START.toString)
      .foreach(ts => if (parseDateString(ts).nonEmpty) builder.setEncounterStartDate(parseDateString(ts).get))
    documentMetadata.get(DocMetadataKeys.ENCOUNTER_DATE_END.toString)
      .foreach(ts => if (parseDateString(ts).nonEmpty)  builder.setEncounterEndDate(parseDateString(ts).get))

    documentMetadata.get(DocMetadataKeys.LST_FILED_INST_DTTM.toString)
      .foreach(ts => if (parseDateString(ts).nonEmpty)  builder.setLastEditTime(parseDateString(ts).get))
    documentMetadata.get(DocMetadataKeys.ENT_INST_LOCAL_DTTM.toString)
      .foreach(ts => if (parseDateString(ts).nonEmpty)  builder.setEncounterTime(parseDateString(ts).get))
    documentMetadata.get(DocMetadataKeys.DOCUMENT_UPDATE_DATE.toString)
      .foreach(ts => if (parseDateString(ts).nonEmpty)  builder.setDocumentUpdateDate(parseDateString(ts).get))
    documentMetadata.get(DocMetadataKeys.ENTRY_INSTANT_DTTM.toString)
      .foreach(ts => if (parseDateString(ts).nonEmpty)  builder.setEntryInstantDate(parseDateString(ts).get))
    documentMetadata.get(DocMetadataKeys.UPD_AUT_LOCAL_DTTM.toString)
      .foreach(ts => if (parseDateString(ts).nonEmpty) builder.setUpdatedAuthorLocalDate(parseDateString(ts).get))
    documentMetadata.get(DocMetadataKeys.NOT_FILETM_LOC_DTTM.toString)
      .foreach(ts => if (parseDateString(ts).nonEmpty) builder.setNoteFileLocalDate(parseDateString(ts).get))
    documentMetadata.get(DocMetadataKeys.UPD_AUTHOR_INS_DTTM.toString)
      .foreach(ts => if (parseDateString(ts).nonEmpty) builder.setUpdatedAuthorDate(parseDateString(ts).get))
    documentMetadata.get(DocMetadataKeys.NOTE_FILE_TIME_DTTM.toString)
      .foreach(ts => if (parseDateString(ts).nonEmpty) builder.setNoteFileDate(parseDateString(ts).get))
    documentMetadata.get(DocMetadataKeys.ENCOUNTER_ID.toString).foreach(builder.setEncounterId)
    documentMetadata.get(DocMetadataKeys.ENCOUNTER_NAME.toString).foreach(builder.setEncounterName)
    documentMetadata.get(DocMetadataKeys.NOTE_TYPE.toString).foreach(builder.setNoteType)
    builder.build()
  }

  def convertAddress(address: patient.Address): Address = {
    Option(address).map(_ => {
      val builder = Address.newBuilder()
      val filteredStreetAddresses = address.getStreetAddresses.map(nullToEmpty).filterNot(_.isEmpty)
      if (filteredStreetAddresses.nonEmpty) builder.addAllStreetAddresses(filteredStreetAddresses.asJava)
      if (nullToEmpty(address.getCity).trim.nonEmpty) builder.setCity(address.getCity)
      if (nullToEmpty(address.getState).trim.nonEmpty) builder.setState(address.getState)
      if (nullToEmpty(address.getZip).trim.nonEmpty) builder.setZip(address.getZip)
      if (nullToEmpty(address.getCountry).trim.nonEmpty) builder.setCountry(address.getCountry)
      if (nullToEmpty(address.getCounty).trim.nonEmpty) builder.setCounty(address.getCounty)
      if (address.getAddressType != null) builder.setAddressType(address.getAddressType.name())
      builder.build()
    }).orNull
  }

  def convertOrganization(org: patient.Organization): Organization = {
    Option(org).map(_ => {
      val builder = Organization.newBuilder()
      if (nullToEmpty(org.getName).trim.nonEmpty) builder.setName(org.getName)
      if (org.getPrimaryId != null) builder.setPrimaryId(convertExternalId(org.getPrimaryId))
      builder.addAllAlternateIds(org.getAlternateIds.map(convertExternalId).asJava)
      if (org.getContactDetails != null) builder.setContactDetails(convertContactDetailInfo(org.getContactDetails))
      builder.build()
    }).orNull
  }

  def convertAllPhoneNumbers(listOfPhoneNumbers: util.List[patient.TelephoneNumber]): Seq[TelephoneNumber] = {
    Option(listOfPhoneNumbers).filter(_.nonEmpty).map(phoneNumList => {
      phoneNumList.map(phoneNum => {
        convertPhoneNumber(phoneNum)
      })
    }).orNull
  }

  def convertPhoneNumber(phone: patient.TelephoneNumber): TelephoneNumber = {
    Option(phone).map(_ => {
      val builder = TelephoneNumber.newBuilder()
      if (nullToEmpty(phone.getPhoneNumber).trim.nonEmpty) builder.setPhoneNumber(phone.getPhoneNumber)
      if (phone.getPhoneType != null) builder.setTelephoneType(phone.getPhoneType.toString)
      builder.build()
    }).orNull
  }

  def convertName(name: patient.Name): Name = {
    Option(name).map(_ => {
      val builder = Name.newBuilder()
      if (name.getGivenNames != null) builder.addAllGivenNames(name.getGivenNames)
      if (name.getFamilyNames != null) builder.addAllFamilyNames(name.getFamilyNames)
      if (name.getPrefixes != null) builder.addAllPrefixes(name.getPrefixes)
      if (name.getSuffixes != null) builder.addAllSuffixes(name.getSuffixes)
      if (name.getNameType != null) builder.setNameType(name.getNameType.toString)
      builder.build()
    }).orNull
  }

  def convertAnatomy(anatomy: patient.Anatomy): Anatomy = {
    Option(anatomy).map(_ => {
      val builder = Anatomy.newBuilder()
      val anatomyCode = anatomy.getCode
      Option(anatomyCode).map(_ => {
        if (nullToEmpty(anatomyCode.getCode).trim.nonEmpty) builder
          .setBodySite(convertClinicalCode(anatomyCode))
      })
      if (ArgumentUtil.isNotEmpty(anatomy.getAnatomicalStructureName))
        builder.setAnatomicalStructureName(anatomy.getAnatomicalStructureName)

      builder.build()
    }).orNull
  }

  def convertDataCatalogMeta(pdsId: String, baseObject: BaseObject, oid: String): DataCatalogMeta = {
    val builder = DataCatalogMeta.newBuilder()

    if (baseObject.getOriginalId != null)
      builder.setOriginalId(convertExternalId(baseObject.getOriginalId))

    val otherOriginalIds = baseObject.getOtherOriginalIds.map(convertExternalId)
    if (otherOriginalIds != null)
      builder.addAllOtherOriginalIds(otherOriginalIds.asJava)

    builder.setLastEditTime(baseObject.getLastEditDateTime.getMillis)
    builder.setPdsId(pdsId)
    builder.setOid(oid)

    builder.setEditType(convertEditType(baseObject.getEditType))

    builder.build()
  }

  def convertCodeTranslations(codedBaseObject: CodedBaseObject): Seq[ClinicalCodeProto] = {
    val codes = codedBaseObject.getCodeTranslations
      .filterNot(_ == null) // CCDA?
      .sortBy(c => Option(c.getCode).getOrElse("")) //Consistent Sorting
    codes.map(convertClinicalCode(_))
  }

  def convertCode(codedBaseObject: CodedBaseObject): ClinicalCodeProto = {
    convertClinicalCode(codedBaseObject.getCode)
  }

  def convertAllergyInfo(allergy: Allergy): AllergyInfo = {
    Option(allergy).map(_ => {
      val infoBuilder = AllergyInfo.newBuilder()

      if (allergy.getAllergen != null) infoBuilder.setAllergen(allergy.getAllergen)
      if (allergy.getReactionSeverity != null)  infoBuilder.setReactionSeverity(allergy.getReactionSeverity)

      val reactionCode = convertClinicalCode(allergy.getReaction)
      if (reactionCode != null) infoBuilder.setReaction(reactionCode)

      Option(allergy.getDiagnosisDate).foreach(d => {
        infoBuilder.setDiagnosisDate(fromDateTime(d))
      })

      Option(allergy.getReactionDate).foreach(d => {
        infoBuilder.setReactionDate(fromDateTime(d))
      })

      Option(allergy.getResolvedDate).foreach(d => {
        infoBuilder.setResolvedDate(fromDateTime(d))
      })

      val codeTranslations = convertCodeTranslations(allergy)
      infoBuilder.addAllCodeTranslations(codeTranslations.asJava)

      val code = convertCode(allergy)
      if (code != null) infoBuilder.setCode(code)

      infoBuilder.build()
    }).orNull
  }

  def convertClinicalActorInfo(actor: patient.ClinicalActor): ClinicalActorInfo = {
    Option(actor).map(_ => {
      val infoBuilder = ClinicalActorInfo.newBuilder

      //Try set primary ID from primary, else from original
      if (actor.getPrimaryId != null)
        infoBuilder.setPrimaryId(convertExternalId(actor.getPrimaryId))
      else if (actor.getOriginalId != null)
        infoBuilder.setPrimaryId(convertExternalId(actor.getOriginalId))

      if (actor.getRole != null) infoBuilder.setActorRole(actor.getRole.toString)
      Option(actor.getTitle).filter(_.nonEmpty).foreach(infoBuilder.setTitle)
      if (actor.getActorGivenName != null) infoBuilder.setActorGivenName(convertName(actor.getActorGivenName))
      if (actor.getActorSupplementalNames != null) infoBuilder
        .addAllActorSupplementalNames(actor.getActorSupplementalNames.asScala.map(convertName).asJava)
      if (actor.getAssociatedOrg != null) infoBuilder.setAssociatedOrg(convertOrganization(actor.getAssociatedOrg))
      if (actor.getAlternateIds != null) infoBuilder
        .addAllAlternateIds(actor.getAlternateIds.asScala.map(convertExternalId).asJava)
      if (actor.getContactDetails != null) infoBuilder
        .setContactDetails(convertContactDetailInfo(actor.getContactDetails))
      infoBuilder.build()
    }).orNull
  }


  def convertEncounterInfo(apoEncounter: patient.Encounter, convertedCaresite: CareSite): EncounterInfo = {
    val encounterInfoBuilder = EncounterInfo.newBuilder()

    Option(apoEncounter.getOriginalId).foreach(id => {
      encounterInfoBuilder.setPrimaryId(convertExternalId(id))
    })

    Option(apoEncounter.getCode).foreach(c => {
      encounterInfoBuilder.setCode(convertClinicalCode(c))
    })

    val codeTranslations = convertCodeTranslations(apoEncounter)
    encounterInfoBuilder.addAllCodeTranslations(codeTranslations.asJava)

    Option(apoEncounter.getEncounterStartDate).foreach(d => {
      encounterInfoBuilder
        .setStartDate(fromDateTime(d))
    })

    Option(apoEncounter.getEncounterEndDate).foreach(d => {
      encounterInfoBuilder
        .setEndDate(fromDateTime(d))
    })

    Option(apoEncounter.getEncType).foreach(t => encounterInfoBuilder.setEncounterType(t.name()))

    if (apoEncounter.getChiefComplaints != null) encounterInfoBuilder
      .addAllChiefComplaints(apoEncounter.getChiefComplaints.asScala.map(convertClinicalCode(_)).asJava)

    Option(convertedCaresite).foreach(encounterInfoBuilder.setCaresite)

    encounterInfoBuilder.build()
  }

  def convertVitalSign(bv: BiometricValue): VitalSign = {
    Option(bv).map(_ => {
      val builder = VitalSignOuterClass.VitalSign.newBuilder()

      val value = convertValueAsString(bv.getValue)
      if (value != null) builder.setValue(value)

      if (bv.getUnitsOfMeasure != null) builder.setUnitsOfMeasure(bv.getUnitsOfMeasure)

      if (bv.getResultDate != null) {
        builder.setResultDate(fromDateTime(bv.getResultDate))
      }

      if (bv.getName != null) builder.setName(bv.getName)

      val code = convertCode(bv)
      if (code != null) builder.setCode(code)

      val codeTranslations = convertCodeTranslations(bv)
      builder.addAllCodeTranslations(codeTranslations.asJava)

      builder.build()
    }).orNull
  }

  def convertSocialHistoryInfo(sh: patient.SocialHistory): SocialHistoryInfo = {
    Option(sh).map(_ => {
      val builder = SocialHistoryInfo.newBuilder()

      if (sh.getType != null) builder.setType(convertClinicalCode(sh.getType))
      if (sh.getValue != null) builder.setValue(sh.getValue)
      if (sh.getFieldName != null) builder.setFieldName(sh.getFieldName)
      if (sh.getDate != null) builder.setDate(fromDateTime(sh.getDate))

      val codeTranslations = convertCodeTranslations(sh)
      builder.addAllCodeTranslations(codeTranslations.asJava)

      val code = convertCode(sh)
      if (code != null) builder.setCode(code)

      builder.build()
    }).orNull
  }

  def convertTypedDate(typedDate: TypedDate): LabResultProto.TypedDate = {
    Option(typedDate).map(_ => {
      val builder = LabResultProto.TypedDate.newBuilder()
      builder.setEpochMs(typedDate.getDate.getMillis)
      builder.setType(typedDate.getType)
      builder.build()
    }).orNull
  }

  def getLabResultInfo(apo: patient.Patient, labResultApo: patient.LabResult): LabResultInfo = {
    Option(labResultApo).map(_ => {

      val builder = LabResultInfo.newBuilder()


      Option(labResultApo.getLabName).foreach(builder.setName)
      Option(convertValueAsString(labResultApo.getValue)).foreach(builder.setValue)
      Option(labResultApo.getRange).foreach(builder.setRange)
      Option(convertLabFlag(labResultApo.getFlag)).foreach(builder.setFlag)
      Option(labResultApo.getUnits).foreach(builder.setUnits)
      Option(labResultApo.getLabNote).foreach(builder.setLabNote)
      Option(labResultApo.getSpecimen).foreach(c => builder.setSpecimen(convertClinicalCode(c)))
      Option(labResultApo.getPanel).foreach(c => builder.setPanel(convertClinicalCode(c)))
      Option(labResultApo.getSuperPanel).foreach(c => builder.setSuperPanel(convertClinicalCode(c)))


      Option(labResultApo.getSampleDate).foreach(d => builder.setSampleDate(fromDateTime(d)))

      Option(labResultApo.getCareSiteId)
        .map(apo.getCareSiteById)
        .map(convertCareSite)
        .foreach(builder.setCareSite)

      val otherDates = labResultApo.getOtherDates.filterNot(_.equals(null)).map(convertTypedDate)
      if (otherDates.nonEmpty) builder.addAllOtherDates(otherDates.asJava)

      Option(labResultApo.getSequenceNumber).foreach(n=> builder.setSequenceNumber(wrapInt(n)))


      // Vendor details
      val vendorIdOpt = Option(labResultApo.getMetaTag(LabResultMetadataKeys.VENDOR_ID.toString))
      val vendorCodeOpt = Option(labResultApo.getMetaTag(LabResultMetadataKeys.VENDOR_CODE.toString))
      (vendorIdOpt, vendorCodeOpt) match {
        case (None, None) =>
        case _ =>
          val vendorBuilder = VendorDetails.newBuilder()
          vendorIdOpt.foreach(vendorBuilder.setVendorId)
          vendorCodeOpt.foreach(vendorBuilder.setVendorCode)
          builder.setVendorDetails(vendorBuilder.build())
      }

      if (labResultApo.getCode != null) builder.setCode(convertClinicalCode(labResultApo.getCode))
      val codeTranslations = convertCodeTranslations(labResultApo)
      builder.addAllCodeTranslations(codeTranslations.asJava)

      builder.build()
    }).orNull
  }

  def getPrescriptionInfo(prescriptionApo: patient.Prescription): PrescriptionInfo = {
    Option(prescriptionApo).map(_ => {

      val builder = PrescriptionInfo.newBuilder()

      Option(prescriptionApo.getCode).foreach(c => builder.setCode(convertClinicalCode(c)))
      val codeTranslations = convertCodeTranslations(prescriptionApo)
      if (codeTranslations.nonEmpty) builder.addAllCodeTranslations(codeTranslations.asJava)

      Option(prescriptionApo.getPrescriptionDate).foreach(d => builder.setPrescriptionDate(fromDateTime(d)))
      Option(prescriptionApo.getAmount).foreach(builder.setAmount)
      Option(prescriptionApo.getSig).foreach(builder.setDirections)
      Option(prescriptionApo.getEndDate).foreach(d => builder.setEndDate(fromDateTime(d)))
      Option(prescriptionApo.getFillDate).foreach(d => builder.setFillDate(fromDateTime(d)))
      Option(prescriptionApo.getDosage).foreach(builder.setDosage)
      Option(prescriptionApo.isActivePrescription).foreach(b => builder.setIsActivePrescription(wrapBoolean(b)))
      Option(prescriptionApo.getRefillsRemaining).foreach(rf => builder.setRefillsRemaining(wrapInt(rf)))
      Option(prescriptionApo.getQuantity).foreach(q => builder.setQuantity(wrapDouble(q)))
      Option(prescriptionApo.getFrequency).foreach(builder.setFrequency)


      Option(getMedicationInfo(prescriptionApo.getAssociatedMedication)).foreach(builder.setAssociatedMedication)

      builder.build()
    }).orNull
  }

  def getFamilyHistoryInfo(familyHistoryApo: patient.FamilyHistory): FamilyHistoryInfo = {
    Option(familyHistoryApo).map(_ => {

      val fhInfoBuilder = FamilyHistoryInfo.newBuilder()
      if (familyHistoryApo.getFamilyHistory != null)
        fhInfoBuilder.setFamilyHistory(familyHistoryApo.getFamilyHistory)
      if (familyHistoryApo.getCode != null)
        fhInfoBuilder.setCode(convertClinicalCode(familyHistoryApo.getCode))
      val codeTranslations = convertCodeTranslations(familyHistoryApo)
      fhInfoBuilder.addAllCodeTranslations(codeTranslations.asJava)

      fhInfoBuilder.build()
    }).orNull
  }

  def getImmunizationInfo(administration: patient.Administration): ImmunizationInfo = {
    Option(administration).map(_ => {

      val builder = ImmunizationInfo.newBuilder()


      val medicationInfo = getMedicationInfo(administration.getMedication)
      Option(medicationInfo).foreach(builder.setMedicationInfo)

      if (administration.getAmount != null) builder.setAmount(administration.getAmount)
      if (administration.getEndDate != null) builder.setEndDate(fromDateTime(administration.getEndDate))
      if (administration.getStartDate != null) builder.setStartDate(fromDateTime(administration.getStartDate))
      if (administration.getAdminDate != null) builder.setAdminDate(fromDateTime(administration.getAdminDate))
      if (administration.getDosage != null) builder.setDosage(administration.getDosage)

      Option(administration.getQuantity).foreach(q => builder.setQuantity(wrapDouble(q)))
      Option(administration.getMedicationSeriesNumber).foreach(msn => builder.setMedicationSeriesNumber(wrapInt(msn)))

      Option(convertCode(administration)).foreach(builder.setCode)
      val codeTranslations = convertCodeTranslations(administration)
      if (codeTranslations.nonEmpty) builder.addAllCodeTranslations(codeTranslations.asJava)

      builder.build()
    }).orNull
  }

  def getMedicationInfo(medicationApo: patient.Medication): MedicationInfo = {
    Option(medicationApo).map(_ => {
      val medicationInfoBuilder = MedicationInfo.newBuilder()

      if (medicationApo.getCode != null)
        medicationInfoBuilder.setCode(convertClinicalCode(medicationApo.getCode))

      if (medicationApo.getUnits != null) medicationInfoBuilder.setUnits(medicationApo.getUnits)
      if (medicationApo.getBrandName != null) medicationInfoBuilder.setBrandName(medicationApo.getBrandName)
      if (medicationApo.getGenericName != null) medicationInfoBuilder.setGenericName(medicationApo.getGenericName)
      if (medicationApo.getForm != null) medicationInfoBuilder.setForm(medicationApo.getForm)
      if (medicationApo.getIngredients != null) medicationInfoBuilder.addAllIngredients(medicationApo.getIngredients)
      if (medicationApo.getRouteOfAdministration != null) medicationInfoBuilder
        .setRouteOfAdministration(medicationApo.getRouteOfAdministration)
      if (medicationApo.getStrength != null) medicationInfoBuilder.setStrength(medicationApo.getStrength)
      val codeTranslations = convertCodeTranslations(medicationApo)
      if (codeTranslations.nonEmpty) medicationInfoBuilder.addAllCodeTranslations(codeTranslations.asJava)

      medicationInfoBuilder.build()
    }).orNull
  }

  def convertLabFlag(flag: patient.LabFlags): LabResultProto.LabFlag = {
    Option(flag).map(_ => {
      LabResultProto.LabFlag.valueOf(flag.toString.toUpperCase())
    }).orNull
  }

  def convertValueAsString(numberOrString: EitherStringOrNumber): ValueAsStringOuterClass.ValueAsString = {
    Option(numberOrString).map(_ => {

      val builder = ValueAsStringOuterClass.ValueAsString.newBuilder()

      if (numberOrString.left() != null) {
        builder.setType(ValueAsStringOuterClass.Type.String)
        builder.setValue(numberOrString.left())
      }

      if (numberOrString.right() != null) {
        builder.setType(ValueAsStringOuterClass.Type.BigDecimal)
        builder.setValue(numberOrString.right().toString) // TODO! Right now ccda uses bigdecimal. Talk to anthony
      }

      builder.build()
    }).orNull
  }

  def getClaimType(cbo: CodedBaseObject): String = {
    val tag = cbo.getMetaTag(AxmConstants.CLAIM_TYPE)
    tag match {
      case null => ""
      case ct => ct
    }
  }

  def buildCodedBase(parsingDetailsId: UUIDOuterClass.UUID,
                     sourceId: UUIDOuterClass.UUID,
                     primaryActorId: UUIDOuterClass.UUID,
                     supplementaryActorIds: Seq[UUIDOuterClass.UUID],
                     encounter: CodedBaseObjects.Encounter,
                     dataCatalogMeta: DataCatalogMetaOuterClass.DataCatalogMeta): CodedBase = {
    val codedBaseBuilder = CodedBase.newBuilder()
    if (parsingDetailsId != null) codedBaseBuilder.addParsingDetailsIds(parsingDetailsId)
    if (sourceId != null) codedBaseBuilder.addSourceIds(sourceId)
    if (primaryActorId != null) codedBaseBuilder.setPrimaryActorId(primaryActorId)
    if (supplementaryActorIds != null) codedBaseBuilder.addAllSupplementaryActorIds(supplementaryActorIds.asJava)
    if (encounter != null) codedBaseBuilder.addEncounterIds(encounter.getInternalId)
    if (dataCatalogMeta != null) codedBaseBuilder.setDataCatalogMeta(dataCatalogMeta)
    codedBaseBuilder.build()
  }

}