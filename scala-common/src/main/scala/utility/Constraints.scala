package com.apixio.scala.utility

import io.dropwizard.{validation => dwv}

import scala.annotation.meta.param

package object Constraints {
  type ValidationMethod = dwv.ValidationMethod @param
}
