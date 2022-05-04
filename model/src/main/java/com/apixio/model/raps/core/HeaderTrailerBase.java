package com.apixio.model.raps.core;

abstract class HeaderTrailerBase extends Record {
    protected final String submitterId;
    protected final String fileId;

    HeaderTrailerBase(String submitterId, String fileId, String filler) {
        super(filler);
        if (submitterId.length() != 6) {
            throw new IllegalArgumentException("Submitter Id length must be 6");
        }
        this.submitterId = submitterId;

        if (fileId.length() != 10) {
            throw new IllegalArgumentException("File id length must be 10");
        }
        this.fileId = fileId;
    }

    public String getSubmitterId() {
        return submitterId;
    }

    public String getFileId() {
        return fileId;
    }

}
