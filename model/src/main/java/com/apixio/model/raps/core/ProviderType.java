package com.apixio.model.raps.core;

public enum ProviderType {
    HOSPITAL_INPATIENT_PRINCIPLE_DIAGNOSIS("01"),
    HOSPITAL_INPATIENT_OTHER_DIAGNOSIS("02"),
    HOSPITAL_OUTPATIENT("10"),
    PHYSICIAN("20");

    private final String rapsCode;

    ProviderType(String rapsCode) {
        this.rapsCode = rapsCode;
    }

    public String getRapsCode() {
        return rapsCode;
    }

    public static ProviderType fromRapsCode(String rapsCode) {
        for (ProviderType type : ProviderType.values()) {
            if (type.rapsCode.equals(rapsCode)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknow provider type: " + rapsCode);
    }
}
