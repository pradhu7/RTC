package com.apixio.model.patientdataset;

public enum ProjectDataSetState
{
    PUBLISHED("PUBLISHED"),
    ACTIVE("ACTIVE"),
    PAUSED("PAUSED"),
    CLOSED("CLOSED");

    private String code;

    ProjectDataSetState(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ProjectDataSetState fromCode(String code) {
        switch (code)
        {
            case "PUBLISHED":
                return ProjectDataSetState.PUBLISHED;
            case "ACTIVE":
                return ProjectDataSetState.ACTIVE;
            case "PAUSED":
                return ProjectDataSetState.PAUSED;
            case "CLOSED":
                return ProjectDataSetState.CLOSED;
            default:
                return null;
        }
    }
}
