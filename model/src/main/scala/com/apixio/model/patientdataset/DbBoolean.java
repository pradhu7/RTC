package com.apixio.model.patientdataset;

public enum DbBoolean
{
    Y("Y"),
    N("N"),
    UNKNOWN("UNKNOWN");

    private String code;

    DbBoolean(String code) {
        this.code = code;
    }

    public String getCode()
    {
        return code;
    }

    public boolean isTrue()
    {
        return this.code.equalsIgnoreCase("Y");
    }

    public static DbBoolean fromCode(String code) {
        if (code == null) return UNKNOWN;

        switch (code.toUpperCase())
        {
            case "Y":       return Y;
            case "N":       return N;
            case "UNKNOWN": return UNKNOWN;
            default:        return null;
        }
    }
}
