package com.apixio.nassembly.cleantext

class NonOcrCleanTextExchangeImpl extends CleanTextExchangeBase {

  override def getDataTypeName: String = {
    NonOcrCleanTextExchangeImpl.dataTypeName
  }
}

object NonOcrCleanTextExchangeImpl {
  val dataTypeName = "docCacheElements"
}
