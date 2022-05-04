package com.apixio.nassembly.extractedtext

class StringContentBlobPersistableImpl extends BlobPersistableBase[StringContentExchangeImpl] {

  override def getDataTypeName: String = StringContentExchangeImpl.dataTypeName

  override def getImageType: String = "string_content"
}
