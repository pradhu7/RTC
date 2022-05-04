package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.Date;
import com.apixio.model.owl.interfaces.donotimplement.ProcessorType;
import com.apixio.model.owl.interfaces.donotimplement.ProcessorTypeValue;

import java.net.URI;
import java.util.Collection;

/**
 * Created by jctoledo on 6/13/16.
 */
public class NewProcessorType implements ProcessorType
{
    private  XUUID xuuid;
    private URI uri;
    private Collection<XUUID> otherIds;
    private String logicalId;
    private Date lastEditDate;
    private ProcessorTypeValue processorTypeVal;


    public NewProcessorType(){
        this.xuuid = XUUID.create("NewProcessorType");
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

    @Override
    public ProcessorTypeValue getProcessorTypeValue() {
        return this.processorTypeVal;
    }

    @Override
    public void setProcessorTypeValue(ProcessorTypeValue en) {
        this.processorTypeVal = en;
    }
}
