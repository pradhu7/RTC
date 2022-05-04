package com.apixio.app.fluent

import java.util

import org.json4s.JsonAST._
import org.json4s.{JObject, JValue}

private [fluent] object JsonUtils {
  implicit class JObjectHelper(val jObject: JObject) extends AnyVal {
    def +++(other: JValue): JObject = {
      other match {
        case otherJObj: JObject => JObject(jObject.obj ++ otherJObj.obj)
        case _                  => jObject
      }
    }

    private def getSimpleJValueAsString(value: JValue): String = {
      value match {
        case JString(v) => v
        case JInt(v)    => v.toString()
        case JLong(v)    => v.toString
        case JBool(v)   => v.toString
        case JDouble(v) => v.toString
        case _    => ""
      }
    }

    def toJavaMap: util.Map[String, AnyRef] = { //required for fluent logging.
      def flattenJObject(obj: JObject, init: util.Map[String, AnyRef] = new util.HashMap[String, AnyRef]()): util.Map[String, AnyRef] = {
        obj.obj.foldLeft(init) {
          case (acc, (key, value: JObject)) =>
            acc.put(key, flattenJObject(value))
            acc

          case (acc, (key, value: JArray)) =>
            acc.put(key, flattenJArray(value))
            acc

          case (acc, (key, JNothing)) => acc

          case (acc, (key, JNull)) => acc

          case (acc, (key, value: JValue)) =>
            acc.put(key, getSimpleJValueAsString(value))
            acc
        }
      }

      def flattenJArray(obj: JArray): Array[Any] = {
        val coll = obj.arr.collect {
          case each: JObject => flattenJObject(each)
          case each: JArray  => flattenJArray(each)
          case each: JValue  => getSimpleJValueAsString(each)
        }
        coll.toArray
      }
      flattenJObject(jObject)
    }
  }
}
