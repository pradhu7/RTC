package com.apixio.scala.apxapi

import com.apixio.XUUID
import com.apixio.restbase.RestUtil
import com.apixio.scala.dw.{ApxConfiguration, ApxServices}
import com.fasterxml.jackson.databind.ObjectMapper
import org.scalatest.matchers.should.Matchers

class NpiSpec extends AuthSpec with Matchers {
  val npi = "1093864027"
  ApxConfiguration.initializeFromFile("application-dev.yaml")
  ApxServices.init(ApxConfiguration.configuration.get)
  ApxServices.setupObjectMapper(new ObjectMapper())

  val apxapi = login(coder1)

  "getProvider" should "return the correct info" in {
    val provider = apxapi.npi.getProvider(npi)
    provider.nonEmpty should be(true)
    provider.get.ln.toUpperCase() should be("ELLIS")
    provider.get.fn.toUpperCase() should be("ROBERT")
    provider.get.npi should be("1093864027")
  }
}
