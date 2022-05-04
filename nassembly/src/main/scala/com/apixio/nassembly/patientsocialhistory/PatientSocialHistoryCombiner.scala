package com.apixio.nassembly.patientsocialhistory

import com.apixio.datacatalog.SummaryObjects
import com.apixio.model.nassembly.Combiner.CombinerInput
import com.apixio.model.nassembly._
import com.apixio.nassembly.socialhistory.{SocialHistoryExchange, TypeCodeAggregator}

import java.util
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._


class PatientSocialHistoryCombiner extends Combiner[PatientSocialHistoryExchange] {
  override def getDataTypeName: String = {
    PatientSocialHistoryExchange.dataTypeName
  }

  override def fromDataTypeToAggregator: util.Map[String, Aggregator] = {
    Map(SocialHistoryExchange.dataTypeName -> TypeCodeAggregator)
  }

  override def combine(combinerInput: CombinerInput): java.lang.Iterable[PatientSocialHistoryExchange] = {
    val histories: List[SummaryObjects.SocialHistorySummary] = combinerInput.getDataTypeNameToExchanges.get(SocialHistoryExchange.dataTypeName) match {
      case null => List.empty[SummaryObjects.SocialHistorySummary]
      case exchanges => exchanges.toList.map(e => e.asInstanceOf[SocialHistoryExchange]).flatMap(_.getProtos)
    }

    combinerInput.getPassThruData.get(TypeCodeAggregator.PersistedTypeCodeColName) match {
      case null | None =>
        // Do bucketing withing combine method
        histories.groupBy(c => c.getSocialHistoryInfo.getType.getCode)
          .map {
            case (code, groupedHistories) =>
              val wrapper = new PatientSocialHistoryExchange
              wrapper.buildWrapper(code, groupedHistories)
              wrapper
          }.asJava
      case code =>
        // Use bucket from meta
        val wrapper = new PatientSocialHistoryExchange
        wrapper.buildWrapper(code.asInstanceOf[String], histories)
        Iterable(wrapper).asJava
    }
  }
}