package com.apixio.nassembly.demographics

import com.apixio.datacatalog.YearMonthDayOuterClass.YearMonthDay
import com.apixio.model.indexing.{IndexableType, IndexedField}
import com.apixio.model.nassembly.Indexer
import org.joda.time.DateTime

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

class DemographicsIndexer extends Indexer[DemographicsExchange] {

  val DatePattern1 = "yyyy/MM/dd"
  val DatePattern2 = "MM/dd/yyyy"

  //note: fieldName must match column from DataFrame in IndexerService to be able to get the value
  override def getIndexedFields(exchange: DemographicsExchange): java.util.Set[IndexedField] = {
    val protos = exchange.getProtos
    protos.flatMap(p => {
      val name = p.getDemographicsInfo.getName
      val givenNames = name.getGivenNamesList.asScala.map(gn => new IndexedField(gn, IndexableType.FN))
      val familyNames = name.getFamilyNamesList.asScala.map(ln => new IndexedField(ln, IndexableType.LN))

      val bd = dateToMultipleDateStrings(p.getDemographicsInfo.getDob).map(d =>  new IndexedField(d, IndexableType.BD))
      givenNames ++ familyNames ++ bd
    }).toSet.asJava
  }

  /**
   * We index dob with two separate date string patters
   * @param date Year Month Date with nested epochMs
   * @return
   */
  def dateToMultipleDateStrings(date: YearMonthDay): Seq[String] = {
    Try(new DateTime(date.getEpochMs)) match {
      case Success(dt) =>
        val pattern1 = dt.toString(DatePattern1)
        val pattern2 = dt.toString(DatePattern2)
        Seq(pattern1, pattern2)
      case Failure(_) =>
        Seq.empty[String] // Could be empty
    }
  }

  override def getDataTypeName: String = DemographicsExchange.dataTypeName
}
