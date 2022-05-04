package com.apixio.model.cdi;

import java.util.LinkedHashSet;
import java.util.Set;

import org.joda.time.DateTime;

public class File
{
    private Integer id;
    private String folderName;
    private String fileName;
    private long size;
    private String hashCode;
    private DocumentType documentType;
    private DateTime sourceDate;
    private DateTime receivedDate;
    private int version;
    private OpStatus status;
    private String multipartFilename;
    private ErrorType lastError;
    private DateTime createdDate;
    private DateTime modifiedDate;
    private DateTime createdAt;
    private String createdBy;
    private DateTime modifiedAt;
    private String modifiedBy;

    // Transient data
    private File parent;
    private Package filePackage;
    private String fileExtension;
    private Set<BatchedFile> batchedFiles;

    public File()
    {
        batchedFiles = new LinkedHashSet<>();
    }

    public File(String folderName, String fileName)
    {
        this();
        this.folderName = folderName;
        this.fileName = fileName;
        this.sourceDate = new DateTime();
    }

    public File(int id, String folderName, String fileName, DateTime sourceDate)
    {
        this();
        this.id = id;
        this.folderName = folderName;
        this.fileName = fileName;
        this.sourceDate = sourceDate;
    }

    public Integer getId()
    {
        return id;
    }

    public void setId(Integer id)
    {
        this.id = id;
    }

    public String getFolderName()
    {
        return folderName;
    }

    public void setFolderName(String folderName)
    {
        this.folderName = folderName;
    }

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public String getHashCode()
    {
        return hashCode;
    }

    public void setHashCode(String hashCode)
    {
        this.hashCode = hashCode;
    }

    public DateTime getSourceDate()
    {
        return sourceDate;
    }

    public void setSourceDate(DateTime sourceDate)
    {
        this.sourceDate = sourceDate;
    }

    public DateTime getReceivedDate()
    {
        return receivedDate;
    }

    public void setReceivedDate(DateTime receivedDate)
    {
        this.receivedDate = receivedDate;
    }

    public int getVersion()
    {
        return version;
    }

    public void setVersion(int version)
    {
        this.version = version;
    }

    public String getMultipartFilename()
    {
        return multipartFilename;
    }

    public void setMultipartFilename(String multipartFilename)
    {
        this.multipartFilename = multipartFilename;
    }

    public ErrorType getLastError()
    {
        return lastError;
    }

    public void setLastError(ErrorType lastError)
    {
        this.lastError = lastError;
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

    public File getParent()
    {
        return parent;
    }

    public void setParent(File parent)
    {
        this.parent = parent;
    }

    public Package getFilePackage()
    {
        return filePackage;
    }

    public void setFilePackage(Package filePackage)
    {
        this.filePackage = filePackage;
    }

    public Set<BatchedFile> getBatchedFiles()
    {
        return batchedFiles;
    }

    public void setBatchedFiles(Set<BatchedFile> batchedFiles)
    {
        this.batchedFiles = batchedFiles;
    }

    public String getFileExtension()
    {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension)
    {
        this.fileExtension = fileExtension;
    }

    public DateTime getCreatedDate()
    {
        return createdDate;
    }

    public void setCreatedDate(DateTime createdDate)
    {
        this.createdDate = createdDate;
    }

    public DateTime getModifiedDate()
    {
        return modifiedDate;
    }

    public void setModifiedDate(DateTime modifiedDate)
    {
        this.modifiedDate = modifiedDate;
    }

    public long getSize()
    {
        return size;
    }

    public void setSize(long size)
    {
        this.size = size;
    }

    public OpStatus getStatus()
    {
        return status;
    }

    public void setStatus(OpStatus status)
    {
        this.status = status;
    }

    public DocumentType getDocumentType()
    {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType)
    {
        this.documentType = documentType;
    }

    public Boolean exists() {
        return lastError == null || !lastError.equals(ErrorType.FILE_NOT_FOUND);
    }

    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        File file = (File) o;

        if (id != file.id) return false;
        if (size != file.size) return false;
        if (version != file.version) return false;
        if (!folderName.equals(file.folderName)) return false;
        if (!fileName.equals(file.fileName)) return false;
        if (hashCode != null ? !hashCode.equals(file.hashCode) : file.hashCode != null) return false;
        if (documentType != file.documentType) return false;
        if (sourceDate != null ? !sourceDate.equals(file.sourceDate) : file.sourceDate != null) return false;
        if (receivedDate != null ? !receivedDate.equals(file.receivedDate) : file.receivedDate != null) return false;
        if (status != null ? !status.equals(file.status) : file.status != null) return false;
        if (multipartFilename != null ? !multipartFilename.equals(file.multipartFilename) : file.multipartFilename != null)
            return false;
        if (lastError != null ? !lastError.equals(file.lastError) : file.lastError != null) return false;
        if (createdDate != null ? !createdDate.equals(file.createdDate) : file.createdDate != null) return false;
        if (modifiedDate != null ? !modifiedDate.equals(file.modifiedDate) : file.modifiedDate != null) return false;
        if (createdAt != null ? !createdAt.equals(file.createdAt) : file.createdAt != null) return false;
        if (createdBy != null ? !createdBy.equals(file.createdBy) : file.createdBy != null) return false;
        if (modifiedAt != null ? !modifiedAt.equals(file.modifiedAt) : file.modifiedAt != null) return false;
        if (modifiedBy != null ? !modifiedBy.equals(file.modifiedBy) : file.modifiedBy != null) return false;
        if (parent != null ? !parent.equals(file.parent) : file.parent != null) return false;
        if (filePackage != null ? !filePackage.equals(file.filePackage) : file.filePackage != null) return false;
        return fileExtension != null ? fileExtension.equals(file.fileExtension) : file.fileExtension == null;
    }

    public int hashCode()
    {
        int result = id;
        result = 31 * result + folderName.hashCode();
        result = 31 * result + fileName.hashCode();
        result = 31 * result + (int) (size ^ (size >>> 32));
        result = 31 * result + (hashCode != null ? hashCode.hashCode() : 0);
        result = 31 * result + (documentType != null ? documentType.hashCode() : 0);
        result = 31 * result + (sourceDate != null ? sourceDate.hashCode() : 0);
        result = 31 * result + (receivedDate != null ? receivedDate.hashCode() : 0);
        result = 31 * result + version;
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (multipartFilename != null ? multipartFilename.hashCode() : 0);
        result = 31 * result + (lastError != null ? lastError.hashCode() : 0);
        result = 31 * result + (createdDate != null ? createdDate.hashCode() : 0);
        result = 31 * result + (modifiedDate != null ? modifiedDate.hashCode() : 0);
        result = 31 * result + (createdAt != null ? createdAt.hashCode() : 0);
        result = 31 * result + (createdBy != null ? createdBy.hashCode() : 0);
        result = 31 * result + (modifiedAt != null ? modifiedAt.hashCode() : 0);
        result = 31 * result + (modifiedBy != null ? modifiedBy.hashCode() : 0);
        result = 31 * result + (parent != null ? parent.hashCode() : 0);
        result = 31 * result + (filePackage != null ? filePackage.hashCode() : 0);
        result = 31 * result + (fileExtension != null ? fileExtension.hashCode() : 0);
        return result;
    }
}
