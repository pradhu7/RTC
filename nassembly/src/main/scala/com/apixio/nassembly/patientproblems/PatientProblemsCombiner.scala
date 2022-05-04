package com.apixio.nassembly.patientproblems

import com.apixio.datacatalog.SummaryObjects
import com.apixio.model.nassembly.Combiner.CombinerInput
import com.apixio.model.nassembly._
import com.apixio.nassembly.combinerutils.DataBuckets
import com.apixio.nassembly.problem.{ProblemExchange, QuarterlyProblemAggregator}
import com.apixio.util.nassembly.{DataCatalogProtoUtils, DateBucketUtils}

import java.util
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._


class PatientProblemsCombiner extends Combiner[PatientProblemsExchange] {
  override def getDataTypeName: String = {
    PatientProblemsExchange.dataTypeName
  }

  override def fromDataTypeToAggregator: util.Map[String, Aggregator] = {
    Map(ProblemExchange.dataTypeName -> QuarterlyProblemAggregator)
  }

  override def combine(combinerInput: CombinerInput): java.lang.Iterable[PatientProblemsExchange] = {
    val problems: List[SummaryObjects.ProblemSummary] = combinerInput.getDataTypeNameToExchanges.get(ProblemExchange.dataTypeName) match {
        case null => List.empty[SummaryObjects.ProblemSummary]
        case exchanges => exchanges.toList.map(e => e.asInstanceOf[ProblemExchange]).flatMap(_.getProtos)
      }

    combinerInput.getPassThruData.get(DataBuckets.YearQuarter.toString) match {
      case null | None =>
        // Do bucketing withing combine method
        problems.groupBy(c => {
          val dt = DataCatalogProtoUtils.toDateTime(c.getProblemInfo.getEndDate)
          DateBucketUtils.getYearlyQuarter(dt)
        })
          .map {
            case (bucket, claims) =>
              val wrapper = new PatientProblemsExchange
              wrapper.buildSkinnyPatient(bucket, claims)
              wrapper
          }.asJava
      case dateBucket =>
        // Use bucket from meta
        val wrapper = new PatientProblemsExchange
        wrapper.buildSkinnyPatient(dateBucket.asInstanceOf[String], problems)
        Iterable(wrapper).asJava
    }
  }
}