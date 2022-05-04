package com.apixio.model.patient;

import com.apixio.model.Builder;
import com.apixio.model.Constants;
import com.apixio.model.utility.ApixioDateDeserialzer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.joda.time.DateTime;

import java.util.LinkedList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Procedure extends CodedBaseObject {
    private String procedureName;
    private DateTime performedOn;
    private DateTime endDate;
    private String interpretation;
    private Anatomy bodySite;
    private List<ClinicalCode> supportingDiagnosis = new LinkedList<ClinicalCode>();

    public static ProcedureBuilder newBuilder() {
        return new ProcedureBuilder();
    }

    public String getProcedureName() {
        return procedureName;
    }

    public void setProcedureName(String procedureName) {
        this.procedureName = procedureName;
    }

    public DateTime getPerformedOn() {
        return performedOn;
    }

    @JsonDeserialize(as = DateTime.class, using = ApixioDateDeserialzer.class)
    public void setPerformedOn(DateTime performedOn) {
        this.performedOn = performedOn;
    }

    public String getInterpretation() {
        return interpretation;
    }

    public void setInterpretation(String interpretation) {
        this.interpretation = interpretation;
    }

    public DateTime getEndDate() {
        return endDate;
    }

    @JsonDeserialize(as = DateTime.class, using = ApixioDateDeserialzer.class)
    public void setEndDate(DateTime endDate) {
        this.endDate = endDate;
    }

    public Anatomy getBodySite() {
        return bodySite;
    }

    public void setBodySite(Anatomy bodySite) {
        this.bodySite = bodySite;
    }

    public List<ClinicalCode> getSupportingDiagnosis() {
        return supportingDiagnosis;
    }

    public void setSupportingDiagnosis(List<ClinicalCode> supportingDiagnosis) {
        this.supportingDiagnosis = supportingDiagnosis;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, Constants.TO_STRING_STYLE).
                append("procedureName", procedureName).
                append("performedOn", performedOn).
                append("endDate", endDate).
                append("interpretation", interpretation).
                append("bodySite", bodySite).
                append("supportingDiagnosis", supportingDiagnosis).
                appendSuper(super.toString()).
                toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.bodySite).append(this.endDate)
                .append(this.interpretation).append(this.performedOn)
                .append(this.procedureName).appendSuper(super.hashCode()).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof Procedure) {
            Procedure proc = (Procedure) obj;
            return new EqualsBuilder().append(this.bodySite, proc.bodySite)
                    .append(this.endDate, proc.endDate)
                    .append(this.interpretation, proc.interpretation)
                    .append(this.performedOn, proc.performedOn)
                    .append(this.procedureName, proc.procedureName)
                    .appendSuper(super.equals(obj)).isEquals();
        }
        return false;
    }

    public static class ProcedureBuilder implements Builder<Procedure> {
        private Procedure proc = new Procedure();

        public ProcedureBuilder() {
            proc.setCode(new ClinicalCode());
        }

        public Procedure build() {
            return proc;
        }

        public ProcedureBuilder setProcedureName(String procedureName) {
            proc.procedureName = procedureName;
            return this;
        }

        public ProcedureBuilder setCode(String code) {
            proc.getCode().setCode(code);
            return this;
        }

        public ProcedureBuilder setCodingSystem(String codingSystem) {
            proc.getCode().setCodingSystem(codingSystem);
            return this;
        }

        public ProcedureBuilder setPerformedOn(DateTime performedOn) {
            proc.performedOn = performedOn;
            return this;
        }

        public ProcedureBuilder setInterpretation(String inter) {
            proc.interpretation = inter;
            return this;
        }


    }


}
