package com.apixio.nassembly.patientdemographics

import com.apixio.datacatalog.{BaseObjects, SummaryObjects}
import com.apixio.model.nassembly.Combiner.CombinerInput
import com.apixio.model.nassembly.{Aggregator, Combiner}
import com.apixio.nassembly.commonaggregators.DefaultCidAggregator
import com.apixio.nassembly.contactdetails.{ContactDetailsExchange, ContactDetailsUtils}
import com.apixio.nassembly.demographics.DemographicsExchange

import java.{lang, util}
import scala.collection.JavaConverters._

class PatientDemographicsCombiner extends Combiner[PatientDemographicsExchange] {

  override def fromDataTypeToAggregator(): util.Map[String, Aggregator] = {
    Map[String, Aggregator](DemographicsExchange.dataTypeName -> DefaultCidAggregator,
      ContactDetailsExchange.dataTypeName -> DefaultCidAggregator).asJava
  }

  override def combine(combinerInput: CombinerInput): lang.Iterable[PatientDemographicsExchange] = {

    val demographics: List[SummaryObjects.DemographicsSummary] = combinerInput.getDataTypeNameToExchanges.get(DemographicsExchange.dataTypeName) match {
      case null => List.empty
      case exchanges => exchanges.toList.map(e => e.asInstanceOf[DemographicsExchange]).flatMap(_.getProtos)
    }

    val contactDetails: List[SummaryObjects.ContactDetailsSummary] = combinerInput.getDataTypeNameToExchanges.get(ContactDetailsExchange.dataTypeName) match {
      case null => List.empty
      case exchanges => exchanges.toList.map(e => e.asInstanceOf[ContactDetailsExchange]).flatMap(_.getProtos)
    }

    val contactDetailsMerged: Seq[SummaryObjects.ContactDetailsSummary] = ContactDetailsUtils.mergeContactDetailSummaries(contactDetails).toSeq
    val contactDetailsNormalized: Seq[BaseObjects.ContactDetails] = ContactDetailsUtils.normalizeContactDetails(contactDetailsMerged)
    val wrapper = new PatientDemographicsExchange
    wrapper.buildDemographicsWrapper(demographics, contactDetailsNormalized.toList)
    Iterable(wrapper).asJava
  }

  override def getDataTypeName: String = PatientDemographicsExchange.dataTypeName
}
