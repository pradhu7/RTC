package com.apixio.scala.utility

import java.util.{Date, Timer, TimerTask}
import java.io.File

/**
  * @author Ha Pham - hpham@apixio.com
  */
abstract class FileWatcher extends TimerTask {
  var file:File
  var timestamp:Long = file.lastModified()
  val freqInMilliseconds: Int = 1000
  val timer = new Timer()

  def start(): Unit = timer.schedule(this, new Date(), freqInMilliseconds)

  def run(): Unit = if (timestamp != file.lastModified()) {
    timestamp = file.lastModified()
    onChange()
  }

  def onChange(): Unit
}

