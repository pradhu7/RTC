package com.apixio.model.utility

import scala.io.Source

trait ResourceControl {
  // Loan pattern for automatically closing resource
  def using[A](r: Source)(f: Source => A): A =
    try {
      f(r)
    } finally {
      r.close()
    }
}
