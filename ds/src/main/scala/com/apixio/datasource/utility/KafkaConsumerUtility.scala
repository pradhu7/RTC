
package com.apixio.datasource.utility

import java.util.Properties


import kafka.zk.KafkaZkClient
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.utils.Time
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import java.util.concurrent.TimeUnit

import com.apixio.datasource.kafka._
import org.apache.kafka.clients.admin.{AdminClientConfig, AdminClient}

import collection.JavaConverters._

object KafkaConsumerUtility {

  def getZKClient(zkUrl: String): KafkaZkClient = {
    KafkaZkClient(zkUrl, false, 30000, 30000, 1000, Time.SYSTEM)
  }

  def getAdminClient(brokers: String): AdminClient = {
    val config = new Properties()

    config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, brokers)
    config.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "1000")

    AdminClient.create(config)
  }

  def partitionsForTopic(zkUrl: String, topic: String): Seq[Int] = {
    val zk = getZKClient(zkUrl)
    val res = zk.getPartitionsForTopics(Set(topic))(topic)
    zk.close()
    res
  }

  def brokerList(zkUrl: String): String = {
    val zk = getZKClient(zkUrl)
    val res = zk.getAllBrokersInCluster.flatMap(_.endPoints).map(x => List(x.host,x.port).mkString(":")).mkString(",")
    zk.close()
    res
  }

  def listTopics(zkUrl: String): Seq[String] = {
    val zk = getZKClient(zkUrl)
    val res = zk.getAllTopicsInCluster.toSeq.sorted
    zk.close()
    res
  }

  def lastCommitted(blist: String, group: String, topic: String, partition: Int): Option[Long] = {
    val (broker, port) = blist.split(",").head.split(":").map(_.toString).toList match {
      case x : List[String] if x.size == 2 => (x(0).toString, x(1).toInt)
      case x : List[String] if x.size == 1 => (x.toString, 9092)
      case _ => ("localhost", 9092)
    }
    val adminClient = getAdminClient(blist)
    val offsets = adminClient.listConsumerGroupOffsets(group)
    val metadata = offsets.partitionsToOffsetAndMetadata().get(10000, TimeUnit.MILLISECONDS)
    val topicPartition = new TopicPartition(topic, partition)
    val topicPartitionMetadata = metadata.get(topicPartition)
    topicPartitionMetadata.offset().asInstanceOf[Option[Long]]
  }

  def randomId(prefix: String): String = List(prefix, scala.util.Random.alphanumeric.slice(0, 20).mkString).mkString("_")

  def offsets(blist: String, topic: String, partition: Int): (Long,Long) = {
    val props = new Properties()
    props.put("bootstrap.servers", blist)
    props.put("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
    props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
    val consumer = new StreamConsumer[Array[Byte],Array[Byte]](props)
    val topicPartition = new TopicPartition(topic, partition)
    val topicPartitions = List(
      topicPartition
    ).asJava
    val start = consumer.beginningOffsets(topicPartitions).get(topicPartition)
    val end = consumer.endOffsets(topicPartitions).get(topicPartition)
    (start, end)
  }

  /**
  def oldConsumerGroups(zkUrl: String, consumers: Option[Seq[String]] = None): List[(String,String,String,Int,Long,String,Long)] = {
    val zk = getZKClient(zkUrl)
    val ozk = new ZooKeeper(zkUrl, 30000, new Watcher { def process(event: WatchedEvent) = { } })
    val res = consumers.getOrElse(ZkUtils.getConsumerGroups(zk)).map(c => {
      ZkUtils.getTopicsByConsumerGroup(zk, c).map(t => {
        ZkUtils.getPartitionsForTopics(zk, List(t))(t).map(p => {
          val offsetpath = new ZKGroupTopicDirs(c, t).consumerOffsetDir + "/" + p
          val stat = ozk.exists(offsetpath, false)
          if (stat != null)
            (c,"",t,p,zk.readData[String](offsetpath).toLong,"",stat.getMtime)
          else
            (c,"",t,p,0L,"",0L)
        })
      })
    }).flatten.flatten.toList
    zk.close()
    ozk.close()
    res
  }
   **/

  def readStartToEnd[K,V](consumer: StreamConsumer[K,V], topic: String, partition: Int)(f:StreamRecord[K,V] => Boolean) = {
    consumer.subscribe(topic, partition)
    consumer.seekToEnd(topic, partition)
    val end = consumer.position(topic, partition)
    consumer.seekToBeginning(topic, partition)
    var r: StreamRecord[K,V] = null
    var current = consumer.position(topic, partition)
    if (end > current) {
      do {
        r = consumer.get(1000L)
        if (r != null)
          //XXX: edge case for key/value null (not sure what causes it)
          current = if (r.key == null || r.value == null || f(r)) r.metadata.offset else end
      } while (current < (end - 1));
    }
    consumer.unsubscribe(topic, partition)
  }

  def consumerGroups(zkUrl: String): List[(String,String,String,Int,Long,String,Long)] = {
    val adminClient = getAdminClient(brokerList(zkUrl))
    val groups = adminClient.listConsumerGroups().all().get().asScala.map(_.groupId()).asInstanceOf[List[String]]
    val groupData = scala.collection.mutable.Map[String,java.util.Map[TopicPartition, OffsetAndMetadata]]()
    for (group <- groups) {
      groupData +=  group -> adminClient.listConsumerGroupOffsets(group).partitionsToOffsetAndMetadata().get()
    }

    groupData.flatMap { case (group, metadata) =>
      metadata.asScala.map { case ( topic, info) =>
        (group, "X", topic.topic(), topic.partition(), info.offset, info.metadata, Time.SYSTEM.milliseconds())
      }
    }.toList
  }

  def setConsumerOffset(blist: String, group: String, topic: String, partition: Int, offset: Long) = {
    val topicPartition = new TopicPartition(topic, partition)
    val metadata = new OffsetAndMetadata(offset)
    val props = new Properties()
    props.put("bootstrap.servers", blist)
    props.put("group.id", group)
    props.put("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
    props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
    props.put("auto.offset.reset", "earliest")
    val consumer = new StreamConsumer[Array[Byte],Array[Byte]](props)

    val commitData = Map(topicPartition -> metadata).asJava
    consumer.commitSync(commitData)
    consumer.close()
  }

  //TODO: Could add clear consumer, but I'm not sure it is worth it, consumers disappear off __consumer_offsets after 30 days.
}

object Tabulator {
  def format(table: Seq[Seq[Any]]) = table match {
    case Seq() => ""
    case _ =>
      val sizes = for (row <- table) yield (for (cell <- row) yield if (cell == null) 0 else cell.toString.length + 1)
      val colSizes = for (col <- sizes.transpose) yield col.max
      val rows = for (row <- table) yield formatRow(row, colSizes)
      formatRows(rowSeparator(colSizes), rows)
  }

  def formatRows(rowSeparator: String, rows: Seq[String]): String = (
    rowSeparator ::
    rows.head ::
    rowSeparator ::
    rows.tail.toList :::
    rowSeparator ::
    List()).mkString("\n")

  def formatRow(row: Seq[Any], colSizes: Seq[Int]) = {
    val cells = (for ((item, size) <- row.zip(colSizes)) yield if (size == 0) "" else ("%" + size + "s ").format(item))
    cells.mkString("|", "|", "|")
  }

  def rowSeparator(colSizes: Seq[Int]) = colSizes map (x => "-" * (x+1)) mkString("+", "+", "+")
}

