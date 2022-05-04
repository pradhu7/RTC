package com.apixio.nassembly.patientffsclaims

import com.apixio.datacatalog.SummaryObjects
import com.apixio.model.nassembly.Combiner.CombinerInput
import com.apixio.model.nassembly._
import com.apixio.nassembly.combinerutils.DataBuckets
import com.apixio.nassembly.ffsclaims.{FfsClaimExchange, QuarterlyFfsAggregator}
import com.apixio.util.nassembly.{DataCatalogProtoUtils, DateBucketUtils}

import java.util
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._


class PatientFfsClaimsCombiner extends Combiner[PatientFfsClaimsExchange] {
  override def getDataTypeName: String = {
    PatientFfsClaimsExchange.dataTypeName
  }

  override def fromDataTypeToAggregator: util.Map[String, Aggregator] = {
    Map(FfsClaimExchange.dataTypeName -> QuarterlyFfsAggregator)
  }

  override def combine(combinerInput: CombinerInput): java.lang.Iterable[PatientFfsClaimsExchange] = {
    val ffsClaims: List[SummaryObjects.FfsClaimSummary] = combinerInput.getDataTypeNameToExchanges.get(FfsClaimExchange.dataTypeName) match {
      case null => List.empty[SummaryObjects.FfsClaimSummary]
      case exchanges => exchanges.toList.map(e => e.asInstanceOf[FfsClaimExchange]).flatMap(_.getProtos)
    }

    combinerInput.getPassThruData.get(DataBuckets.YearQuarter.toString) match {
      case null | None =>
        // Do bucketing withing combine method
        ffsClaims.groupBy(c => {
          val dt = DataCatalogProtoUtils.toDateTime(c.getFfsClaim.getProcedureInfo.getEndDate)
          DateBucketUtils.getYearlyQuarter(dt)
        })
          .map {
            case (bucket, claims) =>
              val wrapper = new PatientFfsClaimsExchange
              wrapper.buildSkinnyPatient(bucket, claims)
              wrapper
          }.asJava
      case dateBucket =>
        // Use bucket from meta
        val wrapper = new PatientFfsClaimsExchange
        wrapper.buildSkinnyPatient(dateBucket.asInstanceOf[String], ffsClaims)
        Iterable(wrapper).asJava
    }
  }
}