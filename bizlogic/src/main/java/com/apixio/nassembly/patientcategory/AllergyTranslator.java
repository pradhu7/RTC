package com.apixio.nassembly.patientcategory;

import com.apixio.nassembly.patientallergies.PatientAllergiesExchange;

public class AllergyTranslator implements PatientCategoryTranslator {

    @Override
    public String[] getDataTypeNames(String partId) {
        return new String[]{PatientAllergiesExchange.dataTypeName()};
    }
}