package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.*;

import java.net.URI;
import java.util.Collection;

/**
 * Created by jctoledo on 3/14/16.
 */
public class NewContactDetails implements ContactDetails {
    private XUUID xuuid;
    private Address primAddress;
    private Collection<Address> alternateAddresses;
    private TelephoneNumber telephoneNumb;
    private Collection<TelephoneNumber> alternateNumbs;
    private String email;
    private Collection<String> otherEmails;
    private Collection<ExternalIdentifier> otherOriginalIds;
    private String logicalIds;
    private ExternalIdentifier originalId;
    private Collection<XUUID> otherIds;
    private Collection<DataProcessingDetail> dataProcDets;
    private Date lastEditDate;
    private URI uri;


    public NewContactDetails() {
        this.xuuid = XUUID.create("NewContactDetails");
    }

    @Override
    public Address getPrimaryAddress() {
        return this.primAddress;
    }

    @Override
    public void setPrimaryAddress(Address newPrimaryAddress) {
        this.primAddress = newPrimaryAddress;
    }

    @Override
    public Collection<Address> getAlternateAddresses() {
        return this.alternateAddresses;
    }

    @Override
    public Collection<Address> addAlternateAddresses(Address anItem) {
        return null;
    }

    @Override
    public Address getAlternateAddressesById(XUUID anID) {
        if(this.alternateAddresses != null){
            for(Address a:  this.alternateAddresses){
                if(a.getInternalXUUID().equals(anID)){
                    return a;
                }
            }
        }
        return null;
    }

    @Override
    public void setAlternateAddresses(Collection<Address> newAlternateAddresses) {
        this.alternateAddresses = newAlternateAddresses;
    }

    @Override
    public TelephoneNumber getTelephoneNumber() {
        return this.telephoneNumb;
    }

    @Override
    public void setTelephoneNumber(TelephoneNumber newTelephoneNumber) {
        this.telephoneNumb = newTelephoneNumber;
    }

    @Override
    public Collection<TelephoneNumber> getAlternateTelephoneNumbers() {
        return this.alternateNumbs;
    }

    @Override
    public Collection<TelephoneNumber> addAlternateTelephoneNumbers(TelephoneNumber anItem) {
        return null;
    }

    @Override
    public TelephoneNumber getAlternateTelephoneNumbersById(XUUID anID) {
        if(this.alternateNumbs != null){
            for(TelephoneNumber t: this.alternateNumbs){
                if(t.getInternalXUUID().equals(anID)){
                    return t;
                }
            }
        }
        return null;
    }

    @Override
    public void setAlternateTelephoneNumbers(Collection<TelephoneNumber> newAlternateTelephoneNumbers) {
        this.alternateNumbs = newAlternateTelephoneNumbers;
    }

    @Override
    public String getPrimaryEmailAddress() {
        return this.email;
    }

    @Override
    public void setPrimaryEmailAddress(String newPrimaryEmailAddress) {
        this.email = newPrimaryEmailAddress;
    }

    @Override
    public Collection<String> getAlternateEmailAddresses() {
        return this.otherEmails;
    }

    @Override
    public Collection<String> addAlternateEmailAddresses(String anItem) {
        return null;
    }

    @Override
    public String getAlternateEmailAddressesById(XUUID anID) {
        return null;
    }

    @Override
    public void setAlternateEmailAddresses(Collection<String> newAlternateEmailAddresses) {
        this.otherEmails = newAlternateEmailAddresses;
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
        return this.logicalIds;
    }

    @Override
    public void setLogicalID(String anID) {
        this.logicalIds = anID;
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
        return null;
    }

    @Override
    public ExternalIdentifier getOtherOriginalIDssById(XUUID anID) {
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
