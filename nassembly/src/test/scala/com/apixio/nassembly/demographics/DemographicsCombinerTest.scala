package com.apixio.nassembly.demographics

import com.apixio.datacatalog.DemographicsInfoProto.DemographicsInfo
import com.apixio.datacatalog._
import com.apixio.nassembly.apo.APOGenerator
import com.apixio.util.nassembly.DataCatalogProtoUtils
import com.google.protobuf.GeneratedMessageV3
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should._
import org.scalatestplus.junit.JUnitRunner

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class DemographicsCombinerTest extends AnyFlatSpec with Matchers {


  "Demographics" should "Merge demographics" in {
    val demographicsInfo1 = DemographicsInfo.newBuilder()
      .setName(NameOuterClass.Name.newBuilder().addGivenNames("").addFamilyNames("").addFamilyNames("").build())
      .build()

    val demographicsInfo2 = DemographicsInfo.newBuilder()
      .setName(NameOuterClass.Name.newBuilder().addGivenNames("John").addFamilyNames("Smith").build())
      .setDob(DataCatalogProtoUtils.fromDateTime(DateTime.parse("1966-04-04T04:20:00.000Z")))
      .setGender(GenderOuterClass.Gender.MALE)
      .build()

    val demoSummary1 = SummaryObjects.DemographicsSummary.newBuilder().setDemographicsInfo(demographicsInfo1).build()
    val demoSummary2 = SummaryObjects.DemographicsSummary.newBuilder().setDemographicsInfo(demographicsInfo2).build()

    val skinnyPatient = DemographicsUtils.wrapAsSkinnyPatient(List(demoSummary1, demoSummary2), List()).get


    assert(skinnyPatient.getAlternateDemographicsCount == 1) // filtered out bad data
    assert(skinnyPatient.getPrimaryDemographics.getDemographicsInfo.getName.getGivenNames(0) == "John")
    assert(skinnyPatient.getPrimaryDemographics.getDemographicsInfo.getName.getFamilyNames(0) == "Smith")


    val apo = APOGenerator.mergeSkinny(List(skinnyPatient.asInstanceOf[GeneratedMessageV3]).asJava)
    assert(apo.getPrimaryDemographics.getDateOfBirth.getYear == 1966)
  }

  "Demographics" should "Merge demographics with a new Last Name" in {
    val demographicsInfo1 = DemographicsInfo.newBuilder()
      .setName(NameOuterClass.Name.newBuilder().addGivenNames("Beth").addFamilyNames("Smith").build())
      .build()

    val source1 = SourceOuterClass.Source.newBuilder().setCreationDate(1L).build()
    val source2 = SourceOuterClass.Source.newBuilder().setCreationDate(1L).build()

    val parsingDetails1 = ParsingDetailsOuterClass.ParsingDetails.newBuilder().setParsingDate(1L).build()
    val parsingDetails2 = ParsingDetailsOuterClass.ParsingDetails.newBuilder().setParsingDate(2L).build()

    val demographicsInfo2 = DemographicsInfo.newBuilder()
      .setName(NameOuterClass.Name.newBuilder().addGivenNames("Beth").addFamilyNames("Remarried").build())
      .setDob(DataCatalogProtoUtils.fromDateTime(DateTime.parse("1966-04-04T04:20:00.000Z")))
      .setGender(GenderOuterClass.Gender.MALE)
      .build()

    val base1 = SummaryObjects.BaseSummary
      .newBuilder()
      .addSources(source1)
      .addParsingDetails(parsingDetails1)
      .build()

    val demoSummary1 = SummaryObjects.DemographicsSummary
      .newBuilder()
      .setDemographicsInfo(demographicsInfo1)
      .setBase(base1)
      .build()

    val base2 = SummaryObjects.BaseSummary
      .newBuilder()
      .addSources(source2)
      .addParsingDetails(parsingDetails2)
      .build()

    val demoSummary2 = SummaryObjects.DemographicsSummary
      .newBuilder()
      .setDemographicsInfo(demographicsInfo2)
      .setBase(base2)
      .build()

    val skinnyPatient = DemographicsUtils.wrapAsSkinnyPatient(List(demoSummary1, demoSummary2), List()).get

    assert(skinnyPatient.getAlternateDemographicsCount == 2)
    assert(skinnyPatient.getPrimaryDemographics.getDemographicsInfo.getName.getGivenNames(0) == "Beth")
    assert(skinnyPatient.getPrimaryDemographics.getDemographicsInfo.getName.getFamilyNames(0) == "Remarried")

    val apo = APOGenerator.mergeSkinny(List(skinnyPatient.asInstanceOf[GeneratedMessageV3]).asJava)
    assert(apo.getPrimaryDemographics.getDateOfBirth.getYear == 1966)
  }

}