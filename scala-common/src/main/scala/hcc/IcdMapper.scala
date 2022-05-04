package com.apixio.scala.utility

import com.apixio.model.profiler.Code
import com.apixio.scala.apxapi.Project
import com.apixio.scala.dw.ApxServices

// TODO: this class should be migrated to com.apixio.model.profiler package
// TODO: this is an innapropriate place to be reading ApxServices.configuration. please move that logic up the service layer.
/**
  * Created by nlieberman on 7/7/17.
  */
object IcdMapper {
  /**
  * Returns the correct HCC Mapping Version
  *  if icd-hcc mapping:
  *    Mapping version selection is based off of date of service (if supplied) or based off of payment year
  *  if apxcat mapping:
  *    Based off of payment year
  * @param paymentYear paymentYear of proj
  * @param icdMapping icdMapping of proj
  * @param apxcatMapping apxcatMapping of proj
  * @param c: Pass in Code("",Code.HCC) when using icd-hcc mapping
  * @param dos: date of service (Defaulted as empty)
  */
  private val DEFAULT_MAPPING = "2016-icd-hcc"

  def getHCCMappingVersion(paymentYear:String, icdMapping: String, apxcatMapping:String, c: Code, dos: String = ""): String = if (c.isIcd || c.isHcc) {
    icdMapping.isEmpty match {
      case false =>
        if (dos.isEmpty())
          icdMapping
        else
          byDOS(dos, icdMapping)
      case true =>
        if (dos.isEmpty())
          byPaymentYear(paymentYear)
        else
          byDOS(dos,DEFAULT_MAPPING)
    }
  } else {
    apxcatMapping.isEmpty match {
      case false => apxcatMapping
      case true =>
        paymentYear match {
          case py if py == "2016" || py == "2017" => "2016-apxcat-hcc"
          case py if py == "2015" => "2015-apxcat-hcc"
          case py if py.isEmpty => ApxServices.configuration.application.get("apxcatMapping").map(_.toString).getOrElse("2016-apxcat-hcc")
        }
    }
  }

  /**
  Returns True if the date of service occurs after the specified cutoff date
    * @param date: Cutoff date for payment year
    * @param dos: Date of Service
    */
  def isAfterDate(date: String, dos: String) : Boolean = {
    val format = "MM/dd/yyyy"
    val dateOfService = new java.text.SimpleDateFormat(format).parse(dos)
    val cutoff = new java.text.SimpleDateFormat(format).parse(date)
    dateOfService.after(cutoff)
  }

  /**
  If the date of Service falls after October 1st, it uses the new mapping version
    Else it uses its calendar year mapping version
    * @param dos: date of service
    * @param icdMapping: icdMapping passed into project settings or default mapping
    */
  def byDOS(dos:String, icdMapping:String): String = {
    val mappingType = icdMapping.substring(icdMapping.indexOf('-'))
    // Expected output for mapping type:
    //    "-icd-hcc" or "-icd-hcccrv1"
    List("-icd-hcc", "-icd-hcccrv1", "-icd-hcccrv2").contains(mappingType) match {
      case true =>
        val serviceYear = dos.takeRight(4)
        val nextYear = (serviceYear.toInt + 1).toString
        val cutoff = "09/30/" + dos.takeRight(4)
        val afterCutoffDate = isAfterDate(cutoff, dos)
        afterCutoffDate match {
          case true => nextYear + mappingType
          case false => serviceYear + mappingType
        }
      case false =>
        //Play it safe and just use the project specified mapping (This should never happen though)
        icdMapping
    }
  }

  /**
  Helper method to choose the icd-hcc version based off of the payment year
    * @param paymentYear: payment year from the project settings
    *  TO-DO. Once 2017 mapping is valid, don't default it to 2017..set 2018 to default to 2017
    */
  def byPaymentYear(paymentYear: String): String = {
    Option(paymentYear) match {
      case Some(py) if py == "2016" || py == "2017" => "2016-icd-hcc"
      case Some(py) if py == "2015" => "2015-icd-hcc"
      case None => ApxServices.configuration.application.get("icdMapping").map(_.toString).getOrElse(DEFAULT_MAPPING)
    }
  }


}
