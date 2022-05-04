package com.apixio.nassembly.patientcategory;

import com.apixio.nassembly.patientbiometricvalues.PatientBiometricValuesExchange;

public class VitalSignTranslator implements PatientCategoryTranslator {

    @Override
    public String[] getDataTypeNames(String partId) {
        return new String[]{PatientBiometricValuesExchange.dataTypeName()};
    }
}