package com.apixio.model.patientdataset;

public enum CompletenessReportType
{
    ALL("ALL"),
    COMPLETE("COMPLETE"),
    INCOMPLETE("INCOMPLETE");

    private String code;

    CompletenessReportType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static CompletenessReportType fromCode(String code) {
        switch (code)
        {
            case "ALL":
                return CompletenessReportType.ALL;
            case "COMPLETE":
                return CompletenessReportType.COMPLETE;
            case "INCOMPLETE":
                return CompletenessReportType.INCOMPLETE;
            default:
                return null;
        }
    }
}
