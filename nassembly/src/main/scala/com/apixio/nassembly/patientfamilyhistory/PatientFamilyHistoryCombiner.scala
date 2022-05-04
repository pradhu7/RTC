package com.apixio.nassembly.patientfamilyhistory

import com.apixio.model.nassembly.Combiner.CombinerInput
import com.apixio.model.nassembly._
import com.apixio.nassembly.commonaggregators.DefaultCidAggregator
import com.apixio.nassembly.familyhistory.FamilyHistoryExchange

import java.util
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

class PatientFamilyHistoryCombiner extends Combiner[PatientFamilyHistoryExchange] {
  override def getDataTypeName: String = {
    PatientFamilyHistoryExchange.dataTypeName
  }

  override def fromDataTypeToAggregator: util.Map[String, Aggregator] = {
    Map(FamilyHistoryExchange.dataTypeName -> DefaultCidAggregator) // Just roll up to a single patient object
  }

  override def combine(combinerInput: CombinerInput): java.lang.Iterable[PatientFamilyHistoryExchange] = {

    (combinerInput.getDataTypeNameToExchanges.get(FamilyHistoryExchange.dataTypeName) match {
      case null => Iterable.empty[PatientFamilyHistoryExchange]
      case exchanges =>
        // Roll up
        val familyHistorySummaries = exchanges.map(e => e.asInstanceOf[FamilyHistoryExchange])
          .flatMap(_.getProtos)
        val wrapper = new PatientFamilyHistoryExchange
        wrapper.buildWrapper(familyHistorySummaries.toIterable)
        Iterable(wrapper)
    }).asJava
  }
}