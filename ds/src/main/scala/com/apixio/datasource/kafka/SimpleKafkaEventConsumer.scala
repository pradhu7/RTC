package com.apixio.datasource.kafka

import com.apixio.datasource.utility.EventDataUtility
import com.apixio.model.event.EventType
import java.util.Properties
import org.apache.kafka.common.serialization.{StringDeserializer, Deserializer}


class EventDecoder() extends Deserializer[EventType] {
  val edu = new EventDataUtility()
  override def deserialize(topic: String, bytes: Array[Byte]): EventType = edu.getEvent(bytes, true)
}

class SimpleKafkaEventConsumer(props: Properties, topic: String, kdec: Deserializer[String], vdec: Deserializer[EventType])
    extends SimpleKafkaConsumer[String,EventType](props, topic, kdec, vdec) {
  def this(props: Properties) =
    this({val np = props.clone.asInstanceOf[Properties]; np.remove("topic"); np}, props.getProperty("topic"),
      new StringDeserializer(), new EventDecoder())
  def this(props: Properties, topic: String) =
    this(props, topic, new StringDeserializer(), new EventDecoder())
}
