package com.apixio.nassembly.extractedtext

class StringContentExchangeImpl extends ExtractedTextExchangeBase {


  override def getDataTypeName: String = {
    StringContentExchangeImpl.dataTypeName
  }
}

object StringContentExchangeImpl {
  val dataTypeName = "stringContent"
}
