package com.apixio.model.trace;

import java.util.UUID;

/**
 * Document trace data written to Cassandra
 *
 * @author lance
 */

public class DocumentEntry
{
    private UUID   documentUUID;
    private String documentHash;
    private String orgId;
    private String documentId;
    private String sourceSystem;
    private String userName;
    private String patientId;
    private long   time;
    private String batchId;
    private long   documentSize;
    private String version;

    public DocumentEntry()
    {
        this.time = System.currentTimeMillis();
        version = "2";
    }

    public DocumentEntry(UUID documentUUID, String documentHash, String orgId, String patientId, String documentId, String sourceSystem, String username) {
        this.documentUUID = documentUUID;
        this.documentHash = documentHash;
        this.orgId = orgId;
        this.documentId = documentId;
        this.patientId = patientId;
        this.sourceSystem = sourceSystem;
        this.userName = username;
        this.time = System.currentTimeMillis();
        version = "1";
    }

    public UUID getDocumentUUID()
    {
        return documentUUID;
    }

    public void setDocumentUUID(UUID documentUUID)
    {
        this.documentUUID = documentUUID;
    }

    public String getDocumentHash()
    {
        return documentHash;
    }

    public void setDocumentHash(String documentHash)
    {
        this.documentHash = documentHash;
    }

    public String getOrgId()
    {
        return orgId;
    }

    public void setOrgId(String orgId)
    {
        this.orgId = orgId;
    }

    public String getDocumentId()
    {
        return documentId;
    }

    public void setDocumentId(String documentId)
    {
        this.documentId = documentId;
    }

    public String getSourceSystem()
    {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem)
    {
        this.sourceSystem = sourceSystem;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public long getTimeInMillis()
    {
        return time;
    }

    public void setTimeInMillis(long timeInMillis)
    {
        this.time = timeInMillis;
    }

    public String getPatientId()
    {
        return patientId;
    }

    public void setPatientId(String patientId)
    {
        this.patientId = patientId;
    }

    public long getTime()
    {
        return time;
    }

    public void setTime(long time)
    {
        this.time = time;
    }

    public String getBatchId()
    {
        return batchId;
    }

    public void setBatchId(String batchId)
    {
        this.batchId = batchId;
    }

    public long getDocumentSize()
    {
        return documentSize;
    }

    public void setDocumentSize(long documentSize)
    {
        this.documentSize = documentSize;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    @Override
    public String toString()
    {
        return ("[DocumentEntry: "+
                "; documentUUID=" + documentUUID +
                "; documentHash=" + documentHash +
                "; orgId=" + orgId +
                "; documentId=" + documentId +
                "; sourceSystem=" + sourceSystem +
                "; userName=" + userName +
                "; patientId=" + patientId +
                "; time=" + time +
                "; batchId=" + batchId +
                "; documentSize=" + documentSize +
                "; version=" + version +
                "]");
    }
}
