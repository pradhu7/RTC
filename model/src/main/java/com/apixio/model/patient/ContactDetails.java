package com.apixio.model.patient;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.apixio.model.Constants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * ContactDetails of the patient.
 * @author vishnu
 *
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class ContactDetails extends BaseObject {
	private Address primaryAddress;
	private List<Address> alternateAddresses;
	
	private String primaryEmail;
	private List<String> alternateEmails;
	
	private TelephoneNumber primaryPhone;
	private List<TelephoneNumber> alternatePhones;
	
	public ContactDetails() {
		this.alternateEmails = new LinkedList<String>();
		this.alternatePhones = new LinkedList<TelephoneNumber>();	
		this.alternateAddresses = new LinkedList<Address>();
	}
	
	public String getPrimaryEmail() {
		return primaryEmail;
	}

	public void setPrimaryEmail(String primaryEmail) {
		this.primaryEmail = primaryEmail;
	}

	public List<String> getAlternateEmails() {
		return alternateEmails;
	}

	public void setAlternateEmails(List<String> alternateEmails) {
		this.alternateEmails = alternateEmails;
	}

	public TelephoneNumber getPrimaryPhone() {
		return primaryPhone;
	}

	public void setPrimaryPhone(TelephoneNumber primaryPhone) {
		this.primaryPhone = primaryPhone;
	}

	public List<TelephoneNumber> getAlternatePhones() {
		return alternatePhones;
	}

	public void setAlternatePhone(List<TelephoneNumber> alternatePhones) {
		this.alternatePhones = alternatePhones;
	}

	public Address getPrimaryAddress() {
		return primaryAddress;
	}

	public void setPrimaryAddress(Address primaryAddress) {
		this.primaryAddress = primaryAddress;
	}

	public List<Address> getAlternateAddresses() {
		return alternateAddresses;
	}

	public void setAlternateAddress(List<Address> alternateAddresses) {
		this.alternateAddresses = alternateAddresses;
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this, Constants.TO_STRING_STYLE).
	       append("primaryAddress", primaryAddress).
	       append("alternateAddresses", alternateAddresses).
	       append("primaryEmail", primaryEmail).
	       append("alternateEmails", alternateEmails).
	       append("primaryPhone", primaryPhone).
	       append("alternatePhones", alternatePhones).
	       appendSuper(super.toString()).
	       toString();
	}

	@Override
	public int hashCode() {
		HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
		hashCodeBuilder.append(this.primaryAddress).append(this.primaryEmail).append(this.primaryPhone).appendSuper(super.hashCode());
		if(this.alternateAddresses!=null){
			hashCodeBuilder.append(this.alternateAddresses.toArray());
		}
		if(this.alternateEmails!=null){
			hashCodeBuilder.append(this.alternateEmails.toArray());
		}
		if(this.alternatePhones!=null){
			hashCodeBuilder.append(this.alternatePhones.toArray());
		}
		return hashCodeBuilder.toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj!=null && obj instanceof ContactDetails){
			ContactDetails cd = (ContactDetails)obj;
			EqualsBuilder equalsBuilder = new EqualsBuilder().append(this.primaryAddress, cd.primaryAddress).append(this.primaryEmail, cd.primaryEmail).append(this.primaryPhone, cd.primaryPhone).appendSuper(super.equals(obj));
			
			if(this.alternateAddresses!=null && cd.alternateAddresses!=null){
				Object[]arr1 = this.alternateAddresses.toArray();
				Object[]arr2 = cd.alternateAddresses.toArray();
				Arrays.sort(arr1);
				Arrays.sort(arr2);
				equalsBuilder.append(arr1, arr2);
			}else if(this.alternateAddresses!=null || cd.alternateAddresses!=null){
				return false;
			}
			
			if(this.alternateEmails!=null && cd.alternateEmails!=null){
				Object[]arr1 = this.alternateEmails.toArray();
				Object[]arr2 = cd.alternateEmails.toArray();
				Arrays.sort(arr1);
				Arrays.sort(arr2);
				equalsBuilder.append(arr1, arr2);
			}else if(this.alternateEmails!=null || cd.alternateEmails!=null){
				return false;
			}
			
			if(this.alternatePhones!=null && cd.alternatePhones!=null){
				Object[]arr1 = this.alternatePhones.toArray();
				Object[]arr2 = cd.alternatePhones.toArray();
				Arrays.sort(arr1);
				Arrays.sort(arr2);
				equalsBuilder.append(arr1, arr2);
			}else if(this.alternatePhones!=null || cd.alternatePhones!=null){
				return false;
			}
			
		}
		return false;
		
	}
	
	
}
