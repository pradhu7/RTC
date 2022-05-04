package com.apixio.model.profiler

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.ScalaObjectMapper
import org.json4s.JsonAST._
import org.json4s.JsonDSL._

case class Provider(
    @JsonProperty("fn") fn: String = "",    // First Name
    @JsonProperty("ln") ln: String = "",    // Last Name
    @JsonProperty("mn") mn: String = "",    // Middle Name
    @JsonProperty("cr") cr: String = "",    // Credentials
    @JsonProperty("npi") npi: String = "",  // NPI Id
    @JsonProperty("role") role: String = "" // Role: Rendering | Billing
                   ) {
  def asJson()(implicit mapper: ObjectMapper): String = mapper.writeValueAsString(this)
  def toJson: JObject =
    ("fn"-> fn) ~
      ("ln"-> ln) ~
      ("mn" -> mn) ~
      ("cr" -> cr) ~
      ("npi" -> npi) ~
      ("role" -> role)
}

object Provider {

  val empty: Provider = Provider()

  def apply(v: JValue): Option[Provider] = v match {
    case JNothing | JNull => None
    case JObject(obj) =>
      Some(obj.foldLeft(empty) {
        case (acc, ("fn", JString(c)))    => acc.copy(fn = c)
        case (acc, ("ln", JString(c)))    => acc.copy(ln = c)
        case (acc, ("mn", JString(c)))    => acc.copy(mn = c)
        case (acc, ("cr", JString(c)))    => acc.copy(cr = c)
        case (acc, ("npi", JString(c)))   => acc.copy(npi = c)
        case (acc, ("role", JString(c)))  => acc.copy(role = c)
        case (acc, _)                     => acc
      })
    case _ =>
      throw new IllegalArgumentException(s"${v.getClass.getCanonicalName} cannot be transformed to Provider")
  }

  def fromAnnotationEvent(e: EventTypeX): Provider =
    new Provider(e.evidence.attributes.getOrElse("physicianFirstName", ""),
      e.evidence.attributes.getOrElse("physicianLastName", ""),
      e.evidence.attributes.getOrElse("physicianMiddleName", ""),
      e.evidence.attributes.getOrElse("NPI", "")
    )

  def fromJson(obj: String)(implicit mapper: ObjectMapper with ScalaObjectMapper): Provider =
    mapper.readValue[Provider](obj)
}

