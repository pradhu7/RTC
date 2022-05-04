package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.Date;
import com.apixio.model.owl.interfaces.donotimplement.GenderType;
import com.apixio.model.owl.interfaces.donotimplement.GenderValues;

import java.net.URI;
import java.util.Collection;

/**
 * Created by jctoledo on 4/27/16.
 */
public class NewGenderType implements GenderType {
    private XUUID xuuid;
    private URI uri;
    private Date lastEditDate;
    private Collection<XUUID> otherIds;
    private GenderValues genderValues;


    public NewGenderType (){
        xuuid = XUUID.create("GenderType");
    }

    @Override
    public GenderValues getGenderValues() {
        return this.genderValues;
    }

    @Override
    public void setGenderValues(GenderValues genderValues) {
        this.genderValues = genderValues;
    }

    @Override
    public XUUID getInternalXUUID() {
        return this.xuuid;
    }

    @Override
    public void setInternalXUUID(XUUID xuuid) {
        this.xuuid= xuuid;
    }

    @Override
    public URI getURI() {
        return this.uri;
    }

    @Override
    public void setURI(URI uri) {
        this.uri = uri;
    }

    @Override
    public Collection<XUUID> getOtherIds() {
        return this.otherIds;
    }

    @Override
    public void setOtherIds(Collection<XUUID> collection) {
        this.otherIds = collection;
    }

    @Override
    public String getLogicalID() {
        return null;
    }

    @Override
    public void setLogicalID(String s) {

    }

    @Override
    public Date getLastEditDate() {
        return this.lastEditDate;
    }

    @Override
    public void setLastEditDate(Date date) {
        this.lastEditDate = date;
    }
}
