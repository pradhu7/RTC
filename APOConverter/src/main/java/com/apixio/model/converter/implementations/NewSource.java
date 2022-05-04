package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.*;
import com.apixio.model.owl.interfaces.donotimplement.Entity;
import com.apixio.model.owl.interfaces.donotimplement.Human;
import com.apixio.model.owl.interfaces.donotimplement.SourceType;

import java.net.URI;
import java.util.Collection;

/**
 * Created by jctoledo on 3/14/16.
 */
public class NewSource implements Source {

    private Organization org;
    private XUUID xuuid;
    private Date lastEditDate;
    private Entity internalObject;
    private File file;
    private Integer lineNumber;
    private Collection<XUUID> otherIds;
    private Human author;
    private Date creationDate;
    private ExternalIdentifier externalId;
    private Collection<ExternalIdentifier> otherOriginalIds;
    private String logicalId;
    private SourceType sourceType;

    public NewSource(){
        this.xuuid = XUUID.create("NewSource");
    }

    @Override
    public ExternalIdentifier getOriginalID() {
        return this.externalId;
    }

    @Override
    public void setOriginalID(ExternalIdentifier newOriginalID) {
        this.externalId = newOriginalID;
    }




    @Override
    public Collection<ExternalIdentifier> getOtherOriginalIDss() {
        return this.otherOriginalIds;
    }

    @Override
    public Collection<ExternalIdentifier> addOtherOriginalIDss(ExternalIdentifier anItem) {
        if(this.otherOriginalIds != null){
            this.otherOriginalIds.add(anItem);
            return this.otherOriginalIds;
        }
        return null;
    }

    @Override
    public ExternalIdentifier getOtherOriginalIDssById(XUUID anID) {
        if(this.otherOriginalIds != null && anID != null){
            for(ExternalIdentifier ei : this.otherOriginalIds){
                if(ei.getInternalXUUID().equals(anID)){
                    return ei;
                }
            }
        }
        return null;
    }

    @Override
    public void setOtherOriginalIDss(Collection<ExternalIdentifier> newOtherOriginalIDss) {
        this.otherOriginalIds = newOtherOriginalIDss;
    }

    @Override
    public Human getSourceAuthor() {
        return this.author;
    }

    @Override
    public void setSourceAuthor(Human newSourceAuthor) {
        this.author = newSourceAuthor;
    }

    @Override
    public File getSourceFile() {
        return this.file;
    }

    @Override
    public void setSourceFile(File newSourceFile) {
        this.file = newSourceFile;
    }

    @Override
    public SourceType getSourceType() {
        return this.sourceType;
    }

    @Override
    public void setSourceType(SourceType newSourceType) {
        this.sourceType = newSourceType;
    }

    @Override
    public Entity getRefersToInternalObject() {
        return this.internalObject;
    }

    @Override
    public void setRefersToInternalObject(Entity newRefersToInternalObject) {
        this.internalObject = newRefersToInternalObject;
    }

    @Override
    public Organization getSourceOrganization() {
        return this.org;
    }

    @Override
    public void setSourceOrganization(Organization newSourceOrganization) {
        this.org = newSourceOrganization;
    }


    @Override
    public Date getSourceCreationDate() {
        return this.creationDate;
    }

    @Override
    public void setSourceCreationDate(Date newSourceCreationDate) {
        this.creationDate = newSourceCreationDate;
    }

    @Override
    public Integer getLineNumber() {
        return this.lineNumber;
    }

    @Override
    public void setLineNumber(Integer newLineNumber) {
        this.lineNumber = newLineNumber;
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
