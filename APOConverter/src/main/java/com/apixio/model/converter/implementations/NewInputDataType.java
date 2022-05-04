package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.Date;
import com.apixio.model.owl.interfaces.donotimplement.InputDataType;
import com.apixio.model.owl.interfaces.donotimplement.InputDataTypeValue;

import java.net.URI;
import java.util.Collection;

/**
 * Created by jctoledo on 6/13/16.
 */
public class NewInputDataType implements InputDataType {

    private XUUID xuuid;
    private InputDataTypeValue inputDatatypeVal;
    private URI uri;
    private String logicalId;
    private Date lastEditDate;
    private Collection<XUUID> otherIds;

    public NewInputDataType (){
        this.xuuid = XUUID.create("NewInputDataType");
    }

    @Override
    public InputDataTypeValue getInputDataTypeValue() {
        return this.inputDatatypeVal;
    }

    @Override
    public void setInputDataTypeValue(InputDataTypeValue en) {
        this.inputDatatypeVal = en;
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
