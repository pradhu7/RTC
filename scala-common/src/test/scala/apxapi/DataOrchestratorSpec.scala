package com.apixio.scala.apxapi

import com.apixio.XUUID
import com.apixio.restbase.RestUtil
import com.apixio.scala.dw.{ApxConfiguration, ApxServices}
import com.fasterxml.jackson.databind.ObjectMapper
import org.scalatest.matchers.should.Matchers

class DataOrchestratorSpec extends AuthSpec with Matchers {
  ApxConfiguration.initializeFromFile("application-dev.yaml")
  ApxServices.init(ApxConfiguration.configuration.getOrElse(ApxConfiguration(Map())))
  ApxServices.setupObjectMapper(new ObjectMapper())
  ApxServices.setupDefaultModels

  val apxapi = login(custops)

  "DataOrchestratorSpec" should "be able to query patient demographic" in {
    val demo1 = ApxApi.internal.dataorchestrator.demographics("3cf21191-2904-49bf-a4a3-b667149687aa")
    assert(demo1.nonEmpty)
    val demo2 = ApxApi.internal.dataorchestrator.demographics("3cf21191-2904-49bf-a4a3-b667149687aa", Some("O_00000000-0000-0000-0000-000000001174"))
    assert(demo2.nonEmpty)
    assert(demo1 == demo2)
  }

  "DataOrchestratorSpec" should "be able to query document metadata" in {
    val doc1 = ApxApi.internal.dataorchestrator.documentMetadata("2e9029a2-010c-43de-9498-7da027b819f5")
    assert(doc1.nonEmpty)
    val doc2 = ApxApi.internal.dataorchestrator.documentMetadata("2e9029a2-010c-43de-9498-7da027b819f5", Some("O_00000000-0000-0000-0000-000000001174"))
    assert(doc2.nonEmpty)
    val doc3 = ApxApi.internal.dataorchestrator.documentMetadata("2e9029a2-010c-43de-9498-7da027b819f5", Some("O_00000000-0000-0000-0000-000000001174"), Some("05a423a6-a0eb-4a3d-96df-19b9506de6d0"))
    assert(doc3.nonEmpty)

    //assert(doc1 == doc2)
    assert(doc2 == doc3)
  }

}
