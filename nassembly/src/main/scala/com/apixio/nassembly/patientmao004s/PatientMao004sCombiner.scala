package com.apixio.nassembly.patientmao004s

import com.apixio.datacatalog.SummaryObjects
import com.apixio.model.nassembly.Combiner.CombinerInput
import com.apixio.model.nassembly._
import com.apixio.nassembly.combinerutils.DataBuckets
import com.apixio.nassembly.mao004.{Mao004Exchange, QuarterlyMao004Aggregator}
import com.apixio.util.nassembly.{DataCatalogProtoUtils, DateBucketUtils}
import org.joda.time.DateTime

import java.util
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._


class PatientMao004sCombiner extends Combiner[PatientMao004sExchange] {
  override def getDataTypeName: String = {
    PatientMao004sExchange.dataTypeName
  }

  override def fromDataTypeToAggregator: util.Map[String, Aggregator] = {
    Map(Mao004Exchange.dataTypeName -> QuarterlyMao004Aggregator)
  }

  override def combine(combinerInput: CombinerInput): java.lang.Iterable[PatientMao004sExchange] = {
    val mao004s: List[SummaryObjects.Mao004Summary] = combinerInput.getDataTypeNameToExchanges.get(Mao004Exchange.dataTypeName) match {
        case null => List.empty[SummaryObjects.Mao004Summary]
        case exchanges => exchanges.toList.map(e => e.asInstanceOf[Mao004Exchange]).flatMap(_.getProtos)
      }

    combinerInput.getPassThruData.get(DataBuckets.YearQuarter.toString) match {
      case null | None =>
        // Do bucketing withing combine method
        mao004s.groupBy(c => {
          val dt = DataCatalogProtoUtils.toDateTime(c.getMao004.getProblemInfo.getEndDate)
          DateBucketUtils.getYearlyQuarter(dt)
        })
          .map {
            case (bucket, claims) =>
              val wrapper = new PatientMao004sExchange
              wrapper.buildSkinnyPatient(bucket, claims)
              wrapper
          }.asJava
      case dateBucket =>
        // Use bucket from meta
        val wrapper = new PatientMao004sExchange
        wrapper.buildSkinnyPatient(dateBucket.asInstanceOf[String], mao004s)
        Iterable(wrapper).asJava
    }
  }
}