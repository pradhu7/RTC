package com.apixio.model.patient;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.joda.time.DateTime;

import com.apixio.model.Constants;

public class Administration extends CodedBaseObject {

	private DateTime adminDate;
	private double quantity;
	private String amount;
	private String dosage;
	private DateTime startDate;
	private DateTime endDate;
	private Medication medication;
	private int medicationSeriesNumber;
	
	public DateTime getAdminDate() {
		return adminDate;
	}
	public void setAdminDate(DateTime adminDate) {
		this.adminDate = adminDate;
	}
	public double getQuantity() {
		return quantity;
	}
	public void setQuantity(double quantity) {
		this.quantity = quantity;
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
	public DateTime getStartDate() {
		return startDate;
	}
	public void setStartDate(DateTime startDate) {
		this.startDate = startDate;
	}
	public DateTime getEndDate() {
		return endDate;
	}
	public void setEndDate(DateTime endDate) {
		this.endDate = endDate;
	}
	public Medication getMedication() {
		return medication;
	}
	public void setMedication(Medication medication) {
		this.medication = medication;
	}
	public int getMedicationSeriesNumber() {
		return medicationSeriesNumber;
	}
	public void setMedicationSeriesNumber(int medicationSeriesNumber) {
		this.medicationSeriesNumber = medicationSeriesNumber;
	}	

	@Override
	public String toString() {
		return new ToStringBuilder(this, Constants.TO_STRING_STYLE).
	       append("adminDate", adminDate).
		   append("quantity", quantity).
	       append("amount", amount).
	       append("dosage", dosage).
	       append("startDate", startDate).
	       append("endDate", endDate).
	       append("medicationSeriesNumber", medicationSeriesNumber).
	       append("medication", medication).
	       appendSuper(super.toString()).
	       toString();
	}
	
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(this.medicationSeriesNumber)
				.append(this.quantity).append(this.adminDate)
				.append(this.amount).append(this.dosage).append(this.endDate)
				.append(this.medication).append(this.startDate)
				.appendSuper(super.hashCode()).toHashCode();
	}
	
	
	@Override
	public boolean equals(Object obj) {
		if(obj!=null && obj instanceof Administration){
			Administration a = (Administration)obj;
			return new EqualsBuilder()
					.append(this.medicationSeriesNumber,
							a.medicationSeriesNumber)
					.append(this.quantity, a.quantity)
					.append(this.adminDate, a.adminDate)
					.append(this.amount, a.amount)
					.append(this.dosage, a.dosage)
					.append(this.endDate, a.endDate)
					.append(this.medication, a.medication)
					.append(this.startDate, a.startDate)
					.appendSuper(super.equals(obj)).isEquals();
		}
		return false;
	}
	
	
}
