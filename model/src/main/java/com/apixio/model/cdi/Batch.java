package com.apixio.model.cdi;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.joda.time.DateTime;

public class Batch
{
    private int id;
    private String name;
    private String customerUUID;
    private String pdsId;
    private String configFileUrl;
    private String workingPath;
    private String required;
    private BatchStatus status;
    private long validatedTimeInSec;
    private long lastUploadedTimeInSec;
    private int lastUploadedApxCnt;
    private DateTime createdAt;
    private String createdBy;
    private DateTime modifiedAt;
    private String modifiedBy;

    private Set<BatchedFile> batchedFiles;
    private Collection<UploadedFile> uploadedFiles;
    private LinkedHashSet<BatchConfig> batchedConfigs;

    public Batch()
    {
        batchedFiles = new LinkedHashSet<>();
        // Uploaded files may be added by multiple threads
        uploadedFiles = Collections.newSetFromMap(new ConcurrentHashMap<UploadedFile, Boolean>());
        batchedConfigs = new LinkedHashSet<>();
    }

    public Batch(String name, String workingPath, String actor, DateTime txTime)
    {
        this();
        this.name = name;
        this.workingPath = workingPath;
        this.createdAt = txTime;
        this.createdBy = actor;
        this.modifiedAt = txTime;
        this.modifiedBy = actor;
    }

    public Batch(String name, String customerUUID, String pdsId, BatchStatus status, DateTime createdAt, String createdBy, DateTime modifiedAt, String modifiedBy)
    {
        this();
        this.name = name;
        this.customerUUID = customerUUID;
        this.pdsId = pdsId;
        this.status = status;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.modifiedAt = modifiedAt;
        this.modifiedBy = modifiedBy;
    }

    public Batch(String name)
    {
        this();
        this.name = name;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getPdsId()
    {
        return pdsId;
    }

    public void setPdsId(String pdsId)
    {
        this.pdsId = pdsId;
    }

    public String getWorkingPath()
    {
        return workingPath;
    }

    public void setWorkingPath(String workingPath)
    {
        this.workingPath = workingPath;
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

    public DateTime getModifiedAt()
    {
        return modifiedAt;
    }

    public void setModifiedAt(DateTime modifiedAt)
    {
        this.modifiedAt = modifiedAt;
    }

    public String getModifiedBy()
    {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy)
    {
        this.modifiedBy = modifiedBy;
    }

    public String getRequired()
    {
        return required;
    }

    public void setRequired(String required)
    {
        this.required = required;
    }

    public String getCustomerUUID()
    {
        return customerUUID;
    }

    public void setCustomerUUID(String customerUUID)
    {
        this.customerUUID = customerUUID;
    }

    public String getConfigFileUrl()
    {
        return configFileUrl;
    }

    public void setConfigFileUrl(String configFileUrl)
    {
        this.configFileUrl = configFileUrl;
    }

    public BatchStatus getStatus()
    {
        return status;
    }

    public void setStatus(BatchStatus status)
    {
        this.status = status;
    }

    public long getValidatedTimeInSec()
    {
        return validatedTimeInSec;
    }

    public void setValidatedTimeInSec(long validatedTimeInSec)
    {
        this.validatedTimeInSec = validatedTimeInSec;
    }

    public long getLastUploadedTimeInSec()
    {
        return lastUploadedTimeInSec;
    }

    public void setLastUploadedTimeInSec(long lastUploadedTimeInSec)
    {
        this.lastUploadedTimeInSec = lastUploadedTimeInSec;
    }

    public int getLastUploadedApxCnt()
    {
        return lastUploadedApxCnt;
    }

    public void setLastUploadedApxCnt(int lastUploadedApxCnt)
    {
        this.lastUploadedApxCnt = lastUploadedApxCnt;
    }

    public Set<BatchedFile> getBatchedFiles()
    {
        return batchedFiles;
    }

    public void setBatchedFiles(Set<BatchedFile> batchedFiles)
    {
        this.batchedFiles = batchedFiles;
    }

    public Collection<UploadedFile> getUploadedFiles()
    {
        return uploadedFiles;
    }

    public void setUploadedFiles(Set<UploadedFile> uploadedFiles)
    {
        this.uploadedFiles = uploadedFiles;
    }

    public LinkedHashSet<BatchConfig> getBatchedConfigs()
    {
        return batchedConfigs;
    }

    public void setBatchedConfigs(LinkedHashSet<BatchConfig> batchedConfigs)
    {
        this.batchedConfigs = batchedConfigs;
    }

    public void addBatchConfig(BatchConfig batchConfig)
    {
        Objects.requireNonNull(batchConfig, "batchConfig is required");
        this.batchedConfigs.add(batchConfig);
    }

    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Batch batch = (Batch) o;

        if (id != batch.id) return false;
        if (validatedTimeInSec != batch.validatedTimeInSec) return false;
        if (lastUploadedTimeInSec != batch.lastUploadedTimeInSec) return false;
        if (lastUploadedApxCnt != batch.lastUploadedApxCnt) return false;
        if (!name.equals(batch.name)) return false;
        if (!customerUUID.equals(batch.customerUUID)) return false;
        if (!pdsId.equals(batch.pdsId)) return false;
        if (configFileUrl != null ? !configFileUrl.equals(batch.configFileUrl) : batch.configFileUrl != null)
            return false;
        if (workingPath != null ? !workingPath.equals(batch.workingPath) : batch.workingPath != null) return false;
        if (required != null ? !required.equals(batch.required) : batch.required != null) return false;
        if (status != batch.status) return false;
        if (createdAt != null ? !createdAt.equals(batch.createdAt) : batch.createdAt != null) return false;
        if (createdBy != null ? !createdBy.equals(batch.createdBy) : batch.createdBy != null) return false;
        if (modifiedAt != null ? !modifiedAt.equals(batch.modifiedAt) : batch.modifiedAt != null) return false;
        return modifiedBy != null ? modifiedBy.equals(batch.modifiedBy) : batch.modifiedBy == null;
    }

    public int hashCode()
    {
        int result = id;
        result = 31 * result + name.hashCode();
        result = 31 * result + customerUUID.hashCode();
        result = 31 * result + pdsId.hashCode();
        result = 31 * result + (configFileUrl != null ? configFileUrl.hashCode() : 0);
        result = 31 * result + (workingPath != null ? workingPath.hashCode() : 0);
        result = 31 * result + (required != null ? required.hashCode() : 0);
        result = 31 * result + status.hashCode();
        result = 31 * result + (int) (validatedTimeInSec ^ (validatedTimeInSec >>> 32));
        result = 31 * result + (int) (lastUploadedTimeInSec ^ (lastUploadedTimeInSec >>> 32));
        result = 31 * result + lastUploadedApxCnt;
        result = 31 * result + (createdAt != null ? createdAt.hashCode() : 0);
        result = 31 * result + (createdBy != null ? createdBy.hashCode() : 0);
        result = 31 * result + (modifiedAt != null ? modifiedAt.hashCode() : 0);
        result = 31 * result + (modifiedBy != null ? modifiedBy.hashCode() : 0);
        return result;
    }
}
