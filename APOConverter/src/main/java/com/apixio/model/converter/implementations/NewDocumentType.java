package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.*;
import com.apixio.model.owl.interfaces.donotimplement.DocumentType;
import com.apixio.model.owl.interfaces.donotimplement.DocumentTypeValue;

import java.net.URI;
import java.util.Collection;

/**
 * Created by jctoledo on 3/14/16.
 */
public class NewDocumentType implements DocumentType {


    private DocumentTypeValue documentTypeValue;
    private XUUID xuuid;
    private String logicalId;
    private Collection<XUUID> otherIds;
    private Date lastEditDate;

    @Override
    public DocumentTypeValue getDocumentTypeValue() {
        return this.documentTypeValue;
    }

    @Override
    public void setDocumentTypeValue(DocumentTypeValue en) {
        this.documentTypeValue = en;
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
