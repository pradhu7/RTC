package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.*;

import java.net.URI;
import java.util.Collection;

/**
 * Created by jctoledo on 3/29/16.
 */
public class NewImmunization implements Immunization {

    private XUUID xuuid;
    private Date adminDate;
    private DateRange dateRange;
    private Double quantity;
    private XUUID sourceEncId;
    private Integer seriesNumber;
    private XUUID medProId;
    private Collection<XUUID> altMedProIds;
    private Collection<XUUID> otherIds;
    private ExternalIdentifier externalID;
    private Date lastEditDate;
    private Collection<Medication> meds;
    private ClinicalCode code;
    private String logicalId;
    private Collection<ExternalIdentifier> otherOriginalIds;
    private Collection<DataProcessingDetail> dataProcDets;
    private URI uri;

    public NewImmunization(){
        this.xuuid = XUUID.create("NewImmunization");
    }

    @Override
    public Date getImmunizationAdminDate() {
        return this.adminDate;
    }

    @Override
    public void setImmunizationAdminDate(Date newImmunizationAdminDate) {
        this.adminDate = newImmunizationAdminDate;
    }

    @Override
    public Collection<Medication> getImmunizationMedications() {
        return this.meds;
    }

    @Override
    public Collection<Medication> addImmunizationMedications(Medication anItem) {
        return null;
    }

    @Override
    public Medication getImmunizationMedicationsById(XUUID anID) {
        return null;
    }

    @Override
    public void setImmunizationMedications(Collection<Medication> newImmunizationMedications) {
        this.meds = newImmunizationMedications;
    }

    @Override
    public DateRange getImmunizationDateRange() {
        return this.dateRange;
    }

    @Override
    public void setImmunizationDateRange(DateRange newImmunizationDateRange) {
        this.dateRange =newImmunizationDateRange;
    }

    @Override
    public Double getImmunizationQuantity() {
        return this.quantity;
    }

    @Override
    public void setImmunizationQuantity(Double newImmunizationQuantity) {
        this.quantity = newImmunizationQuantity;
    }

    @Override
    public Integer getImmunizationMedicationSeriesNumber() {
        return this.seriesNumber;
    }

    @Override
    public void setImmunizationMedicationSeriesNumber(Integer newImmunizationMedicationSeriesNumber) {
        this.seriesNumber = newImmunizationMedicationSeriesNumber;
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
        return this.medProId;
    }

    @Override
    public void setMedicalProfessional(XUUID anId) {
        this.medProId = anId;
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
    public ExternalIdentifier getOriginalID() {
        return this.externalID;
    }

    @Override
    public void setOriginalID(ExternalIdentifier newEntityOriginalID) {
        this.externalID = newEntityOriginalID;
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
        this.uri  = aURI;
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
}
