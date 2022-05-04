package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.*;
import com.apixio.model.owl.interfaces.donotimplement.ResolutionStatusType;

import java.net.URI;
import java.util.Collection;

/**
 * Created by jctoledo on 3/28/16.
 */
public class NewProblem implements Problem {

    private XUUID xuuid;
    private ResolutionStatusType resStatusType;
    private DateRange problemDateRange;
    private String problemTemporalStatus;
    private String name;
    private String severity;
    private Date lastEditDate;
    private Collection<XUUID> otherIds;
    private Collection<DataProcessingDetail> dataProcDetails;
    private XUUID medProId;
    private XUUID encounterId;
    private Collection<XUUID> altMedPros;
    private Collection<ExternalIdentifier> otherOriginalIds;
    private ExternalIdentifier externalId;
    private ClinicalCode code;
    private String logicalId;
    private Date resolutionDate;
    private URI uri;

    public NewProblem (){
        this.xuuid = XUUID.create("NewProblem");
    }

    @Override
    public Date getProblemResolutionDate() {
        return this.resolutionDate;
    }

    @Override
    public void setProblemResolutionDate(Date newProblemResolutionDate) {
        this.resolutionDate = newProblemResolutionDate;
    }

    @Override
    public ResolutionStatusType getResolutionStatusType() {
        return this.resStatusType;
    }

    @Override
    public void setResolutionStatusType(ResolutionStatusType newResolutionStatusType) {
        this.resStatusType = newResolutionStatusType;
    }

    @Override
    public DateRange getProblemDateRange() {
        return this.problemDateRange;
    }

    @Override
    public void setProblemDateRange(DateRange newProblemDateRange) {
        this.problemDateRange = newProblemDateRange;
    }

    @Override
    public String getProblemTemporalStatus() {
        return this.problemTemporalStatus;
    }

    @Override
    public void setProblemTemporalStatus(String newProblemTemporalStatus) {
        this.problemTemporalStatus = newProblemTemporalStatus;
    }

    @Override
    public String getProblemName() {
        return this.name;
    }

    @Override
    public void setProblemName(String newProblemName) {
        this.name = newProblemName;
    }

    @Override
    public String getProblemSeverity() {
        return this.severity;
    }

    @Override
    public void setProblemSeverity(String newProblemSeverity) {
        this.severity = newProblemSeverity;
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
        return this.medProId;
    }

    @Override
    public void setMedicalProfessional(XUUID anId) {
        this.medProId = anId;
    }

    @Override
    public Collection<XUUID> getAlternateMedicalProfessionalIds() {
        return this.altMedPros;
    }

    @Override
    public void setAlternateMedicalProfessionalIds(Collection<XUUID> medicalPros) {
        this.altMedPros = medicalPros;
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
