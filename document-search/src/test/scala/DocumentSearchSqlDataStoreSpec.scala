import com.apixio.app.documentsearch.DocumentSearchManager
import com.apixio.scala.dw.{ApxConfiguration, ApxServices, Utility}
import com.fasterxml.jackson.databind.ObjectMapper
import scalikejdbc.ConnectionPool

import scala.util.{Failure, Success, Try}

object DocumentSearchSqlDataStoreSpec {
  ApxConfiguration.initializeFromFile("./application-dev.yaml")
  ApxServices.init(ApxConfiguration.configuration.get)
  ApxServices.setupObjectMapper(new ObjectMapper())
  val documentSearchManager = new DocumentSearchManager
  def main(args: Array[String]) {
    val patient = "0cadb499-a985-48f9-b50b-05fd5780d425"
//    val patient = "23d22b9a-86d9-44d1-a281-55e15d00f104"
//    val termList = "reason|cbbc061c-5535-400d-9e5f-90ff9e0a0b69|1"
//    val termList = "type 2 diabetes,Hospice Care Plan|b8a91cad-db90-4009-bea3-dfbc74374a00|38,Hospice Care Plan|b8a91cad-db90-4009-bea3-dfbc74374a00|18"
    val termList = "patient's daughter,test,test1,test2,test3,test4,test5,test6,test7,test8,test9"
    //  val termList = "Hospice Care|3f5bbb0d-3c8c-4abf-b0fe-517c0d580aa0|15,order|cad1f867-f856-40bf-adaa-06f14604d0a6|25,test,Hospice|b4ae1a0c-c400-4df9-9bcf-4ff0a901cd2c,Hospice|cad1f867-f856-40bf-adaa-06f14604d0a6"
    ////    val termList = "order"
    Class.forName("org.sqlite.JDBC").newInstance()
    ConnectionPool.singleton("jdbc:sqlite:db/completeness.db", null, null)

    Try {
      documentSearchManager.searchPatientSnippets(patient, termList)
    } match {
      case Success(results) => {
        println("Search Patient Snippets: " + Utility.toJsonResponse(results).getEntity)
      }
      case Failure(f) => println("Error searching patients: " + f)
    }

    Try {
      documentSearchManager.searchPatientOverlays(patient, termList)
    } match {
      case Success(results) => {
        println("Search Patient Overlays: " + Utility.toJsonResponse(results).getEntity)
      }
      case Failure(f) => println("Error searching patients: " + f)
    }
  }
}