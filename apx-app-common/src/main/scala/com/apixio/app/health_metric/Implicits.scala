package com.apixio.app.health_metric

import akka.actor.{ActorContext, ActorSystem}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives.{complete, get, onComplete, path}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.{ConnectionContext, Http}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.apixio.app.commons.utils.{ActorResolver, ConfigUtils}
import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.typesafe.config.Config
import org.json4s.JsonAST.{JNothing, JObject, JString}
import org.json4s.jackson.Json4sScalaModule
import org.json4s.jackson.JsonMethods.compact
import org.json4s.{DefaultFormats, FileInput, Formats, JValue, JsonInput, Reader, ReaderInput, StreamInput, StringInput, Writer}
import sun.management.VMManagement

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success
import scala.util.control.Exception.allCatch

trait JsonMethods2 extends org.json4s.JsonMethods[JValue] {

  private[this] lazy val _defaultMapper = {
    val m = new ObjectMapper()
    m.registerModule(new Json4sScalaModule)
    // for backwards compatibility
    m.configure(USE_BIG_INTEGER_FOR_INTS, true)
    m
  }
  def mapper = _defaultMapper

  def parse(in: JsonInput, useBigDecimalForDouble: Boolean = false, useBigIntForLong: Boolean = true): JValue = {
    var reader = mapper.reader(classOf[JValue])
    if (useBigIntForLong) reader = reader `with` USE_BIG_INTEGER_FOR_INTS

    in match {
      case StringInput(s) => reader.readValue(s)
      case ReaderInput(rdr) => reader.readValue(rdr)
      case StreamInput(stream) => reader.readValue(stream)
      case FileInput(file) => reader.readValue(file)
    }
  }

  def parseOpt(in: JsonInput, useBigDecimalForDouble: Boolean = false, useBigIntForLong: Boolean = true): Option[JValue] = allCatch opt {
    parse(in, useBigDecimalForDouble, useBigIntForLong)
  }

  def render(value: JValue)(implicit formats: Formats = DefaultFormats): JValue =
    formats.emptyValueStrategy.replaceEmpty(value)

  def compact(d: JValue): String = mapper.writeValueAsString(d)

  def pretty(d: JValue): String = {
    val writer = mapper.writerWithDefaultPrettyPrinter()
    writer.writeValueAsString(d)
  }


  def asJValue[T](obj: T)(implicit writer: Writer[T]): JValue = writer.write(obj)
  def fromJValue[T](json: JValue)(implicit reader: Reader[T]): T = reader.read(json)

  def asJsonNode(jv: JValue): JsonNode = mapper.valueToTree[JsonNode](jv)
  def fromJsonNode(jn: JsonNode): JValue = mapper.treeToValue[JValue](jn, classOf[JValue])

}
object JsonMethods2 extends JsonMethods2

object Implicits {
  lazy val pid = getPidOfProcess

  implicit class HealthMetricConfig(appConfig: Config) {
    def initHTTPAdmin()(implicit mat: ActorMaterializer, ec: ExecutionContext, actorResolver: ActorResolver, timeout: Timeout = Timeout(30 seconds)): Option[Future[Http.ServerBinding]] = {
      implicit val system: ActorSystem = mat.system
      val httpConfig = appConfig.getConfig("http_admin")
      if (httpConfig.getBoolean("enabled")) {
        val server = httpConfig.getString("server")
        val port = httpConfig.getInt("port")
        import ConfigUtils._
        val appPort = appConfig.getIntOpt("http.port").map(_.toString).getOrElse("none")
        import collection.JavaConverters._
        val endpoints = appConfig.getStringList("http_admin_endpoints").asScala.toList
        val app_info = JsonMethods2.parse(s"""
                                |{
                                |        "Admin_Port": "$port",
                                |        "Port": "$appPort",
                                |        "Pid": "$pid"
                                |}
              """.stripMargin, false).asInstanceOf[JObject]

        val route = Route.seal(
          get {
            path("healthcheck") {
              onComplete {
                val futures = endpoints.collect {
                  case metricName if actorResolver.resolve(HealthMetricActor.actorName).nonEmpty =>
                    import com.apixio.app.commons.utils.Implicit._
                    (actorResolver.resolve(HealthMetricActor.actorName) ? HealthMetricActor.GetMetricByBucket(metricName)).mapTo[HealthMetricActor.LoadAvgBucket].map { b =>
                      b.metricName -> JString(b.status)
                    }

                  case metricName => Future(metricName -> JString("Unknown"))
                }
                Future.sequence(futures)
              } {
                case Success(list) =>
                  val r = JObject("Application_Health" -> app_info, "Application_Connections" -> JObject(list))
                  complete(HttpEntity(ContentTypes.`application/json`, compact(r)))

                case _ =>   complete(HttpEntity(ContentTypes.`application/json`, compact(JNothing)))
              }
            }
          }
        )
        val connectionContext = SSLConnection.createHTTPSConnectionContext(httpConfig).getOrElse(ConnectionContext.noEncryption())
        Some(Http().bindAndHandle(route, server, port, connectionContext))
      } else Option.empty
    }

    def addHealthMetricService()(implicit context: ActorContext): Unit = {
      context.actorOf(HealthMetricActor.props(appConfig), HealthMetricActor.actorName)
    }
  }



  private def getPidOfProcess: Integer = {
    val runtime = java.lang.management.ManagementFactory.getRuntimeMXBean
    val jvm = runtime.getClass.getDeclaredField("jvm")
    jvm.setAccessible(true)
    val mgmt = jvm.get(runtime).asInstanceOf[VMManagement]
    val pid_method = mgmt.getClass.getDeclaredMethod("getProcessId")
    pid_method.setAccessible(true)

    pid_method.invoke(mgmt).asInstanceOf[Integer]
  }
}
