package com.apixio.app.documentsearch

import java.awt.Font
import java.awt.font.FontRenderContext
import java.awt.geom.AffineTransform
import java.util.UUID
import java.util.concurrent.Executors
import com.apixio.bizlogic.document.{DocTermResults, DocumentLogic}
import com.apixio.bizlogic.patient.assembly.PatientAssembly.PatientCategory
import com.apixio.scala.dw.ApxServices
import com.apixio.scala.logging.ApixioLoggable
import com.google.common.cache._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Whitelist

import javax.xml.parsers.SAXParserFactory
import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.xml.XML

class RealTimeDocumentSearchManager extends ApixioLoggable {
  setupLog(this.getClass.getCanonicalName)

  val WILDCARD = "*"
  val MD_KEY_PREFIX = "s3_ext.ts"
  val STRING_CONTENT = "string_content"

  // Make these global to optimize font transforms
  val affineTransform = new AffineTransform
  val frc = new FontRenderContext(affineTransform, true, true)

  val documentLogic = new DocumentLogic(ApxServices.daoServices.getBlobDAO)

  // Cache for pdsId by documentUuid. We can keep a lot of these as they are small. They are used any time a
  // documents words fall out of cache (so these should naturally last longer than document words)
  val documentPdsId: LoadingCache[String, String] = CacheBuilder.from(
    ApxServices.configuration.application.getOrElse("documentPdsIdCachePolicy","maximumSize=10000, expireAfterWrite=24h").toString)
    .removalListener(new RemovalListener[String,String] {
      override def onRemoval(notification: RemovalNotification[String, String]): Unit = {
        info("Document PDS ID for " + notification.getKey + " is removed because of " + notification.getCause)
      }
    })
    .build(new CacheLoader[String, String] {
      override def load(documentUuid: String): String = {
        info("Document PDS ID for " + documentUuid + " is added.")
        ApxServices.patientLogic.getPdsIDAndPatientUUIDByDocumentUUID(UUID.fromString(documentUuid)).pdsID
      }
    })

  // Cache for list of documentUuids by patientUuid. Only keep these a few minutes as more data may be loading.
  // Used to update patient documents when searching entire patient. size should be an order of magnitude smaller than patients
  // assuming ~10 docs/patient = 1000
  val patientDocuments: LoadingCache[String, List[String]] = CacheBuilder.from(
    ApxServices.configuration.application.getOrElse("patientDocumentCachePolicy","maximumSize=1000, expireAfterWrite=10m").toString)
    .removalListener(new RemovalListener[String, List[String]] {
      override def onRemoval(notification: RemovalNotification[String, List[String]]): Unit = {
        info("Patient Documents for " + notification.getKey + " are removed because of " + notification.getCause)
      }
    })
    .build(new CacheLoader[String, List[String]] {
      override def load(patientUuid: String): List[String] = {
        info("Patient Documents for " + patientUuid + " are added.")
        val pdsId = ApxServices.patientLogic.getPdsIDAndPatientUUIDsByPatientUUID(UUID.fromString(patientUuid)).pdsID
        val documents = ApxServices.patientLogic.getMergedPatientSummaryForCategory(
          pdsId,
          UUID.fromString(patientUuid),
          PatientCategory.DOCUMENT_META.getCategory
        ).getDocuments.asScala.map(document => document.getInternalUUID.toString).toList
        // Add each of these documents to the documentPdsId
        documents.foreach(document => documentPdsId.put(document, pdsId))
        documents
      }
    })

  // Cache for document content by documentUuid. These could be large as some documents alone may have 10k pages.
  // let's try 100,000 and monitor it. We'll automatically rotate the memory out after 24h
  val documentContentCache: LoadingCache[String, String] = CacheBuilder.from(
    ApxServices.configuration.application.getOrElse("documentCachePolicy","maximumWeight=100000, expireAfterWrite=24h").toString)
    .removalListener(new RemovalListener[String, String] {
      override def onRemoval(notification: RemovalNotification[String, String]): Unit = {
        info("Document content for " + notification.getKey + " is removed because of " + notification.getCause)
      }
    })
    .weigher(new  Weigher[String, String]() {
      // This weight is based on the number of pages in the document
      override def weigh(key: String, value: String): Int = {
        DocumentLogic.getDocumentObject(value).getPages.getNumPages.intValue()
      }
    })
    .build(new CacheLoader[String, String] {
      override def load(documentUuid: String): String = {
        info("Document content for " + documentUuid + " is added.")
        documentLogic.getDocumentString(documentPdsId(documentUuid), documentUuid)
      }
    })

  // TODO: This method is not optimized. If a wildcard (*) search is performed on a large patient it could be very slow.
  def getPatientHighlights(patient: String, termsByPageByDocument: Map[String,Map[String,List[String]]]): Map[String, java.util.Map[Integer, DocTermResults]] = {
    // Get the list of documents for this patient that we want to search
    val numThreads = ApxServices.configuration.application.getOrElse("documentLoadThreads","8").toString.toInt
    val timeout = ApxServices.configuration.application.getOrElse("documentLoadTimeoutSeconds","60").toString.toInt
    implicit val context: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(numThreads))
    Await.result(Future.sequence(patientDocuments(patient)
      .filter(documentUuid => termsByPageByDocument.contains(WILDCARD) || termsByPageByDocument.contains(documentUuid))
      .map(documentUuid => {
        val documentPageTerms = termsByPageByDocument.getOrElse(WILDCARD, Map()) ++ termsByPageByDocument.getOrElse(documentUuid, Map())
        Future(documentUuid -> searchDocumentForTerms(documentUuid, documentPageTerms))(context)
      })), Duration.create(timeout, "s")).toMap.filter(highlightsByDocument => !highlightsByDocument._2.keySet.isEmpty)
  }

  def getDocumentWords(documentUuid: String): Map[String,List[Map[String,Any]]] = {
    getPageWordsFromStringContent(documentContentCache(documentUuid))
  }

  def searchDocumentPageForCoords(documentUuid: String, searchPage: String, coords: Map[String, Double]): List[Map[String,Any]] = {
    val documentPageWords = getDocumentWords(documentUuid)
    val pageWords = documentPageWords(searchPage)
    // we now want to filter the page words to any that would intersect with the coords provided.
    pageWords.filter(word => {
      if (word.contains("top")) {
          val wordLeft = word("left").toString.toDouble
          val wordTop = word("top").toString.toDouble
          val wordWidth = word("width").toString.toDouble
          val wordHeight = word("height").toString.toDouble
          wordLeft < (coords("left") + coords("width")) &&
            wordTop < (coords("top") + coords("height")) &&
            (wordTop + wordHeight) > coords("top") &&
            (wordLeft + wordWidth) > coords("left")
        } else {
         false
      }
    })
  }

  def getDocumentPageHocr(documentUuid: String, searchPage: String): String = {
    val stringContent = documentContentCache(documentUuid)
    val ocrextraction = DocumentLogic.getDocumentObject(stringContent)
    DocumentLogic.getDocumentPageHocr(ocrextraction, Integer.parseInt(searchPage))
  }

  // Search this document for terms on each page. the WILDCARD page will search every page
  def searchDocumentForTerms(documentUuid: String, termsByPage: Map[String,List[String]]): java.util.Map[Integer, DocTermResults] = {
    val docLogicResults = DocumentLogic.search(
      UUID.fromString(documentUuid),
      DocumentLogic.getDocumentObject(documentContentCache(documentUuid)),
      termsByPage.map {
        case (k, v) =>
          val terms: java.util.Collection[String] = v.asJava
          (k, terms)
      }.asJava
    )

    docLogicResults
  }

  def getPageWordsFromStringContent(stringContent: String): Map[String,List[Map[String,Any]]] = {
    val elem = XML.loadString(stringContent)
    (elem \\ "pages" \\ "page").map(page => {
      val imgType = (page \ "imgType").text
      val pageNumber = (page \ "pageNumber").text
      val extractedText = (page \ "extractedText" \ "content").text
      val plainText = (page \ "plainText").text
      val pageWords = if (!extractedText.equals("")) {
        getPageWordsExtracted(extractedText)
      } else if (!plainText.equals("")) {
        if (imgType != "PDF") {
          getPageWordsTextDocument(plainText)
        } else {
          getPageWordsPlainText(plainText)
        }
      } else {
        List()
      }
      pageNumber -> pageWords
    }).toMap
  }

  // TODO: This was lifted from the HTML conversion in Hadoop Jobs. Move to a common place.
  // For now we are going to use same structure as PDF but without coordinates for text, just join with space " "
  def getPageWordsTextDocument(plainText: String): List[Map[String,Any]] = {
    val document = Jsoup.parse(plainText)
    document.outputSettings(new Document.OutputSettings().prettyPrint(false));//makes html() preserve linebreaks and spacing
    document.select("br").append("\\n")
    document.select("p").prepend("\\n")
    document.select("span").append(" ")
    document.select("td").append("\\t")
    val s = document.html().replaceAll("\\\\n", "\n").replaceAll("\\\\t", "\t")
    Jsoup.clean(s, "", Whitelist.none(), new Document.OutputSettings().prettyPrint(false))
      .split(" ").map(word => Map("text" -> word)).toList
  }

  def getPageWordsExtracted(extractedText: String): List[Map[String,Any]] = {
    try {
      // TODO: Is it necessary to work with the DTD as in clean text?
      //          # already has a DTD:
      //          # <!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
      //          # need to add everything including eacute é
      //          if cleanedHtml.startswith('<!DOCTYPE'):
      //          cleanedHtml = self.dtd + cleanedHtml[cleanedHtml.index('>')+1:]
      // Perform a series of cleaning operations based on experiments
      val badTags = List(
        "<meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\">",
        "<meta name=\"ocr-system\" content=\"tesseract\">"
      )
      var cleanedHtml = extractedText
      badTags.foreach(badTag => {
        cleanedHtml = cleanedHtml.replace(badTag,"")
      })
      cleanedHtml = cleanedHtml.replace("&?","")
      val customParser = SAXParserFactory.newInstance
      customParser.setValidating(false)
      customParser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
      val saxParser = customParser.newSAXParser()
      val pageRoot = XML.withSAXParser(saxParser).loadString(cleanedHtml)
      val pageDetails = if (pageRoot.label.equals("div") && pageRoot.attribute("class").getOrElse("").equals("ocr_page")) {
        pageRoot.attribute("title").getOrElse(Seq())
      } else {
        pageRoot \\ "div"  find { _ \ "@class" exists (_.text == "ocr_page") } match {
          case Some(ocrPage) => ocrPage.attribute("title").getOrElse(Seq())
          case None => Seq()
        }
      }
      val pageDetailList: Array[String] = pageDetails.headOption match {
        case Some(pageDetailsHead) => pageDetailsHead.text.toString.split(";")
        case None => Array()
      }
      val (pageWidth, pageHeight) = pageDetailList.find(pageDetail => {
        pageDetail.trim.startsWith("bbox")
      }) match {
        case Some(pageDetail) =>
          val coordinates = pageDetail.trim.split(" ").drop(1).map(value => value.replace(";",""))
          (coordinates(2).toInt, coordinates(3).toInt)
        case _ => (0,0)
      }

      (pageRoot \\ "span" filter { _ \ "@class" exists (_.text == "ocr_line") }).flatMap(ocrLine => {
        val lineDetails: Map[String,String] = ocrLine.attribute("title").get.head.text.split(";").map(detail => {
          detail.trim.split(" ")(0) -> detail.trim.split(" ").drop(1).mkString(" ")
        }).toMap
        (ocrLine \\ "span" filter { _ \ "@class" exists (_.text == "ocrx_word") }).map(ocrWord => {
          val text = ocrWord.text.trim
          // ex: title="bbox 1497 1135 2187 1180; textangle 180; x_size 46.388493; x_descenders 9.3611107; x_ascenders 9.4523811"
          val wordDetails: Map[String,String] = ocrWord.attribute("title").get.head.text.split(";").map(detail => {
            detail.trim.split(" ")(0) -> detail.trim.split(" ").drop(1).mkString(" ")
          }).toMap
          val coordinates: Array[Float] = wordDetails("bbox").split(" ").map(coordinate => coordinate.toFloat)
          // Flip the coordinates if textangle is 180

          // Handle other transforms
          // baseline 0 -1;
          //  The two numbers for the baseline are the slope (1st number) and constant term (2nd number) of a
          // linear equation describing the baseline relative to the bottom left corner of the bounding box (red).
          // The baseline crosses the y-axis at -18 and its slope angle is arctan(0.015) = 0.86°.
          //
          // x_size 52.388062; x_descenders 7.3880601; x_ascenders 12

          // TODO: what other textangle values could we see and how should they be handled?
          val (left, top) = if (lineDetails.contains("textangle") && lineDetails("textangle") == "180") {
            (1 - (coordinates(2) / pageWidth), 1 - (coordinates(3) / pageHeight))
          } else {
            ((coordinates(0) / pageWidth),(coordinates(1) / pageHeight))
          }
          Map[String,Any](
            "text" -> text,
            "left" -> left,
            "top" -> top,
            "width" -> ((coordinates(2) - coordinates(0)) / pageWidth),
            "height" -> ((coordinates(3) - coordinates(1)) / pageHeight)
          )
        })
      }).toList
    } catch {
      case e:Throwable =>
        e.printStackTrace()
        error("Exception trying to parse extractedText with content.")
        List[Map[String,Any]]()
    }
  }

  def getPageWordsPlainText(plainText: String): List[Map[String,Any]] = {
    val customParser = SAXParserFactory.newInstance
    customParser.setValidating(false)
    customParser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    val saxParser = customParser.newSAXParser()
    // fix all unclosed image tags (Note, this may need to be extended to other types
    val validXmlPlainText = plainText.replaceAll("<img([^/>]*)>", "<img$1></img>")
    val pageRoot = XML.withSAXParser(saxParser).loadString(s"""
          <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
          <plainText>$validXmlPlainText</plainText>
          """)
    val pageStyle = pageRoot \ "div" find { _ \ "@class" exists (_.text == "page") } match {
      case Some(pageNode) => pageNode.attribute("style") match {
        case Some(styleAttribute) => styleAttribute.text
        case None => ""
      }
      case None => ""
    }
    val pageDetails = pageStyle.split(";")
      .filter(pageDetailPart => pageDetailPart.split(":").length > 1)
      .map(pageDetail => {
          pageDetail.split(':')(0) -> pageDetail.split(':')(1)
      }).toMap

    val pageWidth = pageDetails("width").replace("pt","").toFloat
    val pageHeight = pageDetails("height").replace("pt","").toFloat

    (pageRoot \\ "div" filter { _ \ "@class" exists (_.text == "p") }).map(ocrWord => {
      val text = ocrWord.text.trim

      val wordDetails: Map[String,String] = ocrWord.attribute("style").get.head.text match {
        case "" => Map()
        case style => style.split(";").map(stylePart => {
          stylePart.split(":")(0) -> stylePart.split(":")(1)
        }).toMap
      }

      // TODO: Is line height needed for anything?
      // val lineHeight = wordDetails("line-height").replace("pt","").toFloat
      val fontSize = wordDetails("font-size").replace("pt","").toFloat.toInt

      // If we have width, use it. Otherwise, calculate the word width
      val width = if (wordDetails.contains("width")) {
        wordDetails("width").replace("pt","").toFloat
      } else {
        val fontFamily = wordDetails.getOrElse("font-family", "Helvetica")
        val fontWeight: Int = if (wordDetails.contains("font-weight") && wordDetails("font-weight") == "bold") {
          Font.BOLD
        } else {
          Font.PLAIN
        }
        val font = new Font(fontFamily, fontWeight, fontSize)
//        val height = font.getStringBounds(text, frc).getHeight.toInt
        font.getStringBounds(text, frc).getWidth.toFloat
      }
      Map[String,Any](
        "text" -> text,
        "left" -> wordDetails("left").replace("pt","").toFloat / pageWidth,
        "top" -> wordDetails("top").replace("pt","").toFloat / pageHeight,
        "width" -> width / pageWidth,
        "height" -> fontSize / pageHeight
      )
    }).toList
  }


}
