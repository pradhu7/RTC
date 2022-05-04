package com.apixio.model.patientdataset;

public enum EligibilityType
{
    DATE_RANGE("DATE_RANGE"),
    SWEEP("SWEEP");

    private String code;

    EligibilityType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static EligibilityType fromCode(String code) {
        switch (code)
        {
            case "DATE_RANGE":
                return EligibilityType.DATE_RANGE;
            case "SWEEP":
                return EligibilityType.SWEEP;
            default:
                return null;
        }
    }
}
