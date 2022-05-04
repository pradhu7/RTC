package com.apixio.datasource.kafka

import java.util.Properties
import org.apache.kafka.clients.producer.{ProducerRecord, ProducerConfig, KafkaProducer}
import org.apache.kafka.common.serialization.{StringSerializer, ByteArraySerializer}
import sun.reflect.generics.reflectiveObjects


// Probably going to need to incorporate some Apixio specific "recovery" logic here
class SimpleKafkaProducer[K,V](config: ProducerConfig)
  extends KafkaProducer[K,V](config.originals()) {
    def send(topic: String, msg: V) : Unit = send(new ProducerRecord[K,V](topic, msg))
    def send(topic: String, key: K, msg: V) : Unit = send(new ProducerRecord[K,V](topic, key, msg))
    def setShutdownHook() : Unit = sys addShutdownHook { close }
    def shutdown(): Unit = close
}

class SimpleKafkaDefaultProducer(config: ProducerConfig)
    extends SimpleKafkaProducer[Array[Byte],Array[Byte]](config) {
  def this(props: Properties) { this(new ProducerConfig(
      {props.put("serializer.class", classOf[ByteArraySerializer].getName); props})) }
}

class SimpleKafkaStringProducer(config: ProducerConfig)
    extends SimpleKafkaProducer[String,String](config) {
  def this(props: Properties) { this(new ProducerConfig(
      {props.put("serializer.class", classOf[StringSerializer].getName); props})) }
}
