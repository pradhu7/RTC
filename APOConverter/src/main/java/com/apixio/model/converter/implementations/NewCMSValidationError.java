package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.CMSValidationError;
import com.apixio.model.owl.interfaces.Date;

import java.net.URI;
import java.util.Collection;

/**
 * Created by jctoledo on 6/8/16.
 */
public class NewCMSValidationError implements CMSValidationError {

    private Date lastEditDate;
    private String errorCodeValue;
    private XUUID xuuid;
    private URI uri;
    private Collection<XUUID> otherIds;
    private String logicalId;

    public NewCMSValidationError (){
        this.xuuid = XUUID.create("CMSValidationError");
    }

    @Override
    public String getCMSValidationErrorCodeValue() {
        return this.errorCodeValue;
    }

    @Override
    public void setCMSValidationErrorCodeValue(String newCMSValidationErrorCodeValue) {
        this.errorCodeValue = newCMSValidationErrorCodeValue;
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
        this.uri =aURI;
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
