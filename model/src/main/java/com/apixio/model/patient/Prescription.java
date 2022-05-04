package com.apixio.model.patient;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.joda.time.DateTime;

import com.apixio.model.Constants;
import com.apixio.model.utility.ApixioDateDeserialzer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Prescription extends CodedBaseObject {
	private DateTime prescriptionDate;
	private Double quantity;
	private String frequency;
	private DateTime endDate;
	private DateTime fillDate;
	private String sig;
	private String amount;
	private String dosage;
	private Medication associatedMedication;
	private Boolean isActivePrescription;
	private Integer refillsRemaining;

	public DateTime getPrescriptionDate() {
		return prescriptionDate;
	}
	
	@JsonDeserialize(as=DateTime.class,using=ApixioDateDeserialzer.class)
	public void setPrescriptionDate(DateTime prescriptionDate) {
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
	public DateTime getEndDate() {
		return endDate;
	}
	
	@JsonDeserialize(as=DateTime.class,using=ApixioDateDeserialzer.class)
	public void setEndDate(DateTime endDate) {
		this.endDate = endDate;
	}
	public DateTime getFillDate() {
		return fillDate;
	}
	
	@JsonDeserialize(as=DateTime.class,using=ApixioDateDeserialzer.class)
	public void setFillDate(DateTime fillDate) {
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
	public Medication getAssociatedMedication() {
		return associatedMedication;
	}
	public void setAssociatedMedication(Medication associatedMedication) {
		this.associatedMedication = associatedMedication;
	}

	public Boolean isActivePrescription() {
		return isActivePrescription;
	}

	public void setActivePrescription(Boolean isActivePrescription) {
		this.isActivePrescription = isActivePrescription;
	}

	public Integer getRefillsRemaining() {
		return refillsRemaining;
	}

	public void setRefillsRemaining(Integer refillsRemaining) {
		this.refillsRemaining = refillsRemaining;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this, Constants.TO_STRING_STYLE).
	       append("prescriptionDate", prescriptionDate).
		   append("quantity", quantity).
	       append("frequency", frequency).
	       append("endDate", endDate).
	       append("fillDate", fillDate).
	       append("sig", sig).
	       append("amount", amount).
	       append("dosage", dosage).
	       append("associatedMedication", associatedMedication).
	       append("isActivePrescription", isActivePrescription).
	       append("refillsRemaining", refillsRemaining).
	       appendSuper(super.toString()).
	       toString();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(this.isActivePrescription)
				.append(this.amount).append(this.associatedMedication)
				.append(this.dosage).append(this.endDate).append(this.fillDate)
				.append(this.frequency).append(this.prescriptionDate)
				.append(this.quantity).append(this.refillsRemaining)
				.append(this.sig).appendSuper(super.hashCode()).toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj!= null && obj instanceof Prescription) {
			Prescription p = (Prescription) obj;
			return new EqualsBuilder()
					.append(this.isActivePrescription, p.isActivePrescription)
					.append(this.amount, p.amount)
					.append(this.associatedMedication, p.associatedMedication)
					.append(this.dosage, p.dosage)
					.append(this.endDate, p.endDate)
					.append(this.fillDate, p.fillDate)
					.append(this.frequency, p.frequency)
					.append(this.prescriptionDate, p.prescriptionDate)
					.append(this.quantity, p.quantity)					
					.append(this.refillsRemaining, p.refillsRemaining)
					.append(this.sig, p.sig).appendSuper(super.equals(obj))
					.isEquals();
		}
		return false;
		
	}
	
	
}
