package com.apixio.model.cdi;

public enum BatchStatus
{
    ADDED("ADDED"),
    BATCHED("BATCHED"),
    QUEUED_FOR_VALIDATION("QUEUED_FOR_VALIDATION"),
    VALIDATING("VALIDATING"),
    VALIDATED("VALIDATED"),
    VALIDATED_HAS_ERRORS("VALIDATED_HAS_ERRORS"),
    QUEUED_FOR_UPLOAD("QUEUED_FOR_UPLOAD"),
    SEALED("SEALED"),
    UPLOADING("UPLOADING"),
    UPLOADED("UPLOADED"),
    UPLOAD_INCOMPLETE("UPLOAD_INCOMPLETE"),
    USER_KILLED_WHILE_VALIDATING("USER_KILLED_WHILE_VALIDATING"),
    USER_KILLED_WHILE_UPLOADING("USER_KILLED_WHILE_UPLOADING"),
    AUTO_KILLED_WHILE_VALIDATING("AUTO_KILLED_WHILE_VALIDATING"),
    AUTO_KILLED_WHILE_UPLOADING("AUTO_KILLED_WHILE_UPLOADING"),
    REMOTE_PROCESS_DIED_WHILE_VALIDATING("REMOTE_PROCESS_DIED_WHILE_VALIDATING"),
    REMOTE_PROCESS_DIED_WHILE_UPLOADING ("REMOTE_PROCESS_DIED_WHILE_UPLOADING"),
    DELETED("DELETED");

    private String code;

    BatchStatus(String code)
    {
        this.code = code;
    }

    public String getCode()
    {
        return code;
    }

    public static BatchStatus fromCode(String code)
    {
        switch (code)
        {
            case "ADDED":
                return BatchStatus.ADDED;
            case "BATCHED":
                return BatchStatus.BATCHED;
            case "QUEUED_FOR_VALIDATION":
                return BatchStatus.QUEUED_FOR_VALIDATION;
            case "VALIDATING":
                return BatchStatus.VALIDATING;
            case "VALIDATED_HAS_ERRORS":
                return BatchStatus.VALIDATED_HAS_ERRORS;
            case "VALIDATED":
                return BatchStatus.VALIDATED;
            case "QUEUED_FOR_UPLOAD":
                return BatchStatus.QUEUED_FOR_UPLOAD;
            case "SEALED":
                return BatchStatus.SEALED;
            case "UPLOADING":
                return BatchStatus.UPLOADING;
            case "UPLOAD_INCOMPLETE":
                return BatchStatus.UPLOAD_INCOMPLETE;
            case "UPLOADED":
                return BatchStatus.UPLOADED;
            case "USER_KILLED_WHILE_VALIDATING":
                return BatchStatus.USER_KILLED_WHILE_VALIDATING;
            case "USER_KILLED_WHILE_UPLOADING":
                return BatchStatus.USER_KILLED_WHILE_UPLOADING;
            case "AUTO_KILLED_WHILE_VALIDATING":
                return BatchStatus.AUTO_KILLED_WHILE_VALIDATING;
            case "AUTO_KILLED_WHILE_UPLOADING":
                return BatchStatus.AUTO_KILLED_WHILE_UPLOADING;
            case "REMOTE_PROCESS_DIED_WHILE_VALIDATING":
                return BatchStatus.REMOTE_PROCESS_DIED_WHILE_VALIDATING;
            case "REMOTE_PROCESS_DIED_WHILE_UPLOADING":
                return BatchStatus.REMOTE_PROCESS_DIED_WHILE_UPLOADING;
            case "DELETED":
                return BatchStatus.DELETED;
            default:
                return null;
        }
    }
}
