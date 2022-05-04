package com.apixio.nassembly.demographics

import com.apixio.datacatalog.NameOuterClass.Name
import com.apixio.datacatalog.PatientMetaProto.PatientMeta
import com.apixio.datacatalog.DemographicsInfoProto.DemographicsInfo

import scala.collection.JavaConverters._

object EmptyDemographicUtil {

  def isEmpty(name: Name): Boolean = {
    if (name == null) true
    else {
      name.getFamilyNamesList.asScala.forall(_.trim.isEmpty) &&
        name.getGivenNamesList.asScala.forall(_.trim.isEmpty)
    }
  }

  def isEmpty(patientMeta: PatientMeta): Boolean = {
    val externalIds = patientMeta.getExternalIdsList
    val primary = patientMeta.getPrimaryExternalId
    val hasAlternateId = externalIds.asScala.exists(id => !id.equals(primary) && (id.getId.nonEmpty || id.getAssignAuthority.nonEmpty))
    !hasAlternateId
  }

  def isEmpty(demographicsInfo: DemographicsInfo): Boolean = {
    EmptyDemographicUtil.isEmpty(demographicsInfo.getName) &&
      !demographicsInfo.hasDob &&
      !demographicsInfo.hasDod &&
      !demographicsInfo.hasRace &&
      !demographicsInfo.hasEthnicity &&
      !demographicsInfo.hasPrimaryCareProvider &&
      demographicsInfo.getLanguagesCount == 0 &&
      demographicsInfo.getReligiousAffiliation.trim.isEmpty
  }

}
