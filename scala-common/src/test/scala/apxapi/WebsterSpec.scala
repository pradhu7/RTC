package apxapi

import com.apixio.scala.apxapi.ApxApi
import com.apixio.scala.dw.{ApxConfiguration, ApxServices}
import com.fasterxml.jackson.databind.ObjectMapper
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.util.{Failure, Success, Try}

class WebsterSpec extends AnyFlatSpec with Matchers {

  def getApxConfiguration: ApxConfiguration = {
    ApxConfiguration.initializeFromFile("application-dev.yaml")
    ApxConfiguration.configuration match {
      case Some(configuration) => {
        ApxServices.init(configuration)
        ApxServices.setupObjectMapper(new ObjectMapper())
        configuration
      }
      case None => throw new Exception("Failed to load configuration")
    }
  }
  
  val apxapi = new ApxApi(getApxConfiguration)

  "GetLatestSchema" should "bea able to load the latest schema " in {
    Try {
      apxapi.webster.getLatestSchema()
    } match {
      case Success(s) => {
        assert(s.nonEmpty)
        println(s)
      }
      case Failure(exception) => exception
    }
  }

  "GetLatestSchema" should "bea able to load the latest full schema " in {
    Try {
      apxapi.webster.getLatestSchema(fullSchema = true)
    } match {
      case Success(s) => {
        assert(s.nonEmpty)
        println(s)
      }
      case Failure(exception) => exception
    }
  }

  "GetSchemaById" should "bea able to load the facts schema using id " in {
    Try {
      apxapi.webster.getSchema(1)
    } match {
      case Success(s) => {
        assert(s.nonEmpty)
        println(s)
      }
      case Failure(exception) => exception
    }
  }

  "GetSchemaById" should "be able to load the full facts schema using id " in {
    Try {
      apxapi.webster.getSchema(1, fullSchema = true)
    } match {
      case Success(s) => {
        assert(s.nonEmpty)
        println(s)
      }
      case Failure(exception) => exception
    }
  }

}
