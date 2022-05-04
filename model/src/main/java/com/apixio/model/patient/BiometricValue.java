package com.apixio.model.patient;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.joda.time.DateTime;

import com.apixio.model.EitherStringOrNumber;

public class BiometricValue extends CodedBaseObject{
	private EitherStringOrNumber value;
	private String unitsOfMeasure;
	private DateTime resultDate;
	private String name;
	
	public EitherStringOrNumber getValue() {
		return value;
	}
	public void setValue(EitherStringOrNumber value) {
		this.value = value;
	}
	public String getUnitsOfMeasure() {
		return unitsOfMeasure;
	}
	public void setUnitsOfMeasure(String unitsOfMeasure) {
		this.unitsOfMeasure = unitsOfMeasure;
	}
	public DateTime getResultDate() {
		return resultDate;
	}
	public void setResultDate(DateTime resultDate) {
		this.resultDate = resultDate;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(this.name).append(this.resultDate)
				.append(this.unitsOfMeasure).append(this.value)
				.appendSuper(super.hashCode()).toHashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj!=null && obj instanceof BiometricValue){
			BiometricValue b = (BiometricValue) obj;
			new EqualsBuilder().append(this.name, b.name)
					.append(this.resultDate, b.resultDate)
					.append(this.unitsOfMeasure, b.unitsOfMeasure)
					.append(this.value, b.value).appendSuper(super.equals(obj));
		}
		return false;
	}
	
	

}
