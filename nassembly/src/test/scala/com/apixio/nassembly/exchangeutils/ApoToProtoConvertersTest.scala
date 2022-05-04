package com.apixio.nassembly.exchangeutils

import com.apixio.dao.patient2.PatientUtility
import com.apixio.model.patient._
import com.apixio.nassembly.apo.APOGenerator
import com.apixio.nassembly.exchangeutils.LabResultTestUtil.generateLabResult
import com.apixio.nassembly.exchangeutils.MockApoUtil._
import com.apixio.nassembly.patient.partial.PartialPatientExchange
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should._
import org.scalatestplus.junit.JUnitRunner

import java.util.UUID
import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class ApoToProtoConvertersTest extends AnyFlatSpec with Matchers {

  val parsingDetail: ParsingDetail = generateParsingDetails(DateTime.now.minusYears(10), ParserType.APO)
  val patientSource: Source = generateSource(new DateTime())
  val clinicalActor: ClinicalActor = generateClinicalActor

  def generatePatient: Patient = {
    val patient = new Patient()
    patient.addSource(patientSource)
    patient.setParsingDetails(List(parsingDetail).asJava)
    patient.addClinicalActor(clinicalActor)
    patient.addDocument(DocumentMetaTestUtil.generateDocumentMeta(patientSource))
    patient.addLabResult(generateLabResult)
    patient.addAllergy(AllergyTestUtil.generateAllergy)
    patient.addProcedure(ProcedureTestUtil.generateProcedure)
    patient.addProcedure(ProcedureTestUtil.generateFfsClaimProcedure(parsingDetail, patientSource, clinicalActor))
    patient.addProblem(ProblemTestUtil.generateProblem(patientSource))
    patient.addProblem(ProblemTestUtil.generateRaClaimProblem(patientSource))
    patient.addProblem(ProblemTestUtil.generateMao004Problem(patientSource))
    patient.addEncounter(EncounterTestUtil.generateEncounterData)
    patient.addBiometricValue(BiometricTestUtil.generateBiometricData)
    patient.addAlternateContactDetails(ContactDetailsTestUtil.generateContactDetails)
    patient.addSocialHistory(SocialHistoryTestUtil.generateSocialHistoryData)
    patient.addAlternateDemographics(DemographicsTestUtil.generateDemographicsTestData)
    patient.addPrescription(PrescriptionTestUtil.generatePrescriptionTestData)
    patient.addFamilyHistory(FamilyHistoryTestUtil.generateFamilyHistoryTestData)
    patient.addAdministration(ImmunizationTestUtil.generateAdminImmuTestData)
    patient.addCoverage(CoverageTestUtil.generateCoverageTestData)

    patient
  }

  def generatePatientWithoutEncounterCareSiteAddress: Patient = {
    val patient = new Patient()
    patient.addSource(patientSource)
    patient.setParsingDetails(List(parsingDetail).asJava)
    patient.addDocument(DocumentMetaTestUtil.generateDocumentMeta(patientSource))

    val careSiteId = UUID.randomUUID()
    patient.addEncounter(EncounterTestUtil.generateEncounterWithoutCareSiteAddress(careSiteId))
    patient.addCareSite(EncounterTestUtil.populateCareSiteData(Some(careSiteId)))

    patient
  }

  "ApoToProtoConverter" should "Convert from Apo to proto" in {
    val patient = generatePatient
    val pdsId = "444"

    val converter = new ApoToProtoConverter()
    val patientProto = converter.convertAPO(patient, pdsId)
    val oidKey = PatientUtility.getDocumentKey(patient)

    val exchange = new PartialPatientExchange
    exchange.setPatient(patientProto)
    val parts = exchange.getParts(null)
    assert(!parts.isEmpty)

    DocumentMetaTestUtil.assertDocumentMeta(patientProto, oidKey)
    LabResultTestUtil.assertLabResult(patientProto)
    AllergyTestUtil.assertAllergy(patientProto)
    ProcedureTestUtil.assertProcedure(patientProto)
    ProcedureTestUtil.assertFfsClaim(patientProto)
    ProblemTestUtil.assertProblem(patientProto)
    ProblemTestUtil.assertRaClaimProblem(patientProto)
    ProblemTestUtil.assertMao004Problem(patientProto)
    EncounterTestUtil.assertEncounter(patientProto, patient.getEncounters.iterator().next())
    BiometricTestUtil.assertBiometricValue(patientProto)
    ContactDetailsTestUtil.assertContactDetails(patientProto)
    SocialHistoryTestUtil.assertSocialHistory(patientProto)
    DemographicsTestUtil.assertDemographics(patientProto)
    PrescriptionTestUtil.assertPrescriptions(patientProto)
    FamilyHistoryTestUtil.assertFamilyHistory(patientProto)
    ImmunizationTestUtil.assertAdminImmu(patientProto)
    CoverageTestUtil.assertCoverage(patientProto)
  }

  "Apo Generator" should "use Apo Converter to go back to an APO" in {
    val patient = generatePatient
    val pdsId = "444"

    val converter = new ApoToProtoConverter()
    val patientProto = converter.convertAPO(patient, pdsId)

    val doubleConvertedApo = APOGenerator.fromProto(patientProto)
    assert(doubleConvertedApo!= null)
  }

  "ApoToProtoConverter" should "populate careSite addresses if nested encounter.siteOfService doesn't contain address" in {
    val patient = generatePatientWithoutEncounterCareSiteAddress
    val pdsId = "444"

    val converter = new ApoToProtoConverter()
    val patientProto = converter.convertAPO(patient, pdsId)

    //we need to fetch the populated care site because cv2 doesnt always populated nested care sites and instead references them by careSiteId
    val populatedCareSite = patient.getCareSiteById(patient.getEncounters.iterator().next().getSiteOfService.getCareSiteId)
    val encounter = patient.getEncounters.iterator().next()
    encounter.setSiteOfService(populatedCareSite)
    EncounterTestUtil.assertEncounter(patientProto, encounter)
  }
}