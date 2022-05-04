package com.apixio.nassembly.mergeutils

import com.apixio.model.nassembly.Exchange

import scala.collection.JavaConverters._

object MergerUtils {
  /**
   * Easily wrap merger bizlogic with checks for empty, size 1, and non empty
   * @param exchanges List of exchanges
   * @param mergeLogic bizlogic for merging exchanges together
   * @return
   */
  def wrapMergerBizlogic[T<: Exchange](exchanges: java.util.List[T], mergeLogic: (java.util.List[T] => T)): T = {
    exchanges.asScala.toList match {
      case null =>
        throw new Exception("Can't merge a null list")
      case Nil =>
        throw new Exception("Can't merge an empty list")
      case h :: Nil =>
        h
      case h :: _ =>
        mergeLogic(exchanges)
    }
  }
}
