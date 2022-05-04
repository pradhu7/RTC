package com.apixio.nassembly.patientcategory;

import com.apixio.nassembly.patientfamilyhistory.PatientFamilyHistoryExchange;

public class FamilyHistoryTranslator implements PatientCategoryTranslator {

    @Override
    public String[] getDataTypeNames(String partId) {
        return new String[]{PatientFamilyHistoryExchange.dataTypeName()};
    }
}