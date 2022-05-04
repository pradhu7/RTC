package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.Date;
import com.apixio.model.owl.interfaces.Name;
import com.apixio.model.owl.interfaces.donotimplement.NameType;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by jctoledo on 3/15/16.
 */
public class NewName implements Name {

    private NameType nameType;
    private Collection<String> famNames;
    private Collection<String> suffixNames;
    private Collection<String> prefixNames;
    private Collection<String> givenNames;
    private XUUID xuuid;
    private Date lasEditDate;
    private Collection<XUUID> otherids;
    private String logicalId;
    private URI uri;

    public NewName() {
        this.xuuid = XUUID.create("NewName");
    }

    @Override
    public NameType getNameType() {
        return this.nameType;
    }

    @Override
    public void setNameType(NameType newNameType) {
        this.nameType = newNameType;
    }

    @Override
    public Collection<String> getFamilyNames() {
        return this.famNames;
    }

    @Override
    public Collection<String> addFamilyNames(String anItem) {
        return null;
    }

    @Override
    public String getFamilyNamesById(XUUID anID) {
        return null;
    }

    @Override
    public void setFamilyNames(Collection<String> newFamilyNames) {
        this.famNames = newFamilyNames;
    }


    @Override
    public Collection<String> getSuffixNames() {
        return this.suffixNames;
    }

    @Override
    public Collection<String> addSuffixNames(String anItem) {
        return null;
    }

    @Override
    public String getSuffixNamesById(XUUID anID) {
        return null;
    }

    @Override
    public void setSuffixNames(Collection<String> newSuffixNames) {
        this.suffixNames = newSuffixNames;
    }

    @Override
    public Collection<String> getPrefixNames() {
        return this.prefixNames;
    }

    @Override
    public Collection<String> addPrefixNames(String anItem) {
        return null;
    }

    @Override
    public String getPrefixNamesById(XUUID anID) {
        return null;
    }

    @Override
    public void setPrefixNames(Collection<String> newPrefixNames) {
        this.prefixNames = newPrefixNames;
    }

    @Override
    public Collection<String> getGivenNames() {
        return this.givenNames;
    }

    @Override
    public Collection<String> addGivenNames(String anItem) {
        if(this.givenNames != null){
            this.givenNames.add(anItem);
            return this.givenNames;
        }else {
            this.givenNames = new ArrayList<>();
            this.givenNames.add(anItem);
            return this.givenNames;
        }
    }

    @Override
    public String getGivenNamesById(XUUID anID) {
        return null;
    }

    @Override
    public void setGivenNames(Collection<String> newGivenNames) {
        this.givenNames = newGivenNames;
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
        return this.otherids;
    }

    @Override
    public void setOtherIds(Collection<XUUID> others) {
        this.otherids = others;
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
        return this.lasEditDate;
    }

    @Override
    public void setLastEditDate(Date newLastEditDate) {
        this.lasEditDate = newLastEditDate;
    }

}
