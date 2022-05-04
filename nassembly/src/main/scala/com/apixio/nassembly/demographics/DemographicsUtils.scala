package com.apixio.nassembly.demographics

import com.apixio.datacatalog.BaseObjects.PatientDemographics
import com.apixio.datacatalog.DemographicsInfoProto.DemographicsInfo
import com.apixio.datacatalog.ExternalIdOuterClass.ExternalId
import com.apixio.datacatalog.GenderOuterClass.Gender
import com.apixio.datacatalog.MaritalStatusOuterClass.MaritalStatus
import com.apixio.datacatalog.PatientProto.{BasePatient, CodedBasePatient}
import com.apixio.datacatalog.SummaryObjects.DemographicsSummary
import com.apixio.datacatalog.{BaseObjects, ParsingDetailsOuterClass, SkinnyPatientProto, SourceOuterClass}
import com.apixio.nassembly.BaseConsolidator
import com.apixio.nassembly.util.BaseEntityUtil
import com.apixio.util.nassembly.SummaryUtils

import scala.collection.JavaConverters._

object DemographicsUtils {

  def wrapAsSkinnyPatient(demographics: List[DemographicsSummary],
                          contactDetails: List[BaseObjects.ContactDetails]): Option[SkinnyPatientProto.Demographics] = {
    val builder = SkinnyPatientProto.Demographics.newBuilder
    // Be safe and remove empty summaries
    val filteredDemographics = demographics.filterNot(ds => {
      val hasNoAlternateIds = EmptyDemographicUtil.isEmpty(ds.getBase.getPatientMeta)
      lazy val isEmptyDemographicsInfo = EmptyDemographicUtil.isEmpty(ds.getDemographicsInfo)
      hasNoAlternateIds && isEmptyDemographicsInfo
    })

    if (filteredDemographics.isEmpty)
      None
    else {

      //patientMeta, dataCatalogMeta, sources, parsingDetails, demographics
      val patientMeta = BaseConsolidator.mergePatientMeta(filteredDemographics.map(_.getBase.getPatientMeta).asJava)
      val catalogMeta =
        BaseConsolidator.mergeCatalogMeta(filteredDemographics.map(_.getBase.getDataCatalogMeta).asJava)
          .toBuilder
          .setOriginalId(ExternalId.newBuilder().setAssignAuthority("Demographics").build())
          .build()

      val sources = dedupAllSources(filteredDemographics)
      val parsingDetails = dedupAllParsingDetails(filteredDemographics)
      val sortedDemographics = filteredDemographics.sortBy(d => {
        // There should only be 1 source / 1 parsing details [or 0 if really old data]

        val sources = d.getBase.getSourcesList
        val sourceDate: Long = sources.asScala.headOption.map(_.getCreationDate).getOrElse(0L)

        val parsingDetails = d.getBase.getParsingDetailsList
        val parsingDate: Long = parsingDetails.asScala.headOption.map(_.getParsingDate).getOrElse(0L)

        (-1 * sourceDate, -1 * parsingDate)
      })

      val sortedNormalizedDemographics = normalizeDemographics(sortedDemographics)

      if (sortedNormalizedDemographics.nonEmpty) {
        // We "Combine" the demographics to get the primary
        builder.setPrimaryDemographics(sortedNormalizedDemographics.head.toBuilder.setDemographicsInfo(combineDemographicInfo(sortedNormalizedDemographics.filter(_.hasDemographicsInfo).map(_.getDemographicsInfo))).build())
        builder.addAllAlternateDemographics(sortedNormalizedDemographics.asJava)
      }
      if (contactDetails.nonEmpty) {
        // First in the list is the primary
        builder.setPrimaryContactDetails(contactDetails.head)
        builder.addAllAlternateContactDetails(contactDetails.asJava)
      }

      val basePatient: BasePatient = BaseEntityUtil.buildBasePatient(patientMeta, catalogMeta, parsingDetails.asScala, sources.asScala)

      builder.setBasePatient(basePatient)
      Some(builder.build())
    }
  }

  def normalizeDemographics(demographics: Seq[DemographicsSummary]): Seq[PatientDemographics] = {
    demographics.map(SummaryUtils.normalizeDemographic)
  }

  def dedupAllSources(demographics: Seq[DemographicsSummary]): java.util.List[SourceOuterClass.Source] = {
    BaseConsolidator.dedupSources(demographics.flatMap(demo => demo.getBase.getSourcesList.asScala).asJava)
  }

  def dedupAllParsingDetails(demographicsSummaries: List[DemographicsSummary]): java.util.List[ParsingDetailsOuterClass.ParsingDetails] = {
    val rawParsingDetails = demographicsSummaries
      .flatMap(summary => summary.getBase.getParsingDetailsList.asScala)
    BaseConsolidator.dedupParsingDetails(rawParsingDetails.asJava)
  }

  def combineDemographicInfo(demographicInfos: Seq[DemographicsInfo]): DemographicsInfo = {
    if (demographicInfos.isEmpty) null
    else {
      val infoBuilder = DemographicsInfo.newBuilder()
      demographicInfos
        .filterNot(di => EmptyDemographicUtil.isEmpty(di))
        .foreach(partialDemographics => {
          if (infoBuilder.getGender == Gender.UNKNOWN)
            infoBuilder.setGender(partialDemographics.getGender)

          if (infoBuilder.getMartialStatus == MaritalStatus.UNKNOWN_MARITAL_STATUS)
            infoBuilder.setMartialStatus(partialDemographics.getMartialStatus)

          if (infoBuilder.getReligiousAffiliation.isEmpty)
            infoBuilder.setReligiousAffiliation(partialDemographics.getReligiousAffiliation)

          if (!infoBuilder.hasDob && partialDemographics.hasDob)
            infoBuilder.setDob(partialDemographics.getDob)

          if (!infoBuilder.hasDod && partialDemographics.hasDod)
            infoBuilder.setDod(partialDemographics.getDod)

          if (!infoBuilder.hasPrimaryCareProvider && partialDemographics.hasPrimaryCareProvider)
            infoBuilder.setPrimaryCareProvider(partialDemographics.getPrimaryCareProvider)

          if (EmptyDemographicUtil.isEmpty(infoBuilder.getName) && partialDemographics.hasName)
            infoBuilder.setName(partialDemographics.getName)

          if (!infoBuilder.hasRace && partialDemographics.hasRace)
            infoBuilder.setRace(partialDemographics.getRace)

          if (!infoBuilder.hasEthnicity && partialDemographics.hasEthnicity)
            infoBuilder.setEthnicity(partialDemographics.getEthnicity)


          // Add languages
          partialDemographics.getLanguagesList.asScala
            .filterNot(infoBuilder.getLanguagesList.asScala.contains)
            .foreach(infoBuilder.addLanguages)


        })

      infoBuilder.build()
    }
  }


  def createEmpty(codedBasePatient: CodedBasePatient): Option[DemographicsSummary] = {
    val patientMeta = codedBasePatient.getPatientMeta
    if (EmptyDemographicUtil.isEmpty(patientMeta)) {
      None
    }
    else {
      val parsingDetails = codedBasePatient.getParsingDetailsList
      val sources = codedBasePatient.getSourcesList
      val base = BaseObjects.Base.newBuilder
        .setDataCatalogMeta(codedBasePatient.getDataCatalogMeta)
        .addAllSourceIds(sources.asScala.map(_.getInternalId).asJava)
        .addAllParsingDetailsIds(parsingDetails.asScala.map(_.getInternalId).asJava)
        .build()

      // Leave demographics info as null
      val emptyDemographics = BaseObjects.PatientDemographics
        .newBuilder()
        .setBase(base)
        .build()

      val summary = SummaryUtils.createDemographicsSummary(emptyDemographics, patientMeta, parsingDetails, sources)
      Some(summary)
    }
  }

}
