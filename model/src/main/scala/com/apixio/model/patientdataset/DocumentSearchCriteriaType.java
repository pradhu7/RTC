package com.apixio.model.patientdataset;

public enum DocumentSearchCriteriaType {
    UPLOAD_BATCH_NAMES("UPLOAD_BATCH_NAMES"),
    TAGS("TAGS"),
    DOCUMENT_UUIDS("DOCUMENT_UUIDS"),
    PATIENT_LIST("PATIENT_LIST"),
    DOCSET_UUIDS("DOCSET_UUIDS");
    private String code;

    DocumentSearchCriteriaType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static DocumentSearchCriteriaType fromCode(String code) {
        switch (code)
        {
            case "UPLOAD_BATCH_NAMES":
                return DocumentSearchCriteriaType.UPLOAD_BATCH_NAMES;
            case "TAGS":
                return DocumentSearchCriteriaType.TAGS;
            case "DOCUMENT_UUIDS":
                return DocumentSearchCriteriaType.DOCUMENT_UUIDS;
            case "PATIENT_LIST":
                return DocumentSearchCriteriaType.PATIENT_LIST;
            case "DOCSET_UUIDS":
                return DocumentSearchCriteriaType.DOCSET_UUIDS;
            default:
                return null;
        }
    }
}
