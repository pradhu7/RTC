package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.Date;
import com.apixio.model.owl.interfaces.Processor;
import com.apixio.model.owl.interfaces.donotimplement.InputDataType;
import com.apixio.model.owl.interfaces.donotimplement.ProcessorType;

import java.net.URI;
import java.util.Collection;

/**
 * Created by jctoledo on 4/15/16.
 */
public class NewProcessor implements Processor {
    private ProcessorType processorType;
    private String procVersion;
    private String pName;
    private XUUID xuuid;
    private URI uri;
    private Collection<XUUID> otherIds;
    private String logicalId;
    private Date lastEditDate;
    private InputDataType inputDataType;

    public NewProcessor(){
        this.xuuid = XUUID.create("NewProcessor");
    }

    @Override
    public ProcessorType getProcessorType() {
        return this.processorType;
    }

    @Override
    public void setProcessorType(ProcessorType newProcessorType) {
        this.processorType = newProcessorType;
    }

    @Override
    public InputDataType getInputDataType() {
        return this.inputDataType;
    }

    @Override
    public void setInputDataType(InputDataType newInputDataType) {
        this.inputDataType = newInputDataType;
    }

    @Override
    public String getProcessorVersion() {
        return this.procVersion;
    }

    @Override
    public void setProcessorVersion(String newProcessorVersion) {
        this.procVersion = newProcessorVersion;
    }

    @Override
    public String getProcessorName() {
        return this.pName;
    }

    @Override
    public void setProcessorName(String newProcessorName) {
        this.pName = newProcessorName;
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
