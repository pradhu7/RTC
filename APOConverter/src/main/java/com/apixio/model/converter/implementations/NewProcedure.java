package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by jctoledo on 3/24/16.
 */
public class NewProcedure implements Procedure {

    private XUUID xuuid;
    private Collection<XUUID> otherIds;
    private Date lastEditDate;
    private AnatomicalEntity anatomicalEnt;
    private Collection<ClinicalCode> supportingDx;
    private DateRange dateRange;
    private String interpretation;
    private XUUID encounterId;
    private XUUID medicalProUUID;
    private Collection<XUUID> alternateMedProUUIDs;
    private Collection<DataProcessingDetail> dataProcDetails;
    private ExternalIdentifier externalId;
    private String procedureName;
    private Collection<ExternalIdentifier> otherOriginalIds;
    private ClinicalCode code;
    private URI uri;

    public NewProcedure(){
        this.xuuid = XUUID.create("NewProcedure");
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
    public AnatomicalEntity getBodySite() {
        return this.anatomicalEnt;
    }

    @Override
    public void setBodySite(AnatomicalEntity newBodySite) {
        this.anatomicalEnt = newBodySite;
    }

    @Override
    public Collection<ClinicalCode> getDiagnosisCodes() {
        return this.supportingDx;
    }

    @Override
    public Collection<ClinicalCode> addDiagnosisCodes(ClinicalCode anItem) {
        if(anItem != null && this.supportingDx != null){
            this.supportingDx.add(anItem);
        }else{
            this.supportingDx = new ArrayList<>();
            this.supportingDx.add(anItem);
        }
        return this.supportingDx;
    }

    @Override
    public ClinicalCode getDiagnosisCodesById(XUUID anID) {
        if(this.supportingDx != null && anID != null){
            for(ClinicalCode cc : this.supportingDx){
                if(cc.getInternalXUUID().equals(anID)){
                    return cc;
                }
            }
        }
        return null;
    }

    @Override
    public void setDiagnosisCodes(Collection<ClinicalCode> newSupportingDiagnoses) {
        this.supportingDx = newSupportingDiagnoses;
    }

    @Override
    public DateRange getMedicalProcedureDateRange() {
        return this.dateRange;
    }

    @Override
    public void setMedicalProcedureDateRange(DateRange newMedicalProcedureDateRange) {
        this.dateRange = newMedicalProcedureDateRange;
    }

    @Override
    public String getMedicalProcedureInterpretation() {
        return this.interpretation;
    }

    @Override
    public void setMedicalProcedureInterpretation(String newMedicalProcedureInterpretation) {
        this.interpretation = newMedicalProcedureInterpretation;
    }

    @Override
    public String getMedicalProcedureNameValue() {
        return this.procedureName;
    }

    @Override
    public void setMedicalProcedureNameValue(String newMedicalProcedureNameValue) {
        this.procedureName = newMedicalProcedureNameValue;
    }

    @Override
    public XUUID getSourceEncounterId() {
        return this.encounterId;
    }

    @Override
    public void setSourceEncounterId(XUUID anID) {
        this.encounterId = anID;
    }

    @Override
    public XUUID getPrimaryMedicalProfessional() {
        return this.medicalProUUID;
    }

    @Override
    public void setMedicalProfessional(XUUID anId) {
        this.medicalProUUID = anId;
    }

    @Override
    public Collection<XUUID> getAlternateMedicalProfessionalIds() {
        return this.alternateMedProUUIDs;
    }

    @Override
    public void setAlternateMedicalProfessionalIds(Collection<XUUID> medicalPros) {
        this.alternateMedProUUIDs = medicalPros;
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
