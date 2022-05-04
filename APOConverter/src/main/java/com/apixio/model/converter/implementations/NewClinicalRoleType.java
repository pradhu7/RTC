package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.Date;
import com.apixio.model.owl.interfaces.donotimplement.ClinicalRoleType;
import com.apixio.model.owl.interfaces.donotimplement.ClinicalRoleTypeValue;

import java.net.URI;
import java.util.Collection;

/**
 * Created by jctoledo on 4/5/16.
 */
public class NewClinicalRoleType implements ClinicalRoleType {

    private XUUID xuuid;
    private ClinicalRoleTypeValue roleValue;
    private Collection<XUUID> otherIds;
    private String logicalId;
    private Date lastEditDate;

    public NewClinicalRoleType() {
        this.xuuid = XUUID.create("NewClinicalRoleType");
    }

    @Override
    public ClinicalRoleTypeValue getClinicalRoleTypeValue() {
        return this.roleValue;
    }

    @Override
    public void setClinicalRoleTypeValue(ClinicalRoleTypeValue en) {
        this.roleValue = en;
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
