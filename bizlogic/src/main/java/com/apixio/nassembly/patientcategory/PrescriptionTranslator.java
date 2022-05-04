package com.apixio.nassembly.patientcategory;

import com.apixio.nassembly.patientprescriptions.PatientPrescriptionExchange;

public class PrescriptionTranslator implements PatientCategoryTranslator {

    @Override
    public String[] getDataTypeNames(String partId) {
        return new String[]{PatientPrescriptionExchange.dataTypeName()};
    }
}