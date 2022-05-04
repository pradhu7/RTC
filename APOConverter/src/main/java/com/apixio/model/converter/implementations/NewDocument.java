package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.*;
import com.apixio.model.owl.interfaces.donotimplement.ApixioDate;
import com.apixio.model.owl.interfaces.donotimplement.Human;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by jctoledo on 3/11/16.
 */
public class NewDocument implements Document  {

    private Collection<DataProcessingDetail> dataProcDetails;
    private Collection<MedicalProfessional> authors;
    private CareSite careSites;

    private ApixioDate apixioDate;
    private String title;
    private XUUID xuuid;
    private Date lastEditDate;
    private Collection<Page> pages;
    private Collection<XUUID> otherIds;
    private ExternalIdentifier extId;
    private Collection<ExternalIdentifier> otherOriginalIds;
    private String logicalId;


    @Override
    public Collection<MedicalProfessional> getDocumentAuthors() {
        return this.authors;
    }

    @Override
    public Collection<MedicalProfessional> addDocumentAuthors(MedicalProfessional anItem) {
        return null;
    }

    @Override
    public MedicalProfessional getDocumentAuthorsById(XUUID anID) {
        return null;
    }

    @Override
    public void setDocumentAuthors(Collection<MedicalProfessional> newDocumentAuthors) {
        this.authors = newDocumentAuthors;
    }

    @Override
    public CareSite getDocumentCareSite() {
        return this.careSites;
    }

    @Override
    public void setDocumentCareSite(CareSite newDocumentCareSite) {
        this.careSites = newDocumentCareSite;
    }



    @Override
    public Collection<Page> getDocumentPages() {
        return this.pages;
    }

    @Override
    public Collection<Page> addDocumentPages(Page anItem) {
        return null;
    }

    @Override
    public Page getDocumentPagesById(XUUID anID) {
        return null;
    }

    @Override
    public void setDocumentPages(Collection<Page> newDocumentPages) {
        this.pages = newDocumentPages;
    }

    @Override
    public ApixioDate getDocumentDate() {
        return this.apixioDate;
    }

    @Override
    public void setDocumentDate(ApixioDate newDocumentDate) {
        this.apixioDate = newDocumentDate;
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
    public ExternalIdentifier getOriginalID() {
        return this.extId;
    }

    @Override
    public void setOriginalID(ExternalIdentifier newEntityOriginalID) {
        this.extId = newEntityOriginalID;
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
}
