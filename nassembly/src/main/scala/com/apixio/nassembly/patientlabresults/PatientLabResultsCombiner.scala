package com.apixio.nassembly.patientlabresults

import com.apixio.model.nassembly.Combiner.CombinerInput
import com.apixio.model.nassembly._
import com.apixio.nassembly.commonaggregators.DefaultCidAggregator
import com.apixio.nassembly.labresult.LabResultExchange

import java.util
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

class PatientLabResultsCombiner extends Combiner[PatientLabResultsExchange] {
  override def getDataTypeName: String = {
    PatientLabResultsExchange.dataTypeName
  }

  override def fromDataTypeToAggregator: util.Map[String, Aggregator] = {
    Map(LabResultExchange.dataTypeName -> DefaultCidAggregator) // Just roll up to a single patient object
  }

  override def combine(combinerInput: CombinerInput): java.lang.Iterable[PatientLabResultsExchange] = {

    (combinerInput.getDataTypeNameToExchanges.get(LabResultExchange.dataTypeName) match {
      case null => Iterable.empty[PatientLabResultsExchange]
      case exchanges =>
        // Roll up
        val labResults = exchanges.toList.map(e => e.asInstanceOf[LabResultExchange]).flatMap(_.getProtos)
        val wrapper = new PatientLabResultsExchange
        wrapper.buildWrapper(labResults)
        Iterable(wrapper)
    }).asJava
  }
}