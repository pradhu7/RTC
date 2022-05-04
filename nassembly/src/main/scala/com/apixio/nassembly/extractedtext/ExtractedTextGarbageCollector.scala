package com.apixio.nassembly.extractedtext

class ExtractedTextGarbageCollector extends TextGarbageCollectorBase {

  override def getDataTypeName: String = {
    ExtractedTextExchangeImpl.dataTypeName
  }

}