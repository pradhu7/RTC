package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.*;

import java.net.URI;
import java.util.Collection;

/**
 * Created by jctoledo on 3/10/16.
 */
public class NewAllergy implements Allergy {

    private XUUID xuuid;
    private ClinicalCode allergenCode;
    private Date reactionDate;
    private Date resolvedDate;
    private Date diagnosisDate;
    private String severity;
    private XUUID sourceEncId;
    private Collection<DataProcessingDetail> dataProcDets;
    private Collection<XUUID> otherIds;
    private Date lastEditDate;
    private XUUID medProId;
    private ExternalIdentifier externalId;
    private Collection<XUUID> altMedProIds;
    private String logicalId;
    private ClinicalCode code;
    private String allergenString;
    private Collection<ExternalIdentifier> otherOriginalIds;


    public NewAllergy() {
        this.xuuid = XUUID.create("NewAllergy");
    }

    @Override
    public String getLogicalID() {
        return this.logicalId;
    }

    @Override
    public void setLogicalID(String anID) {
        this.logicalId= anID;
    }


    @Override
    public String getAllergyAllergenString() {
        return this.allergenString;
    }

    @Override
    public void setAllergyAllergenString(String newAllergyAllergenString) {
        this.allergenString = newAllergyAllergenString;
    }

    @Override
    public Date getAllergyReactionDate() {
        return this.reactionDate;
    }

    @Override
    public void setAllergyReactionDate(Date newAllergyReactionDate) {
        this.reactionDate = newAllergyReactionDate;
    }

    @Override
    public Date getAllergyResolvedDate() {
        return this.resolvedDate;
    }

    @Override
    public void setAllergyResolvedDate(Date newAllergyResolvedDate) {
        this.resolvedDate = newAllergyResolvedDate;
    }

    @Override
    public ClinicalCode getAllergenCode() {
        return this.allergenCode;
    }

    @Override
    public void setAllergenCode(ClinicalCode newAllergenCode) {
        this.allergenCode = newAllergenCode;
    }


    @Override
    public Date getAllergyDiagnosisDate() {
        return this.diagnosisDate;
    }

    @Override
    public void setAllergyDiagnosisDate(Date newAllergyDiagnosisDate) {
        this.diagnosisDate = newAllergyDiagnosisDate;
    }

    @Override
    public String getAllergyReactionSeverity() {
        return this.severity;
    }

    @Override
    public void setAllergyReactionSeverity(String newAllergyReactionSeverity) {
        this.severity = newAllergyReactionSeverity;
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
        this.dataProcDets  = newDataProcessingDetails;
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
