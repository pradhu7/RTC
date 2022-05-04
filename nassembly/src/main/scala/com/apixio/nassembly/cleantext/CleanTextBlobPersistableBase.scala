package com.apixio.nassembly.cleantext

import com.apixio.model.blob.BlobType
import com.apixio.model.nassembly.{BlobPersistable, Exchange}

import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.xml.bind.DatatypeConverter

trait CleanTextBlobPersistableBase[T <: CleanTextExchangeBase] extends BlobPersistable
{

  override def getSerializedValue(exchange: Exchange): Array[Byte] = {
    val docCacheElementListBytes = exchange.asInstanceOf[CleanTextExchangeBase].getCleanText.getDocCacheElementListProto.toByteArray

    // Copying from ContextExtractTransform
    val base64String = DatatypeConverter.printBase64Binary(docCacheElementListBytes)
    base64String.getBytes(StandardCharsets.UTF_8)
  }

  override def getBlobType(exchange: Exchange): BlobType = {
    val docId = exchange.asInstanceOf[CleanTextExchangeBase].getCleanText.getDocumentId.getUuid
    val documentUUID = UUID.fromString(docId)
    val builder = new BlobType.Builder(documentUUID, "cleantext_protobuf_b64")
    builder.build
  }

  // In-link persist to S3
  override def deferPersist(): Boolean = false
}
