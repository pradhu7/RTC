package com.apixio.nassembly.patientcategory;

import com.apixio.nassembly.patientimmunization.PatientImmunizationExchange;

public class ImmunizationTranslator implements PatientCategoryTranslator {

    @Override
    public String[] getDataTypeNames(String partId) {
        return new String[]{PatientImmunizationExchange.dataTypeName()};
    }
}