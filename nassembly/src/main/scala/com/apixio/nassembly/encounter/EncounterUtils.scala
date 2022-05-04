package com.apixio.nassembly.encounter

import com.apixio.datacatalog.BaseObjects.ClinicalActor
import com.apixio.datacatalog.ClinicalCodeOuterClass.ClinicalCode
import com.apixio.datacatalog.CodedBaseObjects.Encounter
import com.apixio.datacatalog.EncounterInfoOuterClass.EncounterInfo
import com.apixio.datacatalog.PatientProto
import com.apixio.datacatalog.PatientProto.CodedBasePatient
import com.apixio.datacatalog.SummaryObjects.{BaseSummary, EncounterSummary}
import com.apixio.model.patient.Patient
import com.apixio.nassembly.BaseConsolidator
import com.apixio.nassembly.apo.APOGenerator
import com.apixio.nassembly.clinicalactor.ClinicalActorUtils
import com.apixio.nassembly.util.BaseEntityUtil
import com.apixio.util.nassembly.SummaryUtils

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConverters._

object EncounterUtils {

  def mergeEncounterSummaries(encounters: List[EncounterSummary]): Iterable[EncounterSummary] = {
    encounters.filterNot(e => e == null || e.getSerializedSize == 0)
      .groupBy(_.getInternalId).map {
      case (_, duplicateEncounterSummList) =>
        val encounterSummary = duplicateEncounterSummList.head
        val encSummBuilder = encounterSummary.toBuilder

        val duplicateInfos = duplicateEncounterSummList.map(_.getEncounterInfo)
          .filter(e => e != null && e.getSerializedSize > 0)
        if (duplicateInfos.nonEmpty) {
          val encounterInfo = mergeEncounterInfos(duplicateInfos)
          encSummBuilder.setEncounterInfo(encounterInfo)
        }

        val baseSummary = buildBaseSummary(duplicateEncounterSummList)

        // Set primary actor
        duplicateEncounterSummList.find(_.getPrimaryActor.getSerializedSize > 0)
          .map(d => encSummBuilder.setPrimaryActor(d.getPrimaryActor))
        // Supplementary Actors

        val allSupplementaryActors = ClinicalActorUtils
          .mergeClinicalActors(duplicateEncounterSummList.flatMap(_.getSupplementaryActorsList))
        encSummBuilder
          .clearSupplementaryActors()
          .addAllSupplementaryActors(allSupplementaryActors.asJava)

        encSummBuilder.setBase(baseSummary)
        encSummBuilder.build()

    }
  }

  private def buildBaseSummary(summaries: List[EncounterSummary]): BaseSummary = {

    val baseSummaryBuilder = BaseSummary.newBuilder()
    // Set Catalog Meta
    val meta = BaseConsolidator
      .mergeCatalogMeta(summaries.map(_.getBase.getDataCatalogMeta).asJava)
    Option(meta).foreach(baseSummaryBuilder.setDataCatalogMeta)

    // Set Patient Meta
    val patientMeta = BaseConsolidator
      .mergePatientMeta(summaries.map(_.getBase.getPatientMeta).asJava)
    Option(patientMeta).foreach(baseSummaryBuilder.setPatientMeta)

    // Set Parsing Details
    val allParsingDetails = summaries.flatMap(ca => ca.getBase.getParsingDetailsList)
    val parsingDetailsAndIds = BaseConsolidator.dedupParsingDetails(allParsingDetails.asJava)
    baseSummaryBuilder.clearParsingDetails()
      .addAllParsingDetails(parsingDetailsAndIds)

    // Set Sources
    val allSources = summaries.flatMap(_.getBase.getSourcesList)
    val sources = BaseConsolidator.dedupSources(allSources.asJava)
    baseSummaryBuilder.clearSources().addAllSources(sources)
    baseSummaryBuilder.build()
  }

  def mergeEncounters(encounters: List[Encounter]): Iterable[Encounter] = {
    encounters.filterNot(e => e == null || e.getSerializedSize == 0)
      .groupBy(_.getInternalId).map {
      case (_, duplicates) =>
        val baseActor = duplicates.head
        val builder = baseActor.toBuilder

        val duplicateInfos = duplicates.map(_.getEncounterInfo).filter(e => e != null && e.getSerializedSize > 0)

        // encounter info
        if (duplicateInfos.nonEmpty) {
          val encounterInfo = mergeEncounterInfos(duplicateInfos)
          builder.setEncounterInfo(encounterInfo)
        }

        // Set Meta
        val meta = BaseConsolidator.mergeCatalogMeta(duplicates.map(_.getDataCatalogMeta).asJava)
        if (meta != null) builder.setDataCatalogMeta(meta)

        // Set Parsing Details
        val allParsingDetailsId = duplicates.flatMap(ca => ca.getParsingDetailsIdsList).distinct
        builder
          .clearParsingDetailsIds()
          .addAllParsingDetailsIds(allParsingDetailsId.asJava)

        // Set Sources
        val allSourceIds = duplicates.flatMap(ca => ca.getSourceIdsList).distinct
        builder
          .clearSourceIds()
          .addAllSourceIds(allSourceIds.asJava)

        // Set primary actor
        duplicates.find(_.getPrimaryActorId.getSerializedSize > 0)
          .map(d => builder.setPrimaryActorId(d.getPrimaryActorId))
        // Supplementary Actors
        val allSupplementaryActors = duplicates.flatMap(_.getSupplementaryActorIdsList).distinct
        builder
          .clearSupplementaryActorIds()
          .addAllSupplementaryActorIds(allSupplementaryActors.asJava)


        builder.build()
    }
  }

  def consolidateActors(encounters: List[EncounterSummary]): Iterable[ClinicalActor] = {
    val actors = encounters.map(_.getPrimaryActor) ++ encounters.flatMap(_.getSupplementaryActorsList)
    ClinicalActorUtils.mergeClinicalActors(actors)
  }

  def mergeEncounterInfos(infos: List[EncounterInfo]): EncounterInfo = {
    if (infos.isEmpty) null
    else {
      val head = infos.head
      val infoBuilder = head.toBuilder

      val validInfos = infos.filterNot(e => e == null || e.getSerializedSize == 0)

      val complaints: java.util.List[ClinicalCode] = BaseConsolidator
        .mergeCodes(validInfos.flatMap(_.getChiefComplaintsList).asJava)
      infoBuilder.addAllChiefComplaints(complaints)

      val startDateOpt = validInfos.find(_.getStartDate.getSerializedSize > 0).map(_.getStartDate)
      startDateOpt.foreach(infoBuilder.setStartDate)

      val endDateOpt = validInfos.find(_.getEndDate.getSerializedSize > 0).map(_.getEndDate)
      endDateOpt.foreach(infoBuilder.setEndDate)

      val encounterTypeOpt = validInfos.find(_.getEncounterType.nonEmpty).map(_.getEncounterType)
      encounterTypeOpt.foreach(infoBuilder.setEncounterType)

      val caresiteOpt = BaseConsolidator
        .dedupCareSites(validInfos.map(_.getCaresite).filter(_.getSerializedSize > 0).asJava)
      caresiteOpt.foreach(infoBuilder.setCaresite)

      infoBuilder.build()
    }
  }

  def toApo(encounters: List[EncounterSummary]): Patient = {
    // not as efficient, but less error prone
    val proto = toPatient(encounters)
    APOGenerator.fromProto(proto)
    //    val patient = new Patient()
    //    if (encounters.isEmpty) {
    //      patient
    //    }
    //    else {
    //      val clinicalActors = consolidateActors(encounters).map(BaseConverter.convertClinicalActor)
    //
    //      val parsingDetails = BaseConsolidator
    //        .dedupParsingDetails(encounters.flatMap(_.getParsingDetailsList).asJava)
    //        .map(BaseConverter.convertParsingDetails)
    //
    //      val sources = BaseConsolidator
    //        .dedupSources(encounters.flatMap(_.getSourcesList).asJava)
    //        .map(BaseConverter.convertSource)
    //
    //      val patientMeta = BaseConsolidator.mergePatientMeta(encounters.map(_.getPatientMeta).asJava)
    //      val convertedEncounters = {
    //        val mergedEncounters = mergeEncounterSummaries(encounters)
    //        val normalizedMergedEncounters = mergedEncounters.map(SummaryUtils.normalizeEncounter)
    //        normalizedMergedEncounters.map(CodedBaseConverter.convertEncounter)
    //      }
    //
    //      // Add patient Id
    //      val patientId = DataCatalogProtoUtils.convertUuid(patientMeta.getPatientId)
    //      patient.setPatientId(patientId)
    //
    //      // Add Primary ExternalId
    //      val primaryExternalId = BaseConverter.convertExternalId(patientMeta.getPrimaryExternalId)
    //      patient.setPrimaryExternalID(primaryExternalId)
    //
    //      // add externalIds
    //      patientMeta.getExternalIdsList.map(BaseConverter.convertExternalId).foreach(patient.addExternalId)
    //
    //
    //      patient.setClinicalActors(clinicalActors.toList.asJava)
    //      patient.setParsingDetails(parsingDetails.asJava)
    //      patient.setSources(sources.asJava)
    //      patient.setEncounters(convertedEncounters.toList.asJava)
    //
    //      patient
    //    }
  }

  /**
   * convert list of encounter summaries to a java Patient
   * @param encounters
   * @return null if list is empty
   */
  def toPatient(encounters: List[EncounterSummary]): PatientProto.Patient = {
    if (encounters.isEmpty) {
      null
    }
    else {
      val builder = PatientProto.Patient.newBuilder()
      val clinicalActors = consolidateActors(encounters)

      val parsingDetails = BaseConsolidator
        .dedupParsingDetails(encounters.flatMap(_.getBase.getParsingDetailsList).asJava)

      val sources = BaseConsolidator
        .dedupSources(encounters.flatMap(_.getBase.getSourcesList).asJava)

      val patientMeta = BaseConsolidator.mergePatientMeta(encounters.map(_.getBase.getPatientMeta).asJava)
      val dataCatalogMeta = BaseConsolidator
        .mergeCatalogMeta(encounters.map(_.getBase.getDataCatalogMeta).asJava)

      val normalizedEncounters = {
        val mergedEncounters = mergeEncounterSummaries(encounters)
        mergedEncounters.map(SummaryUtils.normalizeEncounter)
      }

      val basePatientBuilder: CodedBasePatient = BaseEntityUtil.buildCodedBasePatient(patientMeta, dataCatalogMeta,
        parsingDetails, sources, clinicalActors, normalizedEncounters)

      builder.setBase(basePatientBuilder)
      builder.build()
    }
  }

}
