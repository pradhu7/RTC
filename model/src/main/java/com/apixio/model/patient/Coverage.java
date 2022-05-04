package com.apixio.model.patient;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.joda.time.LocalDate;

import com.apixio.model.Constants;
import com.apixio.model.utility.ApixioDateDeserialzer;

public class Coverage extends CodedBaseObject {
    private int sequenceNumber;
    private LocalDate startDate;
    private LocalDate endDate;
    private CoverageType type;
    private String healthPlanName;
    private ExternalID groupNumber;

    @Deprecated
    private ExternalID memberNumber;

    private ExternalID subscriberID;

    private ExternalID beneficiaryID;

    public Coverage () {}

    /**
     * Copy constructor
     */
    public Coverage (Coverage that) {
        this.sequenceNumber = that.sequenceNumber;
        this.startDate = that.startDate;
        this.endDate = that.endDate;
        this.type = that.type;
        this.healthPlanName = that.healthPlanName;
        this.groupNumber = that.groupNumber;
        this.subscriberID = that.subscriberID;
        this.beneficiaryID = that.beneficiaryID;
        setSourceId(that.getSourceId());
        setParsingDetailsId(that.getParsingDetailsId());
        setEditType(that.getEditType());
        setMetadata(that.getMetadata());
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public CoverageType getType() {
        return type;
    }

    public void setType(CoverageType type) {
        this.type = type;
    }

    public String getHealthPlanName() {
        return healthPlanName;
    }

    public void setHealthPlanName(String healthPlanName) {
        this.healthPlanName = healthPlanName;
    }

    public ExternalID getGroupNumber() {
        return groupNumber;
    }

    public void setGroupNumber(ExternalID groupNumber) {
        this.groupNumber = groupNumber;
    }

    public ExternalID getMemberNumber() {
        return memberNumber;
    }

    public void setMemberNumber(ExternalID memberNumber) {
        this.memberNumber = memberNumber;
    }

    public ExternalID getSubscriberID() {
        return subscriberID;
    }

    public void setSubscriberID(ExternalID subscriberID) {
        this.subscriberID = subscriberID;
    }

    public ExternalID getBeneficiaryID() {
        return beneficiaryID;
    }

    public void setBeneficiaryID(ExternalID beneficiaryID) {
        this.beneficiaryID = beneficiaryID;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, Constants.TO_STRING_STYLE).
            append("sequenceNumber", getSequenceNumber()).
            append("startDate", getStartDate()).
            append("endDate", getEndDate()).
            append("type", getType()).
            append("healthPlanName", getHealthPlanName()).
            append("subscriberId", subscriberID).
            appendSuper(super.toString()).
            toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.sequenceNumber)
            .append(this.startDate).append(this.endDate)
            .append(this.type).append(this.healthPlanName)
            .append(this.subscriberID)
            .appendSuper(super.hashCode())
            .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj!=null && obj instanceof Coverage){
            Coverage c = (Coverage) obj;
            return new EqualsBuilder()
                .append(this.sequenceNumber, c.sequenceNumber)
                .append(this.startDate, c.startDate)
                .append(this.endDate, c.endDate)
                .append(this.type, c.type)
                .append(this.healthPlanName, c.healthPlanName)
                .append(this.subscriberID, c.subscriberID)
                .appendSuper(super.equals(obj)).isEquals();
        }
        return false;
    }
}
