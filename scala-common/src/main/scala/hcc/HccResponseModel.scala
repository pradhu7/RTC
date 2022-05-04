package com.apixio.scala.utility

case class HccResponseModel(
  code: String,
  labelSetVersion: String,
  icds: List[IcdResponseModel]
)
