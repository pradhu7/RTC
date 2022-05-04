package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by jctoledo on 3/28/16.
 */
public class NewBiometricValue implements BiometricValue {
    private XUUID xuuid;
    private Date resultDate;
    private String unit;
    private String value;
    private String name;
    private XUUID encId;
    private XUUID primaryMedProId;
    private Collection<XUUID> alternateMedProsIds;
    private Date lastEditDate;
    private Collection<XUUID> otherIds;
    private Collection<DataProcessingDetail> dataProcDets;
    private Collection<ExternalIdentifier> otherOriginalIds;
    private ExternalIdentifier originalId;
    private ClinicalCode code;
    private String logicalId;

    public NewBiometricValue (){
        this.xuuid = XUUID.create("NewBiometricValue");
    }
    @Override
    public Date getMeasurmentResultDate() {
        return this.resultDate;
    }

    @Override
    public void setMeasurmentResultDate(Date newMeasurmentResultDate) {
        this.resultDate = newMeasurmentResultDate;
    }

    @Override
    public String getMeasurmentUnit() {
        return this.unit;
    }

    @Override
    public void setMeasurmentUnit(String newMeasurmentUnit) {
        this.unit = newMeasurmentUnit;
    }

    @Override
    public String getBiometricValueString() {
        return this.value;
    }

    @Override
    public void setBiometricValueString(String newBiometricValueString) {
        this.value = newBiometricValueString;
    }

    @Override
    public String getBiometricValueName() {
        return this.name;
    }

    @Override
    public void setBiometricValueName(String newBiometricValueName) {
        this.name = newBiometricValueName;
    }

    @Override
    public XUUID getSourceEncounterId() {
        return this.encId;
    }

    @Override
    public void setSourceEncounterId(XUUID anID) {
        this.encId = anID;
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
        return this.alternateMedProsIds;
    }

    @Override
    public void setAlternateMedicalProfessionalIds(Collection<XUUID> medicalPros) {
        this.alternateMedProsIds = medicalPros;
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
        return this.originalId;
    }

    @Override
    public void setOriginalID(ExternalIdentifier newEntityOriginalID) {
        this.originalId = newEntityOriginalID;
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
}
