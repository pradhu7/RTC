import java.io.{FileNotFoundException, InputStream}
import java.net.{HttpURLConnection, URL}

import com.apixio.app.documentsearch.DocumentSearchManager
import com.apixio.app.documentsearch.RealTimeDocumentSearchManager
import com.apixio.model.patient.Patient
import com.apixio.model.utility.PatientJSONParser
import com.apixio.scala.dw.{ApxConfiguration, ApxServices}
import com.fasterxml.jackson.databind.ObjectMapper

import scala.collection.JavaConverters._
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import scala.util.{Failure, Success, Try}

object RealTimeDocumentSearchSqlDataStoreSpec {
  ApxConfiguration.initializeFromFile("./application-dev.yaml")
  ApxServices.init(ApxConfiguration.configuration.get)
  val documentSearchManager = new DocumentSearchManager
  val realTimeDocumentSearchManager = new RealTimeDocumentSearchManager
  def main(args: Array[String]) {
//    val patient = "0cadb499-a985-48f9-b50b-05fd5780d425"
////    val patient = "23d22b9a-86d9-44d1-a281-55e15d00f104"
////    val termList = "reason|cbbc061c-5535-400d-9e5f-90ff9e0a0b69|1"
////    val termList = "type 2 diabetes,Hospice Care Plan|b8a91cad-db90-4009-bea3-dfbc74374a00|38,Hospice Care Plan|b8a91cad-db90-4009-bea3-dfbc74374a00|18"
//    val termList = "patient's daughter,test,test1,test2,test3,test4,test5,test6,test7,test8,test9"
//    //  val termList = "Hospice Care|3f5bbb0d-3c8c-4abf-b0fe-517c0d580aa0|15,order|cad1f867-f856-40bf-adaa-06f14604d0a6|25,test,Hospice|b4ae1a0c-c400-4df9-9bcf-4ff0a901cd2c,Hospice|cad1f867-f856-40bf-adaa-06f14604d0a6"
//    ////    val termList = "order"


    val objectMapper = new ObjectMapper()
    objectMapper.registerModule(DefaultScalaModule)

    // This is a text document
//    val termsByPageByDocument = Map(
//      "6ab15b92-d62a-47c5-bc06-5e6486c235c9" -> Map(
//        "*" -> List("slums")
//      )
//    )
//    val termsByPageByDocument = Map(
//      "12a74d06-95fa-49a4-b6a6-a3e842cbe02a" -> Map(
//        "*" -> List("Brown")
//      )
//    )

    // This is a PDF document
//        val termsByPageByDocument = Map(
//          "a71331e7-30b9-4045-9017-5868c8726f89" -> Map(
//            "5" -> List(
//              "problem",
//              "diabetes"
//            ),
//            "7" -> List(
//              "problem",
//              "diabetes"
//            )
//          )
//        )

    //


    //    val coords = Map(
//      "top" -> 0.0,
//      "left" -> 0.0,
//      "width" -> 1.0,
//      "height" -> 1.0
//    )
//    val documentUuid = "c49203fc-e636-4e4a-9a60-f562db451309"
//    termsByPageByDocument.keys.toList.foreach(docId => {
//      val testDocument = getPatientObjectForDocument(documentUuid)
//      testDocument match {
//        case Some(patient) => {
//          val document = patient.getDocuments.asScala.head
//          val stringContent = document.getStringContent
//          // Seed the cache with data.
//          RealTimeDocumentSearchManager.documentWordsCache.put(documentUuid, RealTimeDocumentSearchManager.getPageWordsFromStringContent(stringContent))
//          val overlays = RealTimeDocumentSearchManager.searchDocumentForCoords(documentUuid, "4", coords)
//
//          println(objectMapper.writeValueAsString(overlays))
//        }
//        case None => {
//          println("No Bueno.")
//        }
//      }
//    })

    // This document required calculation of width
//    val termsByPageByDocument = Map(
//      "8c2119b7-3779-4fc7-9764-7592e4c1da91" -> Map("*" -> List("demographic"))
//    )

    // This is a patient who had unclosed image tags. Now fixed.
//    val termsByPageByDocument = Map(
//      "83a5f6c9-1089-4722-8761-37f947fe2f70" -> Map("*" -> List("mrn")),
//      "9507cd7b-6db0-4eeb-a53b-759dc47f77aa" -> Map("*" -> List("mrn")),
//      "3af5af83-8dd1-4df8-9d08-804e3c9a95e9" -> Map("*" -> List("mrn")),
//      "1204df82-3e00-47eb-9893-b9493611fd92" -> Map("*" -> List("mrn")),
//      "4ff10e71-2445-419f-a301-be759fcdbe7e" -> Map("*" -> List("mrn")),
//      "d93a05b5-6f8c-46f2-8e78-2fa344eba55d" -> Map("*" -> List("mrn")),
//      "142b512a-ea31-47fe-8c71-09307df55cce" -> Map("*" -> List("mrn")),
//      "c75d3b16-cca0-4f5a-8506-bb2957383e75" -> Map("*" -> List("mrn")),
//      "ccc67b12-420e-4ef1-b2fb-66b9055f02da" -> Map("*" -> List("mrn")),
//      "db4605b4-8c72-49b8-b392-7a30129ac1a4" -> Map("*" -> List("mrn")),
//      "8b9c5807-221b-4d98-96fd-c9ea55d25fe0" -> Map("*" -> List("mrn"))
//    )

    val termsByPageByDocument = Map(
      "6bf343a8-34e9-4faf-8a45-ef6bde1002be" -> Map("5" -> List("bmi 41"))
    )
    termsByPageByDocument.keys.toList.foreach(docId => {
      seedCache(docId)
      val testDocument = getPatientObjectForDocument(docId)
      val overlays = realTimeDocumentSearchManager.searchDocumentForTerms(docId, termsByPageByDocument(docId))
      println(objectMapper.writeValueAsString(overlays))
    })
  }

  private def seedCache(documentUuid: String) = {
    println("Seeding document " + documentUuid + " into cache")
    val testDocument = getPatientObjectForDocument(documentUuid)
    testDocument match {
      case Some(patient) => {
        val document = patient.getDocuments.asScala.head
        val stringContent = document.getStringContent
        // Seed the cache with data.
        realTimeDocumentSearchManager.documentContentCache.put(documentUuid, stringContent)
      }
      case None => {
        println("No Bueno.")
      }
    }
  }

  private def getPatientObjectForDocument(docId: String): Option[Patient] = {
    Try {
      val token = "{PUT TOKEN HERE}"
      var urlPath = s"https://accounts.apixio.com/api/dataorch/document/${docId}/apo"
      val url = new URL(urlPath)
      val urlc = url.openConnection.asInstanceOf[HttpURLConnection]
      urlc.setRequestProperty("Authorization", "Apixio " + token)
      urlc.setAllowUserInteraction(false)
      val parser = new PatientJSONParser
      parser.parsePatientData(urlc.getInputStream)
    } match {
      case Success(patient) => {
        Some(patient)
      }
      case Failure(f) => {
        println("Error searching patients: " + f)
        None
      }
    }
  }
}
