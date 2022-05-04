package com.apixio.model.patient;

import java.util.UUID;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.apixio.model.Constants;

public class ClinicalActor extends Actor {
	private ActorRole role;
	private ContactDetails contactDetails;

	public ContactDetails getContactDetails() {
		return contactDetails;
	}
	public void setContactDetails(ContactDetails contactDetails) {
		this.contactDetails = contactDetails;
	}
	public ActorRole getRole() {
		return role;
	}
	public void setRole(ActorRole role) {
		this.role = role;
	}
	public void setClinicalActorId(UUID actorId) {
		setInternalUUID(actorId);
	}
	public UUID getClinicalActorId() {
		return getInternalUUID();
	}	
	
	@Override
	public String toString() {
		return new ToStringBuilder(this, Constants.TO_STRING_STYLE).
	       append("role", role).
	       append("clinicalActorId", getClinicalActorId()).
	       append("contactDetails", contactDetails).
	       appendSuper(super.toString()).
	       toString();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj!=null && obj instanceof ClinicalActor){
			ClinicalActor ca = (ClinicalActor)obj;
			return new EqualsBuilder().append(this.role, ca.role).appendSuper(super.equals(obj)).isEquals();
		}
		return false;
	}
	
	@Override
	public int hashCode() {		
		return new HashCodeBuilder().append(this.role).appendSuper(super.hashCode()).toHashCode();
	}
	
	
}
