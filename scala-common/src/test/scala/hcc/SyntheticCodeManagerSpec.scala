package com.apixio.scala.utility.hcc

import com.apixio.model.profiler.Code
import com.apixio.scala.dw.{ApxConfiguration, ApxServices}
import com.fasterxml.jackson.databind.ObjectMapper
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SyntheticCodeManagerSpec extends AnyFlatSpec with Matchers {
  ApxConfiguration.initializeFromFile("application-dev.yaml")
  ApxServices.init(ApxConfiguration.configuration.get)
  ApxServices.setupObjectMapper(new ObjectMapper())
  ApxServices.setupDefaultModels

  "SyntheticLogic" should "correctly parse inline mapping" in {
    val raw = "inline,[{\"c\":\"18.19\",\"d\":\"Diabetes\",\"o\":[\"18-HCCV22\",\"19-HCCV22\"],\"v\":\"SYNV1\"}]"
    val res = SyntheticCodeManager.parseInlineMapping(raw)
    res shouldBe Map(Code("18.19", "HCCSYNV1") -> List(Code("18", Code.HCCV22), Code("19", Code.HCCV22)))
  }

  it should "correctly parse cached mapping" in {
    val raw =
      """
        [{"c":"18.19","d":"Diabetes","o":["18-HCCV22","19-HCCV22"],"v":"SYNV1"}]
      """.stripMargin
    val res = SyntheticCodeManager.parseRawMapping(raw)
    res shouldBe Map(Code("18.19", "HCCSYNV1") -> List(Code("18", Code.HCCV22), Code("19", Code.HCCV22)))
  }

  it should "correctly get families from model" in {
    val raw = "HCCSYNV1"
    val res = SyntheticCodeManager.parseSyntheticModel(raw)
    res shouldBe Map(Code("18.19", "HCCSYNV1") -> List(Code("18", Code.HCCV22), Code("19", Code.HCCV22)))
  }
}
