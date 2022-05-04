package com.apixio.model.raps.core;

public enum DiagnosisType {
    ICD9, ICD10, NOT_SPECIFIED;

    public static DiagnosisType fromString(String value)
    {
        value = value.trim();
        if (value.isEmpty())
        {
            return NOT_SPECIFIED;
        }
        else
        {
            return valueOf(value);
        }
    }
}
