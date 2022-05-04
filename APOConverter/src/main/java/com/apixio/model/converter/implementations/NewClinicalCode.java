package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.converter.utils.ConverterUtils;
import com.apixio.model.owl.interfaces.*;
import org.joda.time.DateTime;

import java.net.URI;
import java.util.Collection;

/**
 * Created by jctoledo on 3/11/16.
 */
public class NewClinicalCode implements ClinicalCode {
    private ClinicalCodingSystem clinicalCodingSys;
    private String displayName;
    private String clinicalCodeValue;
    private XUUID xuuid;
    private ExternalIdentifier externalId;
    private Collection<DataProcessingDetail> dataProcDets;
    private String logicalId;
    private Collection<XUUID> otherIds;
    private Date lastEditDate;
    private Collection<ExternalIdentifier> otherOriginalIds;
    private URI uri;

    public NewClinicalCode(){
        this.xuuid = XUUID.create("NewClinicalCode");
        this.setLastEditDate(ConverterUtils.createApixioDate(new DateTime()));
    }

    @Override
    public ClinicalCodingSystem getClinicalCodingSystem() {
        return this.clinicalCodingSys;
    }

    @Override
    public void setClinicalCodingSystem(ClinicalCodingSystem newClinicalCodingSystem) {
        this.clinicalCodingSys = newClinicalCodingSystem;
    }

    @Override
    public String getClinicalCodeDisplayName() {
        return this.displayName;
    }

    @Override
    public void setClinicalCodeDisplayName(String newClinicalCodeDisplayName) {
        this.displayName = newClinicalCodeDisplayName;
    }

    @Override
    public String getClinicalCodeValue() {
        return this.clinicalCodeValue;
    }

    @Override
    public void setClinicalCodeValue(String newClinicalCodeValue) {
        this.clinicalCodeValue = newClinicalCodeValue;
    }

    @Override
    public XUUID getInternalXUUID() {
        return xuuid;
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
}
