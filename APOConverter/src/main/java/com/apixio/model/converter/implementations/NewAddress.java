package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.*;
import com.apixio.model.owl.interfaces.donotimplement.AddressType;

import java.net.URI;
import java.util.Collection;

/**
 * Created by jctoledo on 3/14/16.
 */
public class NewAddress implements Address {

    private String county;
    private String country;
    private String zipcode;
    private String state;
    private String city;
    private XUUID xuuid;
    private AddressType addressType;
    private Collection<XUUID> otherIds;
    private Date lastEditDate;
    private Collection<String> streetAddresses;
    private String logcialId;

    //TODO - look at default constructor - is this an OK approach to having a default XUUID?
    public NewAddress(){
        this.xuuid = XUUID.create("NewAddress");
    }


    @Override
    public AddressType getAddressType() {
        return this.addressType;
    }

    @Override
    public void setAddressType(AddressType newAddressType) {
        this.addressType = newAddressType;
    }

    @Override
    public String getCountry() {
        return this.country;
    }

    @Override
    public void setCountry(String newCountry) {
        this.country = newCountry;
    }

    @Override
    public String getCounty() {
        return this.county;
    }

    @Override
    public void setCounty(String newCounty) {
        this.county = newCounty;
    }

    @Override
    public String getZipCode() {
        return this.zipcode;
    }

    @Override
    public void setZipCode(String newZipCode) {
        this.zipcode = newZipCode;
    }

    @Override
    public String getState() {
        return this.state;
    }

    @Override
    public void setState(String newState) {
        this.state = newState;
    }

    @Override
    public String getCity() {
        return this.city;
    }

    @Override
    public void setCity(String newCity) {
        this.city = newCity;
    }

    @Override
    public Collection<String> getStreetAddresses() {
        return this.streetAddresses;
    }

    @Override
    public Collection<String> addStreetAddresses(String anItem) {
        if(this.getStreetAddresses() != null){
            this.streetAddresses.add(anItem);
            return this.streetAddresses;
        }
        return null;
    }

    @Override
    public String getStreetAddressesById(XUUID anID) {
        return null;
    }

    @Override
    public void setStreetAddresses(Collection<String> collection) {
        this.streetAddresses = collection;
    }


    @Override
    public XUUID getInternalXUUID() {
        return this.xuuid;
    }

    @Override
    public void setInternalXUUID(XUUID anXUUID) {
        this.xuuid = anXUUID;
    }

    @Override
    public URI getURI() {
        return null;
    }

    @Override
    public void setURI(URI aURI) {

    }

    @Override
    public Collection<XUUID> getOtherIds() {
        return this.otherIds;
    }

    @Override
    public void setOtherIds(Collection<XUUID> others) {
        this.otherIds = others;
    }

    @Override
    public String getLogicalID() {
        return this.logcialId;
    }

    @Override
    public void setLogicalID(String anID) {
        this.logcialId = anID;
    }


    @Override
    public Date getLastEditDate() {
        return this.lastEditDate;
    }

    @Override
    public void setLastEditDate(Date newLastEditDate) {
        this.lastEditDate = newLastEditDate;
    }


}
