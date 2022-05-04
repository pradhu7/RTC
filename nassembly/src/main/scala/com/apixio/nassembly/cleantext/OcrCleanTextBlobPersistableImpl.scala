package com.apixio.nassembly.cleantext

class OcrCleanTextBlobPersistableImpl extends CleanTextBlobPersistableBase[OcrCleanTextExchangeImpl] {

  override def getDataTypeName: String = OcrCleanTextExchangeImpl.dataTypeName
}