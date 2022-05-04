package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.Date;
import com.apixio.model.owl.interfaces.donotimplement.Human;
import com.apixio.model.owl.interfaces.donotimplement.PayorTypeValue;
import com.apixio.model.owl.interfaces.donotimplement.PlanType;

import java.net.URI;
import java.util.Collection;

/**
 * Created by jctoledo on 3/29/16.
 */
public class NewInsurancePlanType implements PlanType {

    private XUUID xuuid;
    private PayorTypeValue payorTypeVal;
    private Collection<Human> humans;
    private Collection<XUUID> otherIds;
    private Date lastEditDate;
    private String logicalId;

    public NewInsurancePlanType(){
        this.xuuid = XUUID.create("NewInsurancePlanType");
    }

    @Override
    public PayorTypeValue getPayorTypeValue() {
        return this.payorTypeVal;
    }

    @Override
    public void setPayorTypeValue(PayorTypeValue en) {
        this.payorTypeVal = en;
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
