package com.apixio.model.profiler

import java.io.InputStream

import com.apixio.model.utility.{DateFunctions, DateModel}
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.ScalaObjectMapper
import org.joda.time.{DateTime, DateTimeZone}


case class RiskGroupModel(@JsonProperty("c") code:String = "",
                          @JsonProperty("s") system:String = "",
                          @JsonProperty("g") group:String = "",
                          @JsonProperty("d") description:String = "",
                          @JsonProperty("r") raf:Double = 0.0,
                          @JsonProperty("start") start: String = "",
                          @JsonProperty("end") end: String = "") extends DateModel {

  @Override
  def getStartDate(): DateTime = DateTime.parse(start, mappingDtf).withZone(DateTimeZone.UTC)

  @Override
  def getEndDate(): DateTime = DateTime.parse(end, mappingDtf).withZone(DateTimeZone.UTC)

  def asCode() : Code = Code(code, Code.RISKGROUP + system)

  def asJson()(implicit mapper: ObjectMapper) = mapper.writeValueAsString(this)

}

object RiskGroupModel extends DateFunctions {
  private var model : Map[String, Map[Code, List[RiskGroupModel]]] = Map()
  val AdultGroupName = "ADULT"
  val ChildGroupName = "CHILD"

  def init(modelData : InputStream)(implicit mapper: ObjectMapper with ScalaObjectMapper) : Unit =  {
    model.synchronized {
      val data = mapper.readValue[List[RiskGroupModel]](modelData)
      model = data.groupBy(_.group.toUpperCase).map{ case (group: String, riskGroupModels: List[RiskGroupModel]) =>
        (group, riskGroupModels.groupBy(_.asCode()))}
    }
  }

  def get(group: String, code: Code): Option[List[RiskGroupModel]] = {
    model.get(group) match {
      case Some(g) => g.get(code)
      case None => None
    }
  }

  def getAdultRiskGroups(codes: List[Code], date: Option[DateTime] = None): List[RiskGroupModel] = {
    codes.flatMap(code => RiskGroupModel.get(AdultGroupName, code)).flatten
      .filter(group => filterDate(group, date))
  }

  def getChildRiskGroups(codes: List[Code], date: Option[DateTime] = None): List[RiskGroupModel] = {
    codes.flatMap(code => RiskGroupModel.get(ChildGroupName, code)).flatten
      .filter(group => filterDate(group, date))
  }

  def getAll(): List[RiskGroupModel] = model.values.flatMap(_.values).flatten.toList

  def fromJson(obj: String)(implicit mapper: ObjectMapper with ScalaObjectMapper): RiskGroupModel = mapper.readValue[RiskGroupModel](obj)

}

case class RiskGroupInfo(on: List[Code], off: List[Code])
