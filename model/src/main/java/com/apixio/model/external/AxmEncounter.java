package com.apixio.model.external;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.LocalDate;

import java.util.*;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AxmEncounter extends AxmRow {
    private AxmExternalId originalId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String careSite;
    private List<AxmProviderInRole> providerInRoles = new ArrayList<>();
    private AxmCodeOrName codeOrName;

    public AxmExternalId getOriginalId() {
        return originalId;
    }

    public void setOriginalId(AxmExternalId originalId) {
        this.originalId = originalId;
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

    public String getCareSite() {
        return careSite;
    }

    public void setCareSite(String careSite) {
        this.careSite = careSite;
    }

    public void addProviderInRole(AxmProviderInRole providerInRole) {
        providerInRoles.add(providerInRole);
    }

    public Iterable<AxmProviderInRole> getProviderInRoles() {
        return providerInRoles;
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
        return Objects.hash(originalId, startDate, endDate, careSite, providerInRoles, codeOrName, metadata, source, editType);
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        if (this.getClass() != obj.getClass()) return false;

        final AxmEncounter that = (AxmEncounter) obj;
        return Objects.equals(this.originalId, that.originalId)
            && Objects.equals(this.startDate, that.startDate)
            && Objects.equals(this.endDate, that.endDate)
            && Objects.equals(this.careSite, that.careSite)
            && Objects.equals(this.providerInRoles, that.providerInRoles)
            && Objects.equals(this.codeOrName, that.codeOrName)
            && Objects.equals(this.metadata, that.metadata)
            && Objects.equals(this.source, that.source)
            && Objects.equals(this.editType, that.editType);
    }
}
