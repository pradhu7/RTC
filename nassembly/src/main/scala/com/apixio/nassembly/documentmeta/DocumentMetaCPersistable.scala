package com.apixio.nassembly.documentmeta

import com.apixio.model.nassembly.{AssemblySchema, CPersistable}
import com.apixio.model.nassembly.Base.Oid
import com.apixio.model.nassembly.CPersistable.ColValueWithPersistentField

import java.util
import scala.collection.JavaConversions._

class DocumentMetaCPersistable extends CPersistable[DocumentMetaExchange] {
  override def getDataTypeName: String = {
    DocumentMetaExchange.dataTypeName
  }



  override def getSchema: AssemblySchema = {
    val col1: AssemblySchema.AssemblyCol = new AssemblySchema.AssemblyCol("id", AssemblySchema.ColType.StringType)
    val cols:  Array[AssemblySchema.AssemblyCol] = Array(col1)

    new AssemblySchema(cols)
  }

  // just use default "col" name for schema
  override def clusteringColumnsWithValues(exchange: DocumentMetaExchange): util.List[ColValueWithPersistentField] = {
    val cols: Array[AssemblySchema.AssemblyCol] = getSchema.getClusteringCols
    val oid = exchange.getDocuments.head.getDocumentMeta.getUuid.getUuid
    val docId: ColValueWithPersistentField = new ColValueWithPersistentField(oid, cols(0).getName)
    List(docId)
  }
}
