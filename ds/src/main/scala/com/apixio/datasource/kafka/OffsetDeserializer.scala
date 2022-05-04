package com.apixio.datasource.kafka

import util.matching.Regex
import kafka.tools.DefaultMessageFormatter
import java.io.{ByteArrayOutputStream, PrintStream}

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.Deserializer

case class GroupTopicPartition(group: String, topic: String, partition: Int)
case class OffsetMetadata(offset: Long, metadata: String, timestamp: Long)

class OffsetKeyDeserializer() extends Deserializer[GroupTopicPartition] {
  private val formatter = new DefaultMessageFormatter()
  override def configure(configs: java.util.Map[String,_], isKey: Boolean) = { }
  override def close() = { }

  override def deserialize(topic: String, data: Array[Byte]) : GroupTopicPartition = {
    val bs = new ByteArrayOutputStream()
    val ps = new PrintStream(bs)
    try {
      val cr = new ConsumerRecord[Array[Byte], Array[Byte]](topic, 0, 0, data, null)
      formatter.writeTo(cr, ps)
      val res = bs.toString("UTF-8").split("::").head.replaceAll("\\[","").replaceAll("]","").split(",")
      GroupTopicPartition(res(0),res(1),res(2).toInt)
    }
    catch { case x:Throwable => println(org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(x)); GroupTopicPartition("","",-1) }
    finally { bs.close(); ps.close() }
  }
}

class OffsetValueDeserializer() extends Deserializer[OffsetMetadata] {
  private val formatter = new DefaultMessageFormatter()
  private val offset = """offset=(\d+),metadata""".r
  private val metadata = """metadata=(.*),timestamp=""".r
  private val timestamp = """,timestamp=(\d{13})""".r

  override def configure(configs: java.util.Map[String,_], isKey: Boolean) = { }
  override def close() = { }
  def extract(r: Regex, s: String) : String = r.findAllIn(s).matchData.map(_.group(1)).next

  override def deserialize(topic: String, data: Array[Byte]) : OffsetMetadata = {
    val bs = new ByteArrayOutputStream()
    val ps = new PrintStream(bs)
    try {
      val cr = new ConsumerRecord[Array[Byte], Array[Byte]](topic, 0, 0, null, data)
      formatter.writeTo(cr, ps)
      val res = bs.toString("UTF-8")
      OffsetMetadata(extract(offset, res).toLong, extract(metadata, res), extract(timestamp, res).toLong)
    }
    catch { case x:Throwable => println(org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(x)); OffsetMetadata(-1,"",-1) }
    finally { bs.close(); ps.close() }
  }
}
