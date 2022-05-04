package com.apixio.model.cdi;

import org.joda.time.DateTime;

public class BatchedFile
{
    private int id;
    private String filePath;
    private String fileName;
    private String pdsId;
    private DateTime createdAt;
    private String createdBy;
    private Batch batch;
    private File file;
    private String originalFileId;
    private String lastStatusMessage;

    public BatchedFile()
    {}

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String getFilePath()
    {
        return filePath;
    }

    public BatchedFile setFilePath(String filePath)
    {
        this.filePath = filePath;
        return this;
    }

    public String getFileName()
    {
        return fileName;
    }

    public BatchedFile setFileName(String fileName)
    {
        this.fileName = fileName;
        return this;
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

    public String getPdsId()
    {
        return pdsId;
    }

    public BatchedFile setPdsId(String pdsId)
    {
        this.pdsId = pdsId;
        return this;
    }

    public Batch getBatch()
    {
        return batch;
    }

    public BatchedFile setBatch(Batch batch)
    {
        this.batch = batch;
        return this;
    }

    public File getFile()
    {
        return file;
    }

    public void setFile(File file)
    {
        this.file = file;
    }

    public String getLastStatusMessage() {
        return lastStatusMessage;
    }

    public BatchedFile setLastStatusMessage(String lastStatusMessage) {
        this.lastStatusMessage = lastStatusMessage;
        return this;
    }

    public String getOriginalFileId()
    {
        return originalFileId;
    }

    public void setOriginalFileId(String originalFileId)
    {
        this.originalFileId = originalFileId;
    }
}
