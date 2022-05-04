package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.converter.utils.ConverterUtils;
import com.apixio.model.owl.interfaces.*;
import com.apixio.model.owl.interfaces.InsuranceCompany;
import com.apixio.model.owl.interfaces.donotimplement.PlanType;
import org.joda.time.DateTime;

import java.net.URI;
import java.util.Collection;

/**
 * Created by jctoledo on 3/29/16.
 */
public class NewInsurancePlan implements InsurancePlan {

    private XUUID xuuid;
    private PlanType planType;
    private ExternalIdentifier groupNumber;
    private String planName;
    private Collection<XUUID> otherIds;
    private Date lastEditDate;
    private ExternalIdentifier originalId;
    private Collection<DataProcessingDetail> dataProcDets;
    private Collection<ExternalIdentifier> otherOriginalIds;
    private String logicalId;
    private ExternalIdentifier planIdentifier;
    private ExternalIdentifier contractIdentifier;

    public NewInsurancePlan() {
        this.xuuid = XUUID.create("NewInsurancePlan");
        this.setLastEditDate(ConverterUtils.createApixioDate(new DateTime()));
    }

    @Override
    public PlanType getInsuranceType() {
        return this.planType;
    }

    @Override
    public void setInsuranceType(PlanType newInsuranceType) {
        this.planType = newInsuranceType;
    }

    @Override
    public InsuranceCompany getInsuranceCompany() {
        return null;
    }

    @Override
    public void setInsuranceCompany(InsuranceCompany newInsuranceCompany) {

    }

    @Override
    public ExternalIdentifier getInsurancePlanIdentifier() {
        return this.planIdentifier;
    }

    @Override
    public void setInsurancePlanIdentifier(ExternalIdentifier newInsurancePlanIdentifier) {
        this.planIdentifier = newInsurancePlanIdentifier;
    }

    @Override
    public ExternalIdentifier getContractIdentifier() {
        return this.contractIdentifier;
    }

    @Override
    public void setContractIdentifier(ExternalIdentifier newContractIdentifier) {
        this.contractIdentifier = newContractIdentifier;
    }

    @Override
    public ExternalIdentifier getGroupNumber() {
        return this.groupNumber;
    }

    @Override
    public void setGroupNumber(ExternalIdentifier newGroupNumber) {
        this.groupNumber = newGroupNumber;
    }



    @Override
    public String getInsurancePlanName() {
        return this.planName;
    }

    @Override
    public void setInsurancePlanName(String newInsurancePlanName) {
        this.planName = newInsurancePlanName;
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
}
