package com.apixio.model.trace;

import java.util.List;
import java.util.UUID;

public class DocumentSummaryTrace
{
    private UUID                documentUUID;
    private String              batchId;
    private DocumentEntry       documentEntry;
    private List<DocumentTrace> documentTraces;

    public DocumentSummaryTrace()
    {
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

    public DocumentEntry getDocumentEntry()
    {
        return documentEntry;
    }

    public void setDocumentEntry(DocumentEntry documentEntry)
    {
        this.documentEntry = documentEntry;
    }

    public List<DocumentTrace> getDocumentTraces()
    {
        return documentTraces;
    }

    public void setDocumentTraces(List<DocumentTrace> documentTraces)
    {
        this.documentTraces = documentTraces;
    }
}
