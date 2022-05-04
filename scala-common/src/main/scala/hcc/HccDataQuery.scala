package com.apixio.scala.utility

case class HccDataQuery(
  hccCode: String = "",
  icdCode: String = "",
  labelSetVersion: String = "",
  dateOfService: String = "",
  q: String = "",
  pageIndex: Int = 0,
  pageSize: Int = -1,
  paymentYear: String = "",
  icdMapping: String = "",
  apxcatMapping: String = "",
  mappingV2: Boolean = false,
  snomed: Boolean = false
)
