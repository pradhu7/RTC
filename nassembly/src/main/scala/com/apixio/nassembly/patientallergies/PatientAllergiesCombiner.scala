package com.apixio.nassembly.patientallergies

import com.apixio.model.nassembly.Combiner.CombinerInput
import com.apixio.model.nassembly._
import com.apixio.nassembly.allergy.AllergyExchange
import com.apixio.nassembly.commonaggregators.DefaultCidAggregator

import java.util
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._


class PatientAllergiesCombiner extends Combiner[PatientAllergiesExchange] {
  override def getDataTypeName: String = {
    PatientAllergiesExchange.dataTypeName
  }

  override def fromDataTypeToAggregator: util.Map[String, Aggregator] = {
    Map(AllergyExchange.dataTypeName -> DefaultCidAggregator) // Just roll up to a single patient object
  }

  override def combine(combinerInput: CombinerInput): java.lang.Iterable[PatientAllergiesExchange] = {
    (combinerInput.getDataTypeNameToExchanges.get(AllergyExchange.dataTypeName) match {
      case null => Iterable.empty[PatientAllergiesExchange]
      case exchanges =>
        // Roll up
        val allergies = exchanges.toList.map(e => e.asInstanceOf[AllergyExchange]).flatMap(_.getProtos)
        val wrapper = new PatientAllergiesExchange
        wrapper.buildAllergyWrapper(allergies)
        Iterable(wrapper)
    }).asJava
  }
}