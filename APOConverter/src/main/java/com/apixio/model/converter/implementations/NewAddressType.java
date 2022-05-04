package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.Date;
import com.apixio.model.owl.interfaces.donotimplement.AddressType;
import com.apixio.model.owl.interfaces.donotimplement.AddressTypeValue;

import java.net.URI;
import java.util.Collection;

/**
 * Created by jctoledo on 3/14/16.
 */
public class NewAddressType implements AddressType {
    private AddressTypeValue addressTypeValue;
    private XUUID xuuid;
    private String logicalId;
    private Collection<XUUID> otherIds;
    private Date lastEditDate;
    private URI uri;


    @Override
    public AddressTypeValue getAddressTypeValue() {
        return this.addressTypeValue;
    }

    @Override
    public void setAddressTypeValue(AddressTypeValue en) {
        this.addressTypeValue = en;
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
        return this.uri;
    }

    @Override
    public void setURI(URI aURI) {
        this.uri = aURI;
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
        return this.logicalId;
    }

    @Override
    public void setLogicalID(String anID) {
        this.logicalId = anID;
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
