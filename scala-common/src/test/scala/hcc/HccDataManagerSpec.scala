package com.apixio.scala.utility

import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import com.apixio.scala.dw.{ApxConfiguration, ApxServices}
import com.fasterxml.jackson.databind.ObjectMapper

class HccDataManagerSpec extends AnyFlatSpec with Matchers with BeforeAndAfter {

  before({
    ApxConfiguration.initializeFromFile("application-dev.yaml")
    ApxServices.init(ApxConfiguration.configuration.get)
    ApxServices.setupApxLogging()
    ApxServices.setupObjectMapper(new ObjectMapper())
    ApxServices.setupDefaultModels
  })

  // Sanity
  it should "get HCC 23 with all ICDs including snomed" in {
    val filter = HccDataQuery(
      hccCode = "23",
      labelSetVersion = "HCCV24",
      dateOfService = "01/01/2020",
      mappingV2 = false,
      icdMapping = "2020-icd-hcc",
      snomed = true,
      pageSize=10
    )

    val data = HccDataManager.getHcc(filter)
    assert(data.hccs(0).icds.length == 224,"icd count should match known count for that HCC")
    // assert(22 == data.hccs(0).icds.filter(_.code == "E236").head.snomeds.toList.flatten.length)
    // assert(16 == data.hccs(0).icds.filter(_.code == "E278").head.snomeds.toList.flatten.length)
  }

  it should "recognize this science mapping" in {
    val filter = HccDataQuery(
      hccCode = "999",
      labelSetVersion = "HCCV24",
      dateOfService = "ignore this",
      mappingV2 = true,
      icdMapping = "2021-icd-hcc-scienceV24"
    )

    val data = HccDataManager.getHcc(filter)
    assert(data.hccs(0).icds.nonEmpty,"non empty response and no date parse error error")
  }

  it should "get HCCV07 ICDs" in {
    val filter = HccDataQuery(
      hccCode = "1",
      labelSetVersion = "HCCV07",
      dateOfService = "01/10/2021",
      mappingV2 = true,
      icdMapping = "icd-hcc"
    )
    val data = HccDataManager.getHcc(filter)
    assert(data.hccs(0).icds.nonEmpty,"icds should exist for HCCV07 1")
  }

  it should "get HCC 23 with all ICDs" in {
    val filter = HccDataQuery(
      hccCode = "23",
      labelSetVersion = "HCCV24",
      dateOfService = "01/01/2020",
      mappingV2 = true,
      icdMapping = "2020-icd-hcc"
    )

    val data = HccDataManager.getHcc(filter)
    assert(data.hccs(0).icds.length == 224,"icd count should match known count for that HCC"
    )
  }

  it should "get HCC 46 with all ICDs" in {
    val filter = HccDataQuery(
      hccCode = "46",
      labelSetVersion = "HCCV22",
      dateOfService = "01/01/2015"
    )

    val data = HccDataManager.getHcc(filter)
    assert(data.hccs(0).icds.length == 31,
      "icd count should match known count for that HCC")
  }

  it should "get HCC 10 with all ICDs" in {
    val filter = HccDataQuery(
      hccCode = "10",
      labelSetVersion = "HCCV22",
      dateOfService = "01/01/2016"
    )

    val data = HccDataManager.getHcc(filter)
    assert(
      data.hccs(0).icds.length == 522,
      "icd count should match known count for that HCC"
    )
  }

  behavior of "Pagination"

  it should "default to all hccs" in {
    val filter = HccDataQuery(
      labelSetVersion = "HCCV22",
      dateOfService = "12/31/2016",
      q = "ion"
    )

    val data = HccDataManager.getHcc(filter)
    assert(data.hccs.length == 67,
      "hcc count should match pageSize")
    assert(data.totalCount == 67, "total count should equal all hccs")
    assert(data.pageSize == 67, "page size should equal all hccs")
    assert(data.pageIndex == 0, "page index should be 0")
  }

  it should "paginate by pageSize of 10" in {
    val filter = HccDataQuery(
      labelSetVersion = "HCCV22",
      dateOfService = "12/31/2016",
      q = "ion",
      pageIndex = 0,
      pageSize = 10
    )

    val data = HccDataManager.getHcc(filter)
    assert(data.hccs.length == 10,
      "hcc count should match pageSize")
    assert(data.pageSize == 10, "page size should be 10")
    assert(data.pageIndex == 0, "page index should be 0")
  }

  it should "get the first page" in {
    val filter = HccDataQuery(
      labelSetVersion = "HCCV22",
      dateOfService = "01/31/2016",
      q = "ion",
      pageSize = 10
    )

    val data = HccDataManager.getHcc(filter)
    assert(data.totalCount == 67,"total count should equal all hccs")
    assert(data.hccs.length == 10,"hcc count should match pageSize")
    assert(data.pageSize == 10, "page size should be 10")
    assert(data.pageIndex == 0, "page index should be 1")

    val first = data.hccs.head
    assert(first.code == "188")
    assert(first.icds.length == 25)

    val last = data.hccs.last
    assert(last.code == "186")
    assert(last.icds.length == 25)
  }

  it should "get the second page" in {
    val filter = HccDataQuery(
      labelSetVersion = "HCCV22",
      dateOfService = "01/31/2016",
      q = "ion",
      pageIndex = 1,
      pageSize = 10
    )

    val data = HccDataManager.getHcc(filter)
    assert(data.totalCount == 67,"total count should equal all hccs")
    assert(data.hccs.length == 10,"hcc count should match pageSize")
    assert(data.pageSize == 10,"page size should be 10")
    assert(data.pageIndex == 1,"page index should be 1")

    val first = data.hccs.head
    assert(first.code == "124")
    assert(first.icds.length == 1)

    val last = data.hccs.last
    assert(last.code == "173")
    assert(last.icds.length == 129)
  }

  it should "get the last page" in {
    val filter = HccDataQuery(
      labelSetVersion = "HCCV22",
      dateOfService = "01/31/2016",
      q = "ion",
      pageIndex = 6,
      pageSize = 10
    )

    val data = HccDataManager.getHcc(filter)

    assert(data.totalCount == 67,"total count should equal all hccs")
    assert(data.hccs.length == 7,"hcc count should match pageSize")
    assert(data.pageSize == 10, "page size should be 10")
    assert(data.pageIndex == 6, "page index should be 6")

    val first = data.hccs.head
    assert(first.code == "114")
    assert(first.icds.length == 4)

    val last = data.hccs.last
    assert(last.code == "22")
    assert(last.icds.length == 1)
  }

  behavior of "Years before and including 2015 and after 2016"
  it should "get same result for 2017 as for 2016" in {
    val filter2016 = HccDataQuery(
      hccCode = "55",
      labelSetVersion = "HCCV22",
      dateOfService = "12/31/2016"
    )

    val filter2017 = HccDataQuery(
      hccCode = "55",
      labelSetVersion = "HCCV22",
      dateOfService = "01/31/2017"
    )

    val data2016 = HccDataManager.getHcc(filter2016)
    assert(
      data2016.hccs(0).icds.length == 271,
      "icd count should match known count for that HCC"
    )

    val data2017 = HccDataManager.getHcc(filter2017)
    assert(
      data2016.hccs(0).icds.length == data2017.hccs(0).icds.length,
      "icd count should match between 2016 and 2017"
    )
  }

  it should "get same result for 2014 as for 2015" in {
    val filter2015 = HccDataQuery(
      hccCode = "55",
      labelSetVersion = "HCCV22",
      dateOfService = "01/01/2015"
    )

    val filter2014 = HccDataQuery(
      hccCode = "55",
      labelSetVersion = "HCCV22",
      dateOfService = "12/01/2014"
    )

    val data2015 = HccDataManager.getHcc(filter2015)
    assert(
      data2015.hccs(0).icds.length == 48,
      "icd count should match known count for that HCC"
    )

    val data2014 = HccDataManager.getHcc(filter2014)
    assert(
      data2015.hccs(0).icds.length == data2014.hccs(0).icds.length,
      "icd count should match between 2014 and 2015"
    )
  }

  // Commercial Risk Sanity
  behavior of "Commercial Risk Sanity"

  it should "Search for commercial risk HCCS" in {
    val filter = HccDataQuery(
      hccCode = "67",
      labelSetVersion = "HCCCRV1",
      dateOfService = "01/01/2016",
      icdMapping = "2016-icd-hcccrv1"
    )
    val data = HccDataManager.getHcc(filter)
    assert(data.hccs.length == 1, "no hccs found")
    assert(
      data.hccs(0).icds.length == 14,
      "should return 14 icds for HCC 67 HCCCRV1 ICD10 2016-icd-hcccrv1"
    )
  }

  /*it should "Search for commercial risk by ICD" in {
    val filter = HccDataQuery(
      icdCode = "23873",
      labelSetVersion = "HCCCRV1",
      dateOfService = "01/01/2015",
      icdMapping = "2016-icd-hcccrv1"
    )
    val data = HccDataManager.getHcc(filter)
    assert(data.hccs.length == 1, "no hccs found")
    assert(
      data.hccs(0).icds.length == 1,
      "should return 1 icd for ICD code 23873 in HCCCRV1 ICD9 2016-icd-hcccrv1"
    )
  }*/

  it should "Search for commercial risk by description" in {
    val filter = HccDataQuery(
      labelSetVersion = "HCCCRV1",
      dateOfService = "01/01/2016",
      q = "Other artificial opening status",
      icdMapping = "2016-icd-hcccrv1"
    )

    val data = HccDataManager.getHcc(filter)
    assert(data.hccs.length == 1, "no hccs found")
    assert(
      data.hccs(0).icds.length == 3,
      "should return 3 icds for query in HCCCRV1 ICD10 2016-icd-hcccrv1"
    )
  }

  // Most possible cases
  behavior of "*** Combination of possible search parameters ***"
  /**
   * Search options:
   *                  | Done
   *  HCC | !ICD | !Q | x
   *  HCC | !ICD |  Q | x
   *  HCC |  ICD | !Q | x
   *  HCC |  ICD |  Q | x
   * !HCC | !ICD | !Q | x
   * !HCC | !ICD |  Q | x
   * !HCC |  ICD | !Q | x
   * !HCC |  ICD |  Q | x
   */


  behavior of "search by HCC no ICD no Query - only in HCCV12"
  /**
   * HCC Code in V12 only
   *
   * Should only work with:
   * - payment years <= 2015
   * - ICD9
   * - HCCV12
   */

  it should "get 9 ICDs for HCC 149 v12 for 2015" in {
    val filter = HccDataQuery(
      hccCode = "149",
      labelSetVersion = "HCCV12",
      dateOfService = "01/01/2015"
    )

    val data = HccDataManager.getHcc(filter)
    assert(
      data.hccs(0).icds.length == 9,
      "icd count should match known count for that HCC"
    )
  }

  it should "get 9 ICDs for HCC 149 v12 for year prior to 2015" in {
    val filter = HccDataQuery(
      hccCode = "149",
      labelSetVersion = "HCCV12",
      dateOfService = "12/31/2014"
    )
    val data = HccDataManager.getHcc(filter)
    assert(
      data.hccs(0).icds.length == 9,
      "icd count should match known count for that HCC"
    )
  }

  it should "get 0 HCCs for HCC 149 if label Set Version is v22" in {
    val filter = HccDataQuery(
      hccCode = "149",
      labelSetVersion = "HCCV22",
      dateOfService = "01/01/2015"
    )

    val data = HccDataManager.getHcc(filter)
    assert( data.hccs.isEmpty, "HCC length should be 0")
  }

  it should "get 0 HCCs for HCC 149 if payment year is 2016" in {
    val filter = HccDataQuery(
      hccCode = "149",
      labelSetVersion = "HCCV12",
      dateOfService = "01/01/2016"
    )
    val data = HccDataManager.getHcc(filter)
    assert( data.hccs.isEmpty, "HCC length should be 0")
  }

  it should "get 0 HCCs for HCC 149 if icdSystem is ICD10" in {
    val filter = HccDataQuery(
      hccCode = "149",
      labelSetVersion = "HCCV12",
      dateOfService = "12/31/2016"
    )

    val data = HccDataManager.getHcc(filter)
    assert( data.hccs.isEmpty, "HCC length should be 0")
  }



  behavior of "search by HCC no ICD no Query - only in HCCV22"
  /**
   * HCC Code in V22 Only
   *
   * Should only work with:
   * - HCCV22
   *
   * Should work with:
   * - payment years <= 2015
   * - ICD9
   *
   * - payment years >= 2016
   * - ICD10
   */

  it should "get 58 ICDs for HCC 170 v22 ICD9 for 2015" in {
    val filter = HccDataQuery(
      hccCode = "170",
      labelSetVersion = "HCCV22",
      dateOfService = "01/01/2015"
    )

    val data = HccDataManager.getHcc(filter)
    assert(
      data.hccs(0).icds.length == 58,
      "icd count should match known count for that HCC"
    )
  }

  it should "get 721 ICDs for HCC 170 v22 ICD10 for 2016" in {
    val filter = HccDataQuery(
      hccCode = "170",
      labelSetVersion = "HCCV22",
      dateOfService = "01/31/2016"
    )

    val data = HccDataManager.getHcc(filter)
    assert(
      data.hccs(0).icds.length == 721,
      "icd count should match known count for that HCC"
    )
  }

  /*it should "get 0 HCCs for HCC 170 v22 ICD10 for 2015" in {
    // We don't appear to have mapping for ICD10 in V22 for payment year 2015
    val filter = HccDataQuery(
      hccCode = "170",
      labelSetVersion = "HCCV22",
      dateOfService = "12/31/2015",
      icdMapping = "2015-icd-hcc"
    )

    val data = HccDataManager.getHcc(filter)
    assert( data.hccs.isEmpty, "HCC length should be 0")
  }*/

  it should "get 0 HCCs for HCC 170 v12 ICD9 for 2015" in {
    val filter = HccDataQuery(
      hccCode = "170",
      labelSetVersion = "HCCV12",
      dateOfService = "01/01/2015"
    )

    val data = HccDataManager.getHcc(filter)
    assert( data.hccs.isEmpty, "HCC length should be 0")
  }

  it should "get 0 HCCs for HCC 170 v12 ICD10 for 2016" in {
    val filter = HccDataQuery(
      hccCode = "170",
      labelSetVersion = "HCCV12",
      dateOfService = "01/31/2016"
    )

    val data = HccDataManager.getHcc(filter)
    assert( data.hccs.isEmpty, "HCC length should be 0")
  }



  behavior of "search by HCC no ICD no Query - both HCCV12 and HCCV22"
  /**
   * HCC Code in V12 and v22
   *
   * Should work with everything,
   *
   * Except:
   * ICD10 in Payment year <= 2015
   * HCC12 in Payment Year >= 2016
   */

  it should "get 109 ICDs for HCC 55 v12 ICD9 for 2015" in {
    val filter = HccDataQuery(
      hccCode = "55",
      labelSetVersion = "HCCV12",
      dateOfService = "01/01/2015"
    )

    val data = HccDataManager.getHcc(filter)
    assert(
      data.hccs(0).icds.length == 109,
      "icd count should match known count for that HCC"
    )
  }

  it should "get 48 ICDs for HCC 55 v22 ICD9 for 2015" in {
    val filter = HccDataQuery(
      hccCode = "55",
      labelSetVersion = "HCCV22",
      dateOfService = "01/01/2015"
    )

    val data = HccDataManager.getHcc(filter)
    assert(
      data.hccs(0).icds.length == 48,
      "icd count should match known count for that HCC"
    )
  }

  it should "get 48 ICDs for HCC 55 v22 ICD9 for 2016" in {
    val filter = HccDataQuery(
      hccCode = "55",
      labelSetVersion = "HCCV22",
      dateOfService = "01/01/2015"
    )

    val data = HccDataManager.getHcc(filter)
    assert(
      data.hccs(0).icds.length == 48,
      "icd count should match known count for that HCC"
    )
  }

  it should "get 270 ICDs for HCC 55 v22 ICD10 for 2016" in {
    val filter = HccDataQuery(
      hccCode = "55",
      labelSetVersion = "HCCV22",
      dateOfService = "12/31/2016"
    )

    val data = HccDataManager.getHcc(filter)
    assert(
      data.hccs(0).icds.length == 271,
      "icd count should match known count for that HCC"
    )
  }

  it should "get 0 HCCs for HCC 55 v12 ICD10 for 2015" in {
    val filter = HccDataQuery(
      hccCode = "55",
      labelSetVersion = "HCCV12",
      dateOfService = "12/31/2015"
    )

    val data = HccDataManager.getHcc(filter)
    assert(data.hccs.isEmpty, "Should not get any HCCs")
  }

  /*it should "get 0 HCCs for HCC 55 v22 ICD10 for 2015" in {
    val filter = HccDataQuery(
      hccCode = "55",
      labelSetVersion = "HCCV22",
      dateOfService = "12/31/2015",
      icdMapping = "2015-icd-hcc"
    )

    val data = HccDataManager.getHcc(filter)
    assert(data.hccs.isEmpty, "Should not get any HCCs")
  }

  it should "get 0 HCCs for HCC 55 v12 ICD9 for 2016" in {
    val filter = HccDataQuery(
      hccCode = "55",
      labelSetVersion = "HCCV12",
      dateOfService = "01/01/2015",
      icdMapping = "2016-icd-hcc"
    )

    val data = HccDataManager.getHcc(filter)
    assert(data.hccs.isEmpty, "Should not get any HCCs")
  }*/

  it should "get 0 HCCs for HCC 55 v12 ICD10 for 2016" in {
    val filter = HccDataQuery(
      hccCode = "55",
      labelSetVersion = "HCCV12",
      dateOfService = "12/31/2016"
    )

    val data = HccDataManager.getHcc(filter)
    assert(data.hccs.isEmpty, "Should not get any HCCs")
  }

  behavior of "Search by HCC no ICD by Query"

  it should "get 691 ICDs for HCC 170 v22 ICD10 for 2016 filter by one word" in {
    val filter = HccDataQuery(
      hccCode = "170",
      labelSetVersion = "HCCV22",
      dateOfService = "01/01/2016",
      q = "fracture"
    )

    val data = HccDataManager.getHcc(filter)
    assert(
      data.hccs(0).icds.length == 691,
      "icd count should match known count for that HCC"
    )
  }

  it should "get 275 ICDs for HCC 170 v22 ICD10 for 2016 filter by two words" in {
    val filter = HccDataQuery(
      hccCode = "170",
      labelSetVersion = "HCCV22",
      dateOfService = "01/01/2016",
      q = "closed fracture"
    )

    val data = HccDataManager.getHcc(filter)
    assert(
      data.hccs(0).icds.length == 275,
      "icd count should match known count for that HCC"
    )
  }

  it should "get 121 ICDs for HCC 170 v22 ICD10 for 2016 filter by three words" in {
    val filter = HccDataQuery(
      hccCode = "170",
      labelSetVersion = "HCCV22",
      dateOfService = "01/01/2016",
      q = "closed fracture Unspecified"
    )

    val data = HccDataManager.getHcc(filter)
    assert(
      data.hccs(0).icds.length == 121,
      "icd count should match known count for that HCC"
    )
  }

  it should "get 1 ICD for HCC 170 v22 ICD10 for 2016 filter by four words" in {
    val filter = HccDataQuery(
      hccCode = "170",
      labelSetVersion = "HCCV22",
      dateOfService = "01/01/2016",
      q = "closed fracture Unspecified IV"
    )

    val data = HccDataManager.getHcc(filter)
    assert(
      data.hccs(0).icds.length == 1,
      "icd count should match known count for that HCC"
    )
  }

  it should "get 0 HCCs for HCC 170 v22 ICD10 for 2016 filter by non existing word" in {
    val filter = HccDataQuery(
      hccCode = "170",
      labelSetVersion = "HCCV22",
      dateOfService = "01/01/2016",
      q = "Apixio"
    )

    val data = HccDataManager.getHcc(filter)
    assert( data.hccs.isEmpty, "HCC length should be 0")
  }


  behavior of "Search by HCC by ICD no Query"

  it should "get 721 ICDs for HCC 170 v22 ICD10 for 2016 filter by ICD Code S79149A" in {
    val filter = HccDataQuery(
      hccCode = "170",
      icdCode = "S79149A",
      labelSetVersion = "HCCV22",
      dateOfService = "01/01/2016"
    )

    val data = HccDataManager.getHcc(filter)
    assert(
      data.hccs(0).icds.length == 721,
      "icd count should match known count for that HCC"
    )
    assert(data.hccs(0).icds.find(e => e.displayName.contains("Salter-Harris Type IV physeal fracture of " +
      "lower end of unspecified femur, initial encounter for closed fracture")).nonEmpty,
      "icd description does not match"
    )
  }

  behavior of "Search by HCC by ICD by Query"

  it should "get 1 ICD for HCC 170 v22 ICD10 for 2016 filter by ICD Code S79149A and full description" in {
    val filter = HccDataQuery(
      hccCode = "170",
      icdCode = "S79149A",
      labelSetVersion = "HCCV22",
      dateOfService = "01/01/2016",
      q = "Salter-Harris Type IV physeal fracture of lower end of unspecified femur, initial encounter for closed fracture"
    )

    val data = HccDataManager.getHcc(filter)
    assert(
      data.hccs(0).icds.length == 1,
      "icd count should match known count for that HCC"
    )
    assert(
      data.hccs(0).icds(0).displayName
        .contains("Salter-Harris Type IV physeal fracture of lower end of unspecified femur, initial encounter for closed fracture"),
      "icd description does not match"
    )
  }

  behavior of "Search no HCC no ICD no Query"

  it should "get all HCCs for v12 ICD9 for 2015 " in {
    val filter = HccDataQuery(
      dateOfService = "01/01/2015",
      labelSetVersion = "HCCV12"
    )

    val data = HccDataManager.getHcc(filter)
    assert(data.hccs.length == 70, "too few HCC were returned")
  }

  it should "get all HCCs for v22 ICD9 for 2015 " in {
    val filter = HccDataQuery(
      dateOfService = "01/01/2015",
      labelSetVersion = "HCCV22"
    )

    val data = HccDataManager.getHcc(filter)
    assert(data.hccs.length == 79, "too few HCC were returned")
  }

  it should "get all HCCs for v22 ICD9 for 2016 " in {
    val filter = HccDataQuery(
      dateOfService = "01/01/2015",
      labelSetVersion = "HCCV22"
    )

    val data = HccDataManager.getHcc(filter)
    assert(data.hccs.length == 79, "too few HCC were returned")
  }

  it should "get all HCCs for v22 ICD10 for 2016" in {
    val filter = HccDataQuery(
      dateOfService = "01/01/2016",
      labelSetVersion = "HCCV22"
    )

    val data = HccDataManager.getHcc(filter)
    assert(data.hccs.length == 79, "too few HCC were returned")
  }

  it should "get no HCCs for v12 ICD10 for 2015" in {
    val filter = HccDataQuery(
      dateOfService = "12/31/2015",
      labelSetVersion = "HCCV12"
    )

    val data = HccDataManager.getHcc(filter)
    assert(data.hccs.isEmpty, "too many HCC were returned")
  }

  /*it should "get no HCCs for v22 ICD10 for 2015" in {
    val filter = HccDataQuery(
      dateOfService = "12/31/2015",
      labelSetVersion = "HCCV22",
      icdMapping = "2015-icd-hcc"
    )

    val data = HccDataManager.getHcc(filter)
    assert(data.hccs.isEmpty, "too mamy HCC were returned")
  }

  it should "get no HCCs for v12 ICD9 for 2016" in {
    val filter = HccDataQuery(
      dateOfService = "01/01/2015",
      labelSetVersion = "HCCV12",
      icdMapping = "2016-icd-hcc"
    )

    val data = HccDataManager.getHcc(filter)
    assert(data.hccs.isEmpty, "too mamy HCC were returned")
  }*/

  it should "get no HCCs for v12 ICD10 for 2016" in {
    val filter = HccDataQuery(
      dateOfService = "01/01/2016",
      labelSetVersion = "HCCV12"
    )

    val data = HccDataManager.getHcc(filter)
    assert(data.hccs.isEmpty, "too mamy HCC were returned")
  }


  behavior of "Search no HCC no ICD by Query"

  it should "get 3 HCC with 10 ICDs for v22 ICD10 for 2016 with E1152 in query" in {
    val filter = HccDataQuery(
      dateOfService = "01/01/2016",
      labelSetVersion = "HCCV22",
      q = "E1152"
    )

    val data = HccDataManager.getHcc(filter)
    assert(data.hccs.length == 3, "too few HCC were returned")
  }

  it should "get 2 HCC with 10 ICDs for v22 ICD10 for 2016 with K50812 in query" in {
    val filter = HccDataQuery(
      dateOfService = "01/01/2016",
      labelSetVersion = "HCCV22",
      q = "K50812"
    )

    val data = HccDataManager.getHcc(filter)
    assert(data.hccs.length == 2, "too few HCC were returned")
  }

  it should "get 1 HCC with 10 ICDs for v22 ICD10 for 2016 with S79109A in query" in {
    val filter = HccDataQuery(
      dateOfService = "01/01/2016",
      labelSetVersion = "HCCV22",
      q = "S79109A"
    )

    val data = HccDataManager.getHcc(filter)
    assert(data.hccs.length == 1, "too few HCC were returned")
  }

  it should "get 0 HCC with 10 ICDs for v22 ICD10 for 2016 with code from v12 ICD9 2015" in {
    val filter = HccDataQuery(
      dateOfService = "01/01/2016",
      labelSetVersion = "HCCV22",
      q = "20297"
    )

    val data = HccDataManager.getHcc(filter)
    assert(data.hccs.isEmpty, "too many HCC were returned")
  }

  it should "get 0 HCC with 10 ICDs for v22 ICD10 for 2016 with invalid query" in {
    val filter = HccDataQuery(
      dateOfService = "01/01/2016",
      labelSetVersion = "HCCV22",
      q = "S79109Aasdf"
    )

    val data = HccDataManager.getHcc(filter)
    assert(data.hccs.isEmpty, "too few HCC were returned")
  }


  behavior of "Search no HCC by ICD no Query"

  it should "get some HCCs by ICD code" in {
    val filter = HccDataQuery(
      icdCode = "F10120",
      dateOfService = "01/01/2016",
      labelSetVersion = "HCCV22"
    )

    val data = HccDataManager.getHcc(filter)
    assert(data.hccs.nonEmpty, "too few HCC were returned")
  }

  it should "get 3 HCCs and 1 ICD by ICD code E1052" in {

    val filter = HccDataQuery(
      icdCode = "E1052",
      dateOfService = "01/01/2016",
      labelSetVersion = "HCCV22"
    )

    val data = HccDataManager.getHcc(filter)
    assert(data.hccs.length == 3, "too few HCC were returned")
    data.hccs(0).icds.length == 1
    data.hccs(1).icds.length == 1
    data.hccs(2).icds.length == 1
  }

  it should "get 0 HCCs by invalid ICD code" in {

    val filter = HccDataQuery(
      icdCode = "E1052asdf",
      dateOfService = "01/01/2016",
      labelSetVersion = "HCCV22"
    )

    val data = HccDataManager.getHcc(filter)
    assert(data.hccs.isEmpty, "too few HCC were returned")
  }

  behavior of "Search no HCC by ICD by Query"

  it should "get 3 HCCs and 1 ICD by ICD code E1052 with full description query" in {

    val filter = HccDataQuery(
      icdCode = "E1052",
      dateOfService = "01/01/2016",
      labelSetVersion = "HCCV22",
      q = "Type 1 diabetes mellitus with diabetic peripheral angiopathy with gangrene"
    )

    val data = HccDataManager.getHcc(filter)
    println(data)
    assert(data.hccs.length == 3, "too few HCC were returned")
    data.hccs(0).icds.length == 1
    data.hccs(1).icds.length == 1
    data.hccs(2).icds.length == 1
  }

  behavior of "Manual Coding"

  it should "should return all hccs for 2017+18-icd-all mapping" in {
    val filter = HccDataQuery(
      hccCode = "HCCV22",
      labelSetVersion = "2.16.840.1.113883.6",
      dateOfService = "11/01/2015",
      q="ion",
      paymentYear = "2016",
      icdMapping = "2017+18-icd-all",
      apxcatMapping=""
    )
    val data = HccDataManager.getHcc(filter)
    assert(data.hccs.nonEmpty, "no hccs returned")
  }

  behavior of "Data science mapping 2015+18-ml-icd-hcc"

  it should "get HCC55 for V23 for year 2015" in {
    val filter = HccDataQuery(
      hccCode = "55",
      labelSetVersion = "HCCV23",
      dateOfService = "01/01/2015",
      icdMapping = "2015+18-ml-icd-hcc"
    )
    val data = HccDataManager.getHcc(filter)
    assert(data.hccs.nonEmpty, "no hccss found")
    assert(data.hccs(0).icds.nonEmpty, "no icds found")
  }
}
