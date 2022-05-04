package com.apixio.scala.apxapi

class DocumentSearch(connection: ServiceConnection, tm: ApxSession, options: Map[String,String] = Map()) extends BaseService(connection, tm, options) {

  /**
   * @param documentId
   * @param page
   * @return the HOCR for a particular documentId / page, if it exists.
   */
  def getPageHOCR(documentId: String, page: Int): String = {
    get[String](s"/search/hocr/document/$documentId/$page")
  }
}

