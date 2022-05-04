package com.apixio.nassembly.patientcategory;

import com.apixio.nassembly.patientmao004s.PatientMao004sExchange;
import com.apixio.nassembly.patientproblems.PatientProblemsExchange;
import com.apixio.nassembly.patientraclaims.PatientRaClaimsExchange;

public class ProblemTranslator implements PatientCategoryTranslator {

    @Override
    public String[] getDataTypeNames(String partId) {
        return new String[]{PatientProblemsExchange.dataTypeName(),
                PatientRaClaimsExchange.dataTypeName(),
                PatientMao004sExchange.dataTypeName()};
    }
}
