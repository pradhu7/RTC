// disabling this test for now... most of this stuff is handled by k8s/envoy these days
/*package apxapi

import com.apixio.scala.dw.{ApxConfiguration, ApxServices}
import com.fasterxml.jackson.databind.ObjectMapper
import com.apixio.scala.apxapi.ConsulDao
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ConsulDaoTest extends AnyFlatSpec with Matchers {

  "Integration tests for ConsulDao" should "correctly setup" in {
    ApxConfiguration.initializeFromFile("application-dev.yaml")
    ApxServices.init(ApxConfiguration.configuration.get)
    ApxServices.setupApxLogging()
    ApxServices.setupObjectMapper(new ObjectMapper())
  }

  it should "set up bundler circuit breaker" in {
    val name = "bundler"
    val cb = ConsulDao.getCircuitBreakerConfig(name, name)
    assert(cb.name == name)
    assert(cb.callTimeout == 10)
    assert(cb.resetTimeout == 1)
    assert(cb.maxFailures == 5)
  }

  it should "set up profiler circuit breaker" in {
    val name = "profiler"
    val cb = ConsulDao.getCircuitBreakerConfig(name, name)
    assert(cb.name == name)
    assert(cb.callTimeout == 5)
    assert(cb.resetTimeout == 1)
    assert(cb.maxFailures == 5)
  }

  it should "set up cmp circuit breaker" in {
    val name = "cmp"
    val cb = ConsulDao.getCircuitBreakerConfig(name, name)
    assert(cb.name == name)
    assert(cb.callTimeout == 5)
    assert(cb.resetTimeout == 1)
    assert(cb.maxFailures == 5)
  }

  it should "set up appdashboard circuit breaker" in {
    val name = "appdashboard"
    val cb = ConsulDao.getCircuitBreakerConfig(name, name)
    assert(cb.name == name)
    assert(cb.callTimeout == 60)
    assert(cb.resetTimeout == 1)
    assert(cb.maxFailures == 5)
  }

  it should "set up an un-configured circuit breaker" in {
    val name = "some-name"
    val cb = ConsulDao.getCircuitBreakerConfig(name, name)
    assert(cb.name == name)
    assert(cb.callTimeout == 10)
    assert(cb.resetTimeout == 3)
    assert(cb.maxFailures == 5)
  }

}
*/
