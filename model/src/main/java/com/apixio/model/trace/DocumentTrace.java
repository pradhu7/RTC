package com.apixio.model.trace;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.apixio.model.patient.ParsingDetail;

/**
 * Temporary structure containing all details of processing a document.
 * Used across Hadoop jobs.
 *
 * @author lance
 */

public class DocumentTrace
{
    // Those fields are stored in more details
    public static final String TRACE_PATIENTKEY = "patientkey";
    public static final String TRACE_COLUMNFAMILY = "columnfamily";
    public static final String TRACE_DOCSIZE = "docsize";

    private UUID   documentUUID;
    private String batchId; // pipeline/coordinator batch id
    private String uploadBatchId; // batch name that comes from the original catalog file uploaded
    private String extractBatchId; // calculated based on properties of an extraction. if a document is in multiple extract batches it should be traced once for each.
    private String jobId;
    private String sequenceFileName;
    private String processName;
    private String processStatusMessage;

    // TODO I need an object which resembles ParsingDetail, which has contentSourceDocumentURI, so I just used this.
    // Depending on the file abstraction changes, this may not be useful. In that case, I will create a different trace object.
    private ParsingDetail       parsingDetail;
    private Map<String, Object> moreDetails;
    private String              version;
    private long                time;

    public DocumentTrace()
    {
        this.time = System.currentTimeMillis();
        setVersion("2");
    }

    public UUID getDocumentUUID()
    {
        return documentUUID;
    }

    public void setDocumentUUID(UUID documentUUID)
    {
        this.documentUUID = documentUUID;
    }

    public String getBatchId()
    {
        return batchId;
    }

    public void setBatchId(String batchId)
    {
        this.batchId = batchId;
    }

    public String getUploadBatchId()
    {
        return uploadBatchId;
    }

    public void setUploadBatchId(String uploadBatchId)
    {
        this.uploadBatchId = uploadBatchId;
    }

    public String getExtractBatchId()
    {
        return extractBatchId;
    }

    public void setExtractBatchId(String extractBatchId)
    {
        this.extractBatchId = extractBatchId;
    }

    public String getJobId()
    {
        return jobId;
    }

    public void setJobId(String jobId)
    {
        this.jobId = jobId;
    }

    public String getSequenceFileName()
    {
        return sequenceFileName;
    }

    public void setSequenceFileName(String sequenceFileName)
    {
        this.sequenceFileName = sequenceFileName;
    }

    public String getProcessName()
    {
        return processName;
    }

    public void setProcessName(String processName)
    {
        this.processName = processName;
    }

    public String getProcessStatusMessage()
    {
        return processStatusMessage;
    }

    public void setProcessStatusMessage(String processStatusMessage)
    {
        this.processStatusMessage = processStatusMessage;
    }

    public ParsingDetail getParsingDetail()
    {
        return parsingDetail;
    }

    public void setParsingDetail(ParsingDetail parsingDetail)
    {
        this.parsingDetail = parsingDetail;
    }

    public Map<String, Object> getMoreDetails()
    {
        if (moreDetails == null)
            moreDetails = new HashMap<>();

        return moreDetails;
    }

    public void setMoreDetails(Map<String, Object> moreDetails)
    {
        this.moreDetails = moreDetails;
    }

    public Object getDetail(String detailName)
    {
        if (moreDetails == null)
            return null;

        return moreDetails.get(detailName);
    }

    public void setDetail(String detailName, Object detailValue)
    {
        if (moreDetails == null)
            moreDetails = new HashMap<>();

        moreDetails.put(detailName, detailValue);
    }

    public long getTimeInMillis()
    {
        return time;
    }

    public void setTimeInMillis(long timeInMillis)
    {
        this.time = timeInMillis;
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
        return ("[DocumentTrace: "+
                "; documentUUID=" + documentUUID +
                "; batchId=" + batchId +
                "; jobId=" + jobId +
                "; sequenceFileName=" + sequenceFileName +
                "; processName=" + processName +
                "; processStatusMessage=" + processStatusMessage +
                "; parsingDetail=" + parsingDetail +
                "; moreDetails=" + moreDetails +
                "; version=" + version +
                "; time=" + time +
                "]");
    }
}
