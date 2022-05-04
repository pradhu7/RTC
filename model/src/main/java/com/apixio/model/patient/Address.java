package com.apixio.model.patient;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.apixio.model.Constants;

public class Address {
	
	private List<String> streetAddresses = new LinkedList<String>();
	private String city;
	private String state;
	private String zip;
	private String country;
	private String county;
	private AddressType addressType;

	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}
	public String getZip() {
		return zip;
	}
	public void setZip(String zip) {
		this.zip = zip;
	}
	public String getCountry() {
		return country;
	}
	public void setCountry(String country) {
		this.country = country;
	}
	public String getCounty() {
		return county;
	}
	public void setCounty(String county) {
		this.county = county;
	}
	public AddressType getAddressType() {
		return addressType;
	}
	public void setAddressType(AddressType addressType) {
		this.addressType = addressType;
	}
	public void setStreetAddresses(List<String> streetAddresses) {
		this.streetAddresses = streetAddresses;
	}
	public List<String> getStreetAddresses() {
		if (streetAddresses != null) {
			streetAddresses = streetAddresses.stream().filter(Objects::nonNull).collect(Collectors.toList());
		} else {
			streetAddresses = new ArrayList<>();
		}

		return streetAddresses;
	}
	public void setState(String state) {
		this.state = state;
	}
	public String getState() {
		return state;
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this, Constants.TO_STRING_STYLE).
	       append("streetAddresses", streetAddresses).
	       append("city", city).
	       append("state", state).
	       append("zip", zip).
	       append("country", country).
	       append("county", county).
	       append("addressType", addressType).
	       appendSuper(super.toString()).
	       toString();
	}
	
	
	@Override
	public int hashCode() {
		HashCodeBuilder hashCodeBuilder = new HashCodeBuilder()
				.append(this.addressType).append(this.city)
				.append(this.country).append(this.county).append(this.state)
				.append(this.zip).appendSuper(super.hashCode());
		if(this.streetAddresses!=null){
			hashCodeBuilder.append(this.streetAddresses.toArray());
		}
		return hashCodeBuilder.toHashCode();
	}
	
	
	@Override
	public boolean equals(Object obj) {
		if(obj!=null && obj instanceof Address){
			Address a = (Address) obj;
			EqualsBuilder equalsBuilder = new EqualsBuilder()
					.append(this.addressType, a.addressType)
					.append(this.city, a.city).append(this.country, a.country)
					.append(this.county, a.county).append(this.state, a.state)
					.append(this.zip, a.zip).appendSuper(super.equals(obj));
			if(this.streetAddresses!=null && a.streetAddresses!=null){
				Object[] arr1 = this.streetAddresses.toArray();
				Object[] arr2 = a.streetAddresses.toArray();
				// sorting the list of street addresses might not be correct behavior
				// but we want a real use case to justify not sorting
				Arrays.sort(arr1);
				Arrays.sort(arr2);
				equalsBuilder.append(arr1, arr2);
			}
			return equalsBuilder.isEquals();
		}
		return false;
	}
	
	
}
