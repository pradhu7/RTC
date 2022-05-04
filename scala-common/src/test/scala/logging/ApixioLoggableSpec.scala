package com.apixio.scala.logging

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.JavaConverters._

class ApixioLoggableSpec extends AnyFlatSpec with Matchers {

  def resetGlobalLogging() = ApixioLoggable.initializeLogging((s) => s match { case _ => None })

  "ApixioLoggable when being tracked" should "correctly setup the app container for an event" in {
    ApixioLoggable.initializeLogging((s) => s match {
      case "prefix" => Some("test.testingapp")
      case _ => None
    })
    val en = "test_container"
    class TestContainer(n:String) extends TrackApixioLoggable {
      setupLog(this.getClass.getSimpleName)
      event(Map[String,Object]("stuff" -> "someValue").asJava, eventName = en)
    }
    val t = new TestContainer(en)
    t.errors.size should be (0)
    t.warns.size should be (0)
    t.debugs.size should be (0)
    t.infos.size should be (0)
    t.events.size should be (1)
    val ed = t.events(0)._2
    ed("app.app_name") should be ("testingapp")
    ed("app.app_data.event_name") should be (en)
    ed("app.app_data.event_data.stuff") should be ("someValue")
  }

  it should "return a transformed value" in {
    resetGlobalLogging()
    val en = "test_transform"
    case class ReturnedValue(value:String)
    case class TransformedValue(value:String)
    class TestTransform(n:String) extends TrackApixioLoggable {
      setupLog(this.getClass.getSimpleName)
      def testFunc(foo:String, bar:String) =
        withLogging(en, Map("foo" -> foo, "bar" -> bar)) {
          Some(ReturnedValue(n))
        } {a => TransformedValue(a.value) }
    }
    val t = new TestTransform("theValue")
    val ret = t.testFunc("aValue", "anotherValue")
    ret should be (TransformedValue("theValue"))
    t.errors.size should be (0)
    t.warns.size should be (0)
    t.debugs.size should be (0)
    t.infos.map(_._2) should be (List(s"Starting: ${en}", s"Ending: ${en}"))
    t.events.size should be (1)
    val ed = t.events(0)._2
    ed("app.app_name") should be ("unknown")
    ed("app.app_version") should be ("unknown")
    ed("app.app_data.event_name") should be (en)
    ed("app.app_data.event_data.foo") should be ("aValue")
    ed("app.app_data.event_data.bar") should be ("anotherValue")
  }

  it should "correctly attach new metrics in withLogging" in {
    resetGlobalLogging()
    val en = "test_attach"
    class TestAttach(n:String) extends TrackApixioLoggable {
      setupLog(this.getClass.getSimpleName)
      def testFunc(foo:String, bar:String) =
        withLogging[Unit,Unit](en, Map("foo" -> foo, "bar" -> bar)) {
          pushMetric("stuff", "someValue")
          Some(Unit)
        } {a => a}
    }
    val t = new TestAttach("theValue")
    val ret = t.testFunc("aValue", "anotherValue")
    t.errors.size should be (0)
    t.warns.size should be (0)
    t.debugs.size should be (0)
    t.infos.map(_._2) should be (List(s"Starting: ${en}", s"Ending: ${en}"))
    t.events.size should be (1)
    val ed = t.events(0)._2
    ed("app.app_name") should be ("unknown")
    ed("app.app_version") should be ("unknown")
    ed("app.app_data.event_name") should be (en)
    ed("app.app_data.event_data.foo") should be ("aValue")
    ed("app.app_data.event_data.bar") should be ("anotherValue")
    ed("app.app_data.event_data.stuff") should be ("someValue")
  }

  it should "correctly handle an event within withLogging" in {
    resetGlobalLogging()
    val en = "test_double"
    class TestDouble(n:String) extends TrackApixioLoggable {
      setupLog(this.getClass.getSimpleName)
      def testFunc(foo:String, bar:String) =
        withLogging[Unit,Unit](en, Map("foo" -> foo, "bar" -> bar)) {
          event(Map[String,Object]("secondstuff" -> "2").asJava, eventName = "test_double_before")
          pushMetric("stuff", "someValue")
          Some(Unit)
        } {a => a}
    }
    val t = new TestDouble("theValue")
    val ret = t.testFunc("aValue", "anotherValue")
    t.errors.size should be (0)
    t.warns.size should be (0)
    t.debugs.size should be (0)
    t.infos.map(_._2) should be (List(s"Starting: ${en}", s"Ending: ${en}"))
    t.events.size should be (2)
    val ed1 = t.events(0)._2
    val ed2 = t.events(1)._2
    ed1("app.app_name") should be ("unknown")
    ed1("app.app_version") should be ("unknown")
    ed1("app.app_data.event_name") should be ("test_double_before")
    ed1("app.app_data.event_data.secondstuff") should be ("2")

    ed2("app.app_name") should be ("unknown")
    ed2("app.app_version") should be ("unknown")
    ed2("app.app_data.event_name") should be (en)
    ed2("app.app_data.event_data.foo") should be ("aValue")
    ed2("app.app_data.event_data.bar") should be ("anotherValue")
    ed2("app.app_data.event_data.stuff") should be ("someValue")
  }

  it should "correctly handle errors in withLogging" in {
    resetGlobalLogging()
    val en = "test_error"
    class TestError(n:String) extends TrackApixioLoggable {
      setupLog(this.getClass.getSimpleName)
      def testError(foo:String, bar:String) =
        withLogging[Unit,Unit](en, Map("foo" -> foo, "bar" -> bar)) {
          throw new IllegalArgumentException("Test")
        } {a => a}
    }
    val t = new TestError("theValue")
    t.testError("aValue", "anotherValue")
    t.errors.size should be (1)
    t.warns.size should be (0)
    t.debugs.size should be (0)
    t.infos.map(_._2) should be (List(s"Starting: ${en}", s"Ending: ${en}"))
    t.events.size should be (1)
  }

  it should "correctly handle fatal errors in withLogging" in {
    resetGlobalLogging()
    val en = "test_fatal_error"
    class TestError(n:String) extends TrackApixioLoggable {
      setupLog(this.getClass.getSimpleName)
      def testError(foo:String, bar:String) =
        withLogging[Unit,Unit](en, Map("foo" -> foo, "bar" -> bar)) {
          throw new OutOfMemoryError("Test")
        } {a => a}
    }
    val t = new TestError("theValue")
    assertThrows[OutOfMemoryError] {
      t.testError("aValue", "anotherValue")
    }
  }

  it should "correctly attach user info if it is provided" in {
    resetGlobalLogging()
    val en = "test_user_info"
    class TestAttach(n:String) extends TrackApixioLoggable {
      setupLog(this.getClass.getSimpleName)
      def testFunc(foo:String, bar:String) =
        withLogging[Unit,Unit](en, Map("foo" -> foo, "bar" -> bar), Map("user" -> "test@apixio.com", "user_agent" -> "Test Agent", "ip" -> "ip-127-0-0-1")) {
          Some(Unit)
        } {a => a}
    }
    val t = new TestAttach("theValue")
    val ret = t.testFunc("aValue", "anotherValue")
    t.errors.size should be (0)
    t.warns.size should be (0)
    t.debugs.size should be (0)
    t.infos.map(_._2) should be (List(s"Starting: ${en}", s"Ending: ${en}"))
    t.events.size should be (1)
    val ed = t.events(0)._2
    ed("app.app_name") should be ("unknown")
    ed("app.app_version") should be ("unknown")
    ed("app.app_user_info.user") should be ("test@apixio.com")
    ed("app.app_user_info.user_agent") should be ("Test Agent")
    ed("app.app_user_info.ip") should be ("ip-127-0-0-1")
    ed("app.app_data.event_name") should be (en)
    ed("app.app_data.event_data.foo") should be ("aValue")
    ed("app.app_data.event_data.bar") should be ("anotherValue")
  }
}
