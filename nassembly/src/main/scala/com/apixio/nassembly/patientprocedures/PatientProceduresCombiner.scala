package com.apixio.nassembly.patientprocedures

import com.apixio.datacatalog.SummaryObjects
import com.apixio.model.nassembly.Combiner.CombinerInput
import com.apixio.model.nassembly._
import com.apixio.nassembly.combinerutils.DataBuckets
import com.apixio.nassembly.procedure.{ProcedureExchange, QuarterlyProcedureAggregator}
import com.apixio.util.nassembly.{DataCatalogProtoUtils, DateBucketUtils}

import java.util
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._


class PatientProceduresCombiner extends Combiner[PatientProceduresExchange] {
  override def getDataTypeName: String = {
    PatientProceduresExchange.dataTypeName
  }

  override def fromDataTypeToAggregator: util.Map[String, Aggregator] = {
    Map(ProcedureExchange.dataTypeName -> QuarterlyProcedureAggregator)
  }

  override def combine(combinerInput: CombinerInput): java.lang.Iterable[PatientProceduresExchange] = {
    val procedures: List[SummaryObjects.ProcedureSummary] = combinerInput.getDataTypeNameToExchanges.get(ProcedureExchange.dataTypeName) match {
        case null => List.empty[SummaryObjects.ProcedureSummary]
        case exchanges => exchanges.toList.map(e => e.asInstanceOf[ProcedureExchange]).flatMap(_.getProtos)
      }

    combinerInput.getPassThruData.get(DataBuckets.YearQuarter.toString) match {
      case null | None =>
        // Do bucketing withing combine method
        procedures.groupBy(c => {
          val dt = DataCatalogProtoUtils.toDateTime(c.getProcedureInfo.getEndDate)
          DateBucketUtils.getYearlyQuarter(dt)
        })
          .map {
            case (bucket, claims) =>
              val wrapper = new PatientProceduresExchange
              wrapper.buildSkinnyPatient(bucket, claims)
              wrapper
          }.asJava
      case dateBucket =>
        // Use bucket from meta
        val wrapper = new PatientProceduresExchange
        wrapper.buildSkinnyPatient(dateBucket.asInstanceOf[String], procedures)
        Iterable(wrapper).asJava
    }
  }
}