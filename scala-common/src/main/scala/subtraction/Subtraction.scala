package com.apixio.scala.subtraction

import com.apixio.model.profiler.{Code, MonthMap, EventTypeX}
import com.apixio.model.utility.Conversions.dateTimeOrdering
import com.apixio.scala.apxapi.Project
import com.apixio.scala.dw.ApxServices
import org.joda.time.{DateTime,DateTimeZone}
import scala.util.parsing.combinator.JavaTokenParsers

//TODO: Should actually use providerType for grouping and forming the key, when we actually trust it
@deprecated("Legacy: see com.apixio.app.model.Subtraction")
case class SubtractionEvent(code: Code, dos: DateTime, transaction: DateTime, providerType: Code, add: Boolean, ineligible: MonthMap, source: String, loaded: DateTime) {
  def key = List(code.key, dos.toDateTime(DateTimeZone.UTC).toString, transaction.toDateTime(DateTimeZone.UTC).toString).mkString(",")

  def or(that: SubtractionEvent) : SubtractionEvent = {
    assert(code == that.code, println(s"${code.key} != ${that.code.key}"))
    assert(dos == that.dos, println(s"${dos} != ${that.dos}"))
    assert(transaction == that.transaction, println(s"${transaction} != ${that.transaction}"))
    assert(add == that.add, println(s"this and that have different transaction type"))
    assert(providerType == null || that.providerType == null || providerType == that.providerType, println(s"providerType mismatch: ${providerType} ${that.providerType}"))
    val m = new MonthMap(ineligible.startRaw, ineligible.endRaw)
    m.load(that.ineligible.toLong)
    m.or(ineligible)
    copy(providerType = (if (providerType == null) that.providerType else providerType),
      ineligible = m,
      source = (source.split(",").toSet ++ that.source.split(",").toSet).toList.sorted.mkString(","),
      loaded = List(loaded, that.loaded).max)
  }

  def and(that: SubtractionEvent) : SubtractionEvent = {
    assert(code == that.code, println(s"${code.key} != ${that.code.key}"))
    assert(dos == that.dos, println(s"${dos} != ${that.dos}"))
    assert(transaction == that.transaction, println(s"${transaction} != ${that.transaction}"))
    assert(add == that.add, println(s"this and that have different transaction type"))
    assert(providerType == null || that.providerType == null || providerType == that.providerType, println(s"providerType mismatch: ${providerType} ${that.providerType}"))
    val m = new MonthMap(ineligible.startRaw, ineligible.endRaw)
    m.load(that.ineligible.toLong)
    m.and(ineligible)
    copy(providerType = (if (providerType == null) that.providerType else providerType),
      ineligible = m,
      source = (source.split(",").toSet ++ that.source.split(",").toSet).toList.sorted.mkString(","),
      loaded = List(loaded, that.loaded).max)
  }

  def xor(that: SubtractionEvent) : SubtractionEvent = {
    assert(code == that.code, println(s"${code.key} != ${that.code.key}"))
    assert(dos == that.dos, println(s"${dos} != ${that.dos}"))
    assert(transaction == that.transaction, println(s"${transaction} != ${that.transaction}"))
    assert(add == that.add, println(s"this and that have different transaction type"))
    assert(providerType == null || that.providerType == null || providerType == that.providerType, println(s"providerType mismatch: ${providerType} ${that.providerType}"))
    val m = new MonthMap(ineligible.startRaw, ineligible.endRaw)
    m.load(that.ineligible.toLong)
    m.xor(ineligible)
    copy(providerType = (if (providerType == null) that.providerType else providerType),
      ineligible = m,
      source = (source.split(",").toSet ++ that.source.split(",").toSet).toList.sorted.mkString(","),
      loaded = List(loaded, that.loaded).max)
  }
  
  def andnot(that: SubtractionEvent) : SubtractionEvent = {
    assert(code == that.code, println(s"${code.key} != ${that.code.key}"))
    assert(dos == that.dos, println(s"${dos} != ${that.dos}"))
    assert(transaction == that.transaction, println(s"${transaction} != ${that.transaction}"))
    assert(add == that.add, println(s"this and that have different transaction type"))
    assert(providerType == null || that.providerType == null || providerType == that.providerType, println(s"providerType mismatch: ${providerType} ${that.providerType}"))

    val m = new MonthMap(ineligible.startRaw, ineligible.endRaw)
    m.load(~that.ineligible.toLong)
    m.and(ineligible)

    copy(providerType = (if (providerType == null) that.providerType else providerType),
      ineligible = m,
      loaded = List(loaded, that.loaded).max)
  }
}

case class Subtraction(proj: Project, pat: String) extends JavaTokenParsers {
  val expression = proj.properties.getOrElse("gen", Map.empty).get("claimstype").map(_.toString.split(",").last).getOrElse("empty")
  val subtractions = List("legacy", "raps", "mao", "cr", "empty")
  val cache = collection.mutable.Map[String, List[SubtractionEvent]]()

  def or(left: List[SubtractionEvent], right: List[SubtractionEvent]) : List[SubtractionEvent] =
    (left ++ right).groupBy(_.key).map(_._2.reduce(_ or _)).toList

  def and(left: List[SubtractionEvent], right: List[SubtractionEvent]) : List[SubtractionEvent] =
    (left ++ right).groupBy(_.key).filter(_._2.size > 1).map(_._2.reduce(_ and _)).toList

  def xor(left: List[SubtractionEvent], right: List[SubtractionEvent]) : List[SubtractionEvent] =
    (left ++ right).groupBy(_.key).map(_._2.reduce(_ xor _)).toList

  def andnot(left: List[SubtractionEvent], right: List[SubtractionEvent]) : List[SubtractionEvent] = {
    val rmap = right.map(x => x.key -> x).toMap
    left.map(l => rmap.get(l.key) match {
      case None => l
      case Some(r) => l andnot r
    })
  }

  private lazy val expr: Parser[List[SubtractionEvent]] = excl ~ rep("|" ~ excl) ^^ { case f1 ~ fs => (f1 /: fs)((x,y) => or(x, y._2)) }
  private lazy val excl: Parser[List[SubtractionEvent]] = (subt ~ rep("^" ~ subt)) ^^ { case f1 ~ fs => (f1 /: fs)((x,y) => xor(x, y._2)) }
  private lazy val subt: Parser[List[SubtractionEvent]] = (term ~ rep("-" ~ term)) ^^ { case f1 ~ fs => (f1 /: fs)((x,y) => andnot(x, y._2)) }
  private lazy val term: Parser[List[SubtractionEvent]] = factor ~ rep("&" ~ factor) ^^ { case f1 ~ fs => (f1 /: fs)((x,y) => and(x, y._2)) }
  private lazy val factor: Parser[List[SubtractionEvent]] = variable | ("(" ~ expr ~ ")" ^^ { case "(" ~ exp ~ ")" => exp })
  // This will construct the list of variables for this parser
  private lazy val variable: Parser[List[SubtractionEvent]] = subtractions.map(Parser(_)).reduceLeft(_ | _) ^^ {
    case x@"legacy" => cache.getOrElseUpdate(x, new LegacySubtraction(proj, pat).events())
    case x@"raps" => cache.getOrElseUpdate(x, new RAPSSubtraction(proj, pat).events())
    case x@"mao" => cache.getOrElseUpdate(x, new MAOSubtraction(proj, pat).events())
    case x@"cr" => cache.getOrElseUpdate(x, new CommercialRiskSubtraction(proj, pat).events())
    case "empty" => List.empty
  }

  def process(sevents: List[SubtractionEvent], andChildren: Boolean = true) : List[EventTypeX] =
    sevents.filter(_.add).flatMap(x => x.code.toHcc(proj.icdMapping, andChildren).map(c => (c, x))).groupBy(_._1).map(x => (x._1, x._2.map(_._2))).flatMap(x => {
      val mm = x._2.head.ineligible
      x._2.tail.foreach(y => mm.or(y.ineligible))
      if (mm.toLong() == 0)
        None
      else
        Some(EventTypeX.makeInEligible(x._1, proj.start, proj.end, x._2.map(_.loaded).max, mm.toLong, pat, Some(("scala-common", x._2.flatMap(_.source.split(",")).distinct.sorted.mkString(",")))))
    }).toList

  def clusters(): List[SubtractionEvent] = parseAll(expr, expression) match {
    case Success(events, _) => events.asInstanceOf[List[SubtractionEvent]]
  }

  def events() : List[EventTypeX] = parseAll(expr, expression) match {
    case Success(events, _) => process(events.asInstanceOf[List[SubtractionEvent]])
  }
}
