package com.apixio.app.consul

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest._
import matchers.should._

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class ConfigDiscoverySpec extends AnyFlatSpec with Matchers with GivenWhenThen with BeforeAndAfterAll {
  implicit val system: ActorSystem = ActorSystem("ConfigDiscoverySpec")
  implicit val mat: ActorMaterializer = ActorMaterializer()

  import system.dispatcher
  implicit val dummyGetServiceEndpoints: (ConsulEndpoint, String) => Future[List[ServiceEndpoint]] = {
    case (_, "redis") => Future (List(ServiceEndpoint("redis", "1.1.1.1", 6450)))
    case (_, "elasticsearch") => Future (List(ServiceEndpoint("elasticsearch", "1.1.1.2", 1234)))
    case (_, "crdb") => Future (List(ServiceEndpoint("crdb", "1.1.1.3", 1111)))
    case (_, "cassandra") => Future (List(ServiceEndpoint("cassandra", "1.1.1.5", 1112)))
  }

  "NoConsulConfig" should "perform noop" in {
    val config =
      """
        |sample_cfg {
        |
        |}
      """.stripMargin

    Given(config)
    val cfg = ConfigFactory.parseString(config).getConfig("sample_cfg")

    When("Consul startup discovery is called")
    import Implicit._
    val result = cfg.addStartupEndpoints()

    Then("config is retained")
    import com.apixio.app.commons.utils.ConfigUtils._
    result.getChildren.isEmpty should be(true)
  }

  "ServerPort Config Discovery" should "discover" in {
    val config =
      """
        |some_config {
        |   redis {
        |      prefix = "development-"
        |      server = ""
        |      port = 0
        |   }
        |
        |   consul {
        |     enable_discovery = true
        |     startup_endpoints {
        |       "redis2" {
        |         name = "redis"
        |         server_path = "redis.server"
        |         port_path = "redis.port"
        |       }
        |     }
        |   }
        |}
      """.stripMargin

    Given(config)
    val cfg = ConfigFactory.parseString(config).getConfig("some_config")

    When("Consul startup discovery is called with intent to discover redis.")
    import Implicit._
    val result = cfg.addStartupEndpoints()

    Then("config is updated with redis changes: redis.server = 1.1.1.1")
    cfg.getString("redis.server") should be("")
    result.getString("redis.server") should be("1.1.1.1")
    result.getInt("redis.port") should be(6450)
  }

  "Pattern Config Discovery" should "discover" in {
    val config =
      """
        |some_config {
        |   test {
        |     driverURL = "jdbc:postgresql://${server}:${port}/metric?sslmode=disable"
        |   }
        |
        |   consul {
        |     enable_discovery = true
        |     startup_endpoints {
        |       "db" {
        |         name = "crdb"
        |         pattern_path = "test.driverURL"
        |       }
        |     }
        |   }
        |}
      """.stripMargin

    Given(config)
    val cfg = ConfigFactory.parseString(config).getConfig("some_config")

    When("Consul startup discovery is called with intent to discover redis.")
    import Implicit._
    val result = cfg.addStartupEndpoints()

    Then("config is updated with driverURL = jdbc:postgresql://1.1.1.3:1111/metric?sslmode=disable")
    cfg.getString("test.driverURL") should be("jdbc:postgresql://${server}:${port}/metric?sslmode=disable")
    result.getString("test.driverURL") should be("jdbc:postgresql://1.1.1.3:1111/metric?sslmode=disable")
  }

  override protected def afterAll(): Unit = {
    system.terminate()
  }
}