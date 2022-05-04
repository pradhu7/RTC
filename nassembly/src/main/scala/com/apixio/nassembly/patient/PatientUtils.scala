package com.apixio.nassembly.patient

import com.apixio.datacatalog.SkinnyPatientProto.ClinicalActors
import com.apixio.datacatalog.SummaryObjects.DocumentMetaSummary
import com.apixio.datacatalog._
import com.apixio.nassembly.BaseConsolidator
import com.apixio.nassembly.documentmeta.DocumentMetaUtils
import com.apixio.util.nassembly.CaretParser
import com.apixio.util.nassembly.DataCatalogProtoUtils.convertUuid
import com.google.protobuf.GeneratedMessageV3
import org.apache.commons.lang.StringUtils

import java.util
import java.util.UUID
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConverters._
import scala.util.{Success, Try}


object PatientUtils {

  def parsingDetailMap(patientProto: PatientProto.Patient): java.util.Map[UUIDOuterClass.UUID, ParsingDetailsOuterClass.ParsingDetails] = BaseConsolidator
    .parsingDetailsMap(getBasePatient(patientProto).getParsingDetailsList)

  def sourcesMap(patientProto: PatientProto.Patient): java.util.Map[UUIDOuterClass.UUID, SourceOuterClass.Source] =
    BaseConsolidator.sourcesMap(getBasePatient(patientProto).getSourcesList)

  def clinicalActorMap(patientProto: PatientProto.Patient): java.util.Map[UUIDOuterClass.UUID, BaseObjects.ClinicalActor] = BaseConsolidator
    .clinicalActorMap(getBasePatient(patientProto).getClinicalActorsList)

  def caresiteMap(patientProto: PatientProto.Patient): java.util.Map[UUIDOuterClass.UUID, CareSiteOuterClass.CareSite] = BaseConsolidator
    .careSiteMap(getCareSitesList(patientProto))


  /**
   * Get the sourceFileArchiveUUID for a patient given a batchId
   *
   * @param patient Patient Proto
   * @return
   */
  def getSourceFileArchiveUUID(patient: PatientProto.Patient, batchId: String): Option[UUIDOuterClass.UUID] = {
    if (patient == null || StringUtils.isEmpty(batchId)) {
      None
    }
    else {
      getBasePatient(patient).getParsingDetailsList.find(p => p.getUploadBatchId == batchId)
        .map(_.getSourceFileArchiveUUID)
    }
  }

  /**
   * Get the sourceFileArchiveUUID for a patient using the catalogMeta batchId
   *
   * @param patient Patient Proto
   * @return
   */
  def getSourceFileArchiveUUID(patient: PatientProto.Patient): Option[UUIDOuterClass.UUID] = {
    getSourceFileArchiveUUID(patient, getUploadBatchId(patient))
  }


  /**
   * Return batchId for a partial patient proto
   *
   * @param patient Patient Proto
   * @return null if no relevant batchId info
   */
  def getUploadBatchId(patient: PatientProto.Patient): String = {
    Try(getBasePatient(patient).getParsingDetails(0).getUploadBatchId) match {
      case Success(value) => value
      case _ => null
    }
  }

  //TODO: Move to common Utils as being used by other Utils as well
  private def getBasePatient(patient: PatientProto.Patient) = {
    patient.getBase
  }

  def convertSkinny(skinnyProto: GeneratedMessageV3): PatientProto.Patient = {
    PatientProto.Patient.parseFrom(skinnyProto.toByteString)
  }

  /**
   * "Hydrate" a "skinny patient" to a Patient
   * @param proto SkinnyPatient for clinical actors
   * @return null if proto is null
   */
  def convertSkinnyClinicalActor(proto: ClinicalActors): PatientProto.Patient = {
    Option(proto).map(_ => {
      val base = PatientProto.CodedBasePatient.newBuilder
        .addAllClinicalActors(proto.getClinicalActorsList)
        .addAllParsingDetails(proto.getParsingDetailsList)
        .addAllSources(proto.getSourcesList)
        .setPatientMeta(proto.getPatientMeta)
        .build

      PatientProto.Patient.newBuilder.setBase(base).build
    }).orNull
  }

  def getCareSitesList(patient: PatientProto.Patient): java.util.List[CareSiteOuterClass.CareSite] = {
    val fromEncounters = getBasePatient(patient).getEncountersList
      .filter(_.hasEncounterInfo)
      .map(_.getEncounterInfo)
      .filter(_.hasCaresite)
      .map(_.getCaresite)

    val fromLabResults = patient.getLabResultsList
      .map(_.getLabResultInfo)
      .filter(_.hasCareSite)
      .map(_.getCareSite)

    (fromEncounters ++ fromLabResults).toList.asJava
  }

  def setIds(patientMeta: PatientMetaProto.PatientMeta, patientId: UUID, primaryExternalId: ExternalIdOuterClass.ExternalId): PatientMetaProto.PatientMeta = {
    patientMeta
      .toBuilder
      .setPatientId(convertUuid(patientId))
      .setPrimaryExternalId(primaryExternalId)
      .build()
  }

  def getAllEids(patientMeta: PatientMetaProto.PatientMeta): Seq[String] = {
    patientMeta.getExternalIdsList.map(CaretParser.toString).sorted.distinct
  }

  def getOidFromPartialPatient(patient: PatientProto.Patient): String = {
    getOid(patient.getBase.getParsingDetailsList, patient.getDocumentsList.map(_.getDocumentMeta).asJava)
  }

  def getOidFromDocMeta(docMeta: SummaryObjects.DocumentMetaSummary): String = {
    getOid(docMeta.getBase.getParsingDetailsList, List(docMeta.getDocumentMeta).asJava)
  }

  def getOid(parsingDetailsList: util.List[ParsingDetailsOuterClass.ParsingDetails], docMetaList: util.List[DocumentMetaOuterClass.DocumentMeta]) = {
    val parsingDetailsOid = getOidFromParsingDetailsList(parsingDetailsList)
    if (parsingDetailsOid != null && parsingDetailsOid.nonEmpty) {
      parsingDetailsOid
    } else {
      docMetaList.find(_.getUuid.getUuid.nonEmpty)
        .map(_.getUuid.getUuid)
        .orNull
    }
  }

  /**
   * Oid for patient data [no documents]
   * @param parsingDetailsList
   * @return
   */
  def getOidFromParsingDetailsList(parsingDetailsList: util.List[ParsingDetailsOuterClass.ParsingDetails]): String = {
    parsingDetailsList.find(_.hasSourceFileArchiveUUID).map(_.getSourceFileArchiveUUID.getUuid).orNull
  }


  /**
   * Document key for patient data that doesn't have a document associated with it
   * @param parsingDetailsList
   * @return
   */
  def getOidKeyFromParsingDetailsList(parsingDetailsList: util.List[ParsingDetailsOuterClass.ParsingDetails]): String = {
    parsingDetailsList.asScala
      .find(_.getSourceFileHash.nonEmpty)
      .map(_.getSourceFileHash)
      .getOrElse("")
  }

  /**
   * Get OidKey from document meta
   * @param documentMeta
   * @return
   */
  def getDocumentKey(documentMeta: DocumentMetaSummary): String = {
    val apo = DocumentMetaUtils.toApo(Seq(documentMeta)) // todo: Lazy to rewrite code
    com.apixio.dao.patient2.PatientUtility.getDocumentKey(apo)
  }
}
