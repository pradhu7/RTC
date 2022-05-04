package com.apixio.model.cdi;

import java.util.Set;

import org.joda.time.DateTime;

public class Package
{
    private int id;
    private String customerName;
    private String customerUUID;
    private String packageName;
    private String password;
    private OpStatus status;
    private int filesOnSftp;
    private int filesCopied;
    private int zipfiles;
    private int filesUnzipped;
    private int filesOnSource;
    private String lastError;
    private DateTime createdAt;
    private String createdBy;
    private DateTime modifiedAt;
    private String modifiedBy;

    private Set<File> files;

    public Package()
    {
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String getPackageName()
    {
        return packageName;
    }

    public void setPackageName(String packageName)
    {
        this.packageName = packageName;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public OpStatus getStatus()
    {
        return status;
    }

    public void setStatus(OpStatus status)
    {
        this.status = status;
    }

    public int getFilesOnSftp()
    {
        return filesOnSftp;
    }

    public void setFilesOnSftp(int filesOnSftp)
    {
        this.filesOnSftp = filesOnSftp;
    }

    public int getFilesCopied()
    {
        return filesCopied;
    }

    public void setFilesCopied(int filesCopied)
    {
        this.filesCopied = filesCopied;
    }

    public int getZipfiles()
    {
        return zipfiles;
    }

    public void setZipfiles(int zipfiles)
    {
        this.zipfiles = zipfiles;
    }

    public int getFilesUnzipped()
    {
        return filesUnzipped;
    }

    public void setFilesUnzipped(int filesUnzipped)
    {
        this.filesUnzipped = filesUnzipped;
    }

    public int getFilesOnSource()
    {
        return filesOnSource;
    }

    public void setFilesOnSource(int filesOnSource)
    {
        this.filesOnSource = filesOnSource;
    }

    public String getLastError()
    {
        return lastError;
    }

    public void setLastError(String lastError)
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

    public String getCustomerName()
    {
        return customerName;
    }

    public void setCustomerName(String customerName)
    {
        this.customerName = customerName;
    }

    public String getCustomerUUID()
    {
        return customerUUID;
    }

    public void setCustomerUUID(String customerUUID)
    {
        this.customerUUID = customerUUID;
    }

    public Set<File> getFiles()
    {
        return files;
    }

    public void setFiles(Set<File> files)
    {
        this.files = files;
    }

    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Package aPackage = (Package) o;

        if (id != aPackage.id) return false;
        if (filesOnSftp != aPackage.filesOnSftp) return false;
        if (filesCopied != aPackage.filesCopied) return false;
        if (zipfiles != aPackage.zipfiles) return false;
        if (filesUnzipped != aPackage.filesUnzipped) return false;
        if (filesOnSource != aPackage.filesOnSource) return false;
        if (customerName != null ? !customerName.equals(aPackage.customerName) : aPackage.customerName != null)
            return false;
        if (!customerUUID.equals(aPackage.customerUUID)) return false;
        if (!packageName.equals(aPackage.packageName)) return false;
        if (password != null ? !password.equals(aPackage.password) : aPackage.password != null) return false;
        if (status != null ? !status.equals(aPackage.status) : aPackage.status != null) return false;
        if (lastError != null ? !lastError.equals(aPackage.lastError) : aPackage.lastError != null) return false;
        if (!createdAt.equals(aPackage.createdAt)) return false;
        if (!createdBy.equals(aPackage.createdBy)) return false;
        if (modifiedAt != null ? !modifiedAt.equals(aPackage.modifiedAt) : aPackage.modifiedAt != null) return false;
        return modifiedBy != null ? modifiedBy.equals(aPackage.modifiedBy) : aPackage.modifiedBy == null;
    }

    public int hashCode()
    {
        int result = id;
        result = 31 * result + (customerName != null ? customerName.hashCode() : 0);
        result = 31 * result + customerUUID.hashCode();
        result = 31 * result + packageName.hashCode();
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + filesOnSftp;
        result = 31 * result + filesCopied;
        result = 31 * result + zipfiles;
        result = 31 * result + filesUnzipped;
        result = 31 * result + filesOnSource;
        result = 31 * result + (lastError != null ? lastError.hashCode() : 0);
        result = 31 * result + createdAt.hashCode();
        result = 31 * result + createdBy.hashCode();
        result = 31 * result + (modifiedAt != null ? modifiedAt.hashCode() : 0);
        result = 31 * result + (modifiedBy != null ? modifiedBy.hashCode() : 0);
        return result;
    }
}
