package com.apixio.model.external;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.LocalDate;

import java.util.*;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AxmProblem extends AxmRow {
    private AxmExternalId originalId;
    private List<AxmProviderInRole> providerInRoles = new ArrayList<>();
    private String name;
    private AxmResolution resolution;
    private LocalDate diagnosisDate;
    private String temporalStatus;
    private LocalDate startDate;
    private LocalDate endDate;
    private AxmCodeOrName codeOrName;

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

    public AxmResolution getResolution() {
        return resolution;
    }

    public void setResolution(AxmResolution resolution) {
        this.resolution = resolution;
    }

    public LocalDate getDiagnosisDate() {
        return diagnosisDate;
    }

    public void setDiagnosisDate(LocalDate diagnosisDate) {
        this.diagnosisDate = diagnosisDate;
    }

    public String getTemporalStatus() {
        return temporalStatus;
    }

    public void setTemporalStatus(String temporalStatus) {
        this.temporalStatus = temporalStatus;
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

    @Override
    public int hashCode()
    {
        return Objects.hash(originalId, providerInRoles, name, resolution, diagnosisDate, temporalStatus, startDate, endDate, codeOrName, metadata, source, editType);
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        if (this.getClass() != obj.getClass()) return false;

        final AxmProblem that = (AxmProblem) obj;
        return Objects.equals(this.originalId, that.originalId)
            && Objects.equals(this.providerInRoles, that.providerInRoles)
            && Objects.equals(this.name, that.name)
            && Objects.equals(this.resolution, that.resolution)
            && Objects.equals(this.diagnosisDate, that.diagnosisDate)
            && Objects.equals(this.temporalStatus, that.temporalStatus)
            && Objects.equals(this.startDate, that.startDate)
            && Objects.equals(this.endDate, that.endDate)
            && Objects.equals(this.codeOrName, that.codeOrName)
            && Objects.equals(this.metadata, that.metadata)
            && Objects.equals(this.source, that.source)
            && Objects.equals(this.editType, that.editType);
    }
}
