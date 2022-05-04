package com.apixio.nassembly.patientprescriptions

import com.apixio.model.nassembly.Combiner.CombinerInput
import com.apixio.model.nassembly._
import com.apixio.nassembly.commonaggregators.DefaultCidAggregator
import com.apixio.nassembly.prescription.PrescriptionExchange

import java.util
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

class PatientPrescriptionCombiner extends Combiner[PatientPrescriptionExchange] {
  override def getDataTypeName: String = {
    PatientPrescriptionExchange.dataTypeName
  }

  override def fromDataTypeToAggregator: util.Map[String, Aggregator] = {
    Map(PrescriptionExchange.dataTypeName -> DefaultCidAggregator) // Just roll up to a single patient object
  }

  override def combine(combinerInput: CombinerInput): java.lang.Iterable[PatientPrescriptionExchange] = {

    (combinerInput.getDataTypeNameToExchanges.get(PrescriptionExchange.dataTypeName) match {
      case null => Iterable.empty[PatientPrescriptionExchange]
      case exchanges =>
        // Roll up
        val prescriptionSummaries = exchanges
          .toList
          .map(e => e.asInstanceOf[PrescriptionExchange])
          .flatMap(_.getProtos)
        val wrapper = new PatientPrescriptionExchange
        wrapper.buildWrapper(prescriptionSummaries)
        Iterable(wrapper)
    }).asJava
  }
}