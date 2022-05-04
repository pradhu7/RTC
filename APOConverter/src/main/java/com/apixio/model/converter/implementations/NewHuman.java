package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.*;
import com.apixio.model.owl.interfaces.donotimplement.Human;

import java.net.URI;
import java.util.Collection;

/**
 * Created by jctoledo on 3/24/16.
 */
public class NewHuman implements Human {
    //TODO - fill me

    @Override
    public Demographics getDemographics() {
        return null;
    }

    @Override
    public void setDemographics(Demographics newDemographics) {

    }


    @Override
    public ContactDetails getContactDetails() {
        return null;
    }

    @Override
    public void setContactDetails(ContactDetails newContactDetails) {

    }

    @Override
    public Collection<ContactDetails> getAlternateContactDetailss() {
        return null;
    }

    @Override
    public Collection<ContactDetails> addAlternateContactDetailss(ContactDetails anItem) {
        return null;
    }

    @Override
    public ContactDetails getAlternateContactDetailssById(XUUID anID) {
        return null;
    }

    @Override
    public void setAlternateContactDetailss(Collection<ContactDetails> newAlternateContactDetailss) {

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
        return null;
    }

    @Override
    public Collection<ExternalIdentifier> addOtherOriginalIDss(ExternalIdentifier anItem) {
        return null;
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
        return null;
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
        return null;
    }

    @Override
    public void setInternalXUUID(XUUID anXUUID) {

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
        return null;
    }

    @Override
    public void setOtherIds(Collection<XUUID> others) {

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
        return null;
    }

    @Override
    public void setLastEditDate(Date newLastEditDate) {

    }
}
