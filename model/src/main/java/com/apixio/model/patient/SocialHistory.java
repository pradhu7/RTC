package com.apixio.model.patient;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.joda.time.DateTime;

import com.apixio.model.Constants;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class SocialHistory extends CodedBaseObject {

	private DateTime date;
    private ClinicalCode type;
    private String fieldName;
    private String value;
    
    public DateTime getDate() {
		return date;
	}
	public void setDate(DateTime date) {
		this.date = date;
	}
	public ClinicalCode getType() {
		return type;
	}
	public void setType(ClinicalCode type) {
		this.type = type;
	}
	public String getFieldName() {
		return fieldName;
	}
	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this, Constants.TO_STRING_STYLE).
	       append("date", date).
	       append("type", type).
	       append("fieldName", fieldName).
	       append("value", value).
	       appendSuper(super.toString()).
	       toString();
	}
	
	/**
	 * Please modify this message if any other fields are added to this class or notify Uttama
	 */
	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(this.fieldName).toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj!=null && obj instanceof SocialHistory){
			SocialHistory s = (SocialHistory) obj;
			return new EqualsBuilder().append(this.fieldName, s.fieldName).isEquals();
		}
		return false;
	}
	
	
}
