package com.apixio.model.external;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AxmLabResult extends AxmRow {
    private AxmExternalId originalId;
    private List<AxmProviderInRole> providerInRoles = new ArrayList<>();
    private String name;
    private AxmClinicalCode code;
    private AxmClinicalCode panelCode;
    private LocalDate sampleDate;
    private String value;
    private String units;
    private String range;
    private AxmFlag flag;
    private String note;

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

    public AxmClinicalCode getCode() { return code; }

    public void setCode(AxmClinicalCode code) { this.code = code; }

    public AxmClinicalCode getPanelCode() { return panelCode; }

    public void setPanelCode(AxmClinicalCode panelCode) { this.panelCode = panelCode; }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public LocalDate getSampleDate() {
        return sampleDate;
    }

    public void setSampleDate(LocalDate resultDate) {
        this.sampleDate = resultDate;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public AxmFlag getFlag() {
        return flag;
    }

    public void setFlag(AxmFlag flag) {
        this.flag = flag;
    }

    public String getRange() { return range; }

    public void setRange(String range) {
        this.range = range;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(originalId, providerInRoles, name, code, panelCode, sampleDate, value, units, range, flag, note, metadata, source, editType);
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        if (this.getClass() != obj.getClass()) return false;

        final AxmLabResult that = (AxmLabResult) obj;
        return Objects.equals(this.originalId, that.originalId)
            && Objects.equals(this.providerInRoles, that.providerInRoles)
            && Objects.equals(this.name, that.name)
            && Objects.equals(this.code, that.code)
            && Objects.equals(this.panelCode, that.panelCode)
            && Objects.equals(this.sampleDate, that.sampleDate)
            && Objects.equals(this.value, that.value)
            && Objects.equals(this.units, that.units)
            && Objects.equals(this.range, that.range)
            && Objects.equals(this.flag, that.flag)
            && Objects.equals(this.note, that.note)
            && Objects.equals(this.metadata, that.metadata)
            && Objects.equals(this.source, that.source)
            && Objects.equals(this.editType, that.editType);
    }
}
