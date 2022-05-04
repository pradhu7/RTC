package com.apixio.scala.apxapi


class Prospective(connection :ServiceConnection, tm: ApxSession, options: Map[String,String] = Map()) extends BaseService(connection, tm, options) {
  def getIntegration(accountId: String) : Map[String, Any] = {
    get[Map[String, Any]](s"/integration/accountId/$accountId")
  }
}