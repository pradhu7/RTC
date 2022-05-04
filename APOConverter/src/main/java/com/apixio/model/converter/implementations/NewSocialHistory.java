package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.*;
import com.apixio.model.owl.interfaces.SocialHistory;

import java.net.URI;
import java.util.Collection;

/**
 * Created by jctoledo on 3/29/16.
 */
public class NewSocialHistory implements SocialHistory {

    private XUUID xuuid;
    private Date date;
    private ClinicalCode historyType;
    private String fieldName;
    private String value;
    private Date lastEditDate;
    private XUUID sourceEncId;
    private XUUID primaryMedProId;
    private Collection<XUUID> altMedProIds;
    private Collection<XUUID> otherIds;
    private ExternalIdentifier originalId;
    private Collection<DataProcessingDetail> dataProcDets;
    private Collection<ExternalIdentifier> otherOrigIds;
    private ClinicalCode code;
    private String logicalId;
    private URI uri;

    public NewSocialHistory(){
        this.xuuid = XUUID.create("NewSocialHistory");
    }

    @Override
    public Date getSocialHistoryDate() {
        return this.date;
    }

    @Override
    public void setSocialHistoryDate(Date newSocialHistoryDate) {
        this.date = newSocialHistoryDate;
    }

    @Override
    public ClinicalCode getSocialHistoryType() {
        return this.historyType;
    }

    @Override
    public void setSocialHistoryType(ClinicalCode newSocialHistoryType) {
        this.historyType =newSocialHistoryType;
    }

    @Override
    public String getSocialHistoryFieldName() {
        return this.fieldName;
    }

    @Override
    public void setSocialHistoryFieldName(String newSocialHistoryFieldName) {
        this.fieldName = newSocialHistoryFieldName;
    }

    @Override
    public String getSocialHistoryValue() {
        return this.value;
    }

    @Override
    public void setSocialHistoryValue(String newSocialHistoryValue) {
        this.value = newSocialHistoryValue;
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
        this.lastEditDate = newLastEditDate;
    }

    @Override
    public ExternalIdentifier getOriginalID() {
        return this.originalId;
    }

    @Override
    public void setOriginalID(ExternalIdentifier newEntityOriginalID) {
        this.originalId = newEntityOriginalID;
    }



    @Override
    public Collection<ExternalIdentifier> addOtherOriginalIDss(ExternalIdentifier anItem) {
        return this.otherOrigIds;
    }

    @Override
    public ExternalIdentifier getOtherOriginalIDssById(XUUID anID) {
        return null;
    }


    @Override
    public Collection<ExternalIdentifier> getOtherOriginalIDss() {
        return this.otherOrigIds;
    }

    @Override
    public void setOtherOriginalIDss(Collection<ExternalIdentifier> newOtherEntityOriginalIDss) {
        this.otherOrigIds = newOtherEntityOriginalIDss;
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
