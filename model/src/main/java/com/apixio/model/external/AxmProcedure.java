package com.apixio.model.external;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.LocalDate;

import java.util.*;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AxmProcedure extends AxmRow {
    private AxmExternalId originalId;
    private List<AxmProviderInRole> providerInRoles = new ArrayList<>();
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private AxmCodeOrName codeOrName;
    private String interpretation;
    private AxmCodeOrName bodyPart;
    private List<AxmCodeOrName> diagnoses = new ArrayList<>();

    public AxmExternalId getOriginalId() {
        return originalId;
    }

    public void setOriginalId(AxmExternalId originalId) {
        this.originalId = originalId;
    }

    public void addProviderInRole(AxmProviderInRole providerInRole) {
        providerInRoles.add(providerInRole);
    }

    public Iterable<AxmProviderInRole> getProviderInRoles() {
        return providerInRoles;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public AxmCodeOrName getCodeOrName() {
        return codeOrName;
    }

    public void setCodeOrName(AxmCodeOrName codeOrName) {
        this.codeOrName = codeOrName;
    }

    public String getInterpretation() {
        return interpretation;
    }

    public void setInterpretation(String interpretation) {
        this.interpretation = interpretation;
    }

    public AxmCodeOrName getBodyPart() {
        return bodyPart;
    }

    public void setBodyPart(AxmCodeOrName bodyPart) {
        this.bodyPart = bodyPart;
    }

    public void addDiagnosis(AxmCodeOrName diagnosis) {
        diagnoses.add(diagnosis);
    }

    public Iterable<AxmCodeOrName> getDiagnoses() {
        return diagnoses;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(originalId, providerInRoles, name, startDate, endDate, codeOrName, interpretation, bodyPart, diagnoses, metadata, source, editType);
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        if (this.getClass() != obj.getClass()) return false;

        final AxmProcedure that = (AxmProcedure) obj;
        return Objects.equals(this.originalId, that.originalId)
            && Objects.equals(this.providerInRoles, that.providerInRoles)
            && Objects.equals(this.name, that.name)
            && Objects.equals(this.startDate, that.startDate)
            && Objects.equals(this.endDate, that.endDate)
            && Objects.equals(this.codeOrName, that.codeOrName)
            && Objects.equals(this.interpretation, that.interpretation)
            && Objects.equals(this.bodyPart, that.bodyPart)
            && Objects.equals(this.diagnoses, that.diagnoses)
            && Objects.equals(this.metadata, that.metadata)
            && Objects.equals(this.source, that.source)
            && Objects.equals(this.editType, that.editType);
    }
}
