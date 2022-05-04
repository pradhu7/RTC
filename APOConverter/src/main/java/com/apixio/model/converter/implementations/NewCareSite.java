package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.*;
import com.apixio.model.owl.interfaces.donotimplement.CareSiteType;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by jctoledo on 3/25/16.
 */
public class NewCareSite implements CareSite {

    private XUUID xuuid;
    private Address address;
    private CareSiteType careSiteType;
    private String careSiteName;
    private Collection<XUUID> otherIds;
    private Date lastEditDate;
    private ExternalIdentifier externalid;
    private Collection<DataProcessingDetail> dataProcDets;
    private Collection<ExternalIdentifier> otherOriginalIds;
    private ExternalIdentifier npi;
    private String logicalId;
    private URI uri;

    public NewCareSite(){
        this.xuuid = XUUID.fromString("NewCareSite");
    }

    @Override
    public ExternalIdentifier getNationalProviderIdentifier() {
        return this.npi;
    }

    @Override
    public void setNationalProviderIdentifier(ExternalIdentifier newNationalProviderIdentifier) {
        this.npi = newNationalProviderIdentifier;
    }

    @Override
    public Address getPrimaryAddress() {
        return this.address;
    }

    @Override
    public void setPrimaryAddress(Address newPrimaryAddress) {
        this.address = newPrimaryAddress;
    }

    @Override
    public CareSiteType getCareSiteType() {
        return this.careSiteType;
    }

    @Override
    public void setCareSiteType(CareSiteType newCareSiteType) {
        this.careSiteType = newCareSiteType;
    }

    @Override
    public String getCareSiteName() {
        return this.careSiteName;
    }

    @Override
    public void setCareSiteName(String newCareSiteName) {
        this.careSiteName = newCareSiteName;
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
        return this.externalid;
    }

    @Override
    public void setOriginalID(ExternalIdentifier newEntityOriginalID) {
        this.externalid = newEntityOriginalID;
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
