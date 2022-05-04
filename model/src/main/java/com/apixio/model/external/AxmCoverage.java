package com.apixio.model.external;

import java.util.*;

import org.joda.time.LocalDate;

public class AxmCoverage extends AxmRow {
    private AxmExternalId subscriberId;
    private AxmExternalId beneficiaryId;
    private AxmExternalId groupId;
    private Integer sequenceNumber;
    private LocalDate startDate;
    private LocalDate endDate;
    private AxmCoverageType coverageType;
    private String planName;

    public void setSubscriberId(AxmExternalId subscriberId) {
        this.subscriberId = subscriberId;
    }

    public AxmExternalId getSubscriberId() {
        return subscriberId;
    }

    public void setBeneficiaryId(AxmExternalId beneficiaryId) {
        this.beneficiaryId = beneficiaryId;
    }

    public AxmExternalId getBeneficiaryId() {
        return beneficiaryId;
    }

    public void setGroupId(AxmExternalId groupId) {
        this.groupId = groupId;
    }

    public AxmExternalId getGroupId() {
        return groupId;
    }

    public void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setCoverageType(AxmCoverageType coverageType) {
        this.coverageType = coverageType;
    }

    public AxmCoverageType getCoverageType() {
        return coverageType;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public String getPlanName() {
        return planName;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(subscriberId, beneficiaryId, groupId, sequenceNumber, startDate, endDate, coverageType, planName, metadata, source, editType);
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        if (this.getClass() != obj.getClass()) return false;

        final AxmCoverage that = (AxmCoverage) obj;
        return Objects.equals(this.subscriberId, that.subscriberId)
            && Objects.equals(this.beneficiaryId, that.beneficiaryId)
            && Objects.equals(this.groupId, that.groupId)
            && Objects.equals(this.sequenceNumber, that.sequenceNumber)
            && Objects.equals(this.startDate, that.startDate)
            && Objects.equals(this.endDate, that.endDate)
            && Objects.equals(this.coverageType, that.coverageType)
            && Objects.equals(this.planName, that.planName)
            && Objects.equals(this.metadata, that.metadata)
            && Objects.equals(this.source, that.source)
            && Objects.equals(this.editType, that.editType);
    }
}
