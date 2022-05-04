package com.apixio.nassembly.patientcategory;

import com.apixio.model.nassembly.Exchange;
import com.apixio.nassembly.patientactor.PatientActorExchange;

public class ClinicalActorTranslator implements PatientCategoryTranslator {

    @Override
    public Exchange getExchange(String partId) {
        return new PatientActorExchange();
    }


    public String[] getDataTypeNames(String partId) {
        return new String[]{PatientActorExchange.dataTypeName()};
    }


    @Override
    public String[] translatePartKeys(String partId) {
        if (partId == null) return null;
        else {
            String[] parts = partId.split("\\^");
            if (parts.length == 1) return new String[]{parts[0]};
            else {
                String assignAuthority = parts[1];
                String id = parts[0];
                return new String[]{assignAuthority, id};
            }
        }


    }
}