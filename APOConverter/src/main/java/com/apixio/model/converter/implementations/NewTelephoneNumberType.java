package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.*;
import com.apixio.model.owl.interfaces.donotimplement.TelephoneType;
import com.apixio.model.owl.interfaces.donotimplement.TelephoneTypeValue;

import java.net.URI;
import java.util.Collection;

/**
 * Created by jctoledo on 3/14/16.
 */
public class NewTelephoneNumberType implements TelephoneType {
    private XUUID xuuid;
    private TelephoneTypeValue telephoneTypeVal;
    private String logicalId;
    private Date lastEditDate;

    @Override
    public TelephoneTypeValue getTelephoneTypeValue() {
        return this.telephoneTypeVal;
    }

    @Override
    public void setTelephoneTypeValue(TelephoneTypeValue en) {
        this.telephoneTypeVal = en;
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
        return null;
    }

    @Override
    public void setOtherIds(Collection<XUUID> others) {

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
        this.lastEditDate =newLastEditDate;
    }

}
