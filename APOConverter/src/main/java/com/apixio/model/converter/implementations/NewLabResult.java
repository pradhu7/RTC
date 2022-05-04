package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.*;
import com.apixio.model.owl.interfaces.donotimplement.LabFlagType;

import java.net.URI;
import java.util.Collection;

/**
 * Created by jctoledo on 3/29/16.
 */
public class NewLabResult implements LabResult {
    private XUUID xuuid;
    private LabFlagType labFlagType;
    private Date sampleDate;
    private CareSite careSite;
    private ClinicalCode specimen;
    private ClinicalCode panel;
    private ClinicalCode superPanel;
    private String value;
    private Date lastEditDate;
    private Collection<XUUID> otherIds;
    private Collection<DataProcessingDetail> dataProcDets;
    private String range;
    private String unit;
    private String name;
    private XUUID encId;
    private XUUID medProId;
    private String labNote;
    private Collection<XUUID> altOtherMedProIds;
    private ExternalIdentifier originalId;
    private Collection<ExternalIdentifier> otherOriginalIds;
    private Integer seqNumb;
    private Integer numberOfSamples;
    private ClinicalCode code;
    private String logicalId;

    public NewLabResult() {
        this.xuuid = XUUID.create("NewLabResult");
    }

    @Override
    public LabFlagType getLabFlagType() {
        return this.labFlagType;
    }

    @Override
    public void setLabFlagType(LabFlagType newLabFlagType) {
        this.labFlagType = newLabFlagType;
    }

    @Override
    public Date getLabResultSampleDate() {
        return this.sampleDate;
    }

    @Override
    public void setLabResultSampleDate(Date newLabResultSampleDate) {
        this.sampleDate = newLabResultSampleDate;
    }

    @Override
    public CareSite getLabResultCareSite() {
        return this.careSite;
    }

    @Override
    public void setLabResultCareSite(CareSite newLabResultCareSite) {
        this.careSite = newLabResultCareSite;
    }

    @Override
    public ClinicalCode getSpecimen() {
        return this.specimen;
    }

    @Override
    public void setSpecimen(ClinicalCode newSpecimen) {
        this.specimen = newSpecimen;
    }

    @Override
    public ClinicalCode getLabPanel() {
        return this.panel;
    }

    @Override
    public void setLabPanel(ClinicalCode newLabPanel) {
        this.panel = newLabPanel;
    }

    @Override
    public ClinicalCode getLabSuperPanel() {
        return this.superPanel;
    }

    @Override
    public void setLabSuperPanel(ClinicalCode newLabSuperPanel) {
        this.superPanel = newLabSuperPanel;
    }

    @Override
    public Date getSampleDate() {
        return this.sampleDate;
    }

    @Override
    public void setSampleDate(Date newSampleDate) {
        this.sampleDate = newSampleDate;
    }

    @Override
    public Integer getLabResultNumberOfSamples() {
        return this.numberOfSamples;
    }

    @Override
    public void setLabResultNumberOfSamples(Integer newLabResultNumberOfSamples) {
        this.numberOfSamples = newLabResultNumberOfSamples;
    }

    @Override
    public String getLaboratoryResultValue() {
        return this.value;
    }

    @Override
    public void setLaboratoryResultValue(String newLaboratoryResultValue) {
        this.value = newLaboratoryResultValue;
    }

    @Override
    public Integer getLabResultSequenceNumber() {
        return this.seqNumb;
    }

    @Override
    public void setLabResultSequenceNumber(Integer newLabResultSequenceNumber) {
        this.seqNumb = newLabResultSequenceNumber;
    }

    @Override
    public String getRange() {
        return this.range;
    }

    @Override
    public void setRange(String newRange) {
        this.range = newRange;
    }

    @Override
    public String getLabResultUnit() {
        return this.unit;
    }

    @Override
    public void setLabResultUnit(String newLabResultUnit) {
        this.unit = newLabResultUnit;
    }

    @Override
    public String getLabNote() {
        return this.labNote;
    }

    @Override
    public void setLabNote(String newLabNote) {
        this.labNote = newLabNote;
    }

    @Override
    public String getLabResultNameValue() {
        return this.name;
    }

    @Override
    public void setLabResultNameValue(String newLabResultNameValue) {
        this.name = newLabResultNameValue;
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
        return this.medProId;
    }

    @Override
    public void setMedicalProfessional(XUUID anId) {
        this.medProId = anId;
    }

    @Override
    public Collection<XUUID> getAlternateMedicalProfessionalIds() {
        return this.altOtherMedProIds;
    }

    @Override
    public void setAlternateMedicalProfessionalIds(Collection<XUUID> medicalPros) {
        this.altOtherMedProIds = medicalPros;
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
