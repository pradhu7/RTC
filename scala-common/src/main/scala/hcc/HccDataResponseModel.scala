package com.apixio.scala.utility

case class HccDataResponseModel(
  totalCount: Int = 1,
  pageIndex: Int = 0,
  pageSize: Int,
  hccs: List[HccResponseModel]
)
