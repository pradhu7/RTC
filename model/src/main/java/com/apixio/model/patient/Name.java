package com.apixio.model.patient;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.ArrayList;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.apixio.model.Constants;

public class Name extends BaseObject{
	private List<String> givenNames;
	private List<String> familyNames;
	private List<String> prefixes;
	private List<String> suffixes;
	private NameType nameType;
	
	public Name() {
		givenNames = new LinkedList<String>();
		familyNames = new LinkedList<String>();
		prefixes = new LinkedList<String>();
		suffixes = new LinkedList<String>();
	}

	public List<String> getGivenNames() {
		if (givenNames != null) {
			givenNames = givenNames.stream().filter(Objects::nonNull).collect(Collectors.toList());
		} else {
			givenNames = new ArrayList<>();
		}

		return givenNames;
	}
	public void setGivenNames(List<String> givenNames) {
		this.givenNames = givenNames;
	}
	public List<String> getFamilyNames() {
		if (familyNames != null) {
			familyNames = familyNames.stream().filter(Objects::nonNull).collect(Collectors.toList());
		} else {
			familyNames = new ArrayList<>();
		}

		return familyNames;
	}
	public void setFamilyNames(List<String> familyNames) {
		this.familyNames = familyNames;
	}
	public List<String> getPrefixes() {
		return prefixes;
	}
	public void setPrefixes(List<String> prefixes) {
		this.prefixes = prefixes;
	}
	public List<String> getSuffixes() {
		return suffixes;
	}
	public void setSuffixes(List<String> suffixes) {
		this.suffixes = suffixes;
	}
	public NameType getNameType() {
		return nameType;
	}
	public void setNameType(NameType nameType) {
		this.nameType = nameType;
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this, Constants.TO_STRING_STYLE).
	       append("givenNames", givenNames).
	       append("familyNames", familyNames).
	       append("prefixes", prefixes).
	       append("suffixes", suffixes).
	       append("nameType", nameType).
	       appendSuper(super.toString()).
	       toString();
	}

	@Override
	public int hashCode() {
		HashCodeBuilder hashCodeBuilder = new HashCodeBuilder().append(this.nameType).appendSuper(super.hashCode());
		if(this.familyNames!=null){
			hashCodeBuilder.append(this.familyNames.toArray());
		}
		if(this.givenNames!=null){
			hashCodeBuilder.append(this.givenNames.toArray());
		}
		if(this.prefixes!=null){
			hashCodeBuilder.append(this.prefixes.toArray());
		}
		if(this.suffixes!=null){
			hashCodeBuilder.append(this.suffixes.toArray());
		}
		return hashCodeBuilder.toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj!=null && obj instanceof Name){
			Name n = (Name)obj;
			EqualsBuilder equalsBuilder = new EqualsBuilder().append(this.nameType, n.nameType).appendSuper(super.equals(obj));
			if(this.familyNames!=null && n.familyNames!=null){
				Object[] arr1 = this.familyNames.toArray();
				Object[] arr2 = n.familyNames.toArray();
				Arrays.sort(arr1);
				Arrays.sort(arr2);
				equalsBuilder.append(arr1, arr2);				
			}else if(this.familyNames!=null || n.familyNames!=null){
				return false;
			}
			
			if(this.givenNames!=null && n.givenNames!=null){
				Object[] arr1 = this.givenNames.toArray();
				Object[] arr2 = n.givenNames.toArray();
				Arrays.sort(arr1);
				Arrays.sort(arr2);
				equalsBuilder.append(arr1, arr2);				
			}else if(this.givenNames!=null || n.givenNames!=null){
				return false;
			}
			
			if(this.prefixes!=null && n.prefixes!=null){
				Object[] arr1 = this.prefixes.toArray();
				Object[] arr2 = n.prefixes.toArray();
				Arrays.sort(arr1);
				Arrays.sort(arr2);
				equalsBuilder.append(arr1, arr2);				
			}else if(this.prefixes!=null || n.prefixes!=null){
				return false;
			}
			
			if(this.suffixes!=null && n.suffixes!=null){
				Object[] arr1 = this.suffixes.toArray();
				Object[] arr2 = n.suffixes.toArray();
				Arrays.sort(arr1);
				Arrays.sort(arr2);
				equalsBuilder.append(arr1, arr2);				
			}else if(this.suffixes!=null || n.suffixes!=null){
				return false;
			}
			return equalsBuilder.isEquals();		
		}
		return false;
	}
	
	
}
