package com.apixio.datasource.kafka

import java.time.Duration
import java.util.Properties
import java.util

import org.apache.kafka.clients.consumer.{ConsumerRecord, KafkaConsumer}
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, Deserializer, StringDeserializer}
import org.apache.kafka.common.errors.TimeoutException

// wrapping a Consumer object because it wraps zkconsumerclass (private)
abstract class SimpleKafkaConsumer[K,V](props: Properties, topic: String, kdec: Deserializer[K], vdec: Deserializer[V]) {
  props.put("key.deserializer", kdec.getClass)
  props.put("value.deserializer", vdec.getClass)
  private val conn = new KafkaConsumer[K,V](props)
  private val pollDuration = Duration.ofSeconds(1)
  conn.subscribe(util.Arrays.asList(topic))
  private val iter = conn.poll(pollDuration).iterator()

  def setShutdownHook(): Unit = sys addShutdownHook { shutdown }
  def shutdown() = conn.close
  def iterator(): util.Iterator[ConsumerRecord[K,V]] = iter

  def getMessage(): Option[V] = {
    try {
      if (iter.hasNext)
        Some(iter.next.value())
      else None
    }
    catch { case ex: TimeoutException => None }
  }
}

class SimpleKafkaDefaultConsumer(props: Properties, topic: String, kdec: Deserializer[Array[Byte]], vdec: Deserializer[Array[Byte]])
    extends SimpleKafkaConsumer[Array[Byte],Array[Byte]](props, topic, kdec, vdec) {
  def this(props: Properties) =
    this({val np = props.clone.asInstanceOf[Properties]; np.remove("topic"); np}, props.getProperty("topic"),
      new ByteArrayDeserializer(), new ByteArrayDeserializer())
  def this(props: Properties, topic: String) =
    this(props, topic, new ByteArrayDeserializer(), new ByteArrayDeserializer())
}

class SimpleKafkaStringConsumer(props: Properties, topic: String, kdec: Deserializer[String], vdec: Deserializer[String])
    extends SimpleKafkaConsumer[String,String](props, topic, kdec, vdec) {
  def this(props: Properties) =
    this({val np = props.clone.asInstanceOf[Properties]; np.remove("topic"); np}, props.getProperty("topic"),
      new StringDeserializer(), new StringDeserializer())
  def this(props: Properties, topic: String) =
    this(props, topic, new StringDeserializer(), new StringDeserializer())
}
