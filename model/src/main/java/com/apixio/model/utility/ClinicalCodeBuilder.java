package com.apixio.model.utility;

import com.apixio.model.Builder;
import com.apixio.model.patient.ClinicalCode;

/**
 * Lets us quickly build clinical code objects.
 * Created by vvyas on 1/23/14.
 */
public class ClinicalCodeBuilder implements Builder<ClinicalCode> {

    private ClinicalCode code = new ClinicalCode();

    public ClinicalCodeBuilder code(String code) {
        this.code.setCode(code);
        return this;
    }

    public ClinicalCodeBuilder codingSystem(String codingSystem) {
        this.code.setCodingSystem(codingSystem);
        return this;
    }

    public ClinicalCodeBuilder codingSystemOID(String codingSystemOID) {
        this.code.setCodingSystemOID(codingSystemOID);
        return this;
    }

    public ClinicalCodeBuilder displayName(String displayName) {
        this.code.setDisplayName(displayName);
        return this;
    }

    public ClinicalCodeBuilder codingSystemVersion(String codingSystemVersion) {
        this.code.setCodingSystemVersions(codingSystemVersion);
        return this;
    }

    @Override
    public ClinicalCode build() { return code; }
}
