package com.apixio.scala.apxapi

//generalized token swapping errors
class ApxException(val msg: String) extends Exception(msg)

//other non-401 and non-403 codes in 4xx-5xx
class ApxCodeException(val code: Int, override val msg: String) extends ApxException(msg)

//401
class ApxAuthException(override val msg: String) extends ApxException(msg)

//403
class ApxForbiddenException(override val msg: String) extends ApxException(msg)

