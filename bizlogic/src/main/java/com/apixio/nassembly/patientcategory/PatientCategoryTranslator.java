package com.apixio.nassembly.patientcategory;

import com.apixio.model.nassembly.Exchange;
import com.apixio.nassembly.patient.BackwardsCompatibleApoExchange;

// THIS IS THROWAWAY CODE!! Once downstream consumers moved to new APIs, we will delete PatientAssembly. The exchange knows how to parse data
public interface PatientCategoryTranslator {

    // This is how we parse the data in and convert to APO
    // Default is PatientExchange because of our SkinnyPatient design. It can deserialize all subtypes
    default Exchange getExchange(String partId){
        return new BackwardsCompatibleApoExchange();
    }

    String[] getDataTypeNames(String partId);

    // New Assembly supports multiple clustering cols
    default String[] translatePartKeys(String partId) { return new String[]{ partId }; }
}
