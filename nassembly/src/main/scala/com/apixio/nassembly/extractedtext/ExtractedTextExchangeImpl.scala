package com.apixio.nassembly.extractedtext

class ExtractedTextExchangeImpl extends ExtractedTextExchangeBase {


  override def getDataTypeName: String = {
    ExtractedTextExchangeImpl.dataTypeName
  }
}

object ExtractedTextExchangeImpl {
  val dataTypeName = "extractedText"
}
