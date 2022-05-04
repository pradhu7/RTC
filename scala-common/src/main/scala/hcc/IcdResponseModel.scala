package com.apixio.scala.utility

import com.apixio.model.profiler.{IcdModel, IcdModelV2}

case class IcdResponseModel(
  code: String,
  codeSystem: String,
  codeSystemName: String = "TBD",
  codeSystemVersion: String,
  displayName: String,
  snomeds: Option[Seq[SnomedResponseModel]])

case class SnomedResponseModel(
  code: String,
  codeSystem: String,
  displayName: String,
  mid: String = "",
  type_id: String = "",
  cs_id:String = "")

object IcdResponseModel {
  def apply(raw: IcdModel, icdSystem: String, snomeds: Option[Seq[SnomedResponseModel]]): IcdResponseModel = {
    IcdResponseModel(
      code = raw.code,
      codeSystem = raw.system,
      codeSystemVersion = icdSystem,
      displayName = raw.description,
      snomeds = snomeds
    )
  }

  def apply(raw: IcdModelV2, icdSystem: String, snomeds: Option[Seq[SnomedResponseModel]]): IcdResponseModel = {
    IcdResponseModel(
      code = raw.code,
      codeSystem = raw.system,
      codeSystemVersion = icdSystem,
      displayName = raw.description,
      snomeds = snomeds
    )
  }
}