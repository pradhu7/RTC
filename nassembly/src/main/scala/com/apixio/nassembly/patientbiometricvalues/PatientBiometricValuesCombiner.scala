package com.apixio.nassembly.patientbiometricvalues

import com.apixio.model.nassembly.Combiner.CombinerInput
import com.apixio.model.nassembly._
import com.apixio.nassembly.biometricvalue.BiometricValueExchange
import com.apixio.nassembly.commonaggregators.DefaultCidAggregator

import java.util
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._


class PatientBiometricValuesCombiner extends Combiner[PatientBiometricValuesExchange] {
  override def getDataTypeName: String = {
    PatientBiometricValuesExchange.dataTypeName
  }

  override def fromDataTypeToAggregator: util.Map[String, Aggregator] = {
    Map(BiometricValueExchange.dataTypeName -> DefaultCidAggregator) // Just roll up to a single patient object
  }

  override def combine(combinerInput: CombinerInput): java.lang.Iterable[PatientBiometricValuesExchange] = {
    (combinerInput.getDataTypeNameToExchanges.get(BiometricValueExchange.dataTypeName) match {
      case null => Iterable.empty[PatientBiometricValuesExchange]
      case exchanges =>
        // Roll up
        val metrics = exchanges.toList.map(e => e.asInstanceOf[BiometricValueExchange]).flatMap(_.getProtos)
        val wrapper = new PatientBiometricValuesExchange
        wrapper.buildBVWrapper(metrics)
        Iterable(wrapper)
    }).asJava
  }
}