package com.apixio.nassembly.patientcategory;

import com.apixio.model.nassembly.Exchange;
import com.apixio.nassembly.patient.partial.AllExchange;
import com.apixio.nassembly.patient.partial.PartialPatientExchange;

public class AllTranslator implements PatientCategoryTranslator {

    @Override
    public Exchange getExchange(String partId) {
        if (partId == null)
        {
            return new AllExchange();
        }
        else
        {
            return new PartialPatientExchange();
        }
    }

    @Override
    public String[] getDataTypeNames(String partId) {
       return new String[]{PartialPatientExchange.dataTypeName()};
    }

}
