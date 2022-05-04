package com.apixio.nassembly.util


import com.apixio.datacatalog.BaseObjects.{Base, ClinicalActor}
import com.apixio.datacatalog.CodedBaseObjects.Encounter
import com.apixio.datacatalog.DataCatalogMetaOuterClass.DataCatalogMeta
import com.apixio.datacatalog.ParsingDetailsOuterClass.ParsingDetails
import com.apixio.datacatalog.PatientProto.{BasePatient, CodedBasePatient}
import com.apixio.datacatalog.{DataCatalogMetaOuterClass, PatientMetaProto, SourceOuterClass, UUIDOuterClass}

import scala.collection.JavaConverters._

object BaseEntityUtil {

  def buildBaseObject(parsingDetailsId: UUIDOuterClass.UUID,
                      sourceId: UUIDOuterClass.UUID,
                      meta: DataCatalogMeta): Base = {
    val baseBuilder = Base.newBuilder()
    if (parsingDetailsId != null) baseBuilder.addParsingDetailsIds(parsingDetailsId)
    if (sourceId != null) baseBuilder.addSourceIds(sourceId)
    if (meta != null) baseBuilder.setDataCatalogMeta(meta)
    baseBuilder.build()
  }

  def buildCodedBasePatient(patientMeta: PatientMetaProto.PatientMeta,
                            dataCatalogMeta: DataCatalogMetaOuterClass.DataCatalogMeta,
                            parsingDetails: Iterable[ParsingDetails] = Iterable.empty,
                            sources: Iterable[SourceOuterClass.Source] = Iterable.empty,
                            clinicalActors: Iterable[ClinicalActor] = Iterable.empty,
                            encounters: Iterable[Encounter] = Iterable.empty): CodedBasePatient = {
    val basePatientBuilder = CodedBasePatient.newBuilder()
    if (patientMeta != null) basePatientBuilder.setPatientMeta(patientMeta)
    if (dataCatalogMeta != null) basePatientBuilder.setDataCatalogMeta(dataCatalogMeta)
    if (parsingDetails != null) basePatientBuilder.addAllParsingDetails(parsingDetails.asJava)
    if (sources != null) basePatientBuilder.addAllSources(sources.asJava)
    if (clinicalActors != null) basePatientBuilder.addAllClinicalActors(clinicalActors.asJava)
    if (encounters != null) basePatientBuilder.addAllEncounters(encounters.asJava)
    basePatientBuilder.build()
  }

  def buildBasePatient(patientMeta: PatientMetaProto.PatientMeta,
                       dataCatalogMeta: DataCatalogMetaOuterClass.DataCatalogMeta,
                       parsingDetails: Iterable[ParsingDetails] = Iterable.empty,
                       sources: Iterable[SourceOuterClass.Source] = Iterable.empty): BasePatient = {
    val basePatientBuilder = BasePatient.newBuilder()
    if (patientMeta != null) basePatientBuilder.setPatientMeta(patientMeta)
    if (dataCatalogMeta != null) basePatientBuilder.setDataCatalogMeta(dataCatalogMeta)
    if (parsingDetails != null) basePatientBuilder.addAllParsingDetails(parsingDetails.asJava)
    if (sources != null) basePatientBuilder.addAllSources(sources.asJava)
    basePatientBuilder.build()
  }


}