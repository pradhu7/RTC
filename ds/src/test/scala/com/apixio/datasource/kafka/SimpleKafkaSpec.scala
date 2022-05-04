package com.apixio.datasource.kafka

import annotation.tailrec
import com.apixio.model.event.EventType
import com.apixio.model.event.transformer.EventTypeJSONParser
import java.util.Properties
import java.util.TimeZone

import kafka.zk.KafkaZkClient
import org.apache.kafka.common.utils.Time
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.funspec.AnyFunSpec

@RunWith(classOf[JUnitRunner])
class SimpleKafkaSpec extends AnyFunSpec {
  val consumer_config = new Properties()
  consumer_config.put("group.id", "KafkaSpec_Datasource_test")
  consumer_config.put("zookeeper.connect", "localhost:2181")
  consumer_config.put("auto.commit.interval.ms", "50")
  consumer_config.put("auto.offset.reset", "smallest")
  consumer_config.put("consumer.timeout.ms", "100")

  val producer_config = new Properties()
  producer_config.put("metadata.broker.list", "localhost:9092")

  //Fix up timezone issues for serialization
  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

  private def getZKClient(zkUrl: String): KafkaZkClient = {
    KafkaZkClient(zkUrl, false, 30000, 30000, 1000, Time.SYSTEM)
  }

  private def delGroup(zk: String, groupId: String) =
    if (getZKClient(zk).pathExists(s"/consumers/${groupId}")) {
      getZKClient(zk).deletePath(s"/consumers/${groupId}");
    }

  @tailrec
  private def readUntilTimeOrMsg[T](c: SimpleKafkaConsumer[String,T], t: Long) : Option[T] =
    c.getMessage match {
      case Some(x) => Some(x)
      case None if (System.currentTimeMillis > t) => None
      case None => readUntilTimeOrMsg[T](c, t)
    }

  describe("Datasource's Kafka integration") {
    it("should correctly send and receive a string") {
      delGroup(consumer_config.getProperty("zookeeper.connect"), consumer_config.getProperty("group.id"))
      val now = System.currentTimeMillis.toString
      val topic = s"test_${now}"
      val msg = s"Hello World @${now}"
      val c = new SimpleKafkaStringConsumer(consumer_config, topic)
      val p = new SimpleKafkaStringProducer(producer_config)
      try {
        p.send(topic, msg)
        assert(readUntilTimeOrMsg[String](c, System.currentTimeMillis + 3000) == Some(msg))
      } finally {
        c.shutdown
        p.close
      }
    }

    it("should correctly send and receive an EventType") {
      delGroup(consumer_config.getProperty("zookeeper.connect"), consumer_config.getProperty("group.id"))
      val parser = new EventTypeJSONParser()
      val raw_event : String = """{"subject":{"uri":"8daac322-9f7d-4016-9c06-ef58ea2ef75e","type":"patient"},"fact":{"code":{"code":"10","codeSystem":"HCC2013PYFinal","displayName":"10"},"time":{"startTime":"2013-01-01T08:00:00+0000","endTime":"2013-01-01T08:00:00+0000"},"values":{"problemName":"N/A","originalId":"null:A_H2949_20140510_201406_532204564A  _325edf84-","encounterId":"ApixioEncounterId:UNKNOWN","sourceId":"ApixioSourceId:e0d14c98-2f56-4a63-9bc0-0b3b2ea573a7","sourceSystem":"MOR"}},"source":{"uri":"ebddafa5-c4e2-4d4a-b40e-805274053a30","type":"problem"},"evidence":{"inferred":false},"attributes":{"bucketName":"com.apixio.advdev.StructuredConditionExtractor","bucketType":"StructuredConditions","encHash":"8m0lVsLEdn1curxVfV1Izw==","sourceType":"CMS_KNOWN","editType":"ACTIVE","editTimestamp":"2014-07-17T23:08:45.515Z","$workId":"219354","$jobId":"219368","$orgId":"339","$batchId":"339_hcpnv","$propertyVersion":"1404869030522","$documentId":"doc_id9d7c96ec-cfba-426b-a9c1-75504166b36a","$documentUUID":"1e228b18-3e4e-411e-9814-021af396822b","$patientUUID":"8daac322-9f7d-4016-9c06-ef58ea2ef75e","$eventBatchId":"0ddb78ba-a8ee-432f-b902-bafad16fbed5"}}"""
      val event : EventType = parser.getObjectMapper.readValue(raw_event, classOf[EventType])
      val now = System.currentTimeMillis.toString
      val topic = s"test_${now}"
      val c = new SimpleKafkaEventConsumer(consumer_config, topic)
      val p = new SimpleKafkaEventProducer(producer_config)
      try {
        p.send(topic, event)
        val res_event = readUntilTimeOrMsg[EventType](c, System.currentTimeMillis + 3000)
        assert(res_event.nonEmpty)
        assert(parser.getObjectMapper.writeValueAsString(res_event.get) == raw_event)
      } finally {
        c.shutdown
        p.close
      }
    }
  }
}
