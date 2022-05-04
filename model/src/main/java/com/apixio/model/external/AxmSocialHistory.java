package com.apixio.model.external;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AxmSocialHistory extends AxmRow {
    private AxmExternalId originalId;
    private List<AxmProviderInRole> providerInRoles = new ArrayList<>();
    private AxmExternalId encounterId;
    private AxmClinicalCode groupCode;
    private AxmClinicalCode itemCode;
    private String itemName;
    private String itemValue;
    private LocalDate date;


    public AxmExternalId getOriginalId() {
        return originalId;
    }

    public void setOriginalId(AxmExternalId originalId) {
        this.originalId = originalId;
    }

    public Iterable<AxmProviderInRole> getProviderInRoles() {
        return providerInRoles;
    }

    public void setProviderInRoles(List<AxmProviderInRole> providerInRoles) {
        this.providerInRoles = providerInRoles;
    }

    public void addProviderInRole(AxmProviderInRole providerInRole) {
        providerInRoles.add(providerInRole);
    }

    public AxmExternalId getEncounterId() {
        return encounterId;
    }

    public void setEncounterId(AxmExternalId encounterId) {
        this.encounterId = encounterId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public AxmClinicalCode getGroupCode() {
        return groupCode;
    }

    public void setGroupCode(AxmClinicalCode groupCode) {
        this.groupCode = groupCode;
    }

    public AxmClinicalCode getItemCode() {
        return itemCode;
    }

    public void setItemCode(AxmClinicalCode itemCode) {
        this.itemCode = itemCode;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getItemValue() {
        return itemValue;
    }

    public void setItemValue(String itemValue) {
        this.itemValue = itemValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (this.getClass() != obj.getClass()) return false;
        AxmSocialHistory that = (AxmSocialHistory) obj;
        return Objects.equals(originalId, that.originalId) &&
                Objects.equals(providerInRoles, that.providerInRoles) &&
                Objects.equals(encounterId, that.encounterId) &&
                Objects.equals(itemName, that.itemName) &&
                Objects.equals(groupCode, that.groupCode) &&
                Objects.equals(itemCode, that.itemCode) &&
                Objects.equals(date, that.date) &&
                Objects.equals(itemValue, that.itemValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalId, providerInRoles, encounterId, itemName, groupCode, itemCode, date, itemValue);
    }
}
