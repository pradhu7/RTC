package com.apixio.model.patientdataset;

public enum ProjectDataSetType
{
    DOC_SET("DOC_SET"),
    PAT_SET("PAT_SET");

    private String projectDataSetType;

    ProjectDataSetType(String patientDataSetType) {
        this.projectDataSetType = projectDataSetType;
    }

    public String getType() {
        return this.projectDataSetType;
    }

    public static ProjectDataSetType fromType(String projectDataSetType) {
        switch (projectDataSetType)
        {
            case "DOC_SET":
                return ProjectDataSetType.DOC_SET;
            case "PAT_SET":
                return ProjectDataSetType.PAT_SET;
            default:
                return null;
        }
    }
}
