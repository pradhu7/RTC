package com.apixio.model.patientdataset;

public enum ProjectDataSetSearchCriteriaType
{
    PROJECT_DATA_SET_NAME("PROJECT_DATA_SET_NAME"),
    PROJECT_DATA_SET_STATE("PROJECT_DATA_SET_STATE"),
    PATIENT_UUID("PATIENT_UUID"),
    PROJECT_UUID("PROJECT_UUID"),
    DOCUMENT_UUID("DOCUMENT_UUID"),
    DELETED("DELETED");

    private String code;

    ProjectDataSetSearchCriteriaType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ProjectDataSetSearchCriteriaType fromCode(String code) {
        if (code == null) return null;

        switch (code)
        {
            case "PROJECT_DATA_SET_NAME":
                return ProjectDataSetSearchCriteriaType.PROJECT_DATA_SET_NAME;
            case "PROJECT_DATA_SET_STATE":
                return ProjectDataSetSearchCriteriaType.PROJECT_DATA_SET_STATE;
            case "PATIENT_UUID":
                return ProjectDataSetSearchCriteriaType.PATIENT_UUID;
            case "PROJECT_UUID":
                return ProjectDataSetSearchCriteriaType.PROJECT_UUID;
            case "DOCUMENT_UUID":
                return ProjectDataSetSearchCriteriaType.DOCUMENT_UUID;
            case "DELETED":
                return ProjectDataSetSearchCriteriaType.DELETED;
            default:
                return null;
        }
    }
}
