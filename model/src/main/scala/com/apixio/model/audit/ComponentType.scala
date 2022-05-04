package com.apixio.model.audit

object ComponentType extends Enumeration {
  type ComponentType = Value
  val INTEGRATION_SVC, LOADER_SVC, ETL_V2, SPARK_SIGGEN, CEREBRO, LAMBDAECC, LOADER_APP = Value
}

