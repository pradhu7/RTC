package com.apixio.datasource.kafka

import com.apixio.datasource.utility.EventDataUtility
import com.apixio.model.event.EventType
import java.util.Properties
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.Serializer
import kafka.utils.VerifiableProperties

class EventEncoder(props: VerifiableProperties = null) extends Serializer[EventType] {
  val edu = new EventDataUtility()
  override def serialize(topic: String, value: EventType): Array[Byte] = edu.makeEventBytes(value, true)
}

class SimpleKafkaEventProducer(config: ProducerConfig)
    extends SimpleKafkaProducer[String,EventType](config) {
  def this(props: Properties) { this(new ProducerConfig(
      {props.put("serializer.class", classOf[EventEncoder].getName); props})) }
}
