package com.apixio.nassembly.exchangeutils

import com.apixio.model.patient._
import org.joda.time.DateTime
import MockApoUtil._
import com.apixio.datacatalog.EncounterInfoOuterClass.EncounterInfo
import com.apixio.datacatalog.{CareSiteOuterClass, PatientProto}
import com.apixio.nassembly.exchangeutils.TestVerificationUtil.{verifyClinicalCodeByConverting, verifyDates, verifyStrings}
import com.apixio.nassembly.patient.SeparatorUtils

import java.util.UUID
import scala.collection.JavaConverters._

object EncounterTestUtil {

  //this doesnt work when different encounters are generated
  var srcEncounter: Encounter = null

  def generateEncounterData = {
    val encounter = new Encounter
    encounter.setChiefComplaints(List(generateClinicalCode).asJava)
    encounter.setEncounterEndDate(new DateTime())
    encounter.setEncounterId(UUID.randomUUID())
    encounter.setEncounterStartDate(DateTime.now.minusDays(2))
    encounter.setEncType(EncounterType.SURGERY_CASE)
    encounter.setSiteOfService(populateCareSiteData())
    srcEncounter = encounter
    encounter
  }

  def generateEncounterWithoutCareSiteAddress(careSiteId: UUID): Encounter = {
    val encounter = new Encounter
    encounter.setChiefComplaints(List(generateClinicalCode).asJava)
    encounter.setEncounterEndDate(new DateTime())
    encounter.setEncounterId(UUID.randomUUID())
    encounter.setEncounterStartDate(DateTime.now.minusDays(2))
    encounter.setEncType(EncounterType.SURGERY_CASE)
    encounter.setSiteOfService(populateCareSiteDataWithoutAddress(Some(careSiteId)))
    srcEncounter = encounter
    encounter
  }

  def populateCareSiteData(id: Option[UUID] = Option.empty[UUID]): CareSite = {
    val careSite = new CareSite
    if (id.isDefined) careSite.setCareSiteId(id.get) else careSite.setCareSiteId(UUID.randomUUID())
    careSite.setCareSiteType(CareSiteType.PODIATRY)
    careSite.setCareSiteName("some_care_site_name")
    val address: Address = ContactDetailsTestUtil.populateAddress
    careSite.setAddress(address)
    careSite
  }

  private def populateCareSiteDataWithoutAddress(id: Option[UUID] = Option.empty[UUID]): CareSite = {
    val careSite = new CareSite
    if (id.isDefined) careSite.setCareSiteId(id.get) else careSite.setCareSiteId(UUID.randomUUID())
    careSite.setCareSiteType(CareSiteType.PODIATRY)
    careSite.setCareSiteName("some_care_site_name")
    careSite
  }

  def assertEncounter(patientProto: PatientProto.Patient) = {
    val summaries = SeparatorUtils.separateEncounters(patientProto)
    assert(summaries.size == 1)
    val encounterInfo = summaries.head.getEncounterInfo
    verifyEncounters(encounterInfo)
  }

  def assertEncounter(patientProto: PatientProto.Patient, modelEncounter: Encounter): Unit = {
    val summaries = SeparatorUtils.separateEncounters(patientProto)
    assert(summaries.size == 1)
    val encounterInfo = summaries.head.getEncounterInfo
    verifyEncounters(encounterInfo, modelEncounter)
  }

  private def verifyEncounters(encounterInfo: EncounterInfo, modelEncounter: Encounter): Unit = {
    verifyClinicalCodeByConverting(encounterInfo.getChiefComplaints(0), modelEncounter.getChiefComplaints.get(0))
    verifyDates(encounterInfo.getEndDate, modelEncounter.getEncounterEndDate)
    verifyDates(encounterInfo.getStartDate, modelEncounter.getEncounterStartDate)
    verifyStrings(encounterInfo.getEncounterType, modelEncounter.getEncType.name())
    verifyCareSite(encounterInfo.getCaresite, modelEncounter.getSiteOfService)
  }

  private def verifyEncounters(encounterInfo: EncounterInfo): Unit = {
    verifyEncounters(encounterInfo, srcEncounter)
  }

  private def verifyCareSite(protoCareSite: CareSiteOuterClass.CareSite, modelCareSite: CareSite) = {
    verifyStrings(protoCareSite.getName, modelCareSite.getCareSiteName)
    verifyStrings(protoCareSite.getType, modelCareSite.getCareSiteType.name())
    ContactDetailsTestUtil.verifyAddress(protoCareSite.getAddress, modelCareSite.getAddress)
  }

}