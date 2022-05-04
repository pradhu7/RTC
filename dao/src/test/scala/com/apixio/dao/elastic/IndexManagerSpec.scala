package com.apixio.dao.elastic

import com.apixio.model.profiler.WorkItem
import com.apixio.model.utility.{ApixioDateSerializer,ApixioDateDeserialzer}
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.ScalaObjectMapper
import com.sksamuel.elastic4s.ElasticClient
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.words.ShouldVerb
import org.scalatest.junit.JUnitRunner
import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class IndexManagerSpec extends FlatSpec with Matchers {

  var esclient : ElasticClient = null
  implicit var imgr : IndexManager = null
  var wikey : String = null
  val data = """{"comment":"","code":{"c":"84","s":"HCCV22"},"patient":"26485c04-71d9-40e0-b101-2ccc8c1d1959","users":[],"findings":[{"endDos":"2015-12-31T00:00:00.000Z","predictedDos":"2015-12-12T17:00:47.000Z","code":{"c":"84","s":"HCCV22"},"tags":["dict","plainText","pt"],"startDos":"2015-01-01T00:00:00.000Z","annotations":[],"state":"routable","score":0.9131662959833484,"ineligible":0,"document":"1f82f9a4-134b-44bb-b399-86d790feaf4e","pages":[1]}],"bundle":"00000000-0000-0000-0000-000000000001","project":"PRHCC_4351fd23-2529-4962-a927-68ed8242111b","state":"routable","scores":[0.3928413820064426,0.0,0.0],"pass":1,"phase":""}"""

  implicit val mapper = {
    val m = new ObjectMapper() with ScalaObjectMapper
    m.registerModule(DefaultScalaModule)
    val dateModule = new SimpleModule("DateTimeDeserializerModule")
    dateModule.addDeserializer(classOf[DateTime], new ApixioDateDeserialzer)
    dateModule.addSerializer(classOf[DateTime], new ApixioDateSerializer)
    m.registerModule(dateModule)
    m.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    m.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
    m
  }
  MappingSource.setMapper(mapper)

  "IndexManager" should "be able to setup a connection to a local elastic search" in {
    val config = Map[String,Object]("hosts" -> "localhost:9300", "cluster" -> "Local").asJava
    esclient = IndexManager.configureConnector(config)
    esclient should not be (null)
  }

  it should "should be able to correctly create a new index" in {
    val index = java.util.UUID.randomUUID.toString
    println(s"Making index ${index}")
    imgr = new IndexManager(
      name = index,
      backingStore = s"${index}-1",
      client = esclient,
      indexableTypes = Array(WorkItemDocument)
    )
    imgr.indexExists should be (false)
    imgr.createTypedIndex()
    imgr.indexExists should be (true)
  }

  it should "should be able to correctly store a basic workitem" in {
    val wi = WorkItem.fromJson(data)
    wikey = wi.key
    wikey should not be (null)
    imgr.indexThis(WorkItemDocument(wi))
  }

  it should "should be able to correctly retrieve a basic workitem" in {
    val wi = WorkItemDocument.getEntity(wikey)
    wi should not be (null)
    wi should equal (WorkItem.fromJson(data))
  }
}

