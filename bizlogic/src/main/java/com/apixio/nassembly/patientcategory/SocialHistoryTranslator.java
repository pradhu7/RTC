package com.apixio.nassembly.patientcategory;

import com.apixio.nassembly.patientsocialhistory.PatientSocialHistoryExchange;

public class SocialHistoryTranslator implements PatientCategoryTranslator {

    @Override
    public String[] getDataTypeNames(String partId) {
        return new String[]{PatientSocialHistoryExchange.dataTypeName()};
    }
}
