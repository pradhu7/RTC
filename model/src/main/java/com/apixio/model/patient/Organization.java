package com.apixio.model.patient;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.apixio.model.Constants;

public class Organization extends BaseObject {
	private String name;
	private ExternalID primaryId;
	private List<ExternalID> alternateIds = new LinkedList<ExternalID>();
	private ContactDetails contactDetails;
		
	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}	
	public ExternalID getPrimaryId() {
		return primaryId;
	}
	public void setPrimaryId(ExternalID primaryId) {
		this.primaryId = primaryId;
	}
	public List<ExternalID> getAlternateIds() {
		return alternateIds;
	}
	public void setAlternateIds(List<ExternalID> alternateIds) {
		this.alternateIds = alternateIds;
	}
	public void setContactDetails(ContactDetails contactDetails) {
		this.contactDetails = contactDetails;
	}
	public ContactDetails getContactDetails() {
		return contactDetails;
	}
	@Override
	public String toString() {
		return new ToStringBuilder(this, Constants.TO_STRING_STYLE).
	       append("name", name).
	       append("primaryId", primaryId).
	       append("alternateIds", alternateIds).
	       append("contactDetails", contactDetails).
	       appendSuper(super.toString()).
	       toString();
	}	
}
