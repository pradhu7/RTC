package com.apixio.nassembly.extractedtext

class ExtractedTextBlobPersistableImpl extends BlobPersistableBase[ExtractedTextExchangeImpl] {

  override def getDataTypeName: String = ExtractedTextExchangeImpl.dataTypeName

  override def getImageType: String = "text_extracted"
}
