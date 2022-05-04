package com.apixio.scala.apxapi

import com.apixio.XUUID
import com.apixio.restbase.RestUtil
import com.apixio.scala.dw.{ApxConfiguration, ApxServices}
import com.fasterxml.jackson.databind.ObjectMapper
import org.scalatest.flatspec.AnyFlatSpec


trait AuthSpec extends AnyFlatSpec with AuthCredsSpec {
  ApxConfiguration.initializeFromFile("application-dev.yaml")
  ApxServices.init(ApxConfiguration.configuration.get)
  ApxServices.setupObjectMapper(new ObjectMapper())
  ApxServices.setupDefaultModels

  def login(s: ApxSession): Unit = {
    s.swap_token()
    val xToken = XUUID.fromString(s.internal_token.get)
    assert(xToken != null)
    val token = ApxServices.sysServices.getTokens.findTokenByID(xToken)
    assert(token != null)
    RestUtil.setInternalToken(token)
  }

  def login(external_token: String): ApxApi = {
    val apxapi = new ApxApi(ApxConfiguration.configuration.get,
                            external_token=Option(external_token))
    login(apxapi.session)
    apxapi
  }
}

trait AuthCredsSpec {
  val custops= "TA_90de4c86-8df0-4f66-87e4-0a3532aefcb9"
  val coder1 = "TA_71c9b940-2bb6-4036-bb67-82032b2aba7b"
  val orgId = "UO_a6ecd10b-6e81-4c89-a423-41f9488d0af0"
}
