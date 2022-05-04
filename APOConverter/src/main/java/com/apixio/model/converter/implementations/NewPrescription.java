package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by jctoledo on 3/28/16.
 */
public class NewPrescription implements Prescription{

    private XUUID xuuid;
    private Collection<Medication> meds;
    private Date fillDate;
    private Date endDate;
    private Date presDate;
    private Integer refills;
    private Boolean isActive;
    private XUUID sourceEncId;
    private Collection<XUUID> otherIds;
    private String sig;
    private Date lastEditDate;
    private Double quantity;
    private String freq;
    private ExternalIdentifier externalId;
    private XUUID primaryMedProId;
    private Collection<XUUID> altMedProIds;
    private Collection<ExternalIdentifier> otherOriginalIds;
    private Collection<DataProcessingDetail> dataProcDetails;
    private ClinicalCode code;
    private String logicalId;
    private URI uri;

    public NewPrescription(){
        this.xuuid = XUUID.create("NewPrescription");
    }


    @Override
    public Collection<Medication> getPrescribedDrugs() {
        return this.meds;
    }

    @Override
    public Collection<Medication> addPrescribedDrugs(Medication anItem) {
        if(this.meds == null){
            this.meds = new ArrayList<Medication>();
            this.meds.add(anItem);
        } else {
            this.meds.add(anItem);
        }
        return this.meds;
    }

    @Override
    public Medication getPrescribedDrugsById(XUUID anID) {
        return null;
    }

    @Override
    public void setPrescribedDrugs(Collection<Medication> newPrescribedDrugs) {
        this.meds = newPrescribedDrugs;
    }

    @Override
    public Date getPrescriptionFillDate() {
        return this.fillDate;
    }

    @Override
    public void setPrescriptionFillDate(Date newPrescriptionFillDate) {
        this.fillDate = newPrescriptionFillDate;
    }

    @Override
    public Date getPrescriptionEndDate() {
        return this.endDate;
    }

    @Override
    public void setPrescriptionEndDate(Date newPrescriptionEndDate) {
        this.endDate = newPrescriptionEndDate;
    }

    @Override
    public Date getPrescriptionDate() {
        return this.presDate;
    }

    @Override
    public void setPrescriptionDate(Date newPrescriptionDate) {
        this.presDate = newPrescriptionDate;
    }

    @Override
    public Integer getNumberOfRefillsRemaining() {
        return this.refills;
    }

    @Override
    public void setNumberOfRefillsRemaining(Integer newNumberOfRefillsRemaining) {
        this.refills = newNumberOfRefillsRemaining;
    }

    @Override
    public Boolean getIsActivePrescription() {
        return this.isActive;
    }

    @Override
    public void setIsActivePrescription(Boolean newIsActivePrescription) {
        this.isActive = newIsActivePrescription;
    }

    @Override
    public String getSig() {
        return this.sig;
    }

    @Override
    public void setSig(String newSig) {
        this.sig = newSig;
    }

    @Override
    public Double getPrescriptionQuantity() {
        return this.quantity;
    }

    @Override
    public void setPrescriptionQuantity(Double newPrescriptionQuantity) {
        this.quantity = newPrescriptionQuantity;
    }

    @Override
    public String getPrescriptionFrequency() {
        return this.freq;
    }

    @Override
    public void setPrescriptionFrequency(String newPrescriptionFrequency) {
        this.freq = newPrescriptionFrequency;
    }

    @Override
    public XUUID getSourceEncounterId() {
        return this.sourceEncId;
    }

    @Override
    public void setSourceEncounterId(XUUID anID) {
        this.sourceEncId = anID;
    }

    @Override
    public XUUID getPrimaryMedicalProfessional() {
        return this.primaryMedProId;
    }

    @Override
    public void setMedicalProfessional(XUUID anId) {
        this.primaryMedProId = anId;
    }

    @Override
    public Collection<XUUID> getAlternateMedicalProfessionalIds() {
        return this.altMedProIds;
    }

    @Override
    public void setAlternateMedicalProfessionalIds(Collection<XUUID> medicalPros) {
        this.altMedProIds = medicalPros;
    }

    @Override
    public ClinicalCode getCodedEntityClinicalCode() {
        return this.code;
    }

    @Override
    public void setCodedEntityClinicalCode(ClinicalCode newCodedEntityClinicalCode) {
        this.code = newCodedEntityClinicalCode;
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
        this.lastEditDate= newLastEditDate;
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
}
