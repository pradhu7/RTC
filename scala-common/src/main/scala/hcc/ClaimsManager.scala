package com.apixio.scala.utility.hcc

import java.time.Year
import java.util.UUID

import com.apixio.model.patient.{Patient, Procedure}
import com.apixio.model.utility.PatientJSONParser
import com.apixio.scala.apxapi.{ApxApi, Project}
import com.apixio.model.external.CodingSystem
import com.apixio.scala.logging.ApixioLoggable
import com.apixio.scala.subtraction.FFSManager.RichProcedure

import scala.collection.JavaConverters._
import org.joda.time.DateTime
import com.apixio.scala.dw.ApxServices

// this will throw exceptions for any service that does can not obtain an internal token
@deprecated("Legacy: see com.apixio.app.subtraction.FFSManager")
object ClaimsManager extends ApixioLoggable {
  setupLog(getClass.getCanonicalName)
  val patientParser = new PatientJSONParser()
  val f2fCptMap = CptParser.getF2fCptCodes()

  def getProcedureNpi(patient: Patient)(proc: Procedure): Option[String] =
    Option(patient.getClinicalActorById(proc.getPrimaryClinicalActorId))
      .filter(_.getPrimaryId.getAssignAuthority == "NPI")
      .map(_.getPrimaryId.getId)

  def getPatient(patientId: String): Patient = {
    ApxServices.patientLogic.getPatient(UUID.fromString(patientId))
  }

  def filterLinkableClaims(project: Project)(patient: Either[Patient, String]): Set[Procedure] = {
    val cptVersion = project.properties.getOrElse("gen", Map.empty).get("cptversion").map(_.asInstanceOf[String])
    val billTypeVersion = project.properties.getOrElse("gen", Map.empty).get("billtypeversion").map(_.asInstanceOf[String])

    // predicates to filter procedures by
    def isCPTProcedure(proc: Procedure) = CodingSystem.byOid(proc.getCode.getCodingSystemOID) == CodingSystem.CPT_4
    def isF2FClaim(proc: Procedure) = proc.isValidFFS(cptVersion, billTypeVersion) //projectCptCodes.contains(proc.getCode.getCode)
    def dateInRange(dateRange: (DateTime, DateTime))(proc: Procedure) =
      dateRange._1.isBefore(proc.getEndDate) && proc.getEndDate.isBefore(dateRange._2)

    // allows us to accept either Patient Id _or_ APO for `patient` parameter
    /*
    val procedureToNpi = patient match {
      case Left(pat) => getProcedureNpi(pat) _
      case Right(patientId) =>
        val summary = ApxServices.patientLogic.getMergedPatientSummaryForCategory(
          project.pdsExternalID,
          UUID.fromString(patientId),
          PatientAssembly.PatientCategory.CLINICAL_ACTOR.getCategory
        )
        getProcedureNpi(summary) _
    }

    val procedures = patient match {
      case Left(pat) => pat.getProcedures
      case Right(patientId) =>
        val summary = ApxServices.patientLogic.getMergedPatientSummaryForCategory(
          project.pdsExternalID,
          UUID.fromString(patientId),
          PatientAssembly.PatientCategory.PROCEDURE.getCategory
        )
        summary.getProcedures
    }
    */

    val apo = patient match {
      case Left(pat) => pat
      case Right(patientId) => getPatient(patientId)
    }
    val procedureToNpi = getProcedureNpi(apo) _

    if (apo == null || apo.getProcedures == null) {
      error(s"Failed to get procedures for patient ${patient.fold(_.getPatientId.toString, identity[String])}")
      Set.empty
    } else {
      apo.getProcedures.asScala
        .filter(proc => dateInRange((project.start, project.end))(proc) && isCPTProcedure(proc) && isF2FClaim(proc))
        .map(proc => {
          // if there's a matching actor/npi for this procedure add it to the metadata so we can discard the rest of the APO
          procedureToNpi(proc).foreach(npi => proc.setMetaTag("npi", npi))
          proc
        })
        .toSet
    }
  }

  // F2F CPT codes should ultimately reside in the code mappings database so hide access so no one depends on this
  private[this] object CptParser {
    val defaultYears: Set[Year] = (2014 to 2017).map(Year.of).toSet

    def getF2fCptCodes(years: Set[Year] = defaultYears): Map[Int, Set[String]] = {
      def getCptCsv(name: String) = scala.io.Source.fromInputStream(getClass.getResourceAsStream(s"/cptCodes/$name.csv"), "ISO-8859-1").getLines
      years.map(y => y.toString.toInt -> getCptCsv(y.toString).map(_.split(",")(0)).toSet).toMap
    }
  }
}
