package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.converter.utils.ConverterUtils;
import com.apixio.model.owl.interfaces.*;
import com.apixio.model.owl.interfaces.donotimplement.InsuranceClaimStatusType;
import com.apixio.model.owl.interfaces.donotimplement.InsuranceClaimableEvent;
import org.joda.time.DateTime;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by jctoledo on 3/17/16.
 */
public class NewRiskAdjustmentClaim implements RiskAdjustmentInsuranceClaim {

    private DateRange serviceDateRange;
    private InsuranceClaimStatusType claimStatusType;
    private Date submissionDate;
    private Collection<InsuranceClaimableEvent> claimableEvents;
    private XUUID xuuid;
    private XUUID encounterId;
    private ExternalIdentifier externalId;
    private Date lastEditDate;
    private CareSite careSite;
    private Collection<XUUID> otherIds;
    private Collection<ExternalIdentifier> otherOriginalIds;
    private Collection<DataProcessingDetail> dataProcDetails;
    private String logicalId;
    private Collection<ClinicalCode> diagnosisCodes;
    private URI uri;
    private Boolean deleteIndicator = false;
    private String sourceSystem;
    private CMSClaimValidationResult validationResult;
    private PatientEncounterSwitchType encounterSwitchType = null;
    private Date claimProcessingDate;
    private RiskAdjustmentInsuranceClaimType raClaimType;
    private Boolean addIndicator = false;
    private ClinicalCode providerType;


    public NewRiskAdjustmentClaim() {
        this.xuuid = XUUID.create("RAClaim");
        this.setLastEditDate(ConverterUtils.createApixioDate(new DateTime()));
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
        return this.claimProcessingDate;
    }

    @Override
    public void setInsuranceClaimProcessingDate(Date newInsuranceClaimProcessingDate) {
        this.claimProcessingDate = newInsuranceClaimProcessingDate;
    }

    @Override
    public CMSClaimValidationResult getCMSClaimValidationResult() {
        return this.validationResult;
    }

    @Override
    public void setCMSClaimValidationResult(CMSClaimValidationResult newCMSClaimValidationResult) {
        this.validationResult = newCMSClaimValidationResult;
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
    public void setClaimedEvents(Collection<InsuranceClaimableEvent> newClaimedEvents) {
        this.claimableEvents = newClaimedEvents;
    }

    @Override
    public Collection<InsuranceClaimableEvent> addClaimedEvents(InsuranceClaimableEvent anItem) {
        if (anItem != null && this.claimableEvents != null) {
            this.claimableEvents.add(anItem);
        } else {
            this.claimableEvents = new ArrayList<>();
            this.claimableEvents.add(anItem);
        }
        return this.claimableEvents;
    }

    @Override
    public InsuranceClaimableEvent getClaimedEventsById(XUUID anID) {
        if (anID != null && this.claimableEvents != null) {
            for (InsuranceClaimableEvent ice : this.claimableEvents) {
                if (ice.getInternalXUUID().equals(anID)) {
                    return ice;
                }
            }
        }
        return null;
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
    public XUUID getSourceEncounterId() {
        return this.encounterId;
    }

    @Override
    public void setSourceEncounterId(XUUID anID) {
        this.encounterId = anID;
    }


    @Override
    public DateRange getDateOfServiceRange() {
        return this.serviceDateRange;
    }

    @Override
    public void setDateOfServiceRange(DateRange newDateOfServiceRange) {
        this.serviceDateRange = newDateOfServiceRange;
    }

    @Override
    public PatientEncounterSwitchType getPatientEncounterSwitchType() {
        return this.encounterSwitchType;
    }

    @Override
    public void setPatientEncounterSwitchType(PatientEncounterSwitchType newPatientEncounterSwitchType) {
        this.encounterSwitchType = newPatientEncounterSwitchType;
    }

    @Override
    public RiskAdjustmentInsuranceClaimType getRiskAdjustmentInsuranceClaimType() {
        return this.raClaimType;
    }

    @Override
    public void setRiskAdjustmentInsuranceClaimType(RiskAdjustmentInsuranceClaimType newRiskAdjustmentInsuranceClaimType) {
        this.raClaimType = newRiskAdjustmentInsuranceClaimType;
    }

    @Override
    public Collection<ClinicalCode> getDiagnosisCodes() {
        return this.diagnosisCodes;
    }

    @Override
    public Collection<ClinicalCode> addDiagnosisCodes(ClinicalCode anItem) {
        if (anItem != null && this.diagnosisCodes != null) {
            this.diagnosisCodes.add(anItem);
        } else {
            this.diagnosisCodes = new ArrayList<>();
            this.diagnosisCodes.add(anItem);
        }
        return this.diagnosisCodes;

    }

    @Override
    public ClinicalCode getDiagnosisCodesById(XUUID anID) {
        if (this.diagnosisCodes != null) {
            for (ClinicalCode cc : this.diagnosisCodes) {
                if (cc.getInternalXUUID().equals(anID)) {
                    return cc;
                }
            }
        }
        return null;
    }

    @Override
    public void setDiagnosisCodes(Collection<ClinicalCode> newClusterDiagnosisCodes) {
        this.diagnosisCodes = newClusterDiagnosisCodes;
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
    public Boolean getAddIndicator() {
        return this.addIndicator;
    }

    @Override
    public void setAddIndicator(Boolean newAddIndicator) {
        this.addIndicator = newAddIndicator;
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
    public URI getMedicareAdvantagePolicyURI() {
        return null;
    }

    @Override
    public void setMedicareAdvantagePolicyURI(URI newMedicareAdvantagePolicyURI) {

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
        if (this.otherOriginalIds == null && anItem != null ){
            this.otherOriginalIds = new ArrayList<>();
            this.otherOriginalIds.add(anItem);
            return this.otherOriginalIds;
        }
        if (this.otherOriginalIds != null && anItem != null) {
            this.otherOriginalIds.add(anItem);
            return this.otherOriginalIds;
        }
        return null;
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
        if (this.dataProcDetails != null) {
            this.dataProcDetails.add(anItem);
        } else {
            this.dataProcDetails = new ArrayList<>();
            this.dataProcDetails.add(anItem);
        }
        return this.dataProcDetails;
    }

    @Override
    public DataProcessingDetail getDataProcessingDetailsById(XUUID anID) {
        if (anID != null && this.dataProcDetails != null) {
            for (DataProcessingDetail dpd : this.dataProcDetails) {
                if (dpd.getInternalXUUID().equals(anID))
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
