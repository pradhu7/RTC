package com.apixio.model.cdi;

import org.joda.time.DateTime;

public class UploadedFileLink
{
    private int id;
    private String documentUUID;
    private String patientUUID;
    private DateTime createdAt;
    private String createdBy;
    private UploadedFile uploadedFile;
    private File file;

    public UploadedFileLink()
    {
    }

    public UploadedFileLink(DateTime createdAt, String createdBy)
    {
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public String getDocumentUUID()
    {
        return documentUUID;
    }

    public void setDocumentUUID(String documentUUID)
    {
        this.documentUUID = documentUUID;
    }

    public String getPatientUUID()
    {
        return patientUUID;
    }

    public void setPatientUUID(String patientUUID)
    {
        this.patientUUID = patientUUID;
    }

    public File getFile()
    {
        return file;
    }

    public void setFile(File file)
    {
        this.file = file;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public DateTime getCreatedAt()
    {
        return createdAt;
    }

    public void setCreatedAt(DateTime createdAt)
    {
        this.createdAt = createdAt;
    }

    public String getCreatedBy()
    {
        return createdBy;
    }

    public void setCreatedBy(String createdBy)
    {
        this.createdBy = createdBy;
    }

    public UploadedFile getUploadedFile()
    {
        return uploadedFile;
    }

    public void setUploadedFile(UploadedFile uploadedFile)
    {
        this.uploadedFile = uploadedFile;
    }

    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UploadedFileLink that = (UploadedFileLink) o;

        if (id != that.id) return false;
        if (documentUUID != null ? !documentUUID.equals(that.documentUUID) : that.documentUUID != null) return false;
        if (patientUUID != null ? !patientUUID.equals(that.patientUUID) : that.patientUUID != null) return false;
        if (createdAt != null ? !createdAt.equals(that.createdAt) : that.createdAt != null) return false;
        return createdBy != null ? createdBy.equals(that.createdBy) : that.createdBy == null;
    }

    public int hashCode()
    {
        int result = id;
        result = 31 * result + (documentUUID != null ? documentUUID.hashCode() : 0);
        result = 31 * result + (patientUUID != null ? patientUUID.hashCode() : 0);
        result = 31 * result + (createdAt != null ? createdAt.hashCode() : 0);
        result = 31 * result + (createdBy != null ? createdBy.hashCode() : 0);
        return result;
    }
}
