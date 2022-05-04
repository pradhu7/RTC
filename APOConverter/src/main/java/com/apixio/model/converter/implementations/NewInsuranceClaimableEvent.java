package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.*;
import com.apixio.model.owl.interfaces.donotimplement.InsuranceClaimableEvent;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by jctoledo on 4/28/16.
 */
public class NewInsuranceClaimableEvent implements InsuranceClaimableEvent {

    private XUUID xuuid;
    private ExternalIdentifier originalId;
    private Collection<ExternalIdentifier> otherOriginalIds;
    private Collection<DataProcessingDetail> dataProcDetails;
    private Date lastEditDate;
    private URI uri;
    private Collection<XUUID> otherIds;


    @Override
    public XUUID getSourceEncounterId() {
        return null;
    }

    @Override
    public void setSourceEncounterId(XUUID anID) {

    }

    @Override
    public XUUID getPrimaryMedicalProfessional() {
        return null;
    }

    @Override
    public void setMedicalProfessional(XUUID anId) {

    }

    @Override
    public Collection<XUUID> getAlternateMedicalProfessionalIds() {
        return null;
    }

    @Override
    public void setAlternateMedicalProfessionalIds(Collection<XUUID> medicalPros) {

    }

    @Override
    public ClinicalCode getCodedEntityClinicalCode() {
        return null;
    }

    @Override
    public void setCodedEntityClinicalCode(ClinicalCode newCodedEntityClinicalCode) {

    }

    @Override
    public Collection<String> getCodedEntityNames() {
        return null;
    }

    @Override
    public Collection<String> addCodedEntityNames(String anItem) {
        return null;
    }

    @Override
    public String getCodedEntityNamesById(XUUID anID) {
        return null;
    }

    @Override
    public void setCodedEntityNames(Collection<String> newCodedEntityNames) {

    }


    public NewInsuranceClaimableEvent(){
        this.xuuid = XUUID.create("NewInsuranceClaimableEvent");
    }


    @Override
    public ExternalIdentifier getOriginalID() {
        return this.originalId;
    }

    @Override
    public void setOriginalID(ExternalIdentifier newOriginalID) {
        this.originalId = newOriginalID;
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
        return this.otherIds;
    }

    @Override
    public void setOtherIds(Collection<XUUID> others) {
        this.otherIds = others;
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
        return this.dataProcDetails;
    }

    @Override
    public Collection<DataProcessingDetail> addDataProcessingDetails(DataProcessingDetail anItem) {
        if(this.dataProcDetails != null){
            this.dataProcDetails.add(anItem);
        }else{
            this.dataProcDetails = new ArrayList<>();
            this.dataProcDetails.add(anItem);
        }
        return this.dataProcDetails;
    }

    @Override
    public DataProcessingDetail getDataProcessingDetailsById(XUUID anID) {
        if(anID != null && this.dataProcDetails != null){
            for(DataProcessingDetail dpd: this.dataProcDetails){
                if(dpd.getInternalXUUID().equals(anID))
                    return dpd;
            }
        }
        return null;
    }

    @Override
    public void setDataProcessingDetails(Collection<DataProcessingDetail> newDataProcessingDetails) {
        this.dataProcDetails = newDataProcessingDetails;
    }
}
