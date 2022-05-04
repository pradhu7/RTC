package com.apixio.scala.logging

import collection.JavaConverters._
import collection.mutable.MutableList

trait TrackApixioLoggable extends ApixioLoggable {
  val errors = MutableList[(String,String)]()
  val warns = MutableList[(String,String)]()
  val events = MutableList[(String,Map[String,Object])]()
  val infos = MutableList[(String,String)]()
  val debugs = MutableList[(String,String)]()

  def reset() = {
    errors.clear()
    warns.clear()
    events.clear()
    infos.clear()
    debugs.clear()
  }

  abstract override def error(msg:String, context:String = "") = {
    super.error(msg, context)
    errors += new Tuple2(context, msg)
  }

  abstract override def warn(msg:String, context:String = "") = {
    super.warn(msg, context)
    warns += new Tuple2(context, msg)
  }

  abstract override def info(msg:String, context:String = "") = {
    super.info(msg, context)
    infos += new Tuple2(context, msg)
  }

  abstract override def event(msg:java.util.Map[String, Object], context:String = "", eventName:String = "") = {
    val newmsg = new java.util.HashMap[String,Object](msg)
    super.event(newmsg, context, eventName)
    events += new Tuple2(context, Map[String,Object](newmsg.asScala.toList:_*))
  }

  abstract override def debug(msg:String, context:String = "") = {
    super.debug(msg, context)
    debugs += new Tuple2(context, msg)
  }
}
