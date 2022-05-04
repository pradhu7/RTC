package com.apixio.nassembly.patientimmunization

import com.apixio.model.nassembly.Combiner.CombinerInput
import com.apixio.model.nassembly._
import com.apixio.nassembly.commonaggregators.DefaultCidAggregator
import com.apixio.nassembly.immunization.ImmunizationExchange

import java.util
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

class PatientImmunizationCombiner extends Combiner[PatientImmunizationExchange] {
  override def getDataTypeName: String = {
    PatientImmunizationExchange.dataTypeName
  }

  override def fromDataTypeToAggregator: util.Map[String, Aggregator] = {
    Map(ImmunizationExchange.dataTypeName -> DefaultCidAggregator) // Just roll up to a single patient object
  }

  override def combine(combinerInput: CombinerInput): java.lang.Iterable[PatientImmunizationExchange] = {

    (combinerInput.getDataTypeNameToExchanges.get(ImmunizationExchange.dataTypeName) match {
      case null => Iterable.empty[PatientImmunizationExchange]
      case exchanges =>
        // Roll up
        val familyHistorySummaries = exchanges.map(e => e.asInstanceOf[ImmunizationExchange])
          .flatMap(_.getProtos)
        val wrapper = new PatientImmunizationExchange
        wrapper.buildWrapper(familyHistorySummaries.toIterable)
        Iterable(wrapper)
    }).asJava
  }
}