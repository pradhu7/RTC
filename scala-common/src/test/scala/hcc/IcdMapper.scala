package com.apixio.scala.utility

import com.apixio.model.profiler.Code
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

//WIP

import com.apixio.scala.dw.{ApxConfiguration, ApxServices}
import com.fasterxml.jackson.databind.ObjectMapper

class IcdMapperSpec extends AnyFlatSpec with Matchers {
  "IcdMapper" should "correctly setup" in {

    ApxConfiguration.initializeFromFile("application-dev.yaml")
    ApxServices.init(ApxConfiguration.configuration.get)
    ApxServices.setupApxLogging()
    ApxServices.setupObjectMapper(new ObjectMapper())
  }

  //Cutoff date for mapping version is 10/01/YYYY

  it should "choose 2015-icd-hcc" in {
    val paymentYear = "2015"
    val icdMapping = "2015+16+17-icd-hcc"
    val apxcatMapping = ""
    val code = Code("", Code.HCC)
    val dos = "09/30/2015"

    val args = (paymentYear, icdMapping, apxcatMapping, code, dos)

    val mapping = (IcdMapper.getHCCMappingVersion _).tupled(args)
    assert(
      mapping == "2015-icd-hcc",
      "mapping should be 2015-icd-hcc"
    )
  }

  it should "choose 2016-icd-hcc" in {
    val paymentYear = "2016"
    val icdMapping = "2016+17-icd-hcc"
    val apxcatMapping = ""
    val code = Code("", Code.HCC)
    val dos = "09/30/2016"

    val args = (paymentYear, icdMapping, apxcatMapping, code, dos)

    val mapping = (IcdMapper.getHCCMappingVersion _).tupled(args)
    assert(
      mapping == "2016-icd-hcc",
      "mapping should be 2016-icd-hcc"
    )
  }

  it should "choose 2017-icd-hcc" in {
    val paymentYear = "2016"
    val icdMapping = "2016+17-icd-hcc"
    val apxcatMapping = ""
    val code = Code("",Code.HCC)
    val dos = "10/01/2016"

    val args = (paymentYear, icdMapping, apxcatMapping, code, dos)

    val mapping = (IcdMapper.getHCCMappingVersion _).tupled(args)
    assert(
      mapping == "2017-icd-hcc",
      "mapping should be 2017-icd-hcc"
    )
  }

  it should "choose 2017-icd-hcc with messed up icdmapping year" in {
    val paymentYear = "2016"
    val icdMapping = "20199-icd-hcc"
    val apxcatMapping = ""
    val code = Code("",Code.HCC)
    val dos = "10/01/2016"

    val args = (paymentYear, icdMapping, apxcatMapping, code, dos)

    val mapping = (IcdMapper.getHCCMappingVersion _).tupled(args)
    assert(
      mapping == "2017-icd-hcc",
      "mapping should be 2017-icd-hcc"
    )
  }

  it should "choose 2016+17-icd-hcc without dos" in {
    val paymentYear = "2016"
    val icdMapping = "2016+17-icd-hcc"
    val apxcatMapping = ""
    val code = Code("",Code.HCC)
    val dos = ""

    val args = (paymentYear, icdMapping, apxcatMapping, code, dos)

    val mapping = (IcdMapper.getHCCMappingVersion _).tupled(args)
    assert(
      mapping == "2016+17-icd-hcc",
      "mapping should be 2016+17-icd-hcc"
    )
  }

  it should "choose 2017-icd-hcccrv1" in {
    val paymentYear = "2016"
    val icdMapping = "2016+17-icd-hcccrv1"
    val apxcatMapping = ""
    val code = Code("",Code.HCCCRV1)
    val dos = "10/01/2016"

    val args = (paymentYear, icdMapping, apxcatMapping, code, dos)

    val mapping = (IcdMapper.getHCCMappingVersion _).tupled(args)
    assert(
      mapping == "2017-icd-hcccrv1",
      "mapping should be 2017-icd-hcccrv1"
    )
  }

  it should "choose 2016-icd-hcccrv1" in {
    val paymentYear = "2016"
    val icdMapping = "2016+17-icd-hcccrv1"
    val apxcatMapping = ""
    val code = Code("",Code.HCCCRV1)
    val dos = "09/30/2016"

    val args = (paymentYear, icdMapping, apxcatMapping, code, dos)

    val mapping = (IcdMapper.getHCCMappingVersion _).tupled(args)
    assert(
      mapping == "2016-icd-hcccrv1",
      "mapping should be 2016-icd-hcccrv1"
    )
  }

  it should "use anl override" in {
    val paymentYear = "2020"
    val icdMapping = "2020-anl-icd-hcccrv2-override"
    val apxcatMapping = ""
    val code = Code("",Code.HCCCRV1)
    val dos = "09/30/2015"

    val args = (paymentYear, icdMapping, apxcatMapping, code, dos)

    val mapping = (IcdMapper.getHCCMappingVersion _).tupled(args)
    assert(
      mapping == "2020-anl-icd-hcccrv2-override",
      "mapping should be 2019-anl-icd-hcccrv2-override"
    )
  }

  it should "choose 2016+17-icd-hcccrv1" in {
    val paymentYear = "2016"
    val icdMapping = "2016+17-icd-hcccrv1"
    val apxcatMapping = ""
    val code = Code("",Code.HCCCRV1)
    val dos = ""

    val args = (paymentYear, icdMapping, apxcatMapping, code, dos)

    val mapping = (IcdMapper.getHCCMappingVersion _).tupled(args)
    assert(
      mapping == "2016+17-icd-hcccrv1",
      "mapping should be 2016+17-icd-hcccrv1"
    )
  }

  it should "fall back to 2016-icd-hcc using payment year 2017" in {
    val paymentYear = "2017"
    val icdMapping = ""
    val apxcatMapping = ""
    val code = Code("",Code.HCC)
    val dos = ""

    val args = (paymentYear, icdMapping, apxcatMapping, code, dos)

    val mapping = (IcdMapper.getHCCMappingVersion _).tupled(args)
    assert(
      mapping == "2016-icd-hcc",
      "mapping should be 2016-icd-hcc"
    )
  }

  it should "fall back to 2016-icd-hcc using payment year 2016" in {
    val paymentYear = "2016"
    val icdMapping = ""
    val apxcatMapping = ""
    val code = Code("",Code.HCC)
    val dos = ""

    val args = (paymentYear, icdMapping, apxcatMapping, code, dos)

    val mapping = (IcdMapper.getHCCMappingVersion _).tupled(args)
    assert(
      mapping == "2016-icd-hcc",
      "mapping should be 2016-icd-hcc"
    )
  }
  it should "fall back to 2015-icd-hcc using payment year" in {
    val paymentYear = "2015"
    val icdMapping = ""
    val apxcatMapping = ""
    val code = Code("",Code.HCC)
    val dos = ""

    val args = (paymentYear, icdMapping, apxcatMapping, code, dos)

    val mapping = (IcdMapper.getHCCMappingVersion _).tupled(args)
    assert(
      mapping == "2015-icd-hcc",
      "mapping should be 2015-icd-hcc"
    )
  }

  it should "choose apxcat mapping" in {
    val paymentYear = "2016"
    val icdMapping = ""
    val apxcatMapping = "2016+17-apxcat-hcc"
    val code = Code("",Code.APXCAT)
    val dos = ""

    val args = (paymentYear, icdMapping, apxcatMapping, code, dos)

    val mapping = (IcdMapper.getHCCMappingVersion _).tupled(args)
    assert(
      mapping == "2016+17-apxcat-hcc",
      "mapping should be 2016+17-apxcat-hcc"
    )
  }

  it should "use 2016-apxcat-hcc mapping" in {
    val paymentYear = "2016"
    val icdMapping = ""
    val apxcatMapping = ""
    val code = Code("",Code.APXCAT)
    val dos = ""

    val args = (paymentYear, icdMapping, apxcatMapping, code, dos)

    val mapping = (IcdMapper.getHCCMappingVersion _).tupled(args)
    assert(
      mapping == "2016-apxcat-hcc",
      "mapping should be 2016-apxcat-hcc"
    )
  }



}
