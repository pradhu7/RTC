package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.*;
import com.apixio.model.owl.interfaces.donotimplement.FeeForServiceInsuranceClaimType;
import com.apixio.model.owl.interfaces.donotimplement.InsuranceClaimStatusType;
import com.apixio.model.owl.interfaces.donotimplement.InsuranceClaimableEvent;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by jctoledo on 3/28/16.
 */
public class NewFeeForServiceClaim implements FeeForServiceInsuranceClaim {

    private XUUID xuuid;
    private InsuranceClaimStatusType claimStatusType;
    private CareSite careSite;
    private Date submissionDate;
    private Collection<InsuranceClaimableEvent> claimableEvents;
    private XUUID medProId;
    private XUUID sourceEncounter;
    private Collection<XUUID> otherMedProIds;
    private ExternalIdentifier externalId;
    private Collection<ExternalIdentifier> otherOriginalIds;
    private Collection<XUUID> otherIds;
    private Collection<DataProcessingDetail> dataProcDetails;
    private Date lastEditDate;
    private Collection<ClinicalCode> services;
    private String logicalID;
    private URI uri;
    private Collection<ClinicalCode> diagnosisCodes;
    private String sourceSystem;
    private CMSClaimValidationResult cmsClaimValidationRes;
    private Date processingDate;
    private DateRange dateOfServiceRange;
    private ClinicalCode providerType;
    private ClinicalCode billType;
    private Collection<MedicalProfessional> billingProviders;
    private Collection<MedicalProfessional> renderingProviders;
    private Boolean deleteIndicator = false;
    private Boolean addIndicator=false;
    private FeeForServiceInsuranceClaimType ffsClaimTypeVal;

    public NewFeeForServiceClaim() {
        this.xuuid = XUUID.create("NewFeeForServiceClaims");
    }



    @Override
    public CareSite getInsuranceClaimCareSite() {
        return this.careSite;
    }

    @Override
    public void setInsuranceClaimCareSite(CareSite newInsuranceClaimCareSite) {
        this.careSite = newInsuranceClaimCareSite;
    }


    @Override
    public InsuranceClaimStatusType getInsuranceClaimStatus() {
        return this.claimStatusType;
    }

    @Override
    public void setInsuranceClaimStatus(InsuranceClaimStatusType newInsuranceClaimStatus) {
        this.claimStatusType = newInsuranceClaimStatus;
    }

    @Override
    public Date getInsuranceClaimProcessingDate() {
        return this.processingDate;
    }

    @Override
    public void setInsuranceClaimProcessingDate(Date newInsuranceClaimProcessingDate) {
        this.processingDate = newInsuranceClaimProcessingDate;
    }

    @Override
    public CMSClaimValidationResult getCMSClaimValidationResult() {
        return this.cmsClaimValidationRes;
    }

    @Override
    public void setCMSClaimValidationResult(CMSClaimValidationResult newCMSClaimValidationResult) {
        this.cmsClaimValidationRes = newCMSClaimValidationResult;
    }

    @Override
    public Date getClaimSubmissionDate() {
        return this.submissionDate;
    }

    @Override
    public void setClaimSubmissionDate(Date newClaimSubmissionDate) {
        this.submissionDate = newClaimSubmissionDate;
    }



    @Override
    public Collection<InsuranceClaimableEvent> getClaimedEvents() {
        return this.claimableEvents;
    }

    @Override
    public Collection<InsuranceClaimableEvent> addClaimedEvents(InsuranceClaimableEvent anItem) {
        if(anItem != null && this.claimableEvents != null){
            this.claimableEvents.add(anItem);
        } else {
            this.claimableEvents = new ArrayList<>();
            this.claimableEvents.add(anItem);
        }
        return this.claimableEvents;
    }

    @Override
    public InsuranceClaimableEvent getClaimedEventsById(XUUID anID) {
        if (anID != null && this.claimableEvents !=null) {
            for(InsuranceClaimableEvent ice: this.claimableEvents){
                if(ice.getInternalXUUID().equals(anID)){
                    return ice;
                }
            }
        }
        return null;
    }

    @Override
    public void setClaimedEvents(Collection<InsuranceClaimableEvent> newClaimedEvents) {
        this.claimableEvents = newClaimedEvents;
    }

    @Override
    public String getSourceSystem() {
        return this.sourceSystem;
    }

    @Override
    public void setSourceSystem(String newSourceSystem) {
        this.sourceSystem = newSourceSystem;
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
        if (this.otherOriginalIds != null) {
            this.otherOriginalIds.add(anItem);
            return this.otherOriginalIds;
        } else {
            this.otherOriginalIds = new ArrayList<>();
            this.otherOriginalIds.add(anItem);
            return this.otherOriginalIds;
        }
    }

    @Override
    public ExternalIdentifier getOtherOriginalIDssById(XUUID anID) {
        if (this.otherOriginalIds != null && anID != null) {
            for (ExternalIdentifier ei : this.otherOriginalIds) {
                if (ei.getInternalXUUID().equals(anID)) {
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
        return null;
    }

    @Override
    public DataProcessingDetail getDataProcessingDetailsById(XUUID anID) {
        return null;
    }

    @Override
    public void setDataProcessingDetails(Collection<DataProcessingDetail> newDataProcessingDetails) {
        this.dataProcDetails = newDataProcessingDetails;
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
        return this.logicalID;
    }

    @Override
    public void setLogicalID(String anID) {
        this.logicalID = anID;
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
    public XUUID getSourceEncounterId() {
        return this.sourceEncounter;
    }

    @Override
    public void setSourceEncounterId(XUUID anID) {
        this.sourceEncounter = anID;
    }

    @Override
    public Boolean getDeleteIndicator() {
        return this.deleteIndicator;
    }

    @Override
    public void setDeleteIndicator(Boolean newDeleteIndicator) {
        this.deleteIndicator = newDeleteIndicator;
    }


    @Override
    public Boolean getAddIndicator(){
        return this.addIndicator;
    }

    @Override
    public void setAddIndicator(Boolean b){
        this.addIndicator = b;
    }
    @Override
    public Collection<MedicalProfessional> getRenderingProviders() {
        return this.renderingProviders;
    }

    @Override
    public Collection<MedicalProfessional> addRenderingProviders(MedicalProfessional anItem) {
        if(this.renderingProviders == null){
            this.renderingProviders = new ArrayList<>();
            this.renderingProviders.add(anItem);
            return this.renderingProviders;
        }else{
            this.renderingProviders.add(anItem);
            return this.renderingProviders;
        }
    }

    @Override
    public MedicalProfessional getRenderingProvidersById(XUUID anID) {
        return null;
    }

    @Override
    public void setRenderingProviders(Collection<MedicalProfessional> newRenderingProviders) {
        this.renderingProviders = newRenderingProviders;
    }

    @Override
    public Collection<MedicalProfessional> getBillingProviders() {
        return this.billingProviders;
    }

    @Override
    public Collection<MedicalProfessional> addBillingProviders(MedicalProfessional anItem) {
        if(this.billingProviders != null){
            this.billingProviders.add(anItem);
            return this.billingProviders;
        }else{
            this.billingProviders = new ArrayList<>();
            this.billingProviders.add(anItem);
            return this.billingProviders;
        }
    }

    @Override
    public MedicalProfessional getBillingProvidersById(XUUID anID) {
        return null;
    }

    @Override
    public void setBillingProviders(Collection<MedicalProfessional> newBillingProviders) {
        this.billingProviders = newBillingProviders;
    }


    @Override
    public Collection<ClinicalCode> getServicesRendereds() {
        return this.services;
    }

    @Override
    public Collection<ClinicalCode> addServicesRendereds(ClinicalCode anItem) {
        if(this.services != null){
            this.services.add(anItem);
            return this.services;
        }else {
            this.services = new ArrayList<>();
            this.services.add(anItem);
            return this.services;
        }
    }


    @Override
    public ClinicalCode getServicesRenderedsById(XUUID anID) {
        return null;
    }

    @Override
    public void setServicesRendereds(Collection<ClinicalCode> newServicesRendereds) {
        this.services = newServicesRendereds;
    }

    @Override
    public DateRange getDateOfServiceRange() {
        return this.dateOfServiceRange;
    }

    @Override
    public void setDateOfServiceRange(DateRange newDateOfServiceRange) {
        this.dateOfServiceRange = newDateOfServiceRange;
    }

    @Override
    public Collection<ClinicalCode> getDiagnosisCodes() {
        return this.diagnosisCodes;
    }

    @Override
    public Collection<ClinicalCode> addDiagnosisCodes(ClinicalCode anItem) {
        if(anItem != null && this.diagnosisCodes != null){
            this.diagnosisCodes.add(anItem);
            return this.diagnosisCodes;
        }else{
            this.diagnosisCodes = new ArrayList<>();
            if(anItem != null){
                this.diagnosisCodes.add(anItem);
                return this.diagnosisCodes;
            }
        }
        return null;
    }

    @Override
    public ClinicalCode getDiagnosisCodesById(XUUID anID) {
        return null;
    }

    @Override
    public void setDiagnosisCodes(Collection<ClinicalCode> newDiagnosisCodes) {
        this.diagnosisCodes = newDiagnosisCodes;
    }

    @Override
    public FeeForServiceInsuranceClaimType getFeeForServiceClaimTypeValue() {
        return this.ffsClaimTypeVal;
    }

    @Override
    public void setFeeForServiceClaimTypeValue(FeeForServiceInsuranceClaimType newFeeForServiceClaimTypeValue) {
        this.ffsClaimTypeVal = newFeeForServiceClaimTypeValue;
    }

    @Override
    public ClinicalCode getProviderType() {
        return this.providerType;
    }

    @Override
    public void setProviderType(ClinicalCode newProviderType) {
        this.providerType = newProviderType;
    }

    @Override
    public ClinicalCode getBillType() {
        return this.billType;
    }

    @Override
    public void setBillType(ClinicalCode newBillType) {
        this.billType = newBillType;
    }


}
