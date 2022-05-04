package com.apixio.nassembly.patientcategory;

import com.apixio.model.nassembly.Exchange;
import com.apixio.nassembly.documentmeta.DocumentMetaExchange;

public class DocumentMetaTranslator implements PatientCategoryTranslator {

    @Override
    public Exchange getExchange(String partId){
        return new DocumentMetaExchange();
    }

    @Override
    public String[] getDataTypeNames(String partId) {
        return new String[]{DocumentMetaExchange.dataTypeName()};
    }
}