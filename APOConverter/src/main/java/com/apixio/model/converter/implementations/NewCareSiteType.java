package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.donotimplement.CareSiteType;
import com.apixio.model.owl.interfaces.Date;
import com.apixio.model.owl.interfaces.donotimplement.CareSiteTypeValue;
import com.apixio.model.owl.interfaces.donotimplement.Human;

import java.net.URI;
import java.util.Collection;

/**
 * Created by jctoledo on 3/28/16.
 */
public class NewCareSiteType implements CareSiteType {

    private XUUID xuuid;
    private CareSiteTypeValue cstVal;
    private Collection<Human> persons;
    private Collection<XUUID> otherIds;
    private Date lastEditDate;

    public NewCareSiteType (){
        this.xuuid = XUUID.create("newcaresitetype");
    }

    @Override
    public CareSiteTypeValue getCareSiteTypeValue() {
        return this.cstVal;
    }

    @Override
    public void setCareSiteTypeValue(CareSiteTypeValue en) {
        this.cstVal = en;
    }


    @Override
    public XUUID getInternalXUUID() {
        return xuuid;
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
        return null;
    }

    @Override
    public void setLogicalID(String anID) {

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
