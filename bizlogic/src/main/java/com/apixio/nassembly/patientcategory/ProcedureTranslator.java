package com.apixio.nassembly.patientcategory;

import com.apixio.nassembly.patientffsclaims.PatientFfsClaimsExchange;
import com.apixio.nassembly.patientmao004s.PatientMao004sExchange;
import com.apixio.nassembly.patientprocedures.PatientProceduresExchange;

public class ProcedureTranslator implements PatientCategoryTranslator {

    @Override
    public String[] getDataTypeNames(String partId) {
        return new String[]{PatientProceduresExchange.dataTypeName(),
                PatientFfsClaimsExchange.dataTypeName()};
    }
}
