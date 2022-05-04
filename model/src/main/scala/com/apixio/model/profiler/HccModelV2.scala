package com.apixio.model.profiler


import java.io.InputStream

import com.fasterxml.jackson.annotation.JsonProperty
import com.apixio.model.utility.{DateFunctions, DateModel}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.ScalaObjectMapper
import org.joda.time.{DateTime, DateTimeZone}


case class HccModelV2(
     @JsonProperty("c") code:String = "",
     @JsonProperty("v") version:String = "",
     @JsonProperty("d") description:String = "",
     @JsonProperty("o") children:List[String] = List(),
     @JsonProperty("r") raf:Double = 0.0,
     @JsonProperty("adultRg") adultRiskGroup:RiskGroupVariable = RiskGroupVariable(List(), List()),
     @JsonProperty("childRg") childRiskGroup:RiskGroupVariable = RiskGroupVariable(List(), List()),
     @JsonProperty("start") start: String = "",
     @JsonProperty("end") end: String = "") extends DateModel {

  def withChildren() : List[Code] = childrenCodes ++ List(asCode())

  def childrenCodes(): List[Code] = children.map(_.split("-").toList)
    .map(x => Code(x.head, x.lift(1).getOrElse("HCC" + version)))

  def getAdultRiskGroupsOn(): List[Code] = adultRiskGroup.on.map(code => Code(code, Code.RISKGROUP + version))
  def getAdultRiskGroupsOff(): List[Code] = adultRiskGroup.off.map(code => Code(code, Code.HCC + version))
  def getChildRiskGroupsOn(): List[Code] = childRiskGroup.on.map(code => Code(code, Code.RISKGROUP + version))
  def getChildRiskGroupsOff(): List[Code] = childRiskGroup.off.map(code => Code(code, Code.HCC + version))

  def asCode() : Code = Code(code, Code.HCC + version)

  /**
   * Returns disease category or "None"
   */
  def diseaseCategory(): Option[String] = HccModelV2.getDiseaseCategory(asCode())

  def asJson()(implicit mapper: ObjectMapper): String = mapper.writeValueAsString(this)

  @Override
  def getStartDate(): DateTime = DateTime.parse(start, mappingDtf).withZone(DateTimeZone.UTC)

  @Override
  def getEndDate(): DateTime = DateTime.parse(end, mappingDtf).withZone(DateTimeZone.UTC)
}

object HccModelV2 extends DateFunctions {
  private var model : Map[Code,List[HccModelV2]] = Map()

  def init(modelData : InputStream)(implicit mapper: ObjectMapper with ScalaObjectMapper): Unit =  {
    model.synchronized {
      val data = mapper.readValue[List[HccModelV2]](modelData)
      model = data.groupBy(_.asCode())
    }
  }

  def get(c: Code, date: Option[DateTime]): Option[List[HccModelV2]] = {
    model.get(c) match {
      case Some(codes) =>
        val filteredCodes: List[HccModelV2] = codes.filter(code => filterDate(code, date))
        Option(filteredCodes).filter(_.nonEmpty)
      case None => None
    }
  }

  def getDiseaseCategory(c: Code): Option[String] = CodeToCategoryMapper.getCategory(c)

  def getAll() : List[HccModelV2] = model.values.flatten.toList

  def fromJson(obj: String)(implicit mapper: ObjectMapper with ScalaObjectMapper): HccModelV2 = mapper.readValue[HccModelV2](obj)
}