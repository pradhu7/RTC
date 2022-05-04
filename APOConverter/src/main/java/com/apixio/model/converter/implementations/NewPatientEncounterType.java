package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.AmendmentAnnotation;
import com.apixio.model.owl.interfaces.DataProcessingDetail;
import com.apixio.model.owl.interfaces.Date;
import com.apixio.model.owl.interfaces.ExternalIdentifier;
import com.apixio.model.owl.interfaces.donotimplement.PatientEncounterType;
import com.apixio.model.owl.interfaces.donotimplement.PatientEncounterTypeValue;

import java.net.URI;
import java.util.Collection;

/**
 * Created by jctoledo on 3/25/16.
 */
public class NewPatientEncounterType implements PatientEncounterType {
    private XUUID xuuid;
    private PatientEncounterTypeValue patientEncTypeVal;
    private Collection<XUUID> otherIds;
    private Date lastEditDate;
    private String logicalId;

    public NewPatientEncounterType (){
        this.xuuid = XUUID.create("NewPatientEncounterType");
    }
    @Override
    public PatientEncounterTypeValue getPatientEncounterTypeValue() {
        return this.patientEncTypeVal;
    }

    @Override
    public void setPatientEncounterTypeValue(PatientEncounterTypeValue en) {
        this.patientEncTypeVal = en;
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
