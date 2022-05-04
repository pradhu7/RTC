package com.apixio.model.patient;

import com.apixio.model.Builder;
import com.apixio.model.Constants;
import com.apixio.model.utility.ApixioDateDeserialzer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.joda.time.DateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Problem extends CodedBaseObject {

    private String problemName;
    private ResolutionStatus resolutionStatus;
    private String temporalStatus;    // wether a problem is acute, chronic, time limited, etc..
    private DateTime startDate;    // also called onset date
    private DateTime endDate;    // also called resolution date
    private DateTime diagnosisDate;    // also called resolution date
    private String severity;

    public static ProblemBuilder newBuilder() {
        return new ProblemBuilder();
    }

    public DateTime getDiagnosisDate() {
        return diagnosisDate;
    }

    public void setDiagnosisDate(DateTime diagnosisDate) {
        this.diagnosisDate = diagnosisDate;
    }

    public String getProblemName() {
        return problemName;
    }

    public void setProblemName(String problemName) {
        this.problemName = problemName;
    }

    public DateTime getStartDate() {
        return startDate;
    }

    @JsonDeserialize(as = DateTime.class, using = ApixioDateDeserialzer.class)
    public void setStartDate(DateTime startDate) {
        this.startDate = startDate;
    }

    public DateTime getEndDate() {
        return endDate;
    }

    @JsonDeserialize(as = DateTime.class, using = ApixioDateDeserialzer.class)
    public void setEndDate(DateTime endDate) {
        this.endDate = endDate;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public ResolutionStatus getResolutionStatus() {
        return resolutionStatus;
    }

    public void setResolutionStatus(ResolutionStatus resolutionStatus) {
        this.resolutionStatus = resolutionStatus;
    }

    public String getTemporalStatus() {
        return temporalStatus;
    }

    public void setTemporalStatus(String temporalStatus) {
        this.temporalStatus = temporalStatus;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, Constants.TO_STRING_STYLE).
                append("problemName", problemName).
                append("resolutionStatus", resolutionStatus).
                append("temporalStatus", temporalStatus).
                append("startDate", startDate).
                append("endDate", endDate).
                append("diagnosisDate", diagnosisDate).
                append("severity", severity).
                appendSuper(super.toString()).
                toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.problemName)
                .append(this.endDate).append(this.startDate)
                .append(this.temporalStatus).append(this.severity)
                .append(this.resolutionStatus).appendSuper(super.hashCode())
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof Problem) {
            Problem p = (Problem) obj;
            return new EqualsBuilder().append(this.problemName, p.problemName)
                    .append(this.endDate, p.endDate)
                    .append(this.startDate, p.startDate)
                    .append(this.temporalStatus, p.temporalStatus)
                    .append(this.severity, p.severity)
                    .append(this.resolutionStatus, p.resolutionStatus)
                    .appendSuper(super.equals(obj)).isEquals();
        }
        return false;

    }

    public static class ProblemBuilder implements Builder<Problem> {
        Problem dto = new Problem();

        public Problem build() {
            return dto;
        }

        public ProblemBuilder setProblemName(String name) {
            this.dto.problemName = name;
            return this;
        }

        public ProblemBuilder setStartDate(DateTime startDate) {
            dto.startDate = startDate;
            return this;
        }

        public ProblemBuilder setEndDate(DateTime endDate) {
            dto.endDate = endDate;
            return this;
        }
    }


}
