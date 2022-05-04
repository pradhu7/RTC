package com.apixio.nassembly.patientraclaims

import com.apixio.datacatalog.SummaryObjects
import com.apixio.model.nassembly.Combiner.CombinerInput
import com.apixio.model.nassembly._
import com.apixio.nassembly.combinerutils.DataBuckets
import com.apixio.nassembly.raclaim.{QuarterlyRaClaimAggregator, RaClaimExchange}
import com.apixio.util.nassembly.{DataCatalogProtoUtils, DateBucketUtils}

import java.util
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._


class PatientRaClaimsCombiner extends Combiner[PatientRaClaimsExchange] {
  override def getDataTypeName: String = {
    PatientRaClaimsExchange.dataTypeName
  }

  override def fromDataTypeToAggregator: util.Map[String, Aggregator] = {
    Map(RaClaimExchange.dataTypeName -> QuarterlyRaClaimAggregator)
  }

  override def combine(combinerInput: CombinerInput): java.lang.Iterable[PatientRaClaimsExchange] = {
    val raClaims: List[SummaryObjects.RaClaimSummary] = combinerInput.getDataTypeNameToExchanges.get(RaClaimExchange.dataTypeName) match {
        case null => List.empty[SummaryObjects.RaClaimSummary]
        case exchanges => exchanges.toList
          .map(e => e.asInstanceOf[RaClaimExchange])
          .flatMap(_.getProtos)
      }

    combinerInput.getPassThruData.get(DataBuckets.YearQuarter.toString) match {
      case null | None =>
        // Do bucketing withing combine method
        raClaims.groupBy(c => {
          val dt = DataCatalogProtoUtils.toDateTime(c.getRaClaim.getProblemInfo.getEndDate)
          DateBucketUtils.getYearlyQuarter(dt)
        })
          .map {
            case (bucket, claims) =>
              val wrapper = new PatientRaClaimsExchange
              wrapper.buildSkinnyPatient(bucket, claims)
              wrapper
          }.asJava
      case dateBucket =>
        // Use bucket from meta
        val wrapper = new PatientRaClaimsExchange
        wrapper.buildSkinnyPatient(dateBucket.asInstanceOf[String], raClaims)
        Iterable(wrapper).asJava
    }
  }
}