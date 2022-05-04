package com.apixio.app.consul

import akka.NotUsed
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.javadsl.RunnableGraph
import akka.stream.scaladsl.{Flow, GraphDSL, Sink, Source}
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.util.ByteString
import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.typesafe.config.Config
import org.json4s._
import org.json4s.jackson.Json4sScalaModule

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.Exception.allCatch

case class ServiceEndpoint(nodeName: String, server: String, port: Int)

object ServiceEndpoint {
  def apply(name: String, config: Config): ServiceEndpoint = {
    ServiceEndpoint(name, config.getString("server"), config.getInt("port"))
  }
}

trait JsonMethods2 extends org.json4s.JsonMethods[JValue] {

  private[this] lazy val _defaultMapper = {
    val m = new ObjectMapper()
    m.registerModule(new Json4sScalaModule)
    // for backwards compatibility
    m.configure(USE_BIG_INTEGER_FOR_INTS, true)
    m
  }
  def mapper: ObjectMapper = _defaultMapper

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


object ConsulUtils {
  def getServiceEndpoints(consulServer:ConsulEndpoint, serviceName: String)(implicit mat: ActorMaterializer, ec: ExecutionContext): Future[List[ServiceEndpoint]] = {
    val source = Source.single(serviceName)
    val sink = Sink.seq[ServiceEndpoint]
    val graph = GraphDSL.create(source, sink)((_, _)) { implicit builder => (sr, sk) =>
      import GraphDSL.Implicits._

      val genHttpRequest = Flow[String].map { serviceName =>
        HttpRequest(uri = consulServer.toServiceURI(serviceName))
      }
      val runHttp: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] = Http(mat.system).outgoingConnection(consulServer.server, consulServer.port)
      val toDataBytes: Flow[HttpResponse, String, NotUsed] = Flow[HttpResponse].mapAsync(1) { response =>
        response.entity.getDataBytes().runWith(Sink.seq[ByteString], mat).map(f => f.map(_.utf8String).mkString) //bring together all the chunks.
      }

      val toJSON: Flow[String, JValue, NotUsed] = Flow[String].map { data => JsonMethods2.parse(data, useBigDecimalForDouble = false) }

      val takeJArray = Flow[JValue].collect{ case v: JArray => v}

      val filterElts = Flow[JArray].mapConcat(elts => elts.arr.collect { case jobj: JObject if jobj.values.contains("Node") => jobj })

      val fetchServiceAddressAndPort = Flow[JObject].mapConcat { v =>
        val map = v.values
        (map.get("Node"), map.get("ServiceAddress"), map.get("ServicePort")) match {
          case (Some(node: String), Some(addr: String), Some(port: BigInt)) => List(ServiceEndpoint(node, addr, port.toInt)) //not using option here because mapConcat uses Iterator.
          case _ => List.empty
        }
      }

      sr ~> genHttpRequest ~> runHttp ~> toDataBytes ~> toJSON ~> takeJArray ~> filterElts ~> fetchServiceAddressAndPort ~> sk

      ClosedShape
    }
    RunnableGraph.fromGraph(graph).run(mat)._2.map(_.toList)
  }

  def getServices(consulServer:ConsulEndpoint)(implicit mat: ActorMaterializer, ec: ExecutionContext): Future[Seq[String]] = {
    val source = Source.single(HttpRequest(uri = consulServer.toServicesURI))
    val sink = Sink.head[Seq[String]]
    val graph = GraphDSL.create(source, sink)((_, _)) { implicit builder => (sr, sk) =>
      import GraphDSL.Implicits._
      val runHttp: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] = Http(mat.system).outgoingConnection(consulServer.server, consulServer.port)
      val toDataBytes: Flow[HttpResponse, ByteString, NotUsed] = Flow[HttpResponse].mapAsync(1) { response =>
        response.entity.getDataBytes().runWith(Sink.head[ByteString], mat) //bring together all the chunks.
      }

      val toJSON: Flow[ByteString, JValue, NotUsed] = Flow[ByteString].map { data =>
        JsonMethods2.parse(data.utf8String, useBigDecimalForDouble = false)
      }

      val takeJObject = Flow[JValue].collect{ case v: JObject => v}

      val toServices = Flow[JObject].map(elt => elt.values.keys.toList)

      sr ~> runHttp ~> toDataBytes ~> toJSON ~> takeJObject ~> toServices ~> sk

      ClosedShape
    }
    RunnableGraph.fromGraph(graph).run(mat)._2
  }
}
