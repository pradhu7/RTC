package com.apixio.model.cdi;

import java.nio.file.Path;

import com.apixio.model.external.AxmSource;
import org.joda.time.LocalDate;

public class BatchConfig
{
    private int id;
    private BatchConfigType batchConfigType;
    private String path;
    private String sourceSystem;
    private String sourceType;
    private LocalDate sourceDate;
    private LocalDate dciStartDate;
    private LocalDate dciEndDate;
    private Integer maxFileSize;

    private Batch batch;

    public BatchConfig()
    {
    }

    public BatchConfig(BatchConfigType batchConfigType, String path)
    {
        this();
        this.batchConfigType = batchConfigType;
        this.path = path;
    }

    public BatchConfig(BatchConfigType batchConfigType, String path, String sourceSystem, String sourceType, LocalDate sourceDate, LocalDate dciStartDate, LocalDate dciEndDate)
    {
        this(batchConfigType, path);
        this.sourceSystem = sourceSystem;
        this.sourceType = sourceType;
        this.sourceDate = sourceDate;
        this.dciStartDate = dciStartDate;
        this.dciEndDate = dciEndDate;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public BatchConfigType getBatchConfigType()
    {
        return batchConfigType;
    }

    public void setBatchConfigType(BatchConfigType batchConfigType)
    {
        this.batchConfigType = batchConfigType;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public String getSourceSystem()
    {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem)
    {
        this.sourceSystem = sourceSystem;
    }

    public String getSourceType()
    {
        return sourceType;
    }

    public void setSourceType(String sourceType)
    {
        this.sourceType = sourceType;
    }

    public LocalDate getSourceDate()
    {
        return sourceDate;
    }

    public void setSourceDate(LocalDate sourceDate)
    {
        this.sourceDate = sourceDate;
    }

    public LocalDate getDciStartDate()
    {
        return dciStartDate;
    }

    public void setDciStartDate(LocalDate dciStartDate)
    {
        this.dciStartDate = dciStartDate;
    }

    public LocalDate getDciEndDate()
    {
        return dciEndDate;
    }

    public void setDciEndDate(LocalDate dciEndDate)
    {
        this.dciEndDate = dciEndDate;
    }

    public Batch getBatch()
    {
        return batch;
    }

    public void setBatch(Batch batch)
    {
        this.batch = batch;
    }

    public Integer getMaxFileSize()
    {
        return maxFileSize;
    }

    public void setMaxFileSize(Integer maxFileSize)
    {
        this.maxFileSize = maxFileSize;
    }

    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BatchConfig that = (BatchConfig) o;

        if (id != that.id) return false;
        if (batchConfigType != that.batchConfigType) return false;
        if (!path.equals(that.path)) return false;
        if (sourceSystem != null ? !sourceSystem.equals(that.sourceSystem) : that.sourceSystem != null) return false;
        if (sourceType != null ? !sourceType.equals(that.sourceType) : that.sourceType != null) return false;
        if (sourceDate != null ? !sourceDate.equals(that.sourceDate) : that.sourceDate != null) return false;
        if (dciStartDate != null ? !dciStartDate.equals(that.dciStartDate) : that.dciStartDate != null) return false;
        return dciEndDate != null ? dciEndDate.equals(that.dciEndDate) : that.dciEndDate == null;
    }

    public int hashCode()
    {
        int result = id;
        result = 31 * result + batchConfigType.hashCode();
        result = 31 * result + path.hashCode();
        result = 31 * result + (sourceSystem != null ? sourceSystem.hashCode() : 0);
        result = 31 * result + (sourceType != null ? sourceType.hashCode() : 0);
        result = 31 * result + (sourceDate != null ? sourceDate.hashCode() : 0);
        result = 31 * result + (dciStartDate != null ? dciStartDate.hashCode() : 0);
        result = 31 * result + (dciEndDate != null ? dciEndDate.hashCode() : 0);
        return result;
    }

    public static BatchConfig from(String name, Path dataPath, AxmSource source)
    {
        BatchConfig bc = new BatchConfig();
        bc.setBatchConfigType(BatchConfigType.valueOf(name));
        bc.setPath(dataPath.toAbsolutePath().toString());
        if (source != null)
        {
            bc.setSourceSystem(source.getSystem());
            bc.setSourceType(source.getType());
            bc.setSourceDate(source.getDate());
            bc.setDciStartDate(source.getDciStartDate());
            bc.setDciEndDate(source.getDciEndDate());
        }
        return bc;
    }
}
