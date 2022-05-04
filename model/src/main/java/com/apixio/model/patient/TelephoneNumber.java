package com.apixio.model.patient;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.apixio.model.Constants;

public class TelephoneNumber {

	private String phoneNumber;
	private TelephoneType phoneType;
	
	public String getPhoneNumber() {
		return phoneNumber;
	}
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
	public TelephoneType getPhoneType() {
		return phoneType;
	}
	public void setPhoneType(TelephoneType phoneType) {
		this.phoneType = phoneType;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this, Constants.TO_STRING_STYLE).
	       append("phoneNumber", phoneNumber).
	       append("phoneType", phoneType).
	       toString();
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(this.phoneNumber).append(this.phoneType).toHashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj!=null && obj instanceof TelephoneNumber){
			TelephoneNumber t = (TelephoneNumber) obj;
			return new EqualsBuilder().append(this.phoneNumber, t.phoneNumber).append(this.phoneType, t.phoneType).isEquals();
		}
		return false;
	}
	
	
}
