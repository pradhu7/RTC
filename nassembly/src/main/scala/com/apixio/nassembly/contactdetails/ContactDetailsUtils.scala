package com.apixio.nassembly.contactdetails

import com.apixio.datacatalog.BaseObjects.ContactDetails
import com.apixio.datacatalog.PatientProto
import com.apixio.datacatalog.PatientProto.CodedBasePatient
import com.apixio.datacatalog.SummaryObjects.ContactDetailsSummary
import com.apixio.nassembly.BaseConsolidator
import com.apixio.nassembly.util.BaseEntityUtil
import com.apixio.util.nassembly.SummaryUtils

import scala.math.Ordering.Implicits.seqDerivedOrdering
import scala.collection.JavaConverters._

object ContactDetailsUtils {


  def mergeContactDetailSummaries(values: List[ContactDetailsSummary]): Iterable[ContactDetailsSummary] = {
    // Just like AllergyMerger, uniqueness is entirety of contactInfo
    values.groupBy(a => a.getContactInfo)
      .values
      .map(summaries =>
        summaries.maxBy(summary => summary.getBase.getParsingDetailsList.asScala.map(pd => pd
          .getParsingDate)
        ))
  }

  def mergeContactDetails(cdList: List[ContactDetails]): Iterable[ContactDetails] = {
    cdList.groupBy(a => a.getContactInfo)
      .values
      .map(duplicates => {
        duplicates.maxBy(_.getBase.getDataCatalogMeta.getLastEditTime) // Take the latest
      })
  }

  private def getBase(summary: ContactDetailsSummary) = {
    summary.getBase
  }

  private def getSourceList(summary: ContactDetailsSummary) = {
    getBase(summary).getSourcesList
  }

  private def getParsingDetailsList(summary: ContactDetailsSummary) = {
    getBase(summary).getParsingDetailsList
  }

  def wrapAsPatient(summaryList: List[ContactDetailsSummary]): PatientProto.Patient = {
    val patientBuilder = PatientProto.Patient.newBuilder
    if (summaryList.isEmpty) return patientBuilder.build

    val rawParsingDetails = summaryList.flatMap(summary => getParsingDetailsList(summary).asScala).asJava
    val parsingDetails = BaseConsolidator.dedupParsingDetails(rawParsingDetails)

    val rawSources = summaryList.flatMap(summary => getSourceList(summary).asScala).asJava
    val sources = BaseConsolidator.dedupSources(rawSources)

    val patientMeta = BaseConsolidator.mergePatientMeta(summaryList.map(_.getBase.getPatientMeta).asJava)
    val dataCatalogMeta = BaseConsolidator.mergeCatalogMeta(summaryList.map(_.getBase.getDataCatalogMeta).asJava)
      .toBuilder
      .build()
    val basePatientBuilder: CodedBasePatient = BaseEntityUtil.buildCodedBasePatient(patientMeta, dataCatalogMeta,
      parsingDetails.asScala, sources.asScala)

    normalizeContactDetails(mergeContactDetailSummaries(summaryList).toSeq) match {
      case h :: t =>
        patientBuilder.setPrimaryContactDetails(h)
        patientBuilder.addAllAlternateContactDetails(t.asJava)
      case _ =>
    }

    patientBuilder.setBase(basePatientBuilder)
    patientBuilder.build
  }

  /**
   * ProcedureSummarys are flattened by supporting diagnosis code
   * We need to normalize them and then concat the lists together for the procedure identity
   *
   * @param contactDetails List of denormalized contact details
   * @return
   */
  def normalizeContactDetails(contactDetails: Seq[ContactDetailsSummary]): Seq[ContactDetails] = {
    contactDetails
      .sortBy(_.getBase.getParsingDetailsList.asScala.map(pd => pd.getParsingDate))
      .reverse
      .map(SummaryUtils.normalizeContactDetails)
  }

}