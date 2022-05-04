package com.apixio.scala.utility

import com.apixio.scala.dw.ApxServices
import com.apixio.scala.logging.ApixioLoggable
import com.apixio.model.profiler._
import com.fasterxml.jackson.databind.ObjectMapper
import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}

import scala.collection.mutable
import scala.util.Try

// cannot deprecate because of https://github.com/Apixio/apx-sessionmanager/blob/a3ef7b185c47f2d28c387bf12335b1a7ffa34f0e/src/main/scala/com/apixio/app/sessionmanager/resources/LegacyWorkResource.scala#L1234
// TODO: migrate this business logic to the app platform
object HccDataManager extends ApixioLoggable {

  // to be refactored and delete all the old logic after mapping V2 is rolled out and verified
  setupLog(this.getClass.getName)

  ApxServices.setupObjectMapper(new ObjectMapper())
  ApxServices.setupDefaultModels

  val mappings: Map[(Code, String), List[Code]] = CodeMapping.getMappings
  val mappingsV2: Map[(Code, String), List[V2MappingStruct]] = CodeMappingV2.getMappings

  val mappingDtf: DateTimeFormatter = DateTimeFormat.forPattern("MM/dd/yyyy")

  def getHcc(filter: HccDataQuery): HccDataResponseModel = {
    val mappingVersion = IcdMapper.getHCCMappingVersion(filter.paymentYear, filter.icdMapping, filter.apxcatMapping,
      Code("", Code.HCC), filter.dateOfService)

    // for mapping V2
    val mappingType: String = filter.icdMapping match {
      case maMapping if CodeMappingV2.isNormalMAMappingType(maMapping) => CodeMappingV2.MAMappingType
      case crMapping if CodeMappingV2.isNormalCRMappingType(crMapping) => CodeMappingV2.CRMappingType
      case otherMapping => otherMapping
    }
    val dos: Option[DateTime] = Try(DateTime.parse(filter.dateOfService, mappingDtf).withZone(DateTimeZone.UTC)).toOption

    def isDataScienceHack(): Boolean = {
      val dataScienceHacks = Seq("2020-anl-icd-hcccrv2-override", "2015+18-ml-icd-hcc", "2018+19-icd-hcc-scienceV24", "2021-icd-hcc-scienceV24")
      dataScienceHacks.contains(filter.icdMapping)
    }

    def getVersion(): String = {
      if (isDataScienceHack()) {
        "ICD10"
      } else if (IcdMapper.isAfterDate("09/30/2015", filter.dateOfService)) {
        "ICD10"
      } else {
        "ICD9"
      }
    }

    def getLabelSetVersion(): String = {
      if (filter.labelSetVersion == Code.CODEALL) {
        if (filter.icdMapping.contains("hcccrv1")) {
          Code.HCCCRV1
        } else if (filter.icdMapping.contains("icd-all")) {
          Code.HCCALL
        } else {
          filter.hccCode
        }
      }
      else filter.labelSetVersion
    }

    def getHccCode(): String = {
      if (filter.labelSetVersion == Code.CODEALL) {
        "" //get all HCC's
      } else
        filter.hccCode
    }

    val icdSystem = getVersion()
    val labelSetVersion = getLabelSetVersion()
    val hccCode = getHccCode()

    def isUsableHccHelper(currentHccCode: Code, currentVersion: String, icds: List[Code], targetVersion: String): Boolean = {
      val icdSystemCode = icdSystem match {
        case i: String if i == "ICD9" => Code.ICD9
        case i: String if i == "ICD10" => Code.ICD10
        case _ =>
          warn(s"No ICD system provided for $filter")
          ""
      }

      val hccMatch = hccCode match {
        case hcc: String if hcc.length > 0 => currentHccCode.code == hcc
        case _ => true // Optional
      }

      val labelSetVersionMatch = labelSetVersion match {
        case lsv: String if lsv.length > 0 => currentHccCode.system == lsv
        case _ => false // Required
      }

      val icdMatch = filter.icdCode match {
        case i: String if i.length > 0 =>
          val icd = Code(i, icdSystemCode)
          icds.contains(icd)
        case _ => true // Optional
      }

      val mappingVersionMatch = (currentVersion == targetVersion)

      hccMatch && labelSetVersionMatch && icdMatch && mappingVersionMatch
    }

    def isUsableHcc(m: Tuple2[(Code, String), List[Code]]): Boolean = {
      val currentHccCode = m._1._1
      val mapping = m._1._2
      val icds = m._2

      isUsableHccHelper(currentHccCode, mapping, icds, mappingVersion)
    }

    def isUsableHccV2(m: Tuple2[(Code, String), List[V2MappingStruct]]): Boolean = {
      val (currentHccCode, mapping, icds) = m match {
        case ((currentHccCode, mapping), icdMappingStructs) => (currentHccCode, mapping, icdMappingStructs.map(_.code))
      }

      isUsableHccHelper(currentHccCode, mapping, icds, mappingType)
    }

    def byIcdSystem(icdCode: Code): Boolean = {
      val icdSystemMatch = icdSystem match {
        case iSys: String if iSys == "ICD9" => icdCode.system == Code.ICD9
        case iSys: String if iSys == "ICD10" => icdCode.system == Code.ICD10
        case _ => false // Required
      }
      icdSystemMatch
    }

    def byQuery(icdCode: Code): Boolean = {
      val queryMatch = filter.q match {
        case q: String if q.length > 0 =>
          val code = icdCode.code.toLowerCase
          val description = icdCode.description.toLowerCase
          val label = s"$code $description"

          val searchTerms = q.toLowerCase.split(" ")
          searchTerms.foldLeft(true)(
            (acc: Boolean, term: String) => label.contains(term) && acc
          )
        case _ => true
      }
      queryMatch
    }

    def snomedMapToIcd(snomedCodes: List[Code], snomedCodeToIcd: mutable.Map[String, String]) {
      if (filter.mappingV2) {
        snomedCodes.map(code => {
          mappingsV2.get((code, CodeMappingV2.SNOMEDType)) match {
            case Some(icds) => snomedCodeToIcd.put(code.code, icds.head.code.code)
            case _ =>
          }
        })
      } else {
        snomedCodes.map(code => {
          mappings.get((code, CodeMappingV2.SNOMEDType)) match {
            case Some(icds) => snomedCodeToIcd.put(code.code, icds.head.code)
            case _ =>
          }
        })
      }
    }

    def icdMapToSnomeds(snomedCodes: List[Code], snomedCodeToIcd: mutable.Map[String, String], icdToSnomeds: mutable.Map[String, Seq[SnomedModel]]) {
      snomedCodes.map(sc => {
        snomedCodeToIcd.get(sc.code) match {
          case Some(icd) =>
            SnomedModel.get(sc, None) match {
              case Some(m) =>
                val sModels: Seq[SnomedModel] = icdToSnomeds.getOrElse(icd, Seq.empty)
                icdToSnomeds.put(icd, sModels :+ m.head)
              case _ =>
            }
          case _ =>
        }
      })
    }

    def icdResModelWithSnomed(hccCode: Code, allCodes: List[Code],
                              icdToSnomeds: mutable.Map[String, Seq[SnomedModel]]): (Code, List[IcdResponseModel]) = {
        val icdModels: List[IcdModelV2] = allCodes.filter(_.isIcd()).filter(byQuery).flatMap(IcdModelV2.get(_, dos)).flatten
        val icdResModels = icdModels.map(m => {
          icdToSnomeds.get(m.code) match {
            case Some(sModels) =>
              val snomedResModels: Seq[SnomedResponseModel] = sModels.map(e =>
                SnomedResponseModel(e.code, e.system, e.description, e.module_id, e.type_id, e.case_significance_id))
              IcdResponseModel(m, icdSystem, Option(snomedResModels).filter(_.nonEmpty))
            case _ =>
              IcdResponseModel(m, icdSystem, None)
          }
        })
        (hccCode, icdResModels)
    }

    def toIcdResModelByHcc(code: Code): (Code, List[IcdResponseModel]) = {
      if (filter.snomed) {
        val allCodes = code.toSnomed(mappingVersion, dos)

        val snomedCodes: List[Code] = allCodes.filter(_.isSnomed())
        //assumed a snomed code can only have a unique icd code, while a icd code can have multiple snomed codes
        val snomedCodeToIcd: mutable.Map[String, String] = mutable.Map.empty
        snomedMapToIcd(snomedCodes, snomedCodeToIcd)

        val icdToSnomeds: mutable.Map[String, Seq[SnomedModel]] = mutable.Map.empty
        icdMapToSnomeds(snomedCodes, snomedCodeToIcd, icdToSnomeds)

        icdResModelWithSnomed(code, allCodes, icdToSnomeds)
      } else {
        if (filter.mappingV2) {
          val icds = code.toIcdV2(mappingVersion, dos).filter(byIcdSystem).filter(byQuery)
          (code, icds.flatMap(IcdModelV2.get(_, dos)).flatten.map(IcdResponseModel(_, icdSystem, None)))
        } else {
          val icds = code.toIcd(mappingVersion).filter(byIcdSystem).filter(byQuery)
          (code, icds.flatMap(IcdModel.get).map(IcdResponseModel(_, icdSystem, None)))
        }
      }
    }

    def toHccResponseModel(m: (Code, List[IcdResponseModel])): Option[HccResponseModel] = {
      val hccCode: Code = m._1
      val icdr: List[IcdResponseModel] = m._2
      if (filter.mappingV2 && !isDataScienceHack()) { // very dumb way to stop the hack from breaking, sorry
        HccModelV2.get(hccCode, dos).flatMap(_.headOption).flatMap { hc =>
          if (icdr.nonEmpty)
            Option(HccResponseModel(
              code = hc.code,
              labelSetVersion = hc.version,
              icds = icdr
            ))
          else None
        }
      } else {
        HccModel.get(hccCode).flatMap { hc =>
          if (icdr.nonEmpty)
            Option(HccResponseModel(
              code = hc.code,
              labelSetVersion = hc.version,
              icds = icdr
            ))
          else None
        }
      }
    }

    val hccResponses: List[HccResponseModel] = {
      if (hccCode.nonEmpty) {
        val code = Code(hccCode,labelSetVersion)
        toHccResponseModel(toIcdResModelByHcc(code)) match {
          case Some(hrm) => List(hrm)
          case _ => List.empty
        }
      } else {
        if (filter.mappingV2) {
          mappingsV2.filter(isUsableHccV2)
            .map(e => toIcdResModelByHcc(e._1._1))
            .flatMap(toHccResponseModel)
            .filter(_.icds.nonEmpty)
            .toList
        } else {
          mappings.filter(isUsableHcc)
            .map(e => toIcdResModelByHcc(e._1._1))
            .flatMap(toHccResponseModel)
            .filter(_.icds.nonEmpty)
            .toList
        }
      }
    }

    val pSize = filter.pageSize match {
      case p: Int if p != -1 => p
      case _ => hccResponses.length
    }

    val hccsToRet = filter.pageSize match {
      case pSize: Int if pSize > -1 =>
        val pIndex = filter.pageIndex
        val startIndex = pIndex * pSize
        val endIndex = (pIndex + 1) * pSize
        hccResponses.slice(startIndex, endIndex)
      case _ => hccResponses
    }

    HccDataResponseModel(
      totalCount = hccResponses.length,
      pageSize = pSize,
      pageIndex = filter.pageIndex,
      hccs = hccsToRet)
  }
}
