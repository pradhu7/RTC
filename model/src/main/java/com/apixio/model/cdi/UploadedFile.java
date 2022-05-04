package com.apixio.model.cdi;

import java.util.Set;

import org.joda.time.DateTime;

public class UploadedFile
{
    private int id;
    private DateTime uploadedAt;
    private String patientExternalId;
    private String apxPackageUUID;  // either document UUID or Patient UUID
    private String originalFileId;
    private String fileUrl;
    private String documentTitle;
    private long uploadSize;
    private ApxPackageType apxPackageType;
    private int attemptsCount;
    private int lastStatusCode;
    private String lastError;
    private OpStatus status;
    private String pdsId;
    private DateTime docDate;
    private String mimeType;
    private Boolean persisted;
    private BatchStatus ocrStatus;
    private int pageCount;
    private int ocrSuccessPct;
    private int eventCount;
    private int ocrMissingPageCnt;
    private int pagerMissingPageCnt;
    private int eventCnt;
    private boolean eventExtractionComplete;
    private String patientUUID;
    private boolean hasDemographics;
    private int eligibleCount;
    private int raClaimCount;
    private int ffsClaimCount;
    private int rapCount;
    private int mao004Count;
    private int documentCount;
    private int problemCount;
    private int encounterCount;
    private int procedureCount;
    private int providerCount;
    private int prescriptionsCount;
    private int labResultsCount;
    private DateTime firstSeen;
    private String firstSeenWith;

    private BatchStatus summaryExtractionStatus;
    private Batch batch;

    private Set<UploadedFileLink> uploadedFileToFileLinks;

    public UploadedFile()
    {
        status = OpStatus.ADDED;
    }

    public UploadedFile(DateTime uploadedAt, String patientExternalId, String apxPackageUUID, String originalFileId, String fileUrl, String documentTitle, long uploadSize, ApxPackageType apxPackageType, int attemptsCount, int lastStatusCode, String lastError)
    {
        this();
        this.uploadedAt = uploadedAt;
        this.patientExternalId = patientExternalId;
        this.apxPackageUUID = apxPackageUUID;
        this.originalFileId = originalFileId;
        this.fileUrl = fileUrl;
        this.documentTitle = documentTitle;
        this.uploadSize = uploadSize;
        this.apxPackageType = apxPackageType;
        this.attemptsCount = attemptsCount;
        this.lastStatusCode = lastStatusCode;
        this.lastError = lastError;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public DateTime getUploadedAt()
    {
        return uploadedAt;
    }

    public void setUploadedAt(DateTime uploadedAt)
    {
        this.uploadedAt = uploadedAt;
    }

    public String getPatientExternalId()
    {
        return patientExternalId;
    }

    public void setPatientExternalId(String patientExternalId)
    {
        this.patientExternalId = patientExternalId;
    }

    public String getApxPackageUUID()
    {
        return apxPackageUUID;
    }

    public void setApxPackageUUID(String apxPackageUUID)
    {
        this.apxPackageUUID = apxPackageUUID;
    }

    public String getOriginalFileId()
    {
        return originalFileId;
    }

    public void setOriginalFileId(String originalFileId)
    {
        this.originalFileId = originalFileId;
    }

    public String getFileUrl()
    {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl)
    {
        this.fileUrl = fileUrl;
    }

    public String getDocumentTitle()
    {
        return documentTitle;
    }

    public void setDocumentTitle(String documentTitle)
    {
        this.documentTitle = documentTitle;
    }

    public long getUploadSize()
    {
        return uploadSize;
    }

    public void setUploadSize(long uploadSize)
    {
        this.uploadSize = uploadSize;
    }

    public ApxPackageType getApxPackageType()
    {
        return apxPackageType;
    }

    public void setApxPackageType(ApxPackageType apxPackageType)
    {
        this.apxPackageType = apxPackageType;
    }

    public int getAttemptsCount()
    {
        return attemptsCount;
    }

    public void setAttemptsCount(int attemptsCount)
    {
        this.attemptsCount = attemptsCount;
    }

    public int getLastStatusCode()
    {
        return lastStatusCode;
    }

    public void setLastStatusCode(int lastStatusCode)
    {
        this.lastStatusCode = lastStatusCode;
    }

    public String getLastError()
    {
        return lastError;
    }

    public void setLastError(String lastError)
    {
        this.lastError = lastError;
    }

    public OpStatus getStatus()
    {
        return status;
    }

    public void setStatus(OpStatus status)
    {
        this.status = status;
    }

    public Boolean getPersisted()
    {
        return persisted;
    }

    public void setPersisted(Boolean persisted)
    {
        this.persisted = persisted;
    }

    public BatchStatus getOcrStatus()
    {
        return ocrStatus;
    }

    public void setOcrStatus(BatchStatus ocrStatus)
    {
        this.ocrStatus = ocrStatus;
    }

    public int getPageCount()
    {
        return pageCount;
    }

    public void setPageCount(int pageCount)
    {
        this.pageCount = pageCount;
    }

    public BatchStatus getSummaryExtractionStatus()
    {
        return summaryExtractionStatus;
    }

    public void setSummaryExtractionStatus(BatchStatus summaryExtractionStatus)
    {
        this.summaryExtractionStatus = summaryExtractionStatus;
    }

    public String getPdsId()
    {
        return pdsId;
    }

    public void setPdsId(String pdsId)
    {
        this.pdsId = pdsId;
    }

    public DateTime getDocDate()
    {
        return docDate;
    }

    public void setDocDate(DateTime docDate)
    {
        this.docDate = docDate;
    }

    public String getMimeType()
    {
        return mimeType;
    }

    public void setMimeType(String mimeType)
    {
        this.mimeType = mimeType;
    }

    public int getOcrSuccessPct()
    {
        return ocrSuccessPct;
    }

    public void setOcrSuccessPct(int ocrSuccessPct)
    {
        this.ocrSuccessPct = ocrSuccessPct;
    }

    public int getEventCount()
    {
        return eventCount;
    }

    public void setEventCount(int eventCount)
    {
        this.eventCount = eventCount;
    }

    public int getOcrMissingPageCnt()
    {
        return ocrMissingPageCnt;
    }

    public void setOcrMissingPageCnt(int ocrMissingPageCnt)
    {
        this.ocrMissingPageCnt = ocrMissingPageCnt;
    }

    public int getPagerMissingPageCnt()
    {
        return pagerMissingPageCnt;
    }

    public void setPagerMissingPageCnt(int pagerMissingPageCnt)
    {
        this.pagerMissingPageCnt = pagerMissingPageCnt;
    }

    public int getEventCnt()
    {
        return eventCnt;
    }

    public void setEventCnt(int eventCnt)
    {
        this.eventCnt = eventCnt;
    }

    public boolean isEventExtractionComplete()
    {
        return eventExtractionComplete;
    }

    public void setEventExtractionComplete(boolean eventExtractionComplete)
    {
        this.eventExtractionComplete = eventExtractionComplete;
    }

    public String getPatientUUID()
    {
        return patientUUID;
    }

    public void setPatientUUID(String patientUUID)
    {
        this.patientUUID = patientUUID;
    }

    public boolean isHasDemographics()
    {
        return hasDemographics;
    }

    public void setHasDemographics(boolean hasDemographics)
    {
        this.hasDemographics = hasDemographics;
    }

    public int getEligibleCount()
    {
        return eligibleCount;
    }

    public void setEligibleCount(int eligibleCount)
    {
        this.eligibleCount = eligibleCount;
    }

    public int getRaClaimCount()
    {
        return raClaimCount;
    }

    public void setRaClaimCount(int raClaimCount)
    {
        this.raClaimCount = raClaimCount;
    }

    public int getFfsClaimCount()
    {
        return ffsClaimCount;
    }

    public void setFfsClaimCount(int ffsClaimCount)
    {
        this.ffsClaimCount = ffsClaimCount;
    }

    public int getRapCount()
    {
        return rapCount;
    }

    public void setRapCount(int rapCount)
    {
        this.rapCount = rapCount;
    }

    public int getMao004Count()
    {
        return mao004Count;
    }

    public void setMao004Count(int mao004Count)
    {
        this.mao004Count = mao004Count;
    }

    public int getDocumentCount()
    {
        return documentCount;
    }

    public void setDocumentCount(int documentCount)
    {
        this.documentCount = documentCount;
    }

    public int getProblemCount()
    {
        return problemCount;
    }

    public void setProblemCount(int problemCount)
    {
        this.problemCount = problemCount;
    }

    public int getEncounterCount()
    {
        return encounterCount;
    }

    public void setEncounterCount(int encounterCount)
    {
        this.encounterCount = encounterCount;
    }

    public int getProcedureCount()
    {
        return procedureCount;
    }

    public void setProcedureCount(int procedureCount)
    {
        this.procedureCount = procedureCount;
    }

    public int getProviderCount()
    {
        return providerCount;
    }

    public void setProviderCount(int providerCount)
    {
        this.providerCount = providerCount;
    }

    public int getPrescriptionsCount()
    {
        return prescriptionsCount;
    }

    public void setPrescriptionsCount(int prescriptionsCount)
    {
        this.prescriptionsCount = prescriptionsCount;
    }

    public int getLabResultsCount()
    {
        return labResultsCount;
    }

    public void setLabResultsCount(int labResultsCount)
    {
        this.labResultsCount = labResultsCount;
    }

    public DateTime getFirstSeen()
    {
        return firstSeen;
    }

    public void setFirstSeen(DateTime firstSeen)
    {
        this.firstSeen = firstSeen;
    }

    public String getFirstSeenWith()
    {
        return firstSeenWith;
    }

    public void setFirstSeenWith(String firstSeenWith)
    {
        this.firstSeenWith = firstSeenWith;
    }

    public Batch getBatch()
    {
        return batch;
    }

    public void setBatch(Batch batch)
    {
        this.batch = batch;
    }

    public Set<UploadedFileLink> getUploadedFileToFileLinks()
    {
        return uploadedFileToFileLinks;
    }

    public void setUploadedFileToFileLinks(Set<UploadedFileLink> uploadedFileToFileLinks)
    {
        this.uploadedFileToFileLinks = uploadedFileToFileLinks;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UploadedFile that = (UploadedFile) o;

        if (id != that.id) return false;
        if (uploadSize != that.uploadSize) return false;
        if (attemptsCount != that.attemptsCount) return false;
        if (lastStatusCode != that.lastStatusCode) return false;
        if (pageCount != that.pageCount) return false;
        if (uploadedAt != null ? !uploadedAt.equals(that.uploadedAt) : that.uploadedAt != null) return false;
        if (patientExternalId != null ? !patientExternalId.equals(that.patientExternalId) : that.patientExternalId != null)
            return false;
        if (apxPackageUUID != null ? !apxPackageUUID.equals(that.apxPackageUUID) : that.apxPackageUUID != null)
            return false;
        if (originalFileId != null ? !originalFileId.equals(that.originalFileId) : that.originalFileId != null)
            return false;
        if (fileUrl != null ? !fileUrl.equals(that.fileUrl) : that.fileUrl != null) return false;
        if (documentTitle != null ? !documentTitle.equals(that.documentTitle) : that.documentTitle != null) return false;
        if (apxPackageType != that.apxPackageType) return false;
        if (lastError != null ? !lastError.equals(that.lastError) : that.lastError != null)
            return false;
        if (persisted != null ? !persisted.equals(that.persisted) : that.persisted != null) return false;
        if (ocrStatus != that.ocrStatus) return false;
        if (summaryExtractionStatus != that.summaryExtractionStatus) return false;
        if (batch != null ? !batch.equals(that.batch) : that.batch != null) return false;
        return uploadedFileToFileLinks != null ? uploadedFileToFileLinks.equals(that.uploadedFileToFileLinks) : that.uploadedFileToFileLinks == null;
    }

    @Override
    public int hashCode()
    {
        int result = id;
        result = 31 * result + (patientExternalId != null ? patientExternalId.hashCode() : 0);
        result = 31 * result + (apxPackageUUID != null ? apxPackageUUID.hashCode() : 0);
        result = 31 * result + (originalFileId != null ? originalFileId.hashCode() : 0);
        result = 31 * result + (fileUrl != null ? fileUrl.hashCode() : 0);
        result = 31 * result + (documentTitle != null ? documentTitle.hashCode() : 0);
        result = 31 * result + (int) (uploadSize ^ (uploadSize >>> 32));
        result = 31 * result + (apxPackageType != null ? apxPackageType.hashCode() : 0);
        result = 31 * result + attemptsCount;
        result = 31 * result + lastStatusCode;
        result = 31 * result + (lastError != null ? lastError.hashCode() : 0);
        result = 31 * result + (persisted != null ? persisted.hashCode() : 0);
        result = 31 * result + (ocrStatus != null ? ocrStatus.hashCode() : 0);
        result = 31 * result + pageCount;
        result = 31 * result + (summaryExtractionStatus != null ? summaryExtractionStatus.hashCode() : 0);
        return result;
    }
}
