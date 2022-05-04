package com.apixio.scala.apxapi

class Sessionmanager(connection: ServiceConnection, tm: ApxSession, options: Map[String,String] = Map()) extends BaseService(connection, tm, options) {
  def version() = {
    get[String](s"/version",
      auth = false)
  }

  def getNextWorkItem(transaction_id: String) = {
    get[String](s"/hcc/next_work_item")
  }

  def hold() = {
    post[String](s"/workitems/status/hold")
  }

  def activate() = {
    post[String](s"/workitems/status/active")
  }

  def getWorkItem(wik: String): String = {
    get[String](s"/hcc/get_work_item/${wik}")
  }

  def adhocOpportunity(wik: String, me: Boolean = false): String = {
    val onlyMe = if(me) "/me" else ""
    get[String](s"/adhocreport/opportunities${onlyMe}/${wik}")
  }

}
