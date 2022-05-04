package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.*;
import com.apixio.model.owl.interfaces.donotimplement.GenderType;
import com.apixio.model.owl.interfaces.donotimplement.MaritalStatusType;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by jctoledo on 3/15/16.
 */
public class NewDemographics implements Demographics {
    private GenderType genderType;
    private Name name;
    private Collection<Name> alternateNames;
    private MaritalStatusType maritalstatType;
    private Date dob;
    private Date dod;
    private ClinicalCode race;
    private ClinicalCode ethnicity;
    private Collection<String> languages;
    private String religiousAffiliation;
    private XUUID xuuid;
    private Collection<XUUID> otherIds;
    private ExternalIdentifier externalId;
    private Date lastEditDate;
    private Collection<DataProcessingDetail> dataProcessingDets;
    private Collection<ExternalIdentifier> otherOriginalIds;
    private String logicalId;
    private URI uri;

    public NewDemographics(){
        this.xuuid = XUUID.create("NewDemographics");
    }

    @Override
    public GenderType getGenderType() {
        return this.genderType;
    }

    @Override
    public void setGenderType(GenderType newGenderType) {
        this.genderType = newGenderType;
    }

    @Override
    public NewName getName() {
        return (NewName) this.name;
    }

    @Override
    public void setName(Name newName) {
        this.name = newName;
    }

    @Override
    public Collection<Name> getAlternateNames() {
        return this.alternateNames;
    }

    @Override
    public Collection<Name> addAlternateNames(Name anItem) {
        if(this.alternateNames != null){
            this.alternateNames.add(anItem);
        }else{
            this.alternateNames = new ArrayList<>();
            this.alternateNames.add(anItem);
        }
        return this.alternateNames;
    }

    @Override
    public Name getAlternateNamesById(XUUID anID) {
        if(this.alternateNames != null){
            for(Name n: this.alternateNames){
                if(n.getInternalXUUID().equals(anID)){
                    return n;
                }
            }
        }
        return null;
    }

    @Override
    public void setAlternateNames(Collection<Name> newAlternateNames) {
        this.alternateNames = newAlternateNames;
    }

    @Override
    public MaritalStatusType getMaritalStatusType() {
        return this.maritalstatType;
    }

    @Override
    public void setMaritalStatusType(MaritalStatusType newMaritalStatusType) {
        this.maritalstatType = newMaritalStatusType;
    }

    @Override
    public Date getDateOfBirth() {
        return this.dob;
    }

    @Override
    public void setDateOfBirth(Date newDateOfBirth) {
        this.dob = newDateOfBirth;
    }

    @Override
    public Date getDateOfDeath() {
        return this.dod;
    }

    @Override
    public void setDateOfDeath(Date newDateOfDeath) {
        this.dod = newDateOfDeath;
    }

    @Override
    public ClinicalCode getRace() {
        return this.race;
    }

    @Override
    public void setRace(ClinicalCode newRace) {
        this.race = newRace;
    }

    @Override
    public ClinicalCode getEthnicity() {
        return this.ethnicity;
    }

    @Override
    public void setEthnicity(ClinicalCode newEthnicity) {
        this.ethnicity = newEthnicity;
    }

    @Override
    public String getReligiousAffiliation() {
        return this.religiousAffiliation;
    }

    @Override
    public void setReligiousAffiliation(String newReligiousAffiliation) {
        this.religiousAffiliation = newReligiousAffiliation;
    }

    @Override
    public Collection<String> getLanguages() {
        return this.languages;
    }

    @Override
    public Collection<String> addLanguages(String anItem) {
        if(this.languages != null){
            this.languages.add(anItem);

        } else {
            this.languages = new ArrayList<>();
            this.languages.add(anItem);
        }
        return this.languages;
    }

    @Override
    public String getLanguagesById(XUUID anID) {
        return null;
    }

    @Override
    public void setLanguages(Collection<String> newLanguages) {
        this.languages =newLanguages;
    }

    @Override
    public XUUID getInternalXUUID() {
        return  this.xuuid;
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
        return this.externalId;
    }

    @Override
    public void setOriginalID(ExternalIdentifier newEntityOriginalID) {
        this.externalId = newEntityOriginalID;
    }

    @Override
    public Collection<ExternalIdentifier> getOtherOriginalIDss() {
        return this.otherOriginalIds;
    }

    @Override
    public Collection<ExternalIdentifier> addOtherOriginalIDss(ExternalIdentifier anItem) {
        if(this.otherOriginalIds != null){
            this.otherOriginalIds.add(anItem);
        }else{
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
        return this.dataProcessingDets;
    }

    @Override
    public Collection<DataProcessingDetail> addDataProcessingDetails(DataProcessingDetail anItem) {
        if(this.dataProcessingDets != null){
            this.dataProcessingDets.add(anItem);
        }else{
            this.dataProcessingDets = new ArrayList<>();
            this.dataProcessingDets.add(anItem);
        }
        return this.dataProcessingDets;

    }

    @Override
    public DataProcessingDetail getDataProcessingDetailsById(XUUID anID) {
        return null;
    }

    @Override
    public void setDataProcessingDetails(Collection<DataProcessingDetail> newDataProcessingDetails) {
        this.dataProcessingDets = newDataProcessingDetails;

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
}
