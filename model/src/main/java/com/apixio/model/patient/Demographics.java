package com.apixio.model.patient;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.joda.time.DateTime;

import com.apixio.model.Constants;
import com.apixio.model.utility.ApixioDateDeserialzer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Demographics extends BaseObject {	
	
	private Name name;
	private DateTime dateOfBirth;
	private DateTime dateOfDeath;
	private Gender gender;
	private ClinicalCode race;
	private ClinicalCode ethnicity;
	private MaritalStatus maritalStatus;
	private List<String> languages = new LinkedList<String>();
	private String religiousAffiliation;
	
	public DateTime getDateOfBirth() {
		return dateOfBirth;
	}
	
	@JsonDeserialize(as=DateTime.class,using=ApixioDateDeserialzer.class)
	public void setDateOfBirth(DateTime dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}
	
	public DateTime getDateOfDeath() {
		return dateOfDeath;
	}
	
	@JsonDeserialize(as=DateTime.class,using=ApixioDateDeserialzer.class)
	public void setDateOfDeath(DateTime dateOfDeath) {
		this.dateOfDeath = dateOfDeath;
	}
	
	
	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender sex) {
		this.gender = sex;
	}

	public Name getName() {
		return name;
	}

	public void setName(Name name) {
		this.name = name;
	}

	public void setRace(ClinicalCode race) {
		this.race = race;
	}

	public ClinicalCode getRace() {
		return race;
	}

	public void setEthnicity(ClinicalCode ethnicity) {
		this.ethnicity = ethnicity;
	}

	public ClinicalCode getEthnicity() {
		return ethnicity;
	}

	public void setMaritalStatus(MaritalStatus maritalStatus) {
		this.maritalStatus = maritalStatus;
	}

	public MaritalStatus getMaritalStatus() {
		return maritalStatus;
	}

	public void setLanguages(List<String> languages) {
		this.languages = languages;
	}

	public List<String> getLanguages() {
		if (languages != null) {
			languages = languages.stream().filter(Objects::nonNull).collect(Collectors.toList());
		} else {
			languages = new ArrayList<>();
		}

		return languages;
	}

	public void setReligiousAffiliation(String religiousAffiliation) {
		this.religiousAffiliation = religiousAffiliation;
	}

	public String getReligiousAffiliation() {
		return religiousAffiliation;
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this, Constants.TO_STRING_STYLE).
	       append("name", name).
	       append("dateOfBirth", dateOfBirth).
	       append("gender", gender).
	       append("race", race).
	       append("ethnicity", ethnicity).
	       append("maritalStatus", maritalStatus).
	       append("language", languages).
	       append("religiousAffiliation", religiousAffiliation).
	       appendSuper(super.toString()).
	       toString();
	}

	@Override
	public int hashCode() {
		HashCodeBuilder hashCodeBuilder = new HashCodeBuilder()
				.append(this.dateOfBirth).append(this.ethnicity)
				.append(this.gender).append(this.maritalStatus)
				.append(this.name).append(this.race)
				.append(this.religiousAffiliation)
				.appendSuper(super.hashCode());
		if(this.languages!=null){
			hashCodeBuilder.append(this.languages.toArray());	
		}
		return hashCodeBuilder.toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj!=null && obj instanceof Demographics){
			Demographics d = (Demographics) obj;
			EqualsBuilder equalsBuilder = new EqualsBuilder()
					.append(this.dateOfBirth, d.dateOfBirth)
					.append(this.ethnicity, d.ethnicity)
					.append(this.gender, d.gender)
					.append(this.maritalStatus, d.maritalStatus)
					.append(this.name, d.name).append(this.race, d.race)
					.append(this.religiousAffiliation, d.religiousAffiliation)
					.appendSuper(super.equals(obj));
			if(this.languages!=null && d.languages!=null){
				Object[] arr1 = this.languages.toArray();
				Object[] arr2 = d.languages.toArray();
				Arrays.sort(arr1);
				Arrays.sort(arr2);
				equalsBuilder.append(arr1, arr2);
			}
			return equalsBuilder.isEquals();
		}
		return false;
	}	
}
