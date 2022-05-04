package apxapi

import com.apixio.scala.apxapi.{ApxApi, ApxCodeException, AuthSpec}
import org.scalatest.matchers.should.Matchers

import scala.util.{Failure, Try}

class DocumentSearchSpec extends AuthSpec with Matchers {

  "it" should "retieve the hocr for the documentId/page" in {
    val apxApi = login(custops)
    val documentID = "8ba062bc-d655-4e91-a692-6b547e6f0fc2"
    val page = 1

    val hocr = apxApi.documentSearch.getPageHOCR(documentID, page)

    assert(hocr.nonEmpty)
    assert(hocr.contains("ocr-system"))
    assert(hocr.contains("tesseract"))

  }

  "it" should "return null for the invalid documentId" in {
    val apxApi = login(custops)
    val documentID = "xxxxxxx-d655-4e91-a692-6b547e6f0fc2"
    val page = 1

    verifyDocumentNotFound(apxApi, documentID, page)
  }

  "it" should "return null for the invalid page" in {
    val apxApi = login(custops)
    val documentID = "8ba062bc-d655-4e91-a692-6b547e6f0fc2"
    val page = 10

    verifyDocumentNotFound(apxApi, documentID, page)
  }

  private def verifyDocumentNotFound(apxApi: ApxApi, documentID: String, page: Int) = {
    val failedEx = Try(apxApi.documentSearch.getPageHOCR(documentID, page)) match {
      case Failure(ex: ApxCodeException) =>
        ex
    }

    assert(failedEx.code == 404)
  }

}
