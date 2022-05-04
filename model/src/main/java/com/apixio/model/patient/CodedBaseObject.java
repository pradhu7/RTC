package com.apixio.model.patient;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.apixio.model.Constants;

public class CodedBaseObject extends BaseObject {

	private UUID primaryClinicalActorId;
	private List<UUID> supplementaryClinicalActorIds = new LinkedList<UUID>();
	private UUID sourceEncounter;
	private ClinicalCode code;
	private List<ClinicalCode> codeTranslations = new LinkedList<ClinicalCode>();
	
	public UUID getPrimaryClinicalActorId() {
		return primaryClinicalActorId;
	}
	public void setPrimaryClinicalActorId(UUID primaryClinicalActorId) {
		this.primaryClinicalActorId = primaryClinicalActorId;
	}
	public List<UUID> getSupplementaryClinicalActorIds() {
		return supplementaryClinicalActorIds;
	}
	public void setSupplementaryClinicalActors(
			List<UUID> supplementaryClinicalActorIds) {
		this.supplementaryClinicalActorIds = supplementaryClinicalActorIds;
	}
	public UUID getSourceEncounter() {
		return sourceEncounter;
	}
	public void setSourceEncounter(UUID sourceEncounter) {
		this.sourceEncounter = sourceEncounter;
	}
	public ClinicalCode getCode() {
		return code;
	}
	public void setCode(ClinicalCode code) {
		this.code = code;
	}
	public List<ClinicalCode> getCodeTranslations() {
		if (codeTranslations != null) {
			codeTranslations = codeTranslations.stream().filter(Objects::nonNull).collect(Collectors.toList());
		} else {
			codeTranslations = new ArrayList<>();
		}

		return codeTranslations;
	}
	public void setCodeTranslations(List<ClinicalCode> codeTranslations) {
		this.codeTranslations = codeTranslations;
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this, Constants.TO_STRING_STYLE).
	       append("primaryClinicalActorId", primaryClinicalActorId).
	       append("supplementaryClinicalActorIds", supplementaryClinicalActorIds).
	       append("sourceEncounter", sourceEncounter).
	       append("code", code).
	       append("codeTranslations", codeTranslations).
	       appendSuper(super.toString()).
	       toString();
	}
	
	@Override
	public int hashCode() {
		HashCodeBuilder hashBuilder = new HashCodeBuilder().append(this.code).append(this.primaryClinicalActorId).append(sourceEncounter);
		if(this.codeTranslations!=null){
			hashBuilder.append(this.codeTranslations.toArray());
		}
		
		if(this.supplementaryClinicalActorIds!=null){
			hashBuilder.append(this.supplementaryClinicalActorIds.toArray());
		}
		hashBuilder.appendSuper(super.hashCode());
		return hashBuilder.toHashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj!=null && obj instanceof CodedBaseObject){
			CodedBaseObject c = (CodedBaseObject)obj;
			EqualsBuilder equalsBuilder = new EqualsBuilder().append(this.code, c.code).append(this.primaryClinicalActorId, c.primaryClinicalActorId).append(this.sourceEncounter, c.sourceEncounter);
			
			if(this.codeTranslations!=null && c.codeTranslations!=null){
				Object[] arr1 = this.codeTranslations.toArray();
				Object[] arr2 = c.codeTranslations.toArray();
				Arrays.sort(arr1);
				Arrays.sort(arr2);
				equalsBuilder.append(arr1, arr2);
			}else if(this.codeTranslations!=null || c.codeTranslations!=null){
				//control will come here if one list is null but other is not null
				return false;
			}
			
			if(this.supplementaryClinicalActorIds!=null && c.supplementaryClinicalActorIds!=null){
				Object[] arr1 = this.supplementaryClinicalActorIds.toArray();
				Object[] arr2 = c.supplementaryClinicalActorIds.toArray();
				Arrays.sort(arr1);
				Arrays.sort(arr2);
				equalsBuilder.append(arr1, arr2);
			}else if(this.supplementaryClinicalActorIds!=null || c.supplementaryClinicalActorIds!=null){
				//control will come here if one list is null but other is not null
				return false;
			}
			
			equalsBuilder.appendSuper(super.equals(obj));
			return equalsBuilder.isEquals();
		}
		return false;
	}
	
	
}
