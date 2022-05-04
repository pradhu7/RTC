package com.apixio.model.external;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AxmBiometricValue extends AxmRow {
    private AxmExternalId originalId;
    private List<AxmProviderInRole> providerInRoles = new ArrayList<>();
    private String name;
    private AxmCodeOrName codeOrName;
    private LocalDate resultDate;
    private String value;
    private String units;

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

    public AxmCodeOrName getCodeOrName() {
        return codeOrName;
    }

    public void setCodeOrName(AxmCodeOrName codeOrName) {
        this.codeOrName = codeOrName;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public LocalDate getResultDate() {
        return resultDate;
    }

    public void setResultDate(LocalDate resultDate) {
        this.resultDate = resultDate;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(originalId, providerInRoles, name, resultDate, value, units, metadata, source, editType);
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        if (this.getClass() != obj.getClass()) return false;

        final AxmBiometricValue that = (AxmBiometricValue) obj;
        return Objects.equals(this.originalId, that.originalId)
            && Objects.equals(this.providerInRoles, that.providerInRoles)
            && Objects.equals(this.name, that.name)
            && Objects.equals(this.resultDate, that.resultDate)
            && Objects.equals(this.value, that.value)
            && Objects.equals(this.units, that.units)
            && Objects.equals(this.metadata, that.metadata)
            && Objects.equals(this.source, that.source)
            && Objects.equals(this.editType, that.editType);
    }
}
