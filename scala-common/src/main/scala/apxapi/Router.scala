package com.apixio.scala.apxapi

import scala.collection.JavaConverters._
import com.fasterxml.jackson.databind.node.ObjectNode

class Router(connection :ServiceConnection, tm: ApxSession, options: Map[String,String] = Map()) extends BaseService(connection, tm, options) {
  def nextWorkItem() = { // TODO: will need to accept a transactionId when we want to prefetch
    get[String](s"/hcc/next_work_item")
  }

  def getWorkItem(wik: String): String = {
    get[String](s"/hcc/get_work_item/${wik}")
  }

  def adhocOpportunity(wik: String, me: Boolean = false): String = {
    val onlyMe = if(me) "/me" else ""
    get[String](s"/adhocreport/opportunities${onlyMe}/${wik}")
  }

  def finish(transId: String, regress: Boolean = false): String = {
    post[String](s"/hcc/finish/${transId}", params=Map("regress" -> regress.toString))
  }

  def skip(transId: String): String = {
    post[String](s"/hcc/skip/${transId}")
  }

  def release(transId: String): String = {
    post[String](s"/hcc/release/${transId}")
  }

  def advancePhase(wik: String, phase: String): String = {
    post[String](s"/hcc/advance/${wik}/${phase}")
  }

  def annotate(transId: String, annotations: ObjectNode): String = {
    put[String](s"/hcc/annotate/${transId}", data=asJsonBytes(annotations))
  }

  def deleteAnnotation(wipk: String, isotime: String) = {
    delete[String](s"/hcc/annotate/${wipk}", params=Map("isotime" -> isotime))
  }

  def adhocReport(
    project: String,
    startDate: String,
    endDate: String,
    result: String = "all",
    phase: String = "all",
    coder: String = "all",
    page: String = "0",
    pageSize: String = "10",
    flagged: String = "all",
    tz: String = "-8",
    q: Array[Byte] = Array(),
    me: Boolean = false): String = {
      val onlyMe = if(me) "/me" else ""
      post[String](s"/adhocreport/opportunities${onlyMe}", data=q, params=Map(
        "project" -> project,
        "startDate" -> startDate,
        "endDate" -> endDate,
        "result" -> result,
        "phase" -> phase,
        "coder" -> coder,
        "page" -> page,
        "pageSize" -> pageSize,
        "flagged" -> flagged,
        "tz" -> tz
      ))
    }

  def statusQueue(queue: Option[String] = None) =
    get[Map[String,Int]](s"/status/queue", Map("queue" -> queue.getOrElse("")))

  def projectRemaining(project: String) =
    get[Map[String,Int]](s"/admin/queue/$project/remaining")

  def refresh(wipk: String) =
    post[String](s"/hcc/refresh/$wipk")

  def reloadQueue(queue: String) =
    post[String](s"/admin/queue/$queue/reload")

  def releaseMe(projectId: String) =
    post[String](s"/hcc/release/me/$projectId")

  def statusQueue(queue: String):Map[String,Long] =
    get[Map[String,Long]](s"/status/queue?queue=$queue")
}
