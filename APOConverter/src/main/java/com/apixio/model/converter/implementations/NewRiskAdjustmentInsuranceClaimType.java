package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.Date;
import com.apixio.model.owl.interfaces.RiskAdjustmentInsuranceClaimType;
import com.apixio.model.owl.interfaces.donotimplement.RiskAdjustmentInsuranceClaimTypeValue;

import java.net.URI;
import java.util.Collection;

/**
 * Created by jctoledo on 10/27/16.
 */
public class NewRiskAdjustmentInsuranceClaimType implements RiskAdjustmentInsuranceClaimType {
    private RiskAdjustmentInsuranceClaimTypeValue riskadjustmentclaimtypeval;
    private Date lastEdit;
    private XUUID xuuid;
    private URI uri;
    private Collection<XUUID> otherIds;
    private String logicalid;

    @Override
    public RiskAdjustmentInsuranceClaimTypeValue getRiskAdjustmentInsuranceClaimTypeValue() {
        return this.riskadjustmentclaimtypeval;
    }

    @Override
    public void setRiskAdjustmentInsuranceClaimTypeValue(RiskAdjustmentInsuranceClaimTypeValue en) {
        this.riskadjustmentclaimtypeval = en;
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
        return this.logicalid;
    }

    @Override
    public void setLogicalID(String anID) {
        this.logicalid = anID;
    }

    @Override
    public Date getLastEditDate() {
        return this.lastEdit;
    }

    @Override
    public void setLastEditDate(Date newLastEditDate) {
        this.lastEdit = newLastEditDate;
    }
}
