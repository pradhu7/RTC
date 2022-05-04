package com.apixio.nassembly.legacycoverage

import com.apixio.datacatalog.PatientProto.CodedBasePatient
import com.apixio.datacatalog.SummaryObjects.CoverageSummary
import com.apixio.datacatalog.{PatientProto, SummaryObjects}
import com.apixio.model.patient.Patient
import com.apixio.nassembly.BaseConsolidator
import com.apixio.nassembly.apo.APOGenerator
import com.apixio.nassembly.util.BaseEntityUtil
import com.apixio.util.nassembly.SummaryUtils

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConverters._

object LegacyCoverageUtils {

  def toApo(coverages: List[CoverageSummary]): Patient = {
    // not as efficient, but less error prone
    val proto = toPatient(coverages)
    APOGenerator.fromProto(proto)
  }

  def toPatient(coverages: List[CoverageSummary]): PatientProto.Patient = {
    if (coverages.isEmpty)
      null
    else {
      val builder = PatientProto.Patient.newBuilder()
      val parsingDetails = BaseConsolidator
        .dedupParsingDetails(coverages.flatMap(_.getBase.getParsingDetailsList).asJava)

      val sources = BaseConsolidator
        .dedupSources(coverages.flatMap(_.getBase.getSourcesList).asJava)

      val patientMeta = BaseConsolidator.mergePatientMeta(coverages.map(_.getBase.getPatientMeta).asJava)
      val dataCatalogMeta = BaseConsolidator
        .mergeCatalogMeta(coverages.map(_.getBase.getDataCatalogMeta).asJava)

      val basePatientBuilder: CodedBasePatient = BaseEntityUtil.buildCodedBasePatient(patientMeta, dataCatalogMeta,
        parsingDetails, sources)

      val normalizedCoverages = coverages.map(SummaryUtils.normalizeCoverage)

      builder.setBase(basePatientBuilder)
      builder.addAllCoverage(normalizedCoverages.asJava)
      builder.build()
    }

  }
}
