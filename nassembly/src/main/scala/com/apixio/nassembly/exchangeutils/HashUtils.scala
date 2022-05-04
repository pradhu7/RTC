package com.apixio.nassembly.exchangeutils

import java.security.MessageDigest

object HashUtils {

  def getHash(d: Array[Byte]): String = {
    val digestInstance: MessageDigest = MessageDigest.getInstance("MD5")
    digestInstance.digest(d).toString
  }
}
