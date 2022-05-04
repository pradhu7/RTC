package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by jctoledo on 10/26/16.
 */
public class NewMedicareInsurancePolicy implements MedicareInsurancePolicy {

    private XUUID xuuid;
    private ExternalIdentifier originalId;
    private ExternalIdentifier memberId;
    private ExternalIdentifier subscriberId;
    private DateRange dateRange;
    private Collection<ExternalIdentifier> otherOriginalIds;
    private Collection<DataProcessingDetail> dataProcDets;
    private Date lastEditDate;
    private Collection<XUUID> otherIds;
    private String name;
    private Integer seqNumber;
    private String logicalId;
    private URI uri;
    private InsurancePlan insurancePlan;
    private HICN hicn;

    public NewMedicareInsurancePolicy (){
        this.xuuid = XUUID.create("NewMedicareInsurancePolicy");
    }

    @Override
    public HICN getHICNNumber() {
        return this.hicn;
    }

    @Override
    public void setHICNNumber(HICN newHICNNumber) {
        this.hicn = newHICNNumber;
    }
    @Override
    public InsurancePlan getInsurancePlan() {
        return this.insurancePlan;
    }

    @Override
    public void setInsurancePlan(InsurancePlan newInsurancePlan) {
        this.insurancePlan = newInsurancePlan;
    }

    @Override
    public ExternalIdentifier getMemberNumber() {
        return this.memberId;
    }

    @Override
    public void setMemberNumber(ExternalIdentifier newMemberNumber) {
        this.memberId = newMemberNumber;
    }

    @Override
    public ExternalIdentifier getSubscriberID() {
        return this.subscriberId;
    }

    @Override
    public void setSubscriberID(ExternalIdentifier newSubscriberID) {
        this.subscriberId = newSubscriberID;
    }

    @Override
    public DateRange getInsurancePolicyDateRange() {
        return this.dateRange;
    }

    @Override
    public void setInsurancePolicyDateRange(DateRange newInsurancePolicyDateRange) {
        this.dateRange = newInsurancePolicyDateRange;
    }

    @Override
    public String getInsurnacePolicyName() {
        return this.name;
    }

    @Override
    public void setInsurnacePolicyName(String newInsurnacePolicyName) {
        this.name = newInsurnacePolicyName;
    }

    @Override
    public Integer getInsuranceSequnceNumberValue() {
        return this.seqNumber;
    }

    @Override
    public void setInsuranceSequnceNumberValue(Integer newInsuranceSequnceNumberValue) {
        this.seqNumber = newInsuranceSequnceNumberValue;
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
        if(this.otherOriginalIds == null){
            this.otherOriginalIds = new ArrayList<>();
        }
        if(anItem != null){
            this.otherOriginalIds.add(anItem);
        }
        return this.otherOriginalIds;
    }

    @Override
    public ExternalIdentifier getOtherOriginalIDssById(XUUID anID) {
        return null;
    }

    @Override
    public void setOtherOriginalIDss(Collection<ExternalIdentifier> newOtherOriginalIDss) {

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
