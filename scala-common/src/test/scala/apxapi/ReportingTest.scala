package com.apixio.scala.apxapi

import com.apixio.scala.apxapi.{ApxApi, AuthCredsSpec, FilterParam, ParamValueElement}
import com.apixio.scala.dw.{ApxConfiguration, ApxServices}
import com.fasterxml.jackson.databind.ObjectMapper
import org.scalatest.Ignore

@Ignore
class ReportingTest extends AuthSpec {
  ApxConfiguration.initializeFromFile("application-dev.yaml")
  ApxServices.init(ApxConfiguration.configuration.get)
  ApxServices.setupObjectMapper(new ObjectMapper())

  val apxapi = login(coder1)

  "It" should "pulls output report V2 count" in {
    val projectId = "PRHCC_2fd31af9-3789-4655-895e-a5aa8cf7514b"
    val count = apxapi.reporting.getCountV2(projectId)
    assert(count == 1)
  }

  "It" should "pulls output report V2 count with filter" in {
    val projectId = "PRHCC_2fd31af9-3789-4655-895e-a5aa8cf7514b"
    val filterParam = FilterParam(List(ParamValueElement("nodes", List("prediction-review"))))
    val count = apxapi.reporting.getCountV2(projectId, filterParam)
    assert(count == 1)
  }


  "It" should "returns a count of 0 for invalid output report V2 filter" in {
    val projectId = "PRHCC_2fd31af9-3789-4655-895e-a5aa8cf7514b"
    val filterParam = FilterParam(List(ParamValueElement("nodes", List("mock-node-id"))))
    val count = apxapi.reporting.getCountV2(projectId, filterParam)
    assert(count == 0)
  }
}
