package com.apixio.nassembly.patient.meta

import com.apixio.model.indexing.{IndexableType, IndexedField}
import com.apixio.model.nassembly.Indexer

import scala.collection.JavaConverters._

class PatientMetaIndexer extends Indexer[PatientMetaExchange] {

  //note: fieldName must match column from DataFrame in IndexerService to be able to get the value
  override def getIndexedFields(exchange: PatientMetaExchange): java.util.Set[IndexedField] = {
    val patientMeta = exchange.getProto
    val uuid = new IndexedField(patientMeta.getPatientId.getUuid, IndexableType.UUID)
    val primaryExternalId = new IndexedField(patientMeta.getPrimaryExternalId.getId, IndexableType.PID)
    val externalIds: Seq[IndexedField] = patientMeta.getExternalIdsList.asScala.map(eid => new IndexedField(eid.getId, IndexableType.AID))
    (Set(uuid, primaryExternalId) ++ externalIds).asJava
  }

  override def getDataTypeName: String = PatientMetaExchange.dataTypeName
}
