package com.apixio.model.profiler

import com.apixio.model.event.transformer.EventTypeJSONParser
import com.apixio.model.event.transformer
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest._
import matchers.should._

@RunWith(classOf[JUnitRunner])
class EventTypeXSpec extends AnyFlatSpec with Matchers {

  val parser = new EventTypeJSONParser()

  "EventTypeX" should "correctly be able to be converted" in {
    val raw = """{"attributes":{"bucketType":"maxentModel.v0.0.5a","$jobId":"467175","$patientUUID":"46d1d16d-8ed4-4086-b575-1fc3e218a6bf","$orgId":"509","sourceType":"NARRATIVE","$documentId":"source_system_value2c7a1e98-c5fa-11e5-ba44-0249c618e36b","$documentUUID":"9edbc22b-daa7-4716-93b0-bd813ae78435","editType":"ACTIVE","editTimestamp":"2016-01-28T20:04:15.974Z","$workId":"465368","bucketName":"maxentModel.v0.0.5a.ex6d_big_idf_LDADateExtract20150820_newf2f_apxcatv22","totalPages":"5","$batchId":"509_28012016200315","$propertyVersion":"1453313565958"},"source":{"type":"document","uri":"9edbc22b-daa7-4716-93b0-bd813ae78435"},"evidence":{"source":{"type":"v5apxcat2","uri":"maxentv5Model"},"inferred":true,"attributes":{"appliedDateExtraction":"false","mentionDictionary":"apxcat_dict_20160119.txt","hasMention":"YES","spellChecking":"false","title":"5PageMPdf.PDF","face2face":"false","junkiness":"0.9843205574912892","patternsFile":"negation_triggers_20140211.txt","associatedEncounter":"Apixio:UNKNONW_ENCOUNTER","snippet":"heart failure","pageNumber":"4","mentions":"heart failure","highConfidence":"true","face2faceConfidence":"0.9885302493420721","extractionType":"ocrText","predictionConfidence":"0.9525277947847174"}},"fact":{"code":{"codeSystemVersion":"2.0","code":"V22_85","displayName":"Congestive Heart Failure","codeSystem":"APXCAT"},"time":{"endTime":"2015-12-12T17:00:47+0000","startTime":"2015-12-12T17:00:47+0000"}},"subject":{"type":"patient","uri":"46d1d16d-8ed4-4086-b575-1fc3e218a6bf"}}"""
    EventTypeX(parser.parseEventTypeData(raw)) should not be (null)
  }

  it should "correctly be able to extract tags" in {
    val raw = """{"attributes":{"bucketType":"maxentModel.v0.0.5a","$jobId":"467175","$patientUUID":"46d1d16d-8ed4-4086-b575-1fc3e218a6bf","$orgId":"509","sourceType":"NARRATIVE","$documentId":"source_system_value2c7a1e98-c5fa-11e5-ba44-0249c618e36b","$documentUUID":"9edbc22b-daa7-4716-93b0-bd813ae78435","editType":"ACTIVE","editTimestamp":"2016-01-28T20:04:15.974Z","$workId":"465368","bucketName":"maxentModel.v0.0.5a.ex6d_big_idf_LDADateExtract20150820_newf2f_apxcatv22","totalPages":"5","$batchId":"509_28012016200315","$propertyVersion":"1453313565958"},"source":{"type":"document","uri":"9edbc22b-daa7-4716-93b0-bd813ae78435"},"evidence":{"source":{"type":"v5apxcat2","uri":"maxentv5Model"},"inferred":true,"attributes":{"appliedDateExtraction":"false","mentionDictionary":"apxcat_dict_20160119.txt","hasMention":"YES","spellChecking":"false","title":"5PageMPdf.PDF","face2face":"false","junkiness":"0.9843205574912892","patternsFile":"negation_triggers_20140211.txt","associatedEncounter":"Apixio:UNKNONW_ENCOUNTER","snippet":"heart failure","pageNumber":"4","mentions":"heart failure","highConfidence":"true","face2faceConfidence":"0.9885302493420721","extractionType":"ocrText","predictionConfidence":"0.9525277947847174"}},"fact":{"code":{"codeSystemVersion":"2.0","code":"V22_85","displayName":"Congestive Heart Failure","codeSystem":"APXCAT"},"time":{"endTime":"2015-12-12T17:00:47+0000","startTime":"2015-12-12T17:00:47+0000"}},"subject":{"type":"patient","uri":"46d1d16d-8ed4-4086-b575-1fc3e218a6bf"}}"""
    EventTypeX(parser.parseEventTypeData(raw)).extractTags should equal (List("mention", "v5", "ocrText"))
  }
}
