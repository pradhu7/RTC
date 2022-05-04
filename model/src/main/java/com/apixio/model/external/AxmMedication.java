package com.apixio.model.external;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.*;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AxmMedication extends AxmRow {
    private AxmExternalId originalId;
    private AxmCodeOrName codeOrName;
    private List<AxmProviderInRole> providerInRoles = new ArrayList<>();
    private AxmExternalId encounterId;
    private String brandName;
    private String genericName;
    private List<String> ingredients;
    private String strength;
    private String form;
    private String routeOfAdministration;
    private String units;

    public AxmExternalId getOriginalId() {
        return originalId;
    }

    public void setOriginalId(AxmExternalId originalId) {
        this.originalId = originalId;
    }

    public AxmCodeOrName getCodeOrName() {
        return codeOrName;
    }

    public void setCodeOrName(AxmCodeOrName codeOrName) {
        this.codeOrName = codeOrName;
    }

    public List<AxmProviderInRole> getProviderInRoles() {
        return providerInRoles;
    }

    public void setProviderInRoles(List<AxmProviderInRole> providerInRoles) {
        this.providerInRoles = providerInRoles;
    }

    public AxmExternalId getEncounterId() {
        return encounterId;
    }

    public void setEncounterId(AxmExternalId encounterId) {
        this.encounterId = encounterId;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public String getGenericName() {
        return genericName;
    }

    public void setGenericName(String genericName) {
        this.genericName = genericName;
    }

    public List<String> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<String> ingredients) {
        this.ingredients = ingredients;
    }

    public String getStrength() {
        return strength;
    }

    public void setStrength(String strength) {
        this.strength = strength;
    }

    public String getForm() {
        return form;
    }

    public void setForm(String form) {
        this.form = form;
    }

    public String getRouteOfAdministration() {
        return routeOfAdministration;
    }

    public void setRouteOfAdministration(String routeOfAdministration) {
        this.routeOfAdministration = routeOfAdministration;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }


    @Override
    public int hashCode()
    {
        return Objects.hash(originalId,codeOrName,providerInRoles,encounterId,brandName,genericName,ingredients,strength,form,routeOfAdministration,units,metadata, source, editType);
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        if (this.getClass() != obj.getClass()) return false;

        final AxmMedication that = (AxmMedication) obj;
        return Objects.equals(this.originalId, that.originalId)
                && Objects.equals(this.codeOrName, that.codeOrName)
                && Objects.equals(this.providerInRoles, that.providerInRoles)
                && Objects.equals(this.encounterId, that.encounterId)
                && Objects.equals(this.brandName, that.brandName)
                && Objects.equals(this.genericName, that.genericName)
                && Objects.equals(this.ingredients, that.ingredients)
                && Objects.equals(this.strength, that.strength)
                && Objects.equals(this.form, that.form)
                && Objects.equals(this.routeOfAdministration, that.routeOfAdministration)
                && Objects.equals(this.units, that.units)
                && Objects.equals(this.metadata, that.metadata)
                && Objects.equals(this.source, that.source)
                && Objects.equals(this.editType, that.editType);
    }
}