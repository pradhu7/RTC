package com.apixio.model.patient;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.apixio.model.Constants;

public class Actor extends BaseObject {
	
	private Name actorGivenName;
	private List<Name> actorSupplementalNames;
	private String title;
	private ExternalID primaryId;
	private List<ExternalID> alternateIds = new LinkedList<ExternalID>();
	private Organization associatedOrg;
	
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
	
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public Organization getAssociatedOrg() {
		return associatedOrg;
	}
	public void setAssociatedOrg(Organization associatedOrg) {
		this.associatedOrg = associatedOrg;
	}
	public void setActorGivenName(Name actorGivenName) {
		this.actorGivenName = actorGivenName;
	}
	public Name getActorGivenName() {
		return actorGivenName;
	}
	public void setActorSupplementalNames(List<Name> actorSupplementalNames) {
		this.actorSupplementalNames = actorSupplementalNames;
	}
	public Iterable<Name> getActorSupplementalNames() {
		return actorSupplementalNames;
	}
	
	// custom methods
	public void addActorSupplementalNames(Name n) {
		actorSupplementalNames.add(n);
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this, Constants.TO_STRING_STYLE).
	       append("actorGivenName", actorGivenName).
	       append("actorSupplementalNames", actorSupplementalNames).
	       append("title", title).
	       append("primaryId", primaryId).
	       append("alternateIds", alternateIds).
	       append("associatedOrg", associatedOrg).
	       appendSuper(super.toString()).
	       toString();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj!=null && obj instanceof Actor){
			Actor a = (Actor)obj;
			EqualsBuilder equalsBuilder = new EqualsBuilder()
					.append(this.actorGivenName, a.actorGivenName)
					.append(this.title, a.title)
					.append(this.primaryId, a.primaryId)
					.append(this.associatedOrg, a.associatedOrg)
					.appendSuper(super.equals(obj));
			
			if(this.actorSupplementalNames!=null && a.actorSupplementalNames!=null){
				Object[] arr1 = this.actorSupplementalNames.toArray();
				Object[] arr2 = a.actorSupplementalNames.toArray();
				Arrays.sort(arr1);
				Arrays.sort(arr2);
				equalsBuilder.append(arr1, arr2);				
			}else if(this.actorSupplementalNames!=null || a.actorSupplementalNames!=null){
				return false;
			}
			
			if(this.alternateIds!=null && a.alternateIds!=null){
				Object[] arr1 = this.alternateIds.toArray();
				Object[] arr2 = a.alternateIds.toArray();
				Arrays.sort(arr1);
				Arrays.sort(arr2);
				equalsBuilder.append(arr1, arr2);				
			}else if(this.alternateIds!=null || a.alternateIds!=null){
				return false;
			}
			return equalsBuilder.isEquals();
		}
		return false;
	}

	@Override
	public int hashCode() {
		HashCodeBuilder hashCodeBuilder = new HashCodeBuilder().append(actorGivenName).append(title).append(primaryId).append(associatedOrg).appendSuper(super.hashCode());
		
		if(this.actorSupplementalNames!=null){
			hashCodeBuilder.append(this.actorSupplementalNames.toArray());
		}
		if(this.alternateIds!=null){
			hashCodeBuilder.append(this.alternateIds.toArray());
		}
		return hashCodeBuilder.toHashCode();
	}
}
