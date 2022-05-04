package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.*;

import java.net.URI;
import java.util.Collection;

/**
 * Created by jctoledo on 4/19/16.
 */
public class NewInsuranceCompany implements InsuranceCompany {
    private String name;
    private ExternalIdentifier npi;
    private ContactDetails contactDets;
    private ExternalIdentifier externalId;
    private Collection<ExternalIdentifier> otherOriginalIds;
    private Collection<DataProcessingDetail> dataProcDets;
    private XUUID xuuid;
    private URI uri;
    private Date lastEditDate;


    @Override
    public ExternalIdentifier getNationalProviderIdentifier() {
        return this.npi;
    }

    @Override
    public void setNationalProviderIdentifier(ExternalIdentifier newNationalProviderIdentifier) {
        this.npi = newNationalProviderIdentifier;
    }

    @Override
    public ContactDetails getContactDetails() {
        return this.contactDets;
    }

    @Override
    public void setContactDetails(ContactDetails newContactDetails) {
        this.contactDets = newContactDetails;
    }

    @Override
    public String getOrganizationNameValue() {
        return this.name;
    }

    @Override
    public void setOrganizationNameValue(String newOrganizationNameValue) {
        this.name = newOrganizationNameValue;
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
    public Collection<DataProcessingDetail> getDataProcessingDetails() {
        return this.dataProcDets;
    }

    @Override
    public Collection<DataProcessingDetail> addDataProcessingDetails(DataProcessingDetail anItem) {
        return null;
    }

    @Override
    public DataProcessingDetail getDataProcessingDetailsById(XUUID anID) {
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
        return null;
    }

    @Override
    public void setOtherIds(Collection<XUUID> others) {

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
