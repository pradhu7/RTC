package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.*;
import com.apixio.model.owl.interfaces.donotimplement.ClinicalRoleType;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by jctoledo on 4/13/16.
 */
public class NewPrimaryCareProvider implements PrimaryCareProvider {
    private DateRange dateRangeForCare;
    private Organization org;
    private ClinicalRoleType clinicalRole;
    private ExternalIdentifier extId;
    private String title;
    private Demographics demographics;
    private ContactDetails contDets;
    private Collection<ContactDetails> alternateContactDets;
    private XUUID xuuid;
    private URI uri;
    private Collection<DataProcessingDetail> dataProcDetails;
    private Collection<XUUID> otherIds;
    private String logicalId;
    private Collection<ExternalIdentifier> otherOriginalIds;
    private Date lastEditDate;


    @Override
    public void setOrganization(Organization o) {
        this.org = o;
    }

    @Override
    public Organization getOrganization() {
        return  this.org;
    }

    @Override
    public ClinicalRoleType getClinicalRole() {
        return this.clinicalRole;
    }

    @Override
    public void setClinicalRole(ClinicalRoleType newClinicalRole) {
        this.clinicalRole = newClinicalRole;
    }

    @Override
    public ExternalIdentifier getNationalProviderIdentifier() {
        return this.extId;
    }

    @Override
    public void setNationalProviderIdentifier(ExternalIdentifier newNationalProviderIdentifier) {
        this.extId = newNationalProviderIdentifier;
    }

    @Override
    public String getTitle() {
        return this.title;
    }

    @Override
    public void setTitle(String newTitle) {
        this.title = newTitle;
    }


    @Override
    public Demographics getDemographics() {
        return this.demographics;
    }

    @Override
    public void setDemographics(Demographics newDemographics) {
        this.demographics = newDemographics;
    }


    @Override
    public ContactDetails getContactDetails() {
        return this.contDets;
    }

    @Override
    public void setContactDetails(ContactDetails newContactDetails) {
        this.contDets = newContactDetails;
    }

    @Override
    public Collection<ContactDetails> getAlternateContactDetailss() {
        return this.alternateContactDets;
    }

    @Override
    public Collection<ContactDetails> addAlternateContactDetailss(ContactDetails anItem) {
        if(this.alternateContactDets != null && anItem != null){
            this.alternateContactDets.add(anItem);
        }else{
            this.alternateContactDets = new ArrayList<>();
            this.alternateContactDets.add(anItem);
        }
        return this.alternateContactDets;
    }

    @Override
    public ContactDetails getAlternateContactDetailssById(XUUID anID) {
        return null;
    }

    @Override
    public void setAlternateContactDetailss(Collection<ContactDetails> newAlternateContactDetailss) {
        this.alternateContactDets = newAlternateContactDetailss;
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
    public ExternalIdentifier getOriginalID() {
        return null;
    }

    @Override
    public void setOriginalID(ExternalIdentifier newEntityOriginalID) {

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
        return this.dataProcDetails;
    }

    @Override
    public Collection<DataProcessingDetail> addDataProcessingDetails(DataProcessingDetail anItem) {
        if(this.dataProcDetails != null){
            this.dataProcDetails.add(anItem);
        }else{
            this.dataProcDetails = new ArrayList<>();
            this.dataProcDetails.add(anItem);
        }
        return this.dataProcDetails;
    }

    @Override
    public DataProcessingDetail getDataProcessingDetailsById(XUUID anID) {
        if(anID != null && this.dataProcDetails != null){
            for(DataProcessingDetail dpd: this.dataProcDetails){
                if(dpd.getInternalXUUID().equals(anID))
                    return dpd;
            }
        }
        return null;
    }

    @Override
    public void setDataProcessingDetails(Collection<DataProcessingDetail> newDataProcessingDetails) {
        this.dataProcDetails = newDataProcessingDetails;
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
    public DateRange getDateRangeForCareOfPatient() {
        return this.dateRangeForCare;
    }

    @Override
    public void setDateRangeForCareOfPatient(DateRange newDateRangeForCareOfPatient) {
        this.dateRangeForCare = newDateRangeForCareOfPatient;
    }
}
