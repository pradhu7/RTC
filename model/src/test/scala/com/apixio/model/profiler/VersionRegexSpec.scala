package com.apixio.model.profiler

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class VersionRegexSpec extends AnyFlatSpec with Matchers {
  "VersionRegex" should "be able to recognize normal MA version" in {
    CodeMappingV2.isNormalMAMappingType( "2020-icd-hcc") shouldBe true
  }

  "VersionRegex" should "be able to recognize normal CR version" in {
    CodeMappingV2.isNormalCRMappingType("2020-icd-hcccrv2") shouldBe true
  }

  "VersionRegex" should "not recognize normal MA version with CR regex" in {
    CodeMappingV2.isNormalCRMappingType("2020-icd-hcc") shouldBe false
  }

  "VersionRegex" should "not recognize normal CR version with MA regex" in {
    CodeMappingV2.isNormalMAMappingType("2020-icd-hcccrv2") shouldBe false
  }

  "VersionRegex" should "not recognize weird versions with MA regex" in {
    CodeMappingV2.isNormalMAMappingType("2015+18science-icd-hcc") shouldBe false
  }

  "VersionRegex" should "be able to recognize new MA version" in {
    CodeMappingV2.isNormalMAMappingType( "icd-hcc") shouldBe true
  }

  "VersionRegex" should "be able to recognize new CR version" in {
    CodeMappingV2.isNormalCRMappingType("icd-hcccrv2") shouldBe true
  }

}
