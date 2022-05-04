package apxapi

import com.apixio.scala.apxapi.{ApxApi, AuthSpec, CallProxy, CircuitBreakerProxy}
import com.apixio.scala.dw.{ApxConfiguration, ApxServices}
import com.fasterxml.jackson.databind.ObjectMapper
import org.scalatest.PrivateMethodTester
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ApxApiSpec extends AnyFlatSpec with Matchers with AuthSpec {
  "Integration tests for ApxApi" should "correctly setup" in {
    ApxConfiguration.initializeFromFile("application-dev.yaml")
    ApxServices.init(ApxConfiguration.configuration.getOrElse(ApxConfiguration(Map())))
    ApxServices.setupApxLogging()
    ApxServices.setupObjectMapper(new ObjectMapper())
  }

  it should "set up circuit breaker with default values if no config override is set" in {
    val name = "doesnotexist"
    val cb = ApxApi.getCircuitBreakerConfig(name, name)
    assert(cb.name == name)
    assert(cb.callTimeout == 10)
    assert(cb.resetTimeout == 3)
    assert(cb.maxFailures == 5)
  }

  it should "set up circuit breaker with config values if set" in {
    val name = "existingservice"
    val cb = ApxApi.getCircuitBreakerConfig(name, name)
    assert(cb.name == name)
    assert(cb.callTimeout == 100)
    assert(cb.resetTimeout == 15)
    assert(cb.maxFailures == 2)
  }

  it should "load microservices from config" in {
    val services = ApxApi.loadServices()
    assert(services.keySet.contains("mapping"))
  }

  it should "not use circuitbreaker config if disabled" in {
    ApxServices.configuration.setFeatureFlagsConfig(Map("feature" -> Map("circuitBreaker" -> "false")))
    ApxApi.reload()
    val useraccountSvc = ApxApi.service(ApxApi.USERACCOUNTS)
    assert(useraccountSvc.callProxy.isInstanceOf[CallProxy])
    assert(!useraccountSvc.callProxy.isInstanceOf[CircuitBreakerProxy])

    val apxapi = login(coder1)
    // validate that the calls to services work with the new callproxy
    assert(apxapi.useraccounts.getUser().accountState == "ACTIVE")
  }
}
