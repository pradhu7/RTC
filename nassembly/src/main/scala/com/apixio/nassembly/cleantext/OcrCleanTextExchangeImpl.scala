package com.apixio.nassembly.cleantext

class OcrCleanTextExchangeImpl extends CleanTextExchangeBase {

  override def getDataTypeName: String = {
    OcrCleanTextExchangeImpl.dataTypeName
  }
}

object OcrCleanTextExchangeImpl {
  val dataTypeName = "ocrDocCacheElements"
}