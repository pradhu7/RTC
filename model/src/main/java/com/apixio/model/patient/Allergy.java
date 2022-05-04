package com.apixio.model.patient;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.joda.time.DateTime;

import com.apixio.model.Constants;

public class Allergy extends CodedBaseObject {
	private String allergen;
	private ClinicalCode reaction;
	private String reactionSeverity;
	private DateTime diagnosisDate;
	private DateTime reactionDate;
	private DateTime resolvedDate;
	
	public String getAllergen() {
		return allergen;
	}
	public void setAllergen(String allergen) {
		this.allergen = allergen;
	}
	public ClinicalCode getReaction() {
		return reaction;
	}
	public void setReaction(ClinicalCode reaction) {
		this.reaction = reaction;
	}
	public DateTime getDiagnosisDate() {
		return diagnosisDate;
	}
	public void setDiagnosisDate(DateTime diagnosisDate) {
		this.diagnosisDate = diagnosisDate;
	}
	public DateTime getReactionDate() {
		return reactionDate;
	}
	public void setReactionDate(DateTime reactionDate) {
		this.reactionDate = reactionDate;
	}
	public String getReactionSeverity() {
		return reactionSeverity;
	}
	public void setReactionSeverity(String reactionSeverity) {
		this.reactionSeverity = reactionSeverity;
	}
	
	public void setResolvedDate(DateTime resolvedDate) {
		this.resolvedDate = resolvedDate;
	}
	public DateTime getResolvedDate() {
		return resolvedDate;
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this, Constants.TO_STRING_STYLE).
	       append("allergen", allergen).
	       append("diagnosis date", diagnosisDate).
	       append("reaction", reaction).
	       append("severity", reactionSeverity).
	       append("reactionDate", reactionDate).
	       append("resolvedDate", resolvedDate).
	       appendSuper(super.toString()).
	       toString();
	}
	
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(this.allergen)
				.append(this.diagnosisDate).append(this.reaction)
				.append(this.reactionDate).append(this.reactionSeverity)
				.appendSuper(super.hashCode()).toHashCode();
	}
	
	
	@Override
	public boolean equals(Object obj) {
		if(obj!=null && obj instanceof Allergy){
			Allergy a = (Allergy)obj;
			return new EqualsBuilder().append(this.allergen, a.allergen)
					.append(this.diagnosisDate, a.diagnosisDate)
					.append(this.reaction, a.reaction)
					.append(this.reactionDate, a.reactionDate)
					.append(this.reactionSeverity, a.reactionSeverity)
					.appendSuper(super.equals(obj)).isEquals();
		}
		return false;
	}
	
	

}
