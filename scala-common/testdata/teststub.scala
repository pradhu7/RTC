
import com.apixio.scala.subtraction._
import com.apixio.model.profiler._
import com.apixio.model.event.transformer.EventTypeListJSONParser
import com.apixio.scala.apxapi.Project
import scala.collection.JavaConverters._

val proj = Project(0.0, "", "", "2015-12-31", "2015-01-01", "", "", "", "", "", "", "", "", "2017", "", "", Map.empty, 0.0, 0.0, "", true, "", "hcc")

val pats = List("01d09676-0b39-4c4c-837b-19cf2df512d0","09e28b21-c913-4c01-b8d6-f9af41ed54df","0ad1487f-c14b-4486-8c12-17b637e67d1c","0b001ff9-795f-435c-972e-ac20b0f513cf","2c5522c6-4cdf-41ac-aa90-072ceb388cc8","3f6fdf0e-1d63-481a-8c3b-f5790daae614","4143b23b-9e4d-406f-812e-88a663f1601d","49273775-29a1-4dc1-af60-1beb386e6ff6","4dc2b24d-3ba9-4095-8351-bb9d462def52","542b71f7-1a35-4435-8aa8-e80116ac25fc","544f708e-3312-4b76-b859-80b654269d30","581448bd-be37-4ad6-bd93-fff234b452bb","5f42c7e3-f978-4561-b238-2d35c17eaaf0","62c4dc94-f87c-4457-9435-53b5c8fc1dc7","6cf5a694-1f02-4d16-96db-0f59ed6f46bd","6d031a23-54bd-48ba-a1d2-7a5de63e8940","6d1ae9d3-81ba-49c1-ba6c-2b81ca92921d","7be08757-acd3-4cdb-ba68-18a3982f8489","82743e44-97f6-44ed-85bb-c1d457f06919","86f004b7-f2fc-465b-a9d9-6eba39ecd366","87cbe4ee-26a6-4b8f-8119-955d1cc77b91","9bfd917e-7dbe-42f9-95a8-58bb4a310aad","a1ec4a09-f948-44dd-ad5b-3e25a39a5591","a3925663-370f-433b-a6fe-fb97540ebeff","a8ff5f6a-6190-4615-b773-2fa93e9b6233","b019d2b6-3a83-4df3-9812-5518a1f53d5b","b607c1ed-de44-4fc0-bcb1-e463e6bbe9ff","b7e8dd99-6d30-489a-a5fc-997b76d0b61f","b9a39eda-51c6-41a4-9ab9-8a262918fc08","c1ba53b3-d5a7-4633-91df-d07580f6cfcb","ce1f4663-f130-4ffd-b3a6-0df608271dea","d48d6fe8-5e84-4958-b247-22e84bc7a857","d6c1f27d-4288-43ff-816e-8f417bf018f8","d791b5a1-7039-455a-9194-f47080888479","e711b1ad-76d2-4af2-934d-153613e6e811")

class TestRAPSSubtraction(proj: Project, pat: String) extends RAPSSubtraction(proj: Project, pat: String) {
  override def events() : List[SubtractionEvent] = {
    process(filter((new EventTypeListJSONParser()).parseEventsData(scala.io.Source.fromFile(s"testdata/${pat}.raclaim").mkString("")).asScala.toList.map(EventTypeX(_)).filter(e => e.fact.time.end.compareTo(proj.end) <= 0 && e.fact.time.end.compareTo(proj.start) >= 0)))
  }
}

class TestMAOSubtraction(proj: Project, pat: String) extends MAOSubtraction(proj: Project, pat: String) {
  override def getEvents() : List[EventTypeX] =
    (new EventTypeListJSONParser()).parseEventsData(scala.io.Source.fromFile(s"testdata/${pat}.raclaim").mkString("")).asScala.toList.map(EventTypeX(_))
}

val legacyPats = List("0ea589f0-a51a-4822-8137-d4074f74dac0","187d8b38-de81-4226-b144-999fdac022a0","2702949c-7c74-44e5-bc48-08f1a2aab0db","293c36f5-8c88-40af-ae0e-3aaab646b282","3972f963-bb0a-413d-acc8-a4a7ba9069b9","3f413e0c-8f88-45dd-b3ca-9f88b8dded08","49aa2440-6225-4bf4-b922-02e6d387125e","4c354823-8b1d-43ca-87d3-6e7971bed8fd","69cee31f-bdd3-402e-9aca-34da7059b2bf","6e8bb5bd-2048-42dd-837f-d9114fcf425c","8617f976-0abd-48e4-8b21-792899e1a15a","8dbc3e1e-65c6-4aab-8314-c633f35e6212","ab4d9f34-1bb2-46ef-9b3a-2bb1bfe1ff6a","b4071bcd-f1f8-4dad-b214-4758d4328fb9","bdc3211b-6553-4885-a992-8412d38eeccc","c7c06d0a-8fb5-47f7-8fb4-ccaac161813a","e059ac28-e437-4107-9b46-d378ac1def00","f1fa3e82-af81-4db7-9543-ad470499684e","f6c4f2ec-f928-474b-80bf-f35fa7745255","fb2daaf2-8bc2-430c-b01a-dab1fadffe37","fec0d20c-3185-4a64-ac4a-fe437aa46b41")

class TestLegacySubtraction(proj: Project, pat: String) extends LegacySubtraction(proj: Project, pat: String) {
  override def events() : List[SubtractionEvent] = {
    process(filter((new EventTypeListJSONParser()).parseEventsData(scala.io.Source.fromFile(s"testdata/${pat}.legacy").mkString("")).asScala.toList.map(EventTypeX(_))))
  }
}

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.ScalaObjectMapper
implicit val mapper = {
  val m = new ObjectMapper() with ScalaObjectMapper
  m.registerModule(DefaultScalaModule)
  m.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  m.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
  m
}
CodeMapping.init()
HccModel.init()


val allres = legacyPats.map(p => Subtraction(proj, p).process((new TestLegacySubtraction(proj, p)).events()))
allres.foreach{ e => println(e.map(_.fact.code.key).toSet)}

val allres = pats.map(p => (p -> Subtraction(proj, p).process((new TestRAPSSubtraction(proj, p)).events()))).toMap
allres.foreach{ e => println(e._1 + " " + e._2.map(_.fact.code.key).toSet)}

val allres = pats.map(p => (p -> Subtraction(proj, p).process((new TestMAOSubtraction(proj, p)).events()))).toMap
allres.foreach{ e => println(e._1 + " " + e._2.map(_.fact.code.key).toSet)}


