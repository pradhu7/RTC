package com.apixio.model.file;

import com.apixio.model.file.catalog.jaxb.generated.ApxCatalog;

import java.util.Properties;

/**
 * Created by dyee on 2/12/17.
 */
abstract public class ApxPackagedBase
{
    protected String versionString = "1.0.0";

    protected Properties metadata;

    protected String metadataFileName;

    protected String catalogFileName;

    protected ApxCatalog catalog;

    protected String fileName;

    public Properties getMetadata()
    {
        return metadata;
    }

    public void setMetadata(Properties metadata)
    {
        this.metadata = metadata;
    }

    public String getCatalogFileName()
    {
        return catalogFileName;
    }

    public void setCatalogFileName(String catalogFileName)
    {
        this.catalogFileName = catalogFileName;
    }

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public String getMetadataFileName()
    {
        return metadataFileName;
    }

    public void setMetadataFileName(String metadataFileName)
    {
        this.metadataFileName = metadataFileName;
    }

    public ApxCatalog getCatalog()
    {
        return catalog;
    }

    public void setCatalog(ApxCatalog catalog)
    {
        this.catalog = catalog;
    }

    public String getVersionString()
    {
        return versionString;
    }
}
