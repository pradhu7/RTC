package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.Date;
import com.apixio.model.owl.interfaces.donotimplement.ResolutionStatusType;
import com.apixio.model.owl.interfaces.donotimplement.ResolutionStatusTypeValue;

import java.net.URI;
import java.util.Collection;

/**
 * Created by jctoledo on 3/28/16.
 */
public class NewResolutionStatus implements ResolutionStatusType
{

    private XUUID xuuid;
    private ResolutionStatusTypeValue resolutionStatusTypeVal;
    private Collection<XUUID> otherIds;
    private Date lastEditDate;
    private String logicalId;

    public NewResolutionStatus(){
        this.xuuid = XUUID.create("NewResolutionStatus");
    }
    @Override
    public ResolutionStatusTypeValue getResolutionStatusTypeValue() {
        return this.resolutionStatusTypeVal;
    }

    @Override
    public void setResolutionStatusTypeValue(ResolutionStatusTypeValue en) {
        this.resolutionStatusTypeVal = en;
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
