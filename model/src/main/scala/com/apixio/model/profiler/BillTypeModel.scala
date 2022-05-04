package com.apixio.model.profiler

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.InputStream

import com.fasterxml.jackson.module.scala.ScalaObjectMapper


case class BillTypeModel(
                          @JsonProperty("b") code: String = "", // billType code
                          @JsonProperty("s") system: String = "",
                          @JsonProperty("p") ptype: String = "", // provider type this bill type correspond to
                          @JsonProperty("r") ra: String = "",
                          @JsonProperty("v") version: String = ""
                        ) {

  val asCode: Code = Code(code=code, system=system)

  def asJson()(implicit mapper: ObjectMapper): String = mapper.writeValueAsString(this)
}

object BillTypeModel {
  private var model : List[BillTypeModel] = List.empty
  private var codesByVersion: Map[String, Map[Code, BillTypeModel]] = Map.empty
  private var numOfDigitsByVersion: Map[String, Set[Int]] = Map.empty
  val institutionalProviderType: String = "Institutional"
  val outpatientProviderType: String = "Outpatient"
  val professionalProviderType: String = "Professional"
  val providerTypeI = "I"
  val providerTypeO = "O"
  val providerTypeP = "P"
  val providerType01 = "01"
  val providerType02 = "02"
  val providerType10 = "10"
  val providerType20 = "20"

  def init(modelData : InputStream)(implicit mapper: ObjectMapper with ScalaObjectMapper) : Unit =  {
    model.synchronized {
      model = mapper.readValue[List[BillTypeModel]](modelData)
      codesByVersion = model.groupBy(_.version).map{ case (version, billTypeModels) => (version, billTypeModels.map(m => m.asCode -> m).toMap)}
      numOfDigitsByVersion = codesByVersion.map{ case (version, codeMap) => (version, codeMap.keys.filterNot(isUnknownBillType)
        .map(_.code.length).toSet)}
    }
  }

  private def getCodesBy(version: String): Map[Code, BillTypeModel] = codesByVersion.getOrElse(version, Map.empty)

  // remove leading zeros and take first n digits
  private[profiler] def truncateCode(c: Code, takeDigit: Int): Code = {
    c.code match {
      case null => c //Valid for bill type to be "UNKNOWN" --> null
      case _ => c.copy(code=c.code.replaceFirst("^0", "").slice(0, takeDigit))
    }
  }

  def isUnknownBillType(code: Code): Boolean = {
    !code.code.forall(_.isDigit)
  }

  def get(version: String, code: Code): Seq[BillTypeModel] = {
    val possibleNumOfDigits: Set[Int] = numOfDigitsByVersion.getOrElse(version, Set.empty)
    possibleNumOfDigits.flatMap { digit =>
      val codeToVerity: Code = truncateCode(code, digit)
      getCodesBy(version).get(codeToVerity)
    }.toSeq
  }

  def providerTypeToString(provideType: Code): String = {
    (provideType.isInpatient(), provideType.isOutpatient(), provideType.isProfessional()) match {
      case (true, _, _) => institutionalProviderType
      case (_, true, _) => outpatientProviderType
      case (_, _, true) => professionalProviderType
      case _ => ""
    }
  }

  def isValid(version: String, code: Code, providerType: Option[Code] = None): Boolean = {
    val potentialCodesToMatch: Seq[Code] = numOfDigitsByVersion.getOrElse(version, Set.empty).map {digit => truncateCode(code, digit)}.toSeq
    potentialCodesToMatch.exists { code =>
      val billTypeModels: Map[Code, BillTypeModel] = getCodesBy(version)
      if (providerType == None) {
        billTypeModels.contains(code)
      } else {
        billTypeModels.contains(code) && billTypeModels.lift(code).map(_.ptype)
          .exists(_ == providerTypeToString(providerType.getOrElse(Code("", ""))))
      }
    }
  }

  def getProviderType(version: String, code: Code): Seq[String] = {
    if (isValid(version, code)) {
      get(version, code).map(_.ptype)
    } else {
      Seq("")
    }
  }

  def getAll(version: Option[String] = None): List[BillTypeModel] = version match  {
    case None => model
    case Some(s) => getCodesBy(s).values.toList
  }

  def fromJson(obj: String)(implicit mapper: ObjectMapper with ScalaObjectMapper): BillTypeModel =
    mapper.readValue[BillTypeModel](obj)
}
