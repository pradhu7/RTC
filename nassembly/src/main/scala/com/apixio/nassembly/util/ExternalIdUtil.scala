package com.apixio.nassembly.util

import com.apixio.datacatalog.{ExternalIdOuterClass, PatientMetaProto}
import scala.collection.JavaConverters._

object ExternalIdUtil {

  def getExternalIds(patientMetas: Seq[PatientMetaProto.PatientMeta]): Array[ExternalIdOuterClass.ExternalId] = {
    patientMetas.flatMap(_.getExternalIdsList.asScala)
      .distinct
      .filterNot(_.getSerializedSize == 0)
      .toArray
  }

}
