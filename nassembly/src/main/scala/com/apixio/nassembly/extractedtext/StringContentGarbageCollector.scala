package com.apixio.nassembly.extractedtext

class StringContentGarbageCollector extends TextGarbageCollectorBase {

  override def getDataTypeName: String = {
    StringContentExchangeImpl.dataTypeName
  }

}