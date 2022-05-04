package com.apixio.util.nassembly

import com.apixio.datacatalog.NameOuterClass

import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.collection.JavaConverters._

object NameUtils {

  def combineNames(names: List[NameOuterClass.Name]): NameOuterClass.Name = {
    names.filter(_ != null) match {
      case Nil => null
      case filtered =>
        val givenNames: List[String] = filtered.flatMap(_.getGivenNamesList).distinct
        val familyNames: List[String] = filtered.flatMap(_.getFamilyNamesList).distinct
        val prefixes: List[String] = filtered.flatMap(_.getPrefixesList).distinct
        val suffixes: List[String] = filtered.flatMap(_.getSuffixesList).distinct

        NameOuterClass.Name.newBuilder()
          .addAllGivenNames(givenNames.asJava)
          .addAllFamilyNames(familyNames.asJava)
          .addAllPrefixes(prefixes.asJava)
          .addAllSuffixes(suffixes.asJava)
          .build()
    }
  }

  def isEmpty(name: NameOuterClass.Name): Boolean = {
    !name.getFamilyNamesList.exists(_.replaceAll(" ","").nonEmpty) &&
      !name.getGivenNamesList.exists(_.replaceAll(" ","").nonEmpty) &&
      !name.getPrefixesList.exists(_.replaceAll(" ","").nonEmpty) &&
      !name.getSuffixesList.exists(_.replaceAll(" ","").nonEmpty)
  }
}
