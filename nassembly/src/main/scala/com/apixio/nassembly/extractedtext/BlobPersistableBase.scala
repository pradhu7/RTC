package com.apixio.nassembly.extractedtext

import com.apixio.model.blob.BlobType
import com.apixio.model.nassembly.{BlobPersistable, Exchange}

import java.util.UUID

trait BlobPersistableBase[T <: ExtractedTextExchangeBase] extends BlobPersistable {

  override def getSerializedValue(exchange: Exchange): Array[Byte] = {
    exchange.asInstanceOf[ExtractedTextExchangeBase].getContent.getStringContent.toByteArray
  }

  override def getBlobType(exchange: Exchange): BlobType = {
    val docId = exchange.asInstanceOf[ExtractedTextExchangeBase].getContent.getDocumentId.getUuid
    getBlobType(docId)
  }

  def getBlobType(docId: String): BlobType = {
    val imageType = getImageType
    val documentUUID = UUID.fromString(docId)
    val builder = new BlobType.Builder(documentUUID, imageType)
    builder.build
  }

  def getImageType: String
}
