package com.apixio.nassembly.patientcategory;

import com.apixio.model.nassembly.Exchange;
import com.apixio.nassembly.patientdemographics.PatientDemographicsExchange;

public class DemographicTranslator implements PatientCategoryTranslator {

    @Override
    public String[] getDataTypeNames(String partId) {
        return new String[]{PatientDemographicsExchange.dataTypeName()};
    }

    @Override
    public Exchange getExchange(String partId) {
        return new PatientDemographicsExchange();
    }
}