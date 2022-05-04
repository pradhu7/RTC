package com.apixio.model.profiler

import com.apixio.model.utility.Conversions.dateTimeOrdering
import com.fasterxml.jackson.annotation.{JsonIgnoreProperties,JsonProperty,JsonSetter,JsonGetter}
import com.fasterxml.jackson.databind.ObjectMapper
import java.util
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ConcurrentHashMap
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import scala.collection.{concurrent,mutable}
import scala.collection.JavaConverters._

/**
 * This is a specific long-term job being executed by a service.
 * @note Created on 8/12/15 in com.apixio.hcc.elasticdao.utility
 * @param name The name of the job.
 * @param size The size of the job.
 * @param stop The stop method.
 * @param jobId A jobId for the job.
 * @param start The start time of the job.
 * @param counter The counter of progress.
 * @author rbelcinski@apixio.com
 */
@JsonIgnoreProperties(Array("stop"))
case class JobItem(
  @JsonProperty("name")       name:String = "",
  @JsonProperty("size")   var size:Int = 0,
  @JsonProperty("stop")   var stop:(() => Unit) = () => Unit,
  @JsonProperty("status") var status:String = "started",
  @JsonProperty("jobId")      jobId:String = UUID.randomUUID().toString,
  @JsonProperty("start")      start:DateTime = DateTime.now(),
  @JsonProperty("finish") var finish:DateTime = null,
  @JsonProperty("counter")    counter:AtomicInteger =  new AtomicInteger(0)) {
  @JsonProperty("metrics") val metrics:concurrent.Map[String,Any] = new ConcurrentHashMap[String,Any]().asScala

  def this() = this(name = "")

  def asJson()(implicit mapper: ObjectMapper) = mapper.writeValueAsString(this)

  def incSize(v: Int) = this.synchronized { size += v }

  def increment : Unit = counter.incrementAndGet

  def incMetric(k: String) : Unit = metrics.putIfAbsent(k, new AtomicInteger(0)).getOrElse(metrics(k)).asInstanceOf[AtomicInteger].incrementAndGet

  def addMetric(k: String, v: Int) : Unit = metrics.putIfAbsent(k, new AtomicInteger(0)).getOrElse(metrics(k)).asInstanceOf[AtomicInteger].getAndAdd(v)

  def incMapMetric(k1: String, k2: String) : Unit = metrics.putIfAbsent(k1, (new ConcurrentHashMap[String,AtomicInteger]()).asScala)
    .getOrElse(metrics(k1)).asInstanceOf[concurrent.Map[String,AtomicInteger]].putIfAbsent(k2, new AtomicInteger(0))
    .getOrElse(metrics(k1).asInstanceOf[concurrent.Map[String,AtomicInteger]](k2)).incrementAndGet

  def pack(): util.Map[String,Object] = metrics.synchronized {
    (metrics.flatMap(x => x._2 match {
      case i: AtomicInteger =>
        List((s"metrics.${x._1}" -> i.toString))
      case m: concurrent.Map[String,AtomicInteger] =>
        m.map(y => (s"metrics.${x._1}.${y._1}" -> y._2.toString)).toList
      }).toMap[String,AnyRef] ++
      Map[String,AnyRef]("name" -> name, "size" -> size.toString, "jobId" -> jobId, "status" -> status,
        "finish" -> (if (finish != null) ISODateTimeFormat.dateTime.print(finish) else ""), "start" -> ISODateTimeFormat.dateTime.print(start),
        "counter" -> counter.toString)).asJava
  }
  def getElapsedSeconds : Double = status match {
    case "completed" => (finish.getMillis - start.getMillis) / 1000.0
    case _ => (DateTime.now.getMillis - start.getMillis) / 1000.0
  }
  def getPercentDone : Double = counter.doubleValue / size.toDouble * 100.0

  @JsonSetter("metrics") def setMetrics(m: Map[String,Any]): Unit = metrics ++= map2Concurrent(m)

  def map2Concurrent(m: Map[String,Any]): concurrent.Map[String,Any] = {
    new ConcurrentHashMap[String,Any]().asScala ++= m.map {
      case (k, v: Map[String, Any]) => (k, map2Concurrent(v))
      case kv => kv
    }
  }
}

/**
 * This is a class used to track long-running operations.
 */
object JobTracker {
  private val jobs = mutable.Map[String,JobItem]()
  var maxJobs : Int = 10

  def create(name: String) : JobItem = jobs.synchronized {
    val job = JobItem(name)
    jobs += (job.jobId -> job)
    job
  }

  def get(jobId: String) : Option[JobItem] = jobs.get(jobId)

  def stop(j: JobItem) : Unit = jobs.synchronized {
    j.stop()
    j.status = "completed"
    j.finish = DateTime.now()
    if (jobs.size > maxJobs) {
      jobs.map(_._2).filter(_.status == "completed").toList.sortBy(_.finish.getMillis).take(jobs.size - maxJobs).foreach(x => jobs -= x.jobId)
    }
  }

  def asJson()(implicit mapper: ObjectMapper) : String = jobs.synchronized {
    mapper.writeValueAsString(jobs.map(x => Map[String,String]("name" -> x._2.name, "jobId" -> x._1)).toList)
  }
}
