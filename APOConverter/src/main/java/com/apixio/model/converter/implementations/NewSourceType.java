package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.Date;
import com.apixio.model.owl.interfaces.donotimplement.SourceType;
import com.apixio.model.owl.interfaces.donotimplement.SourceTypeValue;

import java.net.URI;
import java.util.Collection;

/**
 * Created by jctoledo on 6/13/16.
 */
public class NewSourceType implements SourceType {
    private URI uri;
    private XUUID xuuid;
    private SourceTypeValue sourceTypeValue;
    private Collection<XUUID> otherIds;
    private String logicalId;
    private Date lastEditDate;
    private String sourceTypeStringValue;


    public NewSourceType(){
        this.xuuid = XUUID.create("NewSourceType");
    }

    @Override
    public SourceTypeValue getSourceTypeValue() {
        return this.sourceTypeValue;
    }

    @Override
    public void setSourceTypeValue(SourceTypeValue en) {
        this.sourceTypeValue = en;
    }

    @Override
    public String getSourceTypeStringValue() {
        return this.sourceTypeStringValue;
    }

    @Override
    public void setSourceTypeStringValue(String newSourceTypeStringValue) {
        this.sourceTypeStringValue = newSourceTypeStringValue;
    }

    @Override
    public XUUID getInternalXUUID() {
        return this.xuuid;
    }

    @Override
    public void setInternalXUUID(XUUID anXUUID) {
        this.xuuid= anXUUID;
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
