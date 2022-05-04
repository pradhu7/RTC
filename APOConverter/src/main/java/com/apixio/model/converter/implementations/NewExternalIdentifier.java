package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.AmendmentAnnotation;
import com.apixio.model.owl.interfaces.DataProcessingDetail;
import com.apixio.model.owl.interfaces.Date;
import com.apixio.model.owl.interfaces.ExternalIdentifier;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by jctoledo on 3/14/16.
 */
public class NewExternalIdentifier implements ExternalIdentifier {

    private String idVal;
    private String assignAuth;
    private String idType;
    private XUUID xuuid;
    private ExternalIdentifier externalId;
    private Collection<DataProcessingDetail> dataProcDets;
    private Date lastEditDate;
    private Collection<XUUID> otherIds;
    private Collection<ExternalIdentifier> otherOriginalIds;
    private String logicalId;
    private URI uri;

    public NewExternalIdentifier() {
        this.xuuid = XUUID.create("ExternalIdentifier");
    }

    @Override
    public String getIDValue() {
        return this.idVal;
    }

    @Override
    public void setIDValue(String newIDValue) {
        this.idVal = newIDValue;
    }

    @Override
    public String getIDType() {
        return this.idType;
    }

    @Override
    public void setIDType(String newIDType) {
        this.idType = newIDType;
    }

    @Override
    public String getAssignAuthority() {
        return this.assignAuth;
    }

    @Override
    public void setAssignAuthority(String newAssignAuthority) {
        this.assignAuth = newAssignAuthority;
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
    public ExternalIdentifier getOriginalID() {
        return this.externalId;
    }

    @Override
    public void setOriginalID(ExternalIdentifier newEntityOriginalID) {
        this.externalId = newEntityOriginalID;
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
        if(this.otherOriginalIds != null){
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
    public Collection<DataProcessingDetail> getDataProcessingDetails() {
        return this.dataProcDets;
    }

    @Override
    public Collection<DataProcessingDetail> addDataProcessingDetails(DataProcessingDetail anItem) {
        if(this.dataProcDets != null){
            this.dataProcDets.add(anItem);
        }else{
            this.dataProcDets = new ArrayList<>();
            this.dataProcDets.add(anItem);
        }
        return this.dataProcDets;
    }

    @Override
    public DataProcessingDetail getDataProcessingDetailsById(XUUID anID) {
        if(anID != null && this.dataProcDets != null){
            for (DataProcessingDetail d : this.dataProcDets){
                if(d.getInternalXUUID().equals(anID)){
                    return d;
                }
            }
        }
        return null;
    }

    @Override
    public void setDataProcessingDetails(Collection<DataProcessingDetail> newDataProcessingDetails) {
        this.dataProcDets = newDataProcessingDetails;
    }

    @Override
    public Collection<AmendmentAnnotation> getAmendmentAnnotations() {
        return null;
    }

    @Override
    public Collection<AmendmentAnnotation> addAmendmentAnnotations(AmendmentAnnotation anItem) {
        return null;
    }

    @Override
    public AmendmentAnnotation getAmendmentAnnotationsById(XUUID anID) {
        return null;
    }

    @Override
    public void setAmendmentAnnotations(Collection<AmendmentAnnotation> newAmendmentAnnotations) {

    }
}
