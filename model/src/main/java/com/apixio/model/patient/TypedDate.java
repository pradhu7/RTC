package com.apixio.model.patient;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.joda.time.DateTime;

import com.apixio.model.Constants;

public class TypedDate extends BaseObject {

	private DateTime date;
	private String type;
	
	public void setDate(DateTime date) {
		this.date = date;
	}
	public DateTime getDate() {
		return date;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getType() {
		return type;
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this, Constants.TO_STRING_STYLE).
	       append("date", date).
	       append("type", type).
	       appendSuper(super.toString()).
	       toString();
	}
}
