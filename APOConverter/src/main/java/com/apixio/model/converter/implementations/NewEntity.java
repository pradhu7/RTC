package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.Date;
import com.apixio.model.owl.interfaces.donotimplement.Entity;
import org.joda.time.DateTime;

import java.net.URI;
import java.util.Collection;

/**
 * Created by jctoledo on 4/5/16.
 */
public class NewEntity implements Entity {
    private XUUID xuuid;
    private Collection<XUUID> otherIds;
    private Date lastEditDate;
    private String logicalId;

    //TODO - it would be nice that this constructor gets called every time a new implementation is created
    public NewEntity(){
        this.xuuid = XUUID.create("NewEntity");
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
        this.logicalId= anID;
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
