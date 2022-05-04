package com.apixio.model.profiler

import java.io.InputStream

import com.apixio.model.utility.{DateFunctions, DateModel}
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.ScalaObjectMapper
import org.joda.time.{DateTime, DateTimeZone, LocalDate}

case class CptModelV2(
      @JsonProperty("c") code: String = "",
      @JsonProperty("s") system: String = "",
      @JsonProperty("v") version: String = "", // {market} for example MA, CR
      @JsonProperty("d") description: String = "",
      @JsonProperty("start") start: String = "",
      @JsonProperty("end") end: String = "") extends DateModel {

  def asCode(): Code = Code(code=code, system=system)

  @Override
  def getStartDate(): DateTime = DateTime.parse(start, mappingDtf).withZone(DateTimeZone.UTC)

  @Override
  def getEndDate(): DateTime = DateTime.parse(end, mappingDtf).withZone(DateTimeZone.UTC)
}

object CptModelV2 extends DateFunctions {
  private var model: List[CptModelV2] = List.empty
  private var codesByMarket: Map[String, Map[Code, List[CptModelV2]]] = Map.empty

  def init(modelData : InputStream)(implicit mapper: ObjectMapper with ScalaObjectMapper) : Unit =  {
    model.synchronized {
      model = mapper.readValue[List[CptModelV2]](modelData)
      codesByMarket = model.groupBy(_.version).map { case (market: String, cptModels: List[CptModelV2]) =>
        (market, cptModels.groupBy(_.asCode))
      }
    }
  }

  def get(market: String, c: Code, date: Option[DateTime]): Option[List[CptModelV2]] = {
    val cNoModifiers: Code = c.copy(code=c.code.split('-')(0))
    codesByMarket.getOrElse(market, Map.empty).get(cNoModifiers) match {
      case Some(cptModels) =>
        val filteredCodes: List[CptModelV2] = cptModels.filter(code => filterDate(code, date))
        Option(filteredCodes).filter(_.nonEmpty)
      case None => None
    }
  }

  def isValid(market: String, c: Code, date: Option[DateTime]): Boolean = CptModelV2.get(market, c, date) match {
    case Some(cptModelV2s) if cptModelV2s.isEmpty => false
    case None => false
    case _ => true
  }

  def getAll(market: Option[String] = None) : List[CptModelV2] = market match {
    case None => model
    case Some(s) => codesByMarket.getOrElse(s, Map.empty).values.flatten.toList
  }

  def getAllCodes(market: Option[String] = None) : List[Code] = market match {
    case None => model.map(_.asCode).distinct
    case Some(s) => codesByMarket.getOrElse(s, Map.empty).keys.toList
  }

  def getAllMarkets(): List[String] = codesByMarket.keys.toList
}
