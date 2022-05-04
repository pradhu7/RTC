package com.apixio.nassembly.patientcategory;

import com.apixio.model.nassembly.Exchange;
import com.apixio.nassembly.legacycoverage.LegacyCoverageExchange;

public class CoverageTranslator implements PatientCategoryTranslator {

    @Override
    public Exchange getExchange(String partId) {
        return new LegacyCoverageExchange();
    }

    @Override
    public String[] getDataTypeNames(String partId) {
        return new String[]{LegacyCoverageExchange.dataTypeName()};
    }

}
