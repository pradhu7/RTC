package com.apixio.nassembly.patientcategory;

import com.apixio.model.nassembly.Exchange;
import com.apixio.nassembly.encounter.EncounterExchange;

public class EncounterTranslator implements PatientCategoryTranslator {

    @Override
    public Exchange getExchange(String partId){
        return new EncounterExchange();
    }

    @Override
    public String[] getDataTypeNames(String partId) {
        return new String[]{EncounterExchange.dataTypeName()};
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
