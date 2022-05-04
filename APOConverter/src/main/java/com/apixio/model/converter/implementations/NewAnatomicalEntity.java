package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by jctoledo on 3/25/16.
 */
public class NewAnatomicalEntity implements AnatomicalEntity {
    private XUUID xuuid;
    private String anatomyName;
    private XUUID encounterId;
    private XUUID primaryMedPro;
    private Collection<XUUID> alternateMedPros;
    private Collection<XUUID> otherids;
    private Date lastEditDate;
    private ExternalIdentifier externalId;
    private Collection<DataProcessingDetail> dataProcDetails;
    private Collection<ExternalIdentifier> otherOriginalIds;
    private ClinicalCode code;
    private String logicalID;

    public NewAnatomicalEntity() {
        this.xuuid = XUUID.create("AnatomicalEntity");
    }

    @Override
    public String getAnatomicalEntityName() {
        return this.anatomyName;
    }

    @Override
    public void setAnatomicalEntityName(String newAnatomicalEntityName) {
        this.anatomyName = newAnatomicalEntityName;
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
        return this.primaryMedPro;
    }

    @Override
    public void setMedicalProfessional(XUUID anId) {
        this.primaryMedPro = anId;
    }

    @Override
    public Collection<XUUID> getAlternateMedicalProfessionalIds() {
        return this.alternateMedPros;
    }

    @Override
    public void setAlternateMedicalProfessionalIds(Collection<XUUID> medicalPros) {
        this.alternateMedPros = medicalPros;
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
        return this.otherids;
    }

    @Override
    public void setOtherIds(Collection<XUUID> others) {
        this.otherids = others;
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
