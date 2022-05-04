package com.apixio.model.cdi;

import org.joda.time.DateTime;

/**
 * Created by ssiddiqui on 9/17/17.
 */
public class UploadBatchReport {
    Integer id;
    Integer batchId;
    String report;
    DateTime createdAt;

    public UploadBatchReport() {}

    public UploadBatchReport(Integer id, Integer batchId, String report, DateTime createdAt)
    {
        this.id = id;
        this.batchId = batchId;
        this.report = report;
        this.createdAt = createdAt;
    }

    public Integer getId() {
        return id;
    }

    public UploadBatchReport setId(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getBatchId() {
        return batchId;
    }

    public UploadBatchReport setBatchId(Integer batchId) {
        this.batchId = batchId;
        return this;
    }

    public String getReport() {
        return report;
    }

    public UploadBatchReport setReport(String report) {
        this.report = report;
        return this;
    }

    public DateTime getCreatedAt() {
        return createdAt;
    }

    public UploadBatchReport setCreatedAt(DateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }
}
