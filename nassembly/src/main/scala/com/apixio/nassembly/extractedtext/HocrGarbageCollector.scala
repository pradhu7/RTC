package com.apixio.nassembly.extractedtext

class HocrGarbageCollector extends TextGarbageCollectorBase {

  override def getDataTypeName: String = {
    HocrExchangeImpl.dataTypeName
  }

}