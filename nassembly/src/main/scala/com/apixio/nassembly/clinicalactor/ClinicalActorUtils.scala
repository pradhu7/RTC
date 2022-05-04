package com.apixio.nassembly.clinicalactor

import com.apixio.datacatalog.BaseObjects.{Base, ClinicalActor}
import com.apixio.datacatalog.ClinicalActorInfoOuterClass.ClinicalActorInfo
import com.apixio.datacatalog.SummaryObjects.ClinicalActorSummary
import com.apixio.datacatalog._
import com.apixio.nassembly.BaseConsolidator
import com.apixio.nassembly.mergeutils.MergeHelper
import com.apixio.util.nassembly.{ContactInfoUtils, NameUtils}

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

object ClinicalActorUtils {

  def mergeClinicalActorSummaries(actors: List[ClinicalActorSummary]): Iterable[ClinicalActorSummary] = {
    actors.filterNot(e => e == null)
      .groupBy(_.getInternalId).map {
      case (_, duplicates) =>

        val baseActor = duplicates.maxBy(_.getDataCatalogMeta.getLastEditTime)
        val builder = baseActor.toBuilder

        // set actor info
        val actorInfo = mergeClinicalActorInfos(duplicates.map(_.getClinicalActorInfo))
        builder.setClinicalActorInfo(actorInfo)

        // Set Meta
        val meta = BaseConsolidator.mergeCatalogMeta(duplicates.map(_.getDataCatalogMeta).asJava)
        if (meta != null) builder.setDataCatalogMeta(meta)

        // Set Parsing Details
        val allParsingDetails = duplicates.flatMap(ca => ca.getParsingDetailsList)
        val parsingDetailsAndIds = BaseConsolidator.dedupParsingDetails(allParsingDetails.asJava)
          .sortBy(pd => -pd.getParsingDate)
          .take(100) // only take max 100
        builder.clearParsingDetails()
          .addAllParsingDetails(parsingDetailsAndIds.asJava)

        // Set Sources
        val allSources = duplicates.flatMap(_.getSourcesList)
        val sources = BaseConsolidator.dedupSources(allSources.asJava)
          .sortBy(s => -s.getCreationDate)
          .take(100) // only take max 100
        builder.clearSources()
          .addAllSources(sources.asJava)

        builder.build
    }
  }

  def mergeClinicalActors(actors: List[ClinicalActor]): Iterable[ClinicalActor] = {
    actors.filterNot(e => e == null || e.getSerializedSize == 0)
      .groupBy(_.getInternalId).map {
      case (_, duplicates) =>

        //        val baseActor = getMaxByLastEditTime(duplicates)
        val baseActor = duplicates.maxBy(getDataCatalogMetaFromBase(_).getLastEditTime)
        val builder = baseActor.toBuilder
        val baseBuilder = Base.newBuilder()

        // set actor info
        val actorInfo = mergeClinicalActorInfos(duplicates.map(_.getClinicalActorInfo))
        builder.setClinicalActorInfo(actorInfo)

        // Set Meta
        val meta = BaseConsolidator.mergeCatalogMeta(duplicates.map(getDataCatalogMetaFromBase).asJava)
        baseBuilder.setDataCatalogMeta(meta)

        // Set Parsing Details
        val parsingDetailsIds = duplicates.flatMap(ca => ca.getBase.getParsingDetailsIdsList).distinct
        baseBuilder
          .clearParsingDetailsIds()
          .addAllParsingDetailsIds(parsingDetailsIds.asJava)

        // Set Sources
        val sourceIds = duplicates.flatMap(_.getBase.getSourceIdsList).distinct
        baseBuilder.clearSourceIds()
          .addAllSourceIds(sourceIds.asJava)

        builder.setBase(baseBuilder.build())
        builder.build
    }
  }

  private def getDataCatalogMetaFromBase(clinicalActor: ClinicalActor): DataCatalogMetaOuterClass.DataCatalogMeta = {
    clinicalActor.getBase.getDataCatalogMeta
  }

  // Ported from assemblyUtility
  def notEmpty(actor: ClinicalActor): Boolean = {
    lazy val actorInfo = actor.getClinicalActorInfo

    if (actor == null || !actor.hasClinicalActorInfo || actorInfo.getSerializedSize == 0)
      false
    else {
      val primaryId = actorInfo.getPrimaryId
      lazy val hasExternalId = primaryId != null && (primaryId.getId.nonEmpty || primaryId.getAssignAuthority.nonEmpty)
      lazy val hasNameOrContactDetails = actorInfo.hasActorGivenName || actorInfo.hasContactDetails
      lazy val hasRole = actorInfo.getActorRole.nonEmpty

      hasExternalId || hasNameOrContactDetails || hasRole
    }
  }

  def mergeClinicalActorInfos(infos: List[ClinicalActorInfo]): ClinicalActorInfo = {
    val validInfos = infos.filterNot(e => e == null || e.getSerializedSize == 0)

    validInfos.headOption.map(_.toBuilder).map(infoBuilder => {
      // Contact Details
      val contactDetails = ContactInfoUtils.mergeContactDetails(validInfos.map(_.getContactDetails))
      if (contactDetails != null) infoBuilder.setContactDetails(contactDetails)

      // Name
      val name = NameUtils.combineNames(validInfos.filter(_.hasActorGivenName).map(_.getActorGivenName))
      if (name != null) infoBuilder.setActorGivenName(name)

      // Set Supplementary Names
      val supplementaryNames = validInfos.flatMap(_.getActorSupplementalNamesList).filter(_ != null)
      infoBuilder.clearActorSupplementalNames()
        .addAllActorSupplementalNames(supplementaryNames.asJava)

      //Set Alternate Ids
      val alternateIds = validInfos.flatMap(_.getAlternateIdsList).distinct
      infoBuilder.clearAlternateIds()
        .addAllAlternateIds(alternateIds.asJava)

      // Check Title
      if (infoBuilder.getTitle.isEmpty) {
        validInfos.find(_.getTitle.nonEmpty).foreach(a => infoBuilder.setTitle(a.getTitle))
      }
      // Check Role
      if (infoBuilder.getActorRole.isEmpty) {
        validInfos.find(_.getActorRole.nonEmpty).foreach(a => infoBuilder.setActorRole(a.getActorRole))
      }

      // Check Org
      val mergedOrg = MergeHelper.mergeOrganization(validInfos.filter(_.hasAssociatedOrg).map(_.getAssociatedOrg))
      Option(mergedOrg).foreach(infoBuilder.setAssociatedOrg)

      infoBuilder.build()
    }).getOrElse(ClinicalActorInfo.getDefaultInstance)
  }


  def dedupClincalActors(actors: Seq[ClinicalActor]): Iterable[ClinicalActor] = {
    actors.groupBy(_.getInternalId).map {
      case (_, duplicates) => duplicates.maxBy(getDataCatalogMetaFromBase(_).getLastEditTime)
    }
  }
}
