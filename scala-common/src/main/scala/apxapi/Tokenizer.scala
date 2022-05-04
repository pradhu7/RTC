package com.apixio.scala.apxapi

class Tokenizer(connection :ServiceConnection, tm: ApxSession, options: Map[String,String] = Map()) extends BaseService(connection, tm, options) {
  def swap(token: String) : String = {
    val res = post[Map[String,String]]("/tokens",
                                       headers = authHeader(token),
                                       auth = false)
    res("token")
  }
}
