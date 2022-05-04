package com.apixio.nassembly.patientcategory;

import com.apixio.nassembly.patientlabresults.PatientLabResultsExchange;

public class LabResultTranslator implements PatientCategoryTranslator {

    @Override
    public String[] getDataTypeNames(String partId) {
        return new String[]{PatientLabResultsExchange.dataTypeName()};
    }
}