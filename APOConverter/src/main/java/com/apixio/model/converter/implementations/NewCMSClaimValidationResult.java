package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.CMSClaimValidationResult;
import com.apixio.model.owl.interfaces.CMSValidationError;
import com.apixio.model.owl.interfaces.Date;
import com.apixio.model.owl.interfaces.ExternalIdentifier;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by jctoledo on 6/8/16.
 */
public class NewCMSClaimValidationResult implements CMSClaimValidationResult {


    private ExternalIdentifier overpaymentIdentifier;
    private Collection<CMSValidationError> validationErrors;
    private String paymentYear;
    private String claimControlNumber;
    private String validatedPatientDateOfBirth;
    private String validatedProviderType;
    private XUUID xuuid;
    private Date lastEditDate;
    private URI uri;
    private Collection<XUUID> otherIds;
    private String raCodeClusters;
    private Date transactionDate;
    private String logicalId;
    private String planNumber;
    private String fileMode;

    public NewCMSClaimValidationResult (){
        this.xuuid = XUUID.create("NewCMSClaimValidationResult");
    }

    @Override
    public ExternalIdentifier getClaimOverpaymentIdentifier() {
        return this.overpaymentIdentifier;
    }

    @Override
    public void setClaimOverpaymentIdentifier(ExternalIdentifier newClaimOverpaymentIdentifier) {
        this.overpaymentIdentifier = newClaimOverpaymentIdentifier;
    }

    @Override
    public Collection<CMSValidationError> getCMSValidationErrors() {
        return this.validationErrors;
    }

    @Override
    public Collection<CMSValidationError> addCMSValidationErrors(CMSValidationError anItem) {
        if(this.validationErrors != null&& anItem !=null){
            this.validationErrors.add(anItem);
        }else{
            this.validationErrors = new ArrayList<>();
            this.validationErrors.add(anItem);
        }
        return this.validationErrors;
    }

    @Override
    public CMSValidationError getCMSValidationErrorsById(XUUID anID) {
        if(this.validationErrors != null){
            for (CMSValidationError cve : this.validationErrors){
                if(cve.getInternalXUUID().equals(anID)){
                    return cve;
                }
            }
        }
        return null;
    }

    @Override
    public void setCMSValidationErrors(Collection<CMSValidationError> newCMSValidationErrors) {
        this.validationErrors = newCMSValidationErrors;
    }

    @Override
    public Date getValidatedClaimTransactionDate() {
        return this.transactionDate;
    }

    @Override
    public void setValidatedClaimTransactionDate(Date newValidatedClaimTransactionDate) {
        this.transactionDate = newValidatedClaimTransactionDate;
    }

    @Override
    public String getValidatedClaimPaymentYear() {
        return this.paymentYear;
    }

    @Override
    public void setValidatedClaimPaymentYear(String newValidatedClaimPaymentYear) {
        this.paymentYear = newValidatedClaimPaymentYear;
    }

    @Override
    public String getValidatedClaimControlNumber() {
        return this.claimControlNumber;
    }

    @Override
    public void setValidatedClaimControlNumber(String newValidatedClaimControlNumber) {
        this.claimControlNumber = newValidatedClaimControlNumber;
    }

    @Override
    public String getValidatedClaimPatientDateOfBirth() {
        return this.validatedPatientDateOfBirth;
    }

    @Override
    public void setValidatedClaimPatientDateOfBirth(String newValidatedClaimPatientDateOfBirth) {
        this.validatedPatientDateOfBirth = newValidatedClaimPatientDateOfBirth;
    }

    @Override
    public String getValidatedClaimProviderType() {
        return this.validatedProviderType;
    }

    @Override
    public void setValidatedClaimProviderType(String newValidatedClaimProviderType) {
        this.validatedProviderType = newValidatedClaimProviderType;
    }

    @Override
    public String getRiskAssessmentCodeClusters() {
        return this.raCodeClusters;
    }

    @Override
    public void setRiskAssessmentCodeClusters(String newRiskAssessmentCodeClusters) {
        this.raCodeClusters = newRiskAssessmentCodeClusters;
    }

    @Override
    public String getValidatedClaimFileMode() {
        return this.fileMode;
    }

    @Override
    public void setValidatedClaimFileMode(String newValidatedClaimFileMode) {
        this.fileMode = newValidatedClaimFileMode;
    }

    @Override
    public String getValidatedClaimPlanNumber() {
        return this.planNumber;
    }

    @Override
    public void setValidatedClaimPlanNumber(String newValidatedClaimPlanNumber) {
        this.planNumber = newValidatedClaimPlanNumber;
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
        this.otherIds  = others;
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
