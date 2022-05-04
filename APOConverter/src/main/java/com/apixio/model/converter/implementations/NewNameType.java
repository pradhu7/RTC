package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.Date;
import com.apixio.model.owl.interfaces.donotimplement.NameType;
import com.apixio.model.owl.interfaces.donotimplement.Human;
import com.apixio.model.owl.interfaces.donotimplement.NameTypeValue;

import java.net.URI;
import java.util.Collection;

/**
 * Created by jctoledo on 3/25/16.
 */
public class NewNameType implements NameType {

    private XUUID xuuid;
    private NameTypeValue nametypevalue;
    private Collection<Human> persons;
    private Collection<XUUID> otherIds;
    private Date lastEditDate;

    public NewNameType() {
        this.xuuid = XUUID.create("NewNameType");
    }

    @Override
    public NameTypeValue getNameTypeValue() {
        return this.nametypevalue;
    }

    @Override
    public void setNameTypeValue(NameTypeValue en) {
        this.nametypevalue = en;
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
        return null;
    }

    @Override
    public void setLogicalID(String anID) {

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
