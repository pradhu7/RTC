package com.apixio.model.external;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.LocalDate;

import java.util.*;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AxmPrescription extends AxmRow {
    private AxmExternalId originalId;
    private AxmCodeOrName codeOrName;
    private List<AxmProviderInRole> providerInRoles = new ArrayList<>();
    private AxmExternalId encounterId;
    private LocalDate prescriptionDate;
    private Double quantity;
    private String frequency;
    private LocalDate endDate;
    private LocalDate fillDate;
    private String sig;
    private String amount;
    private String dosage;
    private AxmMedication associatedMedication;
    private Boolean isActivePrescription;
    private Integer refillsRemaining;

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

    public Iterable<AxmProviderInRole> getProviderInRoles() {
        return providerInRoles;
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

    public LocalDate getPrescriptionDate() {
        return prescriptionDate;
    }

    public void setPrescriptionDate(LocalDate prescriptionDate) {
        this.prescriptionDate = prescriptionDate;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDate getFillDate() {
        return fillDate;
    }

    public void setFillDate(LocalDate fillDate) {
        this.fillDate = fillDate;
    }

    public String getSig() {
        return sig;
    }

    public void setSig(String sig) {
        this.sig = sig;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public AxmMedication getAssociatedMedication() {
        return associatedMedication;
    }

    public void setAssociatedMedication(AxmMedication associatedMedication) {
        this.associatedMedication = associatedMedication;
    }

    public Boolean getActivePrescription() {
        return isActivePrescription;
    }

    public void setActivePrescription(Boolean activePrescription) {
        isActivePrescription = activePrescription;
    }

    public Integer getRefillsRemaining() {
        return refillsRemaining;
    }

    public void setRefillsRemaining(Integer refillsRemaining) {
        this.refillsRemaining = refillsRemaining;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(originalId,codeOrName,providerInRoles,encounterId,prescriptionDate,quantity,frequency,endDate,fillDate,sig,amount,dosage,associatedMedication,isActivePrescription,refillsRemaining,metadata, source, editType);
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        if (this.getClass() != obj.getClass()) return false;

        final AxmPrescription that = (AxmPrescription) obj;
        return Objects.equals(this.originalId, that.originalId)
                && Objects.equals(this.codeOrName, that.codeOrName)
                && Objects.equals(this.providerInRoles, that.providerInRoles)
                && Objects.equals(this.encounterId, that.encounterId)
                && Objects.equals(this.prescriptionDate, that.prescriptionDate)
                && Objects.equals(this.quantity, that.quantity)
                && Objects.equals(this.frequency, that.frequency)
                && Objects.equals(this.endDate, that.endDate)
                && Objects.equals(this.fillDate, that.fillDate)
                && Objects.equals(this.sig, that.sig)
                && Objects.equals(this.amount, that.amount)
                && Objects.equals(this.dosage, that.dosage)
                && Objects.equals(this.associatedMedication, that.associatedMedication)
                && Objects.equals(this.isActivePrescription, that.isActivePrescription)
                && Objects.equals(this.refillsRemaining, that.refillsRemaining)
                && Objects.equals(this.metadata, that.metadata)
                && Objects.equals(this.source, that.source)
                && Objects.equals(this.editType, that.editType);
    }
}