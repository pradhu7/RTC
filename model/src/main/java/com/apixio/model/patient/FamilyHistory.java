package com.apixio.model.patient;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * 
 */
public class FamilyHistory extends CodedBaseObject {
	private String familyHistory;

	public String getFamilyHistory() {
		return familyHistory;
	}

	public void setFamilyHistory(String familyHistory) {
		this.familyHistory = familyHistory;
	}
	
	/**
	 * Please update this method if any additional fields are addded to this class
	 */
	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(this.familyHistory).toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj!=null && obj instanceof FamilyHistory){
			FamilyHistory f = (FamilyHistory) obj;
			return new EqualsBuilder().append(this.familyHistory, f.familyHistory).isEquals();
		}
		return false;
	}
	
	
}
