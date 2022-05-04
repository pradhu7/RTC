package com.apixio.nassembly.exchangeutils

import com.apixio.model.EitherStringOrNumber
import com.apixio.model.patient._
import com.apixio.nassembly.util.DataConstants
import org.joda.time.DateTime

import java.util.UUID
import scala.collection.JavaConverters._
import scala.util.Random

object MockApoUtil {

  def generateClinicalCode: ClinicalCode = {
    val mockedClinicalCode = new ClinicalCode()
    val randomAlphaNumeric = "_" + generateAlphaNumericString
    mockedClinicalCode.setCode("c" + randomAlphaNumeric)
    mockedClinicalCode.setCodingSystem("cs" + randomAlphaNumeric)
    mockedClinicalCode.setDisplayName("dn" + randomAlphaNumeric)
    mockedClinicalCode.setCodingSystemOID("oid" + randomAlphaNumeric)
    mockedClinicalCode.setCodingSystemVersions("v" + randomAlphaNumeric)
    mockedClinicalCode
  }

  def generateExternalOriginalId = {
    val randomAlphaNumeric = DataConstants.SEP_UNDERSCORE + generateAlphaNumericString
    val originalId = new ExternalID()
    originalId.setId("test_id" + randomAlphaNumeric)
    originalId.setAssignAuthority("test_assign_authority" + randomAlphaNumeric)
    originalId
  }

  def setMockCodedBaseObject(codedBaseObject: CodedBaseObject) = {
    codedBaseObject.setCode(generateClinicalCode)
    codedBaseObject.setCodeTranslations(List(generateClinicalCode).asJava)
    codedBaseObject.setPrimaryClinicalActorId(UUID.randomUUID())
    codedBaseObject.setSourceEncounter(UUID.randomUUID())
    codedBaseObject.setSupplementaryClinicalActors(List(UUID.randomUUID()).asJava)

    setMockBaseObject(codedBaseObject)
  }

  def setMockBaseObject(codedBaseObject: BaseObject) = {
    codedBaseObject.setEditType(EditType.ACTIVE)
    codedBaseObject.setInternalUUID(UUID.randomUUID())
    codedBaseObject.setLastEditDateTime(new DateTime())
    codedBaseObject.setOriginalId(generateExternalOriginalId)
    codedBaseObject.setOtherOriginalIds(List(generateExternalOriginalId).asJava)
    codedBaseObject.setParsingDetailsId(UUID.randomUUID())
    codedBaseObject.setSourceId(UUID.randomUUID())
  }

  def generateTypedDate = {
    val randomAlphaNumeric = DataConstants.SEP_UNDERSCORE + generateAlphaNumericString
    val typedDate = new TypedDate
    typedDate.setType("td" + randomAlphaNumeric)
    typedDate.setDate(DateTime.now())
    typedDate
  }

  def generateSource(creationDate: DateTime) = {
    val source = new Source()
    //    case "RAPS_RETURN" => ProblemOrigin.RAPS
    //    case "RAPS" => ProblemOrigin.RA_CLAIMS
    source.setSourceSystem("RAPS")
    source.setCreationDate(creationDate)
    //sourceId is by default get assigned into source constructor
    //    source.setSourceId(UUID.randomUUID())
    source
  }

  def generateParsingDetails(parsingDateTime: DateTime, parserType: ParserType) = {
    val parsingDetail = new ParsingDetail()
    parsingDetail.setParsingDateTime(parsingDateTime)
    parsingDetail.setParser(parserType)
    parsingDetail
  }

  def generateClinicalActor = {
    val actor = new ClinicalActor()
    val externalID = new ExternalID()
    externalID.setId("0123456789")
    externalID.setType("NPI")
    actor.setPrimaryId(externalID)
    actor.setOriginalId(externalID)
    actor.setRole(ActorRole.SURGEON)
    actor
  }

  def generateSingularListOfAlphaNumericString = {
    List(generateAlphaNumericString).asJava
  }

  def generateAlphaNumericString: String = {
    Random.alphanumeric.toString()
  }

  def getDateBeforeNumOfDays(noOfDays: Int) = {
    DateTime.now().minusDays(noOfDays)
  }

  def getDateAfterNumOfDays(noOfDays: Int) = {
    DateTime.now().plusDays(noOfDays)
  }

  def generateRandomInt = {
    Random.nextInt()
  }

  def generateEitherStringOrNum = {
    val num = generateRandomInt
    var eitherStringOrNum = new EitherStringOrNumber(num)
    if (num % 2 == 0) {
      eitherStringOrNum = new EitherStringOrNumber(generateAlphaNumericString)
    }
    eitherStringOrNum
  }

}