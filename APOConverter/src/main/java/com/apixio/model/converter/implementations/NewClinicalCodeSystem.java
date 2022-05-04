package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.converter.utils.ConverterUtils;
import com.apixio.model.owl.interfaces.*;
import org.joda.time.DateTime;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by jctoledo on 3/17/16.
 */
public class NewClinicalCodeSystem implements ClinicalCodingSystem {

    private XUUID xuuid;
    private String ccsName;
    private String oid;
    private String version;
    private ExternalIdentifier originalId;
    private Collection<ExternalIdentifier> otherOriginalIds;
    private Collection<XUUID> otherIds;
    private String logicalId;
    private Collection<DataProcessingDetail> dataProcDets;
    private Date lastEditDate;
    private URI uri;

    public NewClinicalCodeSystem(){
        this.xuuid = XUUID.create("ClinicalCodingSystem");
        this.setLastEditDate(ConverterUtils.createApixioDate(new DateTime()));
    }

    @Override
    public String getClinicalCodingSystemName() {
        return this.ccsName;
    }

    @Override
    public void setClinicalCodingSystemName(String newClinicalCodingSystemName) {
        this.ccsName = newClinicalCodingSystemName;
    }

    @Override
    public String getClinicalCodingSystemOID() {
        return this.oid;
    }

    @Override
    public void setClinicalCodingSystemOID(String newClinicalCodingSystemOID) {
        this.oid = newClinicalCodingSystemOID;
    }

    @Override
    public String getClinicalCodingSystemVersion() {
        return this.version;
    }

    @Override
    public void setClinicalCodingSystemVersion(String newClinicalCodingSystemVersion) {
        this.version = newClinicalCodingSystemVersion;
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
        }else {
            this.otherOriginalIds = new ArrayList<>();
            this.otherOriginalIds.add(anItem);
        }
        return this.otherOriginalIds;
    }

    @Override
    public ExternalIdentifier getOtherOriginalIDssById(XUUID anID) {
        if(this.otherOriginalIds != null){
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
