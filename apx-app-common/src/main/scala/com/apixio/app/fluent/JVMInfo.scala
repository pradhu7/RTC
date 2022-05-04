package com.apixio.app.fluent

import java.net.InetAddress
import java.time.LocalDate

import org.json4s.JsonAST.{JInt, JObject, JString}

case class JVMInfo(totalMemory: Long, freeMemory: Long, maxMemory: Long, cpu: Int, threadId: Long, threadName: String, threadCount: Int, hostName: String, time: Long, date: String)

object JVMInfo {
  def apply(threadId: Long = Thread.currentThread.getId, threadName: String = Thread.currentThread().getName): JVMInfo = {
    JVMInfo(Runtime.getRuntime.totalMemory, Runtime.getRuntime.freeMemory, Runtime.getRuntime.maxMemory,
      Runtime.getRuntime.availableProcessors, threadId, threadName, Thread.activeCount(),
      s"ip-${InetAddress.getLocalHost.getHostAddress.replace('.', '-')}",
      System.currentTimeMillis(), LocalDate.now.toString
    )
  }

  implicit def jvmInfoToJson(value: JVMInfo): JObject = {
    JObject("memory" -> JObject(
      "total.bytes" -> JInt(value.totalMemory),
      "free.bytes" -> JInt(value.freeMemory),
      "max.bytes" -> JInt(value.maxMemory)
    ),
      "processor.count" -> JInt(value.cpu),
      "thread" -> JInt(value.threadId),
      "thread.name" -> JString(value.threadName),
      "threads.count" -> JInt(value.threadCount),
      "hostname" -> JString(value.hostName),
      "time" -> JInt(value.time),
      "datestamp" -> JString(value.date)
    )
  }
}
