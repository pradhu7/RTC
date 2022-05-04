package com.apixio.nassembly.patient

import com.apixio.model.file.catalog.jaxb.generated.ApxCatalog
import com.apixio.model.patient.Patient

import scala.collection.JavaConversions._

import java.util

object ParserUtil {

  def getDocumentContent(apo: Patient): Option[Array[Byte]] = {
    apo
      .getDocuments
      .headOption
      .flatMap(_.getDocumentContents.headOption)
      .map(_.getContent)
  }

  def getFileTypeFromDocumentOrCatalog(catalogEntry: ApxCatalog.CatalogEntry, apo: Patient): String = {
    var fileType = catalogEntry.getFileFormat
    for (document <- apo.getDocuments) {
      for (documentContent <- document.getDocumentContents) {
        documentContent.getMimeType match {
          case "application/pdf" =>
            fileType = "PDF"

          case "text/plain" =>
            fileType = "TXT"

          case "text/rtf" =>
            fileType = "RTF"

          case "application/msword" =>
            fileType = "DOC"

          case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" =>
            fileType = "DOCX"

          case "application/x-ccr" =>
            fileType = "CCR"

          case "application/x-ccd" =>
            fileType = "CCD"

          case "application/oxps" =>
            fileType = "XPS"

          case "application/vnd.ms-xpsdocument" =>
            fileType = "XPS"

          case "text/html" =>
            fileType = "HTML"

          case _ =>
        }
      }
    }
    fileType
  }


  def isGivenType(fileType: String, fileTypes: util.List[String]): Boolean = {
    fileType != null && fileTypes != null && fileTypes.map(_.toLowerCase).contains(fileType.toLowerCase.trim)
  }
}
