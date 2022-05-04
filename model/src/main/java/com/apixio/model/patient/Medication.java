package com.apixio.model.patient;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.apixio.model.Builder;
import com.apixio.model.Constants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Medication extends CodedBaseObject {
	
	private String brandName;
	private String genericName;
	private List<String> ingredients;
	private String strength;
	private String form;
	private String routeOfAdministration;
	private String units;
	
	public static MedBuilder newBuilder() {
		return new MedBuilder();
	}
	
	public static class MedBuilder implements Builder<Medication>{
		private Medication m;
		
		public MedBuilder() {
			m = new Medication();
			m.setCode(new ClinicalCode());
		}

		public Medication build() {
			// TODO Auto-generated method stub
			return m;
		}
		
		public MedBuilder setBrandName(String brandName) {
			m.brandName = brandName;
			return this;
		}
		
		public MedBuilder setGenericName(String genericName) {
			m.genericName = genericName;
			return this;
		}
		
		public MedBuilder setCode(String code) {
			m.getCode().setCode(code);
			return this;
		}
		
		public MedBuilder setCodingSystem(String codingSystem) {
			m.getCode().setCodingSystem(codingSystem);
			return this;
		}
	}
		
	public String getBrandName() {
		return brandName;
	}
	public void setBrandName(String brandName) {
		this.brandName = brandName;
	}
	public String getGenericName() {
		return genericName;
	}
	public void setGenericName(String genericName) {
		this.genericName = genericName;
	}
	
	public String getStrength() {
		return strength;
	}
	public void setStrength(String strength) {
		this.strength = strength;
	}
	public String getUnits() {
		return units;
	}
	public void setUnits(String units) {
		this.units = units;
	}

	public List<String> getIngredients() {
		return ingredients;
	}

	public void setIngredients(List<String> ingredients) {
		this.ingredients = ingredients;
	}

	public String getForm() {
		return form;
	}

	public void setForm(String form) {
		this.form = form;
	}

	public String getRouteOfAdministration() {
		return routeOfAdministration;
	}

	public void setRouteOfAdministration(String routeOfAdministration) {
		this.routeOfAdministration = routeOfAdministration;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this, Constants.TO_STRING_STYLE).
	       append("brandName", brandName).
		   append("genericName", genericName).
	       append("ingredients", ingredients).
	       append("strength", strength).
	       append("form", form).
	       append("routeOfAdministration", routeOfAdministration).
	       append("units", units).
	       appendSuper(super.toString()).
	       toString();
	}
	
	@Override
	public int hashCode() {
		HashCodeBuilder hashCodeBuilder = new HashCodeBuilder().append(this.brandName).append(this.genericName).append(this.routeOfAdministration).append(this.strength).append(this.units).appendSuper(super.hashCode());
		if(this.ingredients!=null){
			hashCodeBuilder.append(this.ingredients.toArray());
		}
		return hashCodeBuilder.toHashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj!=null && obj instanceof Medication){
			Medication m = (Medication)obj;
			EqualsBuilder equalsBuilder = new EqualsBuilder()
					.append(this.brandName, m.brandName)
					.append(this.form, m.form)
					.append(this.genericName, m.genericName)
					.append(this.routeOfAdministration, m.routeOfAdministration)
					.append(this.strength, m.strength)
					.append(this.units, m.units).appendSuper(super.equals(obj));
			
			if(this.ingredients!=null && m.ingredients!=null){
				Object[] arr1 = this.ingredients.toArray();
				Object[] arr2 = m.ingredients.toArray();
				Arrays.sort(arr1);
				Arrays.sort(arr2);
				equalsBuilder.append(arr1, arr2);
			}else if (this.ingredients!=null || m.ingredients!=null){
				return false;
			}
			
			return equalsBuilder.isEquals();
		}
		return false;
	}
	
	
}
