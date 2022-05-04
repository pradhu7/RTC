package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.converter.utils.ConverterUtils;
import com.apixio.model.owl.interfaces.Date;
import com.apixio.model.owl.interfaces.PatientEncounterSwitchType;
import com.apixio.model.owl.interfaces.donotimplement.PatientEncounterSwitchTypeValue;
import org.joda.time.DateTime;

import java.net.URI;
import java.util.Collection;

/**
 * Created by jctoledo on 10/27/16.
 */
public class NewPatientEncounterSwitchType implements PatientEncounterSwitchType {
    private XUUID xuuid;
    private PatientEncounterSwitchTypeValue patientEncounterSwitchTypeVal;
    private Collection<XUUID> otherIds;
    private Date lastEditDate;
    private URI uri;
    private String logicalId;


    public NewPatientEncounterSwitchType(){
        this.xuuid = XUUID.create("NewPatientEncounterSwitchType");
        this.setLastEditDate(ConverterUtils.createApixioDate(new DateTime()));
    }


    @Override
    public PatientEncounterSwitchTypeValue getPatientEncounterSwitchTypeValue() {
        return this.patientEncounterSwitchTypeVal;
    }

    @Override
    public void setPatientEncounterSwitchTypeValue(PatientEncounterSwitchTypeValue en) {
        this.patientEncounterSwitchTypeVal = en;
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
