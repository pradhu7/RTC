package com.apixio.model.profiler

import com.apixio.model.event.CodeType
import com.apixio.model.profiler.CodeMappingV2.SNOMEDType
import com.apixio.model.quality.{AnnotationField, ConjectureModel, FactModel, MeasureModel}
import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty}
import com.apixio.model.profiler.BillTypeModel
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.ScalaObjectMapper
import org.joda.time.DateTime

case class Code(
    @JsonProperty("c") code: String = "",
    @JsonProperty("s") system: String = "") {

  def asJson()(implicit mapper: ObjectMapper) = mapper.writeValueAsString(this)

  def key() : String = List(code, system).mkString("-")

  def toHcc(v: String, andChildren : Boolean = false) : List[Code] = isHcc() match {
    case true =>
      if (andChildren) withChildren else List(this)
    case false =>
      // ICD with or without decimal should be treated the same. E11.69 ~ E1169
      isSynthetic() match {
        case true => CodeMapping.get(this,v).filter(_.isHcc).flatMap(x => if (andChildren) x.withChildren else List(x))
        case false =>
          val normalized: Code = if (isIcd()) copy(code = code.replace(".", "")) else this
          CodeMapping.get(normalized, v).filter(_.isHcc).flatMap(x => if (andChildren) x.withChildren else List(x))
      }
  }

  /**
   * Fetch HCCs using the V2 API.
   * @param dateTime
   * @param andChildren
   * @param v
   * @return
   */
  def toHccV2(dateTime: Option[DateTime], andChildren: Boolean = false, v: String): List[Code] = isHcc() match {
    case true =>
      if (andChildren) withChildrenV2(dateTime) else List(this)
    case false =>
      // ICD with or without decimal should be treated the same. E11.69 ~ E1169
      isSynthetic() match {
        case true =>
          CodeMappingV2.get(this, v, dateTime).filter(_.isHcc)
            .flatMap(x => if (andChildren) x.withChildrenV2(dateTime) else List(x))
        case false =>
          val sourceCodes: List[Code] = isIcd() match {
            case true => {
              val icdWithoutDecimal: Code = copy(code = code.replace(".", ""))
              IcdModelV2.get(icdWithoutDecimal, dateTime)
                .getOrElse(List.empty[IcdModelV2])
                .map(_.asCode).distinct
            }
            case false => List(this)
          }
          sourceCodes.flatMap(sourceCode => CodeMappingV2.get(sourceCode, v, dateTime))
            .filter(_.isHcc)
            .flatMap(hcc => if (andChildren) hcc.withChildrenV2(dateTime) else List(hcc))
      }
  }

  def getRiskGroupInfo(v: String, date: Option[DateTime] = None, isV2: Boolean = false): RiskGroupInfo = isHcc match {
    case true =>
      val riskGroupsOn = this.adultRiskGroupsOn(date, isV2)
      val riskGroupsOff = if (riskGroupsOn.nonEmpty) this.adultRiskGroupsOff(date, isV2) else List()
      RiskGroupInfo(riskGroupsOn, riskGroupsOff)
    case false =>
      val hccs = isSynthetic match {
        case true => isV2 match {
          case true => CodeMappingV2.get(this, v, date).filter(_.isHcc)
          case false => CodeMapping.get(this, v).filter(_.isHcc)
        }
        case false =>
          val sourceCodes: List[Code] = isIcd() match {
            case true => {
              val icdWithoutDecimal: Code = copy(code = code.replace(".", ""))
              IcdModelV2.get(icdWithoutDecimal, date)
                .getOrElse(List.empty[IcdModelV2])
                .map(_.asCode).distinct
            }
            case false => List(this)
          }
          isV2 match {
            case true => sourceCodes.flatMap(sourceCode => CodeMappingV2.get(sourceCode, v, date)).filter(_.isHcc)
            case false => sourceCodes.flatMap(sourceCode => CodeMapping.get(sourceCode, v)).filter(_.isHcc)
          }
      }
      val riskGroupsOn = hccs.flatMap(code => code.adultRiskGroupsOn(date, isV2)).distinct
      val riskGroupsOff = if (riskGroupsOn.nonEmpty) hccs.flatMap(_.adultRiskGroupsOff(date, isV2)).distinct else List()
      RiskGroupInfo(riskGroupsOn, riskGroupsOff)
  }

  def toIcd(v: String) : List[Code] = isIcd() match {
    case true =>
      List(this)
    case false =>
      CodeMapping.get(this, v).filter(_.isIcd)
  }

  def toIcdV2(v: String, dateTime: Option[DateTime]): List[Code] = isIcd() match {
    case true => List(this)
    case false =>
      CodeMappingV2.get(this, v, dateTime).filter(_.isIcd)
        .flatMap(icd => IcdModelV2.get(icd, dateTime)).flatten
        .map(_.asCode).distinct
  }

  def toFact(v: String) : List[Code] = isQualityFact() match {
    case true =>
      List(this)
    case false =>
      CodeMapping.get(this, v).filter(_.isQualityFact)
  }

  def toFactV2(v: String) : List[Code] = isQualityFact() match {
    case true =>
      List(this)
    case false =>
      CodeMappingV2.get(this, v, None).filter(_.isQualityFact)
  }

  def toConjecture(v: String) : List[Code] = isQualityConjecture() match {
    case true =>
      List(this)
    case false =>
      CodeMapping.get(this, v).filter(_.isQualityConjecture)
  }

  def toConjectureV2(v: String) : List[Code] = isQualityConjecture() match {
    case true =>
      List(this)
    case false =>
      CodeMappingV2.get(this, v, None).filter(_.isQualityConjecture)
  }

  def toMeasure(v: String) : List[Code] = isQualityMeasure() match {
    case true =>
      List(this)
    case false =>
      CodeMapping.get(this, v).filter(_.isQualityMeasure)
  }

  def toMeasureV2(v: String) : List[Code] = isQualityMeasure() match {
    case true =>
      List(this)
    case false =>
      CodeMappingV2.get(this, v, None).filter(_.isQualityMeasure)
  }

  def applySynthetics(v: String) : Code = isSynthetic() match {
    case true =>
      this
    case false =>
      CodeMapping.get(this, v).filter(_.isSynthetic) match {
        case code :: Nil => code
        case Nil => this
        case code :: tail => code //This shouldn't happen
      }
  }

  def applySyntheticsV2(v: String) : Code = isSynthetic() match {
    case true =>
      this
    case false =>
      CodeMappingV2.get(this, v, None).filter(_.isSynthetic) match {
        case code :: Nil => code
        case Nil => this
        case code :: tail => code //This shouldn't happen
      }
  }

  // equivalent and enveloping mappings will always increase, 2020-equivalent will be a subset of 2021-equivalent
  def equivalentCodes(v: String = "2021-equivalent"): List[Code] = {
    CodeMapping.get(this, v)
  }

  def envelopingCodes(v: String = "2021-enveloping"): List[Code] = {
    CodeMapping.get(this, v)
  }

  def canonicalCode(v: String = "2020-canonical"): Code = {
    CodeMapping.get(this, v).headOption.getOrElse(this)
  }

  def equivalentCodesV2(v: String = "2021-equivalent"): List[Code] = {
    CodeMappingV2.get(this, v, None)
  }

  def envelopingCodesV2(v: String = "2021-enveloping"): List[Code] = {
    CodeMappingV2.get(this, v, None)
  }

  def envelopedCodesV2(v: String = "2021-enveloping"): List[Code] = {
    CodeMappingV2.getReversed(this, v, None)
  }

  def canonicalCodeV2(v: String = "2020-canonical"): Code = {
    CodeMappingV2.get(this, v, None).headOption.getOrElse(this)
  }

  def toApxHccFree(vIcd: String = "2021-apxcat-icd", vHcc: String = "2021-apxcat-hcc"): List[Code] = {
    val allApxCats: List[Code] = (this.isHcc(), this.isIcd()) match {
      case (_, true) => CodeMappingV2.get(this, vIcd, None) // should only return one
      case (true, _) => CodeMappingV2.get(this, vHcc, None) // could be more than one
      case _ => throw new Exception(f"Mapping Not Yet Implemented. ${this.system} -> ${Code.APXHCCFREE}")
    }
    allApxCats.filter(_.system == Code.APXHCCFREE)
  }

  def toApxCat(vIcd: String = "2021-apxcat-icd", vHcc: String = "2020-apxcat-hcc", reverseEnvelopEquivalent: Boolean = false): List[Code] = {
    val allApxCats: List[Code] = (this.isHcc(), this.isIcd()) match {
      case (_, true) => CodeMappingV2.get(this, vIcd, None) // should only return one
      case (true, _) => CodeMapping.get(this, vHcc) // could be more than one
      case _ => throw new Exception(f"Mapping Not Yet Implemented. ${this.system} -> ${Code.APXCAT2}")
    }
    val originalApxcat2: List[Code] = allApxCats.filter(c => c.system == Code.APXCAT2 || c.system == Code.APXCAT).map(c => c.copy(system = Code.APXCAT2)).distinct
    // persistence is APXCAT2 old mapping is APXCAT oid
    (this.isHcc(), reverseEnvelopEquivalent) match {
      case (true, true) => {
        originalApxcat2.flatMap { c =>
          val equivalents: List[Code] = c.equivalentCodesV2()
          val enveloped: List[Code] = c.envelopedCodesV2()
          equivalents ++ enveloped ++ List(c)
        }.distinct
      }
      case _ => originalApxcat2
    }
  }

  def fromSynthetic(): List[Code] = {
    isSynthetic match {
      case true =>
        val normalized = this.copy(system = Code.HCC+ system)
        HccModel.get(normalized).map(_.childrenCodes()).getOrElse(List.empty[Code])
      case false => List(this)
    }
  }

  def fromSyntheticV2(): List[Code] = {
    isSynthetic match {
      case true =>
        val normalized = this.copy(system = Code.HCC+ system)
        HccModelV2.get(normalized, None).getOrElse(List.empty[HccModelV2]).flatMap(_.childrenCodes())
      case false => List(this)
    }
  }

  @JsonIgnore def isHcc() : Boolean = system.startsWith(Code.HCC)

  @JsonIgnore def isIcd() : Boolean = system == Code.ICD10 || system == Code.ICD9

  @JsonIgnore def isQualityConjecture() : Boolean = system.startsWith(Code.QUALITYCONJECTURE)

  @JsonIgnore def isQualityMeasure() : Boolean = system.startsWith(Code.QUALITYMEASURE)

  @JsonIgnore def isQualityFact() : Boolean = system.startsWith(Code.QUALITYFACT)

  @JsonIgnore def isSynthetic(): Boolean = system.startsWith(Code.SYNTHETIC)

  @JsonIgnore def isCodeAll() : Boolean = system == Code.CODEALL

  @JsonIgnore def isProviderType() : Boolean = system == Code.RAPSPROV || system == Code.FFSPROV

  @JsonIgnore def isInpatient() : Boolean =
    this == Code(BillTypeModel.providerTypeI, Code.FFSPROV) || this == Code(BillTypeModel.providerType01, Code.RAPSPROV) || this == Code(BillTypeModel.providerType02, Code.RAPSPROV)

  @JsonIgnore def isOutpatient() : Boolean = this == Code(BillTypeModel.providerTypeO, Code.FFSPROV) || this == Code(BillTypeModel.providerType10, Code.RAPSPROV)

  @JsonIgnore def isProfessional() : Boolean = this == Code(BillTypeModel.providerTypeP, Code.FFSPROV) || this == Code(BillTypeModel.providerType20, Code.RAPSPROV)

  @JsonIgnore def isBillType() : Boolean = system == Code.BILLTYPE

  @JsonIgnore def isCpt() : Boolean = system == Code.CPT

  @JsonIgnore def isRiskGroup(): Boolean = system.startsWith(Code.RISKGROUP)

  @JsonIgnore def isSnomed() : Boolean = system == Code.SNOMED

  def description(): String = system match {
    case Code.ICD9 | Code.ICD10 => IcdModel.get(this).map(_.description).getOrElse("")
    case Code.SNOMED => SnomedModel.get(this, None).getOrElse(List.empty[SnomedModel]).headOption.map(_.description).getOrElse("")
    case s if s.startsWith(Code.HCC) => HccModel.get(this).map(_.description).getOrElse("")
    case s if s.startsWith(Code.QUALITYCONJECTURE) => ConjectureModel.get(this).map(_.description).getOrElse("")
    case s if s.startsWith(Code.QUALITYFACT) => FactModel.get(this).map(_.description).getOrElse("")
    case s if s.startsWith(Code.QUALITYMEASURE) => MeasureModel.get(this).map(_.description).getOrElse("")
    case s if s.startsWith(Code.PROSPECTIVE) => IcdModel.get(this).map(_.description).getOrElse("")
    case Code.CODEALL => "Code this entire document"
    case _ => ""
  }

  /**
   * Get descriptions for V2 codes
   * @param dateTime
   * @return
   */
  def descriptionV2(dateTime: Option[DateTime]): String = system match {
    case Code.ICD9 | Code.ICD10 => IcdModelV2.get(this, dateTime).getOrElse(List.empty[IcdModelV2]).headOption.map(_.description).getOrElse("")
    case Code.SNOMED => SnomedModel.get(this, dateTime).getOrElse(List.empty[SnomedModel]).headOption.map(_.description).getOrElse("")
    case s if s.startsWith(Code.HCC) => HccModelV2.get(this, dateTime).getOrElse(List.empty[HccModelV2]).headOption.map(_.description).getOrElse("")
    case s if s.startsWith(Code.QUALITYCONJECTURE) => ConjectureModel.get(this).map(_.description).getOrElse("")
    case s if s.startsWith(Code.QUALITYFACT) => FactModel.get(this).map(_.description).getOrElse("")
    case s if s.startsWith(Code.QUALITYMEASURE) => MeasureModel.get(this).map(_.description).getOrElse("")
    case s if s.startsWith(Code.PROSPECTIVE) => IcdModelV2.get(this, dateTime).getOrElse(List.empty[IcdModelV2]).headOption.map(_.description).getOrElse("")
    case Code.CODEALL => "Code this entire document"
    case _ => ""
  }

  def annotationFields() : Seq[AnnotationField] = system match {
    case s if s.startsWith(Code.QUALITYCONJECTURE) => ConjectureModel.get(this).map(_.annotationFields)
      .getOrElse(List.empty[AnnotationField])
    case _ => Seq.empty[AnnotationField]
  }

  def withChildren() : List[Code] = isHcc match {
    case true => HccModel.get(this).map(_.withChildren).getOrElse(List(this))
    case false => List(this)
  }

  def withChildrenV2(dateTime: Option[DateTime]): List[Code] = isHcc match {
    case true => HccModelV2.get(this, dateTime) match {
      case Some(hccs) => hccs.flatMap(_.withChildren)
      case None => List(this)
    }
    case false => List(this)
  }

  def adultRiskGroupsOn(date: Option[DateTime] = None, isV2: Boolean = false): List[Code] = isHcc match {
    case true =>
      val adultRiskGroups: List[Code] = isV2 match {
        case true => HccModelV2.get(this, date) match {
          case Some(hccModelV2s) => hccModelV2s.flatMap(_.getAdultRiskGroupsOn())
          case None => List()
        }
        case false => HccModel.get(this).map(_.getAdultRiskGroupsOn).getOrElse(List())
      }
      RiskGroupModel.getAdultRiskGroups(adultRiskGroups, date).map(_.asCode)
    case false => List()
  }

  def adultRiskGroupsOff(date: Option[DateTime] = None, isV2: Boolean = false): List[Code] = isHcc match {
    case true => isV2 match {
      case true => HccModelV2.get(this, date) match {
        case Some(hccModelV2s) => hccModelV2s.flatMap(_.getAdultRiskGroupsOff())
        case None => List()
      }
      case false => HccModel.get(this).map(_.getAdultRiskGroupsOff).getOrElse(List())
    }
    case false => List()
  }

  def toSnomed(v: String, dateTime: Option[DateTime]): List[Code] = {
    if (isSnomed()) {
      List(this)
    } else {
      isIcd() match {
        case true => {
          val somedCodes: List[Code] = CodeMappingV2.get(this, SNOMEDType, dateTime).filter(_.isSnomed())
            .flatMap(snomedCode => SnomedModel.get(snomedCode, dateTime)).flatten
            .map(_.asCode).distinct
          List(this) ++ somedCodes
        }
        case _ => {
          val icdCodes: List[Code] = CodeMappingV2.get(this, v, dateTime).filter(_.isIcd)
            .flatMap(icd => IcdModelV2.get(icd, dateTime)).flatten
            .map(_.asCode).distinct
          icdCodes.map(e => Code(e.code, e.system).toSnomed(e.system, dateTime)).flatten
        }
      }
    }
  }
}

object Code {
  val HCC = "HCC"
  val HCCV12 = "HCCV12"
  val HCCV22 = "HCCV22"
  val HCCV23 = "HCCV23"
  val HCCV24 = "HCCV24"
  val HCCALL = "HCCALL"
  val HCCCRV1 = "HCCCRV1"
  val HCCCRV2 = "HCCCRV2"
  val HCCV07 = "HCCV07"
  val HCCCRV3 = HCCV07 // so we don't accidentally continue the same nonsense naming scheme
  val HCCNYL = "HCCNYL"
  val HCCRX = "HCCRX"
  val QUALITYMEASURE = "QM"
  val QUALITYCONJECTURE = "QC"
  val QUALITYFACT = "QF"
  val QUALITYMEASUREV1 = "QMV1"
  val FACTV1 = "QFV1"
  val CONJECTUREV1 = "QCV1"
  val APXCAT = "2.25.323880427068280173988184352073120628196"
  val APXCAT2 = "APXCAT" // trying to align with https://docs.google.com/document/d/1X2IWqrk6PeK81bfHeITSlqEjRsNnwDhOww9pOzMyu7I/edit
  val APXCAT3 = "APXCAT3"
  val APXHCCFREE = "APXHCCFREE"
  val APXCAT4 = APXHCCFREE
  val CODEALL = "2.16.840.1.113883.6"
  val ICD9 = "2.16.840.1.113883.6.103"
  val ICD10 = "2.16.840.1.113883.6.90"
  val CPT = "2.16.840.1.113883.6.12"
  val RAPSPROV = "2.25.427343779826048612975555299345414078866"
  val FFSPROV = "2.25.986811684062365523470895812567751821389"
  val BILLTYPE = "2.25.726424807521776741141709914703687743312"
  val PROSPECTIVE = "PROSP"
  val SYNTHETIC = "SYN"
  val RISKGROUP = "RG"
  val SNOMED = "2.16.840.1.113883.6.96"
  val IMO = "2.16.840.1.113883.3.247.1.1"

  def apply(c: CodeType): Code = {
    val newC = if (c.getCodeSystem == ICD9 || c.getCodeSystem == ICD10) c.getCode.replace(".", "") else c.getCode
    val newS = if (c.getCodeSystem == "APXCAT") APXCAT else c.getCodeSystem
    new Code(newC, newS)
  }

  def fromJson(obj: String)(implicit mapper: ObjectMapper with ScalaObjectMapper) : Code = mapper.readValue[Code](obj)
}
