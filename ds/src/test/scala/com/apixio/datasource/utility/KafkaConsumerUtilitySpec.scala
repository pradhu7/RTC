package com.apixio.datasource.utility

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.funspec.AnyFunSpec

@RunWith(classOf[JUnitRunner])
class KafkaConsumerUtilitySpec extends AnyFunSpec {
  describe("Datasource's Kafka utility") {
    it("should correctly provide partitions for a topic") {
      assert(KafkaConsumerUtility.partitionsForTopic("localhost:2181", "__consumer_offsets").mkString(",") ==
        "0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49")
    }

    it("should correctly provide the brokerlist") {
      assert(KafkaConsumerUtility.brokerList("localhost:2181").endsWith(":9092"))
    }

    /*
    it("should correctly provide consumer group information") {
      val res = KafkaConsumerUtility.consumerGroups("localhost:2181").map(_.productIterator.toList)
      println(List("group", "topic", "partition", "offset", "metadata", "timestamp").mkString(","))
      println(Tabulator.format(List(List("group", "topic", "partition", "offset", "metadata", "timestamp")) ++ res))
      val blist = KafkaConsumerUtility.brokerList("localhost:2181")
      res.foreach(x => println(x(1)+":"+x(2)+" => "+KafkaConsumerUtility.offsets(blist,x(1).asInstanceOf[String],x(2).asInstanceOf[Int])))
    }
    */
  }
}
