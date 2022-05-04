package com.apixio.nassembly.cleantext

import com.apixio.dao.utility.PageUtility
import com.apixio.model.blob.BlobType
import com.apixio.model.nassembly.Combiner.CombinerInput
import com.apixio.model.nassembly.Exchange
import com.apixio.nassembly.documentmeta.DocumentMetaExchange
import com.apixio.nassembly.model.S3Accessor

import java.util
import java.util.UUID

object S3StringContentAccessor extends S3Accessor {
  override def getBlobType(combinerInput: CombinerInput): BlobType = {
    val documentMetaExchanges = combinerInput.getDataTypeNameToExchanges.get(DocumentMetaExchange.dataTypeName)
    documentMetaExchanges.headOption.flatMap(e => {
      e.asInstanceOf[DocumentMetaExchange].getDocuments.headOption.map(docMetaSummary => {
        PageUtility.getBlobTypeForLevel1StringContent(UUID.fromString(docMetaSummary.getDocumentMeta.getUuid.getUuid))
      })
    }).orNull
  }

  override def getDataTypeName: String = PageUtility.STRING_CONTENT

}
