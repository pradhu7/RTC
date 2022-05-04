package com.apixio.model.cdi;

import org.joda.time.DateTime;

public class PatientCompleteness
{
    private int id;
    private String patientExternalId;
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
    private int biometricsCount;
    private int socialHistoryCount;
    private DateTime firstSeen;
    private String firstSeenWith;

    private UploadedFile uploadedFile;

    public PatientCompleteness()
    {
    }

    public PatientCompleteness(String patientExternalId, String patientUUID, boolean hasDemographics, int eligibleCount,
                               int raClaimCount, int ffsClaimCount, int rapCount, int mao004Count,
                               int documentCount, int problemCount, int encounterCount, int procedureCount,
                               int providerCount, int prescriptionsCount, int labResultsCount, int biometricsCount,
                               int socialHistoryCount, DateTime firstSeen, String firstSeenWith)
    {
        this.patientExternalId = patientExternalId;
        this.patientUUID = patientUUID;
        this.hasDemographics = hasDemographics;
        this.eligibleCount = eligibleCount;
        this.raClaimCount = raClaimCount;
        this.ffsClaimCount = ffsClaimCount;
        this.rapCount = rapCount;
        this.mao004Count = mao004Count;
        this.documentCount = documentCount;
        this.problemCount = problemCount;
        this.encounterCount = encounterCount;
        this.procedureCount = procedureCount;
        this.providerCount = providerCount;
        this.prescriptionsCount = prescriptionsCount;
        this.labResultsCount = labResultsCount;
        this.biometricsCount = biometricsCount;
        this.socialHistoryCount = socialHistoryCount;

        this.firstSeen = firstSeen;
        this.firstSeenWith = firstSeenWith;
    }

    public PatientCompleteness merge(PatientCompleteness other)
    {
        PatientCompleteness merged = new PatientCompleteness();

        merged.id = this.id != 0 ? this.id : other.id;
        merged.patientExternalId = this.patientExternalId != null ? this.patientExternalId : other.patientExternalId;
        merged.hasDemographics = this.hasDemographics ? true : other.hasDemographics;
        merged.eligibleCount = this.eligibleCount + other.eligibleCount;
        merged.raClaimCount = this.raClaimCount + other.raClaimCount;
        merged.ffsClaimCount = this.ffsClaimCount + other.ffsClaimCount;
        merged.rapCount = this.rapCount + other.rapCount;
        merged.mao004Count = this.mao004Count + other.mao004Count;
        merged.documentCount = this.documentCount + other.documentCount;
        merged.problemCount = this.problemCount + other.problemCount;
        merged.encounterCount = this.encounterCount + other.encounterCount;
        merged.procedureCount = this.procedureCount + other.procedureCount;
        merged.providerCount = this.providerCount + other.providerCount;
        merged.prescriptionsCount = this.prescriptionsCount + other.prescriptionsCount;
        merged.biometricsCount = this.biometricsCount + other.biometricsCount;
        merged.socialHistoryCount = this.socialHistoryCount + other.socialHistoryCount;
        merged.labResultsCount = this.labResultsCount + other.labResultsCount;

        // The database has a default date of 1970-01-01 00:00:01.0, so take the one that is not this timestamp
        DateTime first = merged.firstSeen;
        if (first == null || first.getMillis() == 28801000)
        {
            first = other.firstSeen;
        }
        merged.firstSeen = first;
        merged.firstSeenWith = this.firstSeenWith != null ? this.firstSeenWith : other.firstSeenWith;

        return merged;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String getPatientExternalId()
    {
        return patientExternalId;
    }

    public void setPatientExternalId(String patientExternalId)
    {
        this.patientExternalId = patientExternalId;
    }

    public String getPatientUUID()
    {
        return patientUUID;
    }

    public void setPatientUUID(String patientUUID)
    {
        this.patientUUID = patientUUID;
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

    public boolean getHasDemographics()
    {
        return hasDemographics;
    }

    public void setHasDemographics(boolean hasDemographics)
    {
        this.hasDemographics = hasDemographics;
    }

    public int getBiometricsCount() { return biometricsCount; }

    public void setBiometricsCount(int biometricsCount) { this.biometricsCount = biometricsCount; }

    public int getSocialHistoryCount() { return socialHistoryCount; }

    public void setSocialHistoryCount(int socialHistoryCount) { this.socialHistoryCount = socialHistoryCount; }

    public int getEligibleCount()
    {
        return eligibleCount;
    }

    public void setEligibleCount(int eligibleCount)
    {
        this.eligibleCount = eligibleCount;
    }

    public UploadedFile getUploadedFile()
    {
        return uploadedFile;
    }

    public void setUploadedFile(UploadedFile uploadedFile)
    {
        this.uploadedFile = uploadedFile;
    }

    public String toString()
    {
        return "PatientCompleteness{" +
               "id=" + id +
               ", patientUUID='" + patientUUID + '\'' +
               ", hasDemographics=" + hasDemographics +
               ", eligibleCount=" + eligibleCount +
               ", raClaimCount=" + raClaimCount +
               ", ffsClaimCount=" + ffsClaimCount +
               ", rapCount=" + rapCount +
               ", mao004Count=" + mao004Count +
               ", documentCount=" + documentCount +
               ", problemCount=" + problemCount +
               ", encounterCount=" + encounterCount +
               ", procedureCount=" + procedureCount +
               ", providerCount=" + providerCount +
               ", prescriptionsCount=" + prescriptionsCount +
               ", labResultsCount=" + labResultsCount +
                ", biometricsCount=" + biometricsCount +
                ", socialHistoryCount=" + socialHistoryCount +
               ", firstSeen=" + firstSeen +
               ", firstSeenWith='" + firstSeenWith + '\'' +
               '}';
    }
}
