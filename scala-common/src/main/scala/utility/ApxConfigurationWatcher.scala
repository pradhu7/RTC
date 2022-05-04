package com.apixio.scala.utility

import java.io.File
import com.apixio.scala.apxapi.ApxApi
import com.apixio.scala.dw.{ApxConfiguration, ApxServices}

/**
  * @author Ha Pham - hpham@apixio.com
  */

class ApxConfigurationWatcher(var file:File=new File("application.yaml")) extends FileWatcher {
  def onChange(): Unit = {
    ApxConfiguration.initializeFromFile(file.getAbsolutePath)      // reload ApxConfiguration
    ApxServices.init(ApxConfiguration.configuration.get)           // reinitialize ApxServices
    ApxApi.reload()                                                // reload ApxApi
  }
}