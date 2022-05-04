package com.apixio.nassembly.patient

import com.apixio.datacatalog.BaseObjects.{ClinicalActor, ContactDetails}
import com.apixio.datacatalog.CareSiteOuterClass.CareSite
import com.apixio.datacatalog.CodedBaseObjects._
import com.apixio.datacatalog.PatientMetaProto.PatientMeta
import com.apixio.datacatalog.PatientProto.CodedBasePatient
import com.apixio.datacatalog._
import com.apixio.nassembly.BaseConsolidator
import com.apixio.nassembly.contactdetails.ContactDetailsUtils
import com.apixio.nassembly.demographics.DemographicsUtils
import com.apixio.nassembly.util.BaseEntityUtil

import java.util
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConverters._


object MergeUtils {


  /**
   * Merge list of patients
   * IF complex merge = true, use entire merge logic for each datatype. Else just aggregate
   * @param patients List of patients
   * @param complexMerge Flag to use complex merge or just aggregate data
   * @return
   */
  def mergePatients(patients: List[PatientProto.Patient], complexMerge: Boolean = false): PatientProto.Patient = {
    if (complexMerge)
      complexMergePatients(patients)
    else
      simpleMergePatients(patients)
  }


  /**
   * TODO: Call util merge functions for all datatypes instead of simple aggregation
   * @param patients List of patient protos
   * @return
   */
  private def complexMergePatients(patients: List[PatientProto.Patient]): PatientProto.Patient = {
    simpleMergePatients(patients)
  }

  /**
   * Aggregate all non-base data and merge base data
   * Assumption is that data was already separated
   * @param patients List of patient protos
   * @return
   */
  private def simpleMergePatients(patients: List[PatientProto.Patient]): PatientProto.Patient = {
    if (patients.isEmpty)
      null

    else if (patients.size == 1) {
      patients.head
    }

    else {
      val patientBuilder = PatientProto.Patient.newBuilder

      //Base
      val patientMeta = BaseConsolidator.mergePatientMeta(patients.map(_.getBase.getPatientMeta).asJava)
      val catalogMeta = BaseConsolidator.mergeCatalogMeta(patients.map(_.getBase.getDataCatalogMeta).asJava)
      val clinicalActors = consolidateActors(patients)
      val parsingDetails = consolidateParsingDetails(patients)
      val sources = consolidateSources(patients)
      val encounters = consolidateEncounters(patients)
      val codedBase: CodedBasePatient = BaseEntityUtil
        .buildCodedBasePatient(patientMeta, catalogMeta, parsingDetails, sources,
          clinicalActors, encounters)

      patientBuilder.setBase(codedBase)

      val procedures = consolidateProcedures(patients)
      if (procedures.nonEmpty) patientBuilder.addAllProcedures(procedures)

      val documents = consolidateDocuments(patients)
      if (documents.nonEmpty) patientBuilder.addAllDocuments(documents)


      val ffsClaims = consolidateFfsClaims(patients)
      if (ffsClaims.nonEmpty) patientBuilder.addAllFfsClaims(ffsClaims)

      val problems = consolidateProblems(patients)
      if (problems.nonEmpty) patientBuilder.addAllProblems(problems)

      val raClaims = consolidateRaClaims(patients)
      if (raClaims.nonEmpty) patientBuilder.addAllRaClaims(raClaims)

      val mao004s = consolidateMao004s(patients)
      if (mao004s.nonEmpty) patientBuilder.addAllMao004S(mao004s)

      val contactDetails = consolidateContactDetails(patients)
      contactDetails.toSeq match {
        case Nil => // nothing
        case h :: t =>
          patientBuilder.setPrimaryContactDetails(h)
          patientBuilder.addAllAlternateContactDetails(contactDetails.asJava)
      }

      val demographics = consolidateDemographics(patients)
      demographics.toSeq match {
        case Nil => // nothing
        case demographics =>
          patientBuilder.addAllAlternateDemographics(demographics.asJava)

          val primaryDemographicsInfo = DemographicsUtils.combineDemographicInfo(
            demographics.filter(_.hasDemographicsInfo).map(_.getDemographicsInfo)
          )
          patientBuilder.setPrimaryDemographics(demographics.head.toBuilder.setDemographicsInfo(primaryDemographicsInfo).build())

      }

      val allergies = consolidateAllergies(patients)
      if (allergies.nonEmpty) patientBuilder.addAllAllergies(allergies.asJava)

      val biometricValues = consolidateBiometrics(patients)
      if (biometricValues.nonEmpty) patientBuilder.addAllBiometricValues(biometricValues)

      val socialHistories = consolidateSocialHistories(patients)
      if (socialHistories.nonEmpty) patientBuilder.addAllSocialHistories(socialHistories)

      val labResults = consolidateLabResults(patients)
      if (labResults.nonEmpty) patientBuilder.addAllLabResults(labResults)

      val prescriptions = consolidatePrescriptions(patients)
      if (prescriptions.nonEmpty) patientBuilder.addAllPrescriptions(prescriptions)

      val familyHistories = consolidateFamilyHistories(patients)
      if (familyHistories.nonEmpty) patientBuilder.addAllFamilyHistories(familyHistories)

      val immunization = consolidateImmunizations(patients)
      if (immunization.nonEmpty) patientBuilder.addAllImmunizations(immunization)

      val extractedText = consolidateExtractedText(patients)
      if (extractedText.nonEmpty) patientBuilder.addAllExtractedText(extractedText)


      patientBuilder.build
    }

  }

  //TODO: Used in MergeUtils, ApoToProtoConverter, ContactDetailsUtils, EncounterUtils, ProcedureUtils, LabResultUtils,
  // as well. Need to consolidate common methods
  // into a base util class

  // Take the actor that was last updated
  def consolidateActors(wrappers: List[PatientProto.Patient]): Iterable[ClinicalActor] = {
    wrappers.flatMap(w => w.getBase.getClinicalActorsList).groupBy(_.getInternalId).map {
      case (id, actors) => actors.maxBy(_.getBase.getDataCatalogMeta.getLastEditTime)
    }
  }

  def mergePatientMeta(patientProto: PatientProto.Patient, patientMeta: PatientMeta): PatientProto.Patient = {
    val basePatient = patientProto.getBase

    val patientMetaList = List(basePatient.getPatientMeta, patientMeta).asJava
    val mergedPatientMeta = BaseConsolidator.mergePatientMeta(patientMetaList)

    basePatient.toBuilder.setPatientMeta(mergedPatientMeta)

    patientProto.toBuilder
      .setBase(basePatient)
      .build()
  }

  def consolidateSources(wrappers: List[PatientProto.Patient]): java.util.List[SourceOuterClass.Source] = {
    BaseConsolidator.dedupSources(wrappers.flatMap(w => w.getBase.getSourcesList).asJava)
  }

  def consolidateCaresites(wrappers: List[PatientProto.Patient]): java.util.List[CareSite] = {
    val caresites = wrappers.flatMap(w => w.getBase.getEncountersList
      .filter(_.hasEncounterInfo)
      .map(_.getEncounterInfo)
      .filter(_.hasCaresite)
      .map(_.getCaresite)
    )
    BaseConsolidator.dedupCareSites(caresites.asJava)
  }

  def consolidateEncounters(wrappers: List[PatientProto.Patient]): Iterable[Encounter] = {
    wrappers.flatMap(w => w.getBase.getEncountersList).groupBy(_.getInternalId).map {
      case (id, actors) => actors.maxBy(_.getDataCatalogMeta.getLastEditTime)
    }
  }

  def consolidateAllergies(wrappers: List[PatientProto.Patient]): Iterable[Allergy] = {
    wrappers.flatMap(w => w.getAllergiesList)
  }

  def consolidateContactDetails(patients: List[PatientProto.Patient]): Iterable[ContactDetails] = {
    val allDetails: Seq[ContactDetails] = patients.filter(_.hasPrimaryContactDetails).map(_.getPrimaryContactDetails) ++
      patients.flatMap(_.getAlternateContactDetailsList)
    ContactDetailsUtils.mergeContactDetails(allDetails.toList)
  }

  def consolidateParsingDetails(wrappers: List[PatientProto.Patient]): java.util.List[ParsingDetailsOuterClass.ParsingDetails] = {
    BaseConsolidator
      .dedupParsingDetails(wrappers.flatMap(w => w.getBase.getParsingDetailsList).asJava)
  }

  def consolidateDocuments(wrappers: List[PatientProto.Patient]): java.util.List[DocumentMetaCBO] = {
    wrappers.flatMap(w => w.getDocumentsList).asJava
  }

  def consolidateProcedures(wrappers: List[PatientProto.Patient]): java.util.List[ProcedureCBO] = {
    wrappers.flatMap(w => w.getProceduresList).asJava
  }

  def consolidateFfsClaims(wrappers: List[PatientProto.Patient]): java.util.List[FfsClaimCBO] = {
    wrappers.flatMap(w => w.getFfsClaimsList).asJava
  }

  def consolidateProblems(wrappers: List[PatientProto.Patient]): java.util.List[ProblemCBO] = {
    wrappers.flatMap(w => w.getProblemsList).asJava
  }

  def consolidateRaClaims(wrappers: List[PatientProto.Patient]): java.util.List[RaClaimCBO] = {
    wrappers.flatMap(w => w.getRaClaimsList).asJava
  }

  def consolidateMao004s(wrappers: List[PatientProto.Patient]): util.List[CodedBaseObjects.Mao004CBO] = {
    wrappers.flatMap(w => w.getMao004SList).asJava
  }

  def consolidateDemographics(wrappers: List[PatientProto.Patient]): Iterable[BaseObjects.PatientDemographics] = {
    wrappers.flatMap(w => w.getAlternateDemographicsList)
  }

  def consolidateBiometrics(wrappers: List[PatientProto.Patient]): util.List[CodedBaseObjects.BiometricValue] = {
    wrappers.flatMap(w => w.getBiometricValuesList).asJava
  }

  def consolidateSocialHistories(wrappers: List[PatientProto.Patient]): util.List[CodedBaseObjects.SocialHistory] = {
    wrappers.flatMap(w => w.getSocialHistoriesList).asJava
  }

  def consolidateLabResults(wrappers: List[PatientProto.Patient]): util.List[CodedBaseObjects.LabResultCBO] = {
    wrappers.flatMap(w => w.getLabResultsList).asJava
  }

  def consolidatePrescriptions(wrappers: List[PatientProto.Patient]): util.List[CodedBaseObjects.PrescriptionCBO] = {
    wrappers.flatMap(w => w.getPrescriptionsList).asJava
  }

  def consolidateFamilyHistories(wrappers: List[PatientProto.Patient]): util.List[CodedBaseObjects.FamilyHistoryCBO] = {
    wrappers.flatMap(w => w.getFamilyHistoriesList).asJava
  }

  def consolidateImmunizations(wrappers: List[PatientProto.Patient]): util.List[CodedBaseObjects.ImmunizationCBO] = {
    wrappers.flatMap(w => w.getImmunizationsList).asJava
  }

  def consolidateExtractedText(wrappers: List[PatientProto.Patient]): util.List[ExtractedTextOuterClass.RawExtractedText] = {
    wrappers.flatMap(w => w.getExtractedTextList).asJava
  }
}
