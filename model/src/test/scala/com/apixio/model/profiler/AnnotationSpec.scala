package com.apixio.model.profiler

import com.apixio.model.event.transformer.EventTypeListJSONParser
import com.apixio.model.utils.TestUtils
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.ScalaObjectMapper
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest._
import matchers.should._

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class AnnotationSpec extends AnyFlatSpec with Matchers {

  implicit val mapper = {
    val m = new ObjectMapper() with ScalaObjectMapper
    m.registerModule(DefaultScalaModule)
    m.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    m.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
    m
  }

  CodeMapping.init(TestUtils.getCodeMappingIfNotExist(TestUtils.CODE_MAPPING))

  "Annotation" should "objects should correctly be created from EventTypes" in {
    val raw = """{"eventTypes":[{"attributes":{"$patientUUID":"da055ae0-3453-4ef3-b127-e059f6f2f677","sourceType":"USER_ANNOTATION","$kafka":"events:0:1805661:1464119749089","HCC_APP_VERSION":"5.8.3","SOURCE_TYPE":"USER_ANNOTATION","$documentUUID":"3607b22b-1580-4323-9796-3daa420170c7"},"source":{"type":"document","uri":"3607b22b-1580-4323-9796-3daa420170c7"},"evidence":{"source":{"type":"user","uri":"nbrown@apixio.com"},"inferred":false,"attributes":{"comment":"Nick B","presentedHcc":"104","code":"","sweep":"finalReconciliation","hccDescription":"","presentedHccModelPaymentYear":"2016","presentedTag":"qa1","physicianLastName":"","dateOfService":"07/20/2015","encounterType":"","physicianFirstName":"","codingOrganization":"UO_6a072f0c-c5af-44a4-a521-cb3583081b12","codeSystem":"","hcc":"104","pageNumber":"","presentedHccSweep":"finalReconciliation","hccPaymentYear":"2016","reviewFlag":"true","modelPaymentYear":"2016","timestamp":"2016-05-24T19:55:38.841113Z","presentedState":"accepted","presentedHccDescription":"","presentedHccLabelSetVersion":"V22","presentedHccMappingVersion":"2016 finalReconciliation","codeSystemVersion":"","NPI":"","physicianMiddleName":"","suggestedPages":"","project":"CP_730e3b40-4408-4f54-84fd-7e264864e6a5","organization":"10000377","transactionId":""}},"fact":{"code":{"codeSystemVersion":"","code":"34290","displayName":"","codeSystem":"2.16.840.1.113883.6.103"},"values":{"rejectReason":"Additional documentation required to accept this HCC","result":"reject"},"time":{"endTime":"2015-07-20T00:00:00+0000","startTime":"2015-07-20T00:00:00+0000"}},"subject":{"type":"patient","uri":"da055ae0-3453-4ef3-b127-e059f6f2f677"}},{"attributes":{"$patientUUID":"da055ae0-3453-4ef3-b127-e059f6f2f677","sourceType":"USER_ANNOTATION","$kafka":"events:0:1805661:1464119749089","HCC_APP_VERSION":"5.8.3","SOURCE_TYPE":"USER_ANNOTATION","$documentUUID":"3607b22b-1580-4323-9796-3daa420170c7"},"source":{"type":"document","uri":"3607b22b-1580-4323-9796-3daa420170c7"},"evidence":{"source":{"type":"user","uri":"nbrown@apixio.com"},"inferred":false,"attributes":{"comment":"Nick B","presentedHcc":"104","code":"","sweep":"finalReconciliation","hccDescription":"","presentedHccModelPaymentYear":"2016","presentedTag":"qa1","physicianLastName":"","dateOfService":"07/20/2015","encounterType":"","physicianFirstName":"","codingOrganization":"UO_6a072f0c-c5af-44a4-a521-cb3583081b12","codeSystem":"","hcc":"104","pageNumber":"","presentedHccSweep":"finalReconciliation","hccPaymentYear":"2016","reviewFlag":"true","modelPaymentYear":"2016","timestamp":"2016-05-24T19:55:38.841113Z","presentedState":"accepted","presentedHccDescription":"","presentedHccLabelSetVersion":"V22","presentedHccMappingVersion":"2016 finalReconciliation","codeSystemVersion":"","NPI":"","physicianMiddleName":"","suggestedPages":"","project":"CP_730e3b40-4408-4f54-84fd-7e264864e6a5","organization":"10000377","transactionId":""}},"fact":{"code":{"codeSystemVersion":"","code":"34290","displayName":"","codeSystem":"2.16.840.1.113883.6.103"},"values":{"rejectReason":"Additional documentation required to accept this HCC","result":"reject"},"time":{"endTime":"2015-07-20T00:00:00+0000","startTime":"2015-07-20T00:00:00+0000"}},"subject":{"type":"patient","uri":"da055ae0-3453-4ef3-b127-e059f6f2f677"}}]}"""
    val events = (new EventTypeListJSONParser()).parseEventsData(raw).asScala.map(EventTypeX(_)).toList.map(Annotation(_))
    events.flatMap(_.code.toHcc("2016-icd-hcc")).toSet should be (Set(Code("103", Code.HCCV22)))
  }

  it should "handle the new annotations accepts correctly" in {
    val raw = """{"eventTypes":[{"subject":{"uri":"0e418e35-e6fa-4269-a3fa-115171a0c2ea","type":"patient"},"fact":{"code":{"code":"88","codeSystem":"HCCV22","codeSystemVersion":"","displayName":"Angina pectoris with documented spasm"},"time":{"startTime":"2015-12-14T16:00:00-0800","endTime":"2015-12-14T16:00:00-0800"},"values":{"rejectReason":"","result":"accept"}},"source":{"uri":"c368743c-d091-4713-8443-0a4af8924f5e","type":"document"},"evidence":{"inferred":false,"source":{"uri":"smt_code@apixio.net","type":"user"},"attributes":{"comment":"asdf","physicianLastName":"","timestamp":"2016-07-11T03:51:06.891926Z","presentedState":"routable","acceptedCodeSystem":"2.16.840.1.113883.6.90","acceptedCode":"I201","project":"PRHCC_ef9a2a55-47c7-4228-a8e6-efbed83c31d4","encounterType":"Physician Face-to-Face Encounter","NPI":"","physicianFirstName":"John","codingOrganization":"UO_1ffeae33-62d2-4fc5-b9bd-351325c43e0e","suggestedPages":"[74, 88, 99, 105, 111, 112, 117]","reviewFlag":"true","pageNumber":"1","presentedPhase":"code","physicianMiddleName":"","transactionId":"PRHCC_ef9a2a55-47c7-4228-a8e6-efbed83c31d4,0e418e35-e6fa-4269-a3fa-115171a0c2ea,88-HCCV22,1,code"}},"attributes":{"HCC_APP_VERSION":"5.8.3","SOURCE_TYPE":"USER_ANNOTATION","$patientUUID":"0e418e35-e6fa-4269-a3fa-115171a0c2ea","$documentUUID":"c368743c-d091-4713-8443-0a4af8924f5e","sourceType":"USER_ANNOTATION"}}]}"""
    val events = (new EventTypeListJSONParser()).parseEventsData(raw).asScala.map(EventTypeX(_)).toList
    val annot = Annotation(events(0))
    annot.code should be (Code("I201", "2.16.840.1.113883.6.90"))
  }

  it should "have the correct temporary fixups from EventTypes" in {
    val raw = """{"eventTypes":[{"attributes":{"$patientUUID":"f4deb54f-aaef-4723-bbb9-f5015dbd833c","sourceType":"USER_ANNOTATION","$kafka":"events:0:2015283:1467942247219","HCC_APP_VERSION":"5.12.0","SOURCE_TYPE":"USER_ANNOTATION","$documentUUID":"84c10efb-9314-49d7-8106-7fd7ee117009"},"source":{"type":"document","uri":"84c10efb-9314-49d7-8106-7fd7ee117009"},"evidence":{"source":{"type":"user","uri":"atorres@apexcodemine.com"},"inferred":false,"attributes":{"comment":"consultation letter, DOS not documented","presentedCode":"96","encounterType":"","NPI":"","physicianFirstName":"","project":"PRHCC_f2ea34f8-295d-4bc9-8f3f-3b0160b729cd","physicianLastName":"","timestamp":"2016-07-08T01:44:05.910474Z","presentedState":"accepted","suggestedPages":"[6, 34]","reviewFlag":"true","presentedCodeSystem":"HCCV22","pageNumber":"","presentedPhase":"qa1","physicianMiddleName":"","transactionId":"PRHCC_f2ea34f8-295d-4bc9-8f3f-3b0160b729cd,f4deb54f-aaef-4723-bbb9-f5015dbd833c,96-HCCV22,1,qa1","codingOrganization":"UO_ba0e1b58-2e51-420f-b0a4-6bd8b518a8a9"}},"fact":{"code":{"codeSystemVersion":"","code":"I4891","displayName":"","codeSystem":""},"values":{"rejectReason":"Invalid document type","result":"reject"},"time":{"endTime":"2015-12-22T00:00:00+0000","startTime":"2015-12-22T00:00:00+0000"}},"subject":{"type":"patient","uri":"f4deb54f-aaef-4723-bbb9-f5015dbd833c"}},{"attributes":{"$patientUUID":"f4deb54f-aaef-4723-bbb9-f5015dbd833c","sourceType":"USER_ANNOTATION","$kafka":"events:0:2029328:1468066222601","HCC_APP_VERSION":"5.12.2","SOURCE_TYPE":"USER_ANNOTATION","$documentUUID":"6a6a224f-949c-4250-8a29-9d0eb2e63b85"},"source":{"type":"document","uri":"6a6a224f-949c-4250-8a29-9d0eb2e63b85"},"evidence":{"source":{"type":"user","uri":"karl.b@aeaallc.com"},"inferred":false,"attributes":{"comment":"","presentedCode":"96","encounterType":"Physician Face-to-Face Encounter","NPI":"1114944832","physicianFirstName":"ANNE BIEDEL, M.D.","project":"PRHCC_f2ea34f8-295d-4bc9-8f3f-3b0160b729cd","physicianLastName":"","timestamp":"2016-07-09T12:10:19.271774Z","presentedState":"routable","suggestedPages":"[7, 21, 22, 33]","reviewFlag":"false","presentedCodeSystem":"HCCV22","pageNumber":"7","presentedPhase":"code","physicianMiddleName":"","transactionId":"PRHCC_f2ea34f8-295d-4bc9-8f3f-3b0160b729cd,f4deb54f-aaef-4723-bbb9-f5015dbd833c,96-HCCV22,1,code","codingOrganization":"UO_0df8c4c5-07f9-4a3f-b767-7dc40af1b587"}},"fact":{"code":{"codeSystemVersion":"","code":"I4891","displayName":"Unspecified atrial fibrillation","codeSystem":"2.16.840.1.113883.6.90"},"values":{"rejectReason":"","result":"accept"},"time":{"endTime":"2015-12-21T00:00:00+0000","startTime":"2015-12-21T00:00:00+0000"}},"subject":{"type":"patient","uri":"f4deb54f-aaef-4723-bbb9-f5015dbd833c"}}]}"""
    val events = (new EventTypeListJSONParser()).parseEventsData(raw).asScala.map(EventTypeX(_)).toList
    val annots = events.map(Annotation(_))

    events.foreach { e =>
      e.evidence.attributes should not contain key ("presentedCode")
      e.evidence.attributes should not contain key ("presentedCodeSystem")
      e.fact.code.isHcc should be (true)
    }
    events(1).evidence.attributes should contain key ("acceptedCode")
    events(1).evidence.attributes should contain key ("acceptedCodeSystem")
    annots(0).code.isHcc should be (true)
    annots(1).code.isIcd should be (true)
  }

  it should "correctly handle annotations with space seperated pages" in {
    val raw = """{"eventTypes":[{"attributes":{"$patientUUID":"d0534cd1-15db-4c0e-acac-149a7338425a","sourceType":"USER_ANNOTATION","$kafka":"events:0:1457356:1454121493188","HCC_APP_VERSION":"5.6.1","SOURCE_TYPE":"USER_ANNOTATION","$documentUUID":"d7a09b8a-a911-41f1-9aa5-1812a824fe90"},"source":{"type":"document","uri":"d7a09b8a-a911-41f1-9aa5-1812a824fe90"},"evidence":{"source":{"type":"user","uri":"marites.e@aeaallc.com"},"inferred":false,"attributes":{"comment":"","presentedHcc":"108","code":"4439","sweep":"midYear","hccDescription":"Vascular Disease","presentedHccModelPaymentYear":"2016","presentedTag":"","physicianLastName":"","dateOfService":"09/17/2015","encounterType":"Physician Face-to-Face Encounter","physicianFirstName":"Nguyen, Joseph DO","codingOrganization":"UO_0df8c4c5-07f9-4a3f-b767-7dc40af1b587","codeSystem":"","hcc":"108","pageNumber":"3 6","presentedHccSweep":"midYear","hccPaymentYear":"2016","reviewFlag":"false","modelPaymentYear":"2016","timestamp":"2016-01-30T02:38:12.668046Z","presentedState":"routable","presentedHccDescription":"Vascular Disease","presentedHccLabelSetVersion":"V22","presentedHccMappingVersion":"2016 midYear","codeSystemVersion":"","NPI":"","physicianMiddleName":"","suggestedPages":"6","project":"CP_3bc0ae0e-55a7-4e9f-a32c-6dfb1a687d6d","organization":"10000278","transactionId":"cae885e1-40c8-49b9-83fe-4d4a187a5b06"}},"fact":{"code":{"codeSystemVersion":"","code":"4439","displayName":"Peripheral vascular disease, unspecified","codeSystem":"2.16.840.1.113883.6.103"},"values":{"rejectReason":"","result":"accept"},"time":{"endTime":"2015-09-17T00:00:00+0000","startTime":"2015-09-17T00:00:00+0000"}},"subject":{"type":"patient","uri":"d0534cd1-15db-4c0e-acac-149a7338425a"}}]}"""
    val events = (new EventTypeListJSONParser()).parseEventsData(raw).asScala.map(x => EventTypeX(x)).toList
    val annot = Annotation(events.head)
    annot.pages should equal (List(3, 6))
  }
}
