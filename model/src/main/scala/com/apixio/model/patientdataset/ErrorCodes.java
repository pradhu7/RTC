package com.apixio.model.patientdataset;

public enum ErrorCodes {
    INTERNAL_ERROR(500),
    INVALID_PROJDATASET_PAYLOAD(1000),
    PROJECT_NOT_FOUND(1001),
    PROJDATASET_NOT_FOUND(1002),
    DOCUMENT_NOT_FOUND(1003),
    PATIENT_NOT_FOUND(1004),
    PROJDATASET_DELETION_NOT_ALLOW(1005),
    PROJDATASET_STATE_TRANSITION_NOT_ALLOW(1006),
    PROJDATASET_IS_ALREADY_CLOSED(1007),
    PROJDATASET_WITH_SAME_NAME_EXISTED(1008),
    INVALID_PROJDATASET_CREATION_CRITERIA_TYPE(1009),
    STATE_INPUT_REQUIRED(1010),
    INVALID_DOC_SEARCH_CRITERIA(1011),
    INVALID_PROJDATASET_SEARCH_CRITERIA(1012),
    INVALID_PROJDATASET_STATE_TYPE(1013),
    PDS_ID_REQUIRED(1014),
    INVALID_COMPLETENESS_REPORT_TYPE(1015),
    PROJECTDATASET_JOB_ALREADY_PENDING(1016),
    MULTI_SAME_PROJDATASETUUID(1017)
    ;

    private int code;

    ErrorCodes(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static ErrorCodes fromCode(int code) {
        switch (code)
        {
            case 500:
                return ErrorCodes.INTERNAL_ERROR;
            case 1000:
                return ErrorCodes.INVALID_PROJDATASET_PAYLOAD;
            case 1001:
                return ErrorCodes.PROJECT_NOT_FOUND;
            case 1002:
                return ErrorCodes.PROJDATASET_NOT_FOUND;
            case 1003:
                return ErrorCodes.DOCUMENT_NOT_FOUND;
            case 1004:
                return ErrorCodes.PATIENT_NOT_FOUND;
            case 1005:
                return ErrorCodes.PROJDATASET_DELETION_NOT_ALLOW;
            case 1006:
                return ErrorCodes.PROJDATASET_STATE_TRANSITION_NOT_ALLOW;
            case 1007:
                return ErrorCodes.PROJDATASET_IS_ALREADY_CLOSED;
            case 1008:
                return ErrorCodes.PROJDATASET_WITH_SAME_NAME_EXISTED;
            case 1009:
                return ErrorCodes.INVALID_PROJDATASET_CREATION_CRITERIA_TYPE;
            case 1010:
                return ErrorCodes.STATE_INPUT_REQUIRED;
            case 1011:
                return ErrorCodes.INVALID_DOC_SEARCH_CRITERIA;
            case 1012:
                return ErrorCodes.INVALID_PROJDATASET_SEARCH_CRITERIA;
            case 1013:
                return ErrorCodes.INVALID_PROJDATASET_STATE_TYPE;
            case 1014:
                return ErrorCodes.PDS_ID_REQUIRED;
            case 1015:
                return ErrorCodes.INVALID_COMPLETENESS_REPORT_TYPE;
            case 1016:
                return ErrorCodes.PROJECTDATASET_JOB_ALREADY_PENDING;
            case 1017:
                return ErrorCodes.MULTI_SAME_PROJDATASETUUID;
            default:
                return null;
        }
    }
}
