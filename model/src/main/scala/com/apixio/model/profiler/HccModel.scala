package com.apixio.model.profiler

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.ScalaObjectMapper
import java.io.InputStream

import scala.io.Source
import com.apixio.model.utility.ResourceControl


case class HccModel(
    @JsonProperty("c") code:String = "",
    @JsonProperty("v") version:String = "",
    @JsonProperty("d") description:String = "",
    @JsonProperty("o") children:List[String] = List(),
    @JsonProperty("r") raf:Double = 0.0,
    @JsonProperty("adultRg") adultRiskGroup:RiskGroupVariable = RiskGroupVariable(List(), List()),
    @JsonProperty("childRg") childRiskGroup:RiskGroupVariable = RiskGroupVariable(List(), List())) {

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
  def diseaseCategory(): Option[String] = HccModel.getDiseaseCategory(asCode())

  def asJson()(implicit mapper: ObjectMapper): String = mapper.writeValueAsString(this)
}

object HccModel {
  private var model : Map[Code,HccModel] = Map()

  def init(modelData : InputStream)(implicit mapper: ObjectMapper with ScalaObjectMapper) : Unit =  {
    model.synchronized {
      val data = mapper.readValue[List[HccModel]](modelData)
      model = data.map(x => x.asCode -> x).toMap
    }
  }

  def get(c: Code) : Option[HccModel] = model.get(c)

  def getDiseaseCategory(c: Code): Option[String] = CodeToCategoryMapper.getCategory(c)

  def getAll() : List[HccModel] = model.values.toList

  def fromJson(obj: String)(implicit mapper: ObjectMapper with ScalaObjectMapper) : HccModel = mapper.readValue[HccModel](obj)

  def hierarchyFilter(hccs: List[HccModel]): List[HccModel] = {
    if (hccs.size < 2) hccs
    else {
      val allChildren = hccs.flatMap(_.childrenCodes()).flatMap(HccModel.get)
      hccs.filterNot(allChildren.contains)
    }
  }
}

/**
 * Risk Group Variable case class that correspond to adult and child risk groups.
 * @param on
 * @param off
 */
case class RiskGroupVariable(@JsonProperty("on") on: List[String], @JsonProperty("off") off: List[String]) {
  def asJson()(implicit mapper: ObjectMapper) = mapper.writeValueAsString(this)
}

object RiskGroupVariable {
  def fromJson(obj: String)(implicit mapper: ObjectMapper with ScalaObjectMapper) : RiskGroupVariable = mapper.readValue[RiskGroupVariable](obj)
}

object CodeToCategoryMapper extends ResourceControl {

  // Stores cached version of the above files, with code to category as map entries
  private var codeToCategoryMappings = Map.empty[String, Map[String, String]]

  private def systemToFileMapping(system: String): Option[String] = system match {
    case "HCCCRV1" | "HCCCRV2"          => Some("CR_Disease_Categories_Mappings.csv")
    case "HCCV22" | "HCCV23" | "HCCV24" => Some("MA_Disease_Categories_Mappings.csv")
    case _ => None
  }

  private def initializeAndGet(system: String): Map[String, String] = {
    systemToFileMapping(system) match {
      case None => Map.empty[String, String]
      case Some(dir) =>
        val fileName = s"hcc/${dir}"
        val stream = getClass.getClassLoader.getResourceAsStream(fileName)
        using(Source.fromInputStream(stream)) { source => {
          val lines = source.getLines()
          val isValidMappingFile = if (lines.hasNext) lines.next.matches("hcc,hcc_cat") else false
          if (!isValidMappingFile) {
            Map.empty[String, String]
          } else {
            val map = lines.map(line => {
              val a = line.split(",")
              a(0) -> a(1)
            }).toMap
            codeToCategoryMappings = codeToCategoryMappings + (system -> map)
            map
          }
        }}
    }
  }

  private def getCodeToCategoryMapping(system: String): Map[String, String] = {
    codeToCategoryMappings.getOrElse(system, initializeAndGet(system))
  }

  /**
   * Maps an HCC code to a category. Code system can be an HCC code or a CRV code.
   * It then maps the code to a category, currently declared in files
   */
  def getCategory(code: Code): Option[String] = {
    getCodeToCategoryMapping(code.system).get(code.code)
  }
}
