package com.apixio.model.patient;

import java.util.UUID;

/**
 * The CareSite, represents a site of care, a physical (brick&mortar) OR administrative location that
 * was associated with a service provided, a consultation or an encounter.
 * Consider making this a coded base object?.
 * @author vvyas
 *
 */
public class CareSite extends BaseObject {
	private String careSiteName;	// the name of the care site.
	private Address address;		// the address of the care site, sometimes, if the care site is non-physical, this maybe omitted.
	private CareSiteType careSiteType;	// the type of caresite.
	
	public String getCareSiteName() {
		return careSiteName;
	}
	public void setCareSiteName(String careSiteName) {
		this.careSiteName = careSiteName;
	}
	public Address getAddress() {
		return address;
	}
	public void setAddress(Address address) {
		this.address = address;
	}
	public CareSiteType getCareSiteType() {
		return careSiteType;
	}
	public void setCareSiteType(CareSiteType careSiteType) {
		this.careSiteType = careSiteType;
	}
	public void setCareSiteId(UUID careSiteId) {
		this.setInternalUUID(careSiteId);
	}
	public UUID getCareSiteId() {
		return this.getInternalUUID();
	}
}
