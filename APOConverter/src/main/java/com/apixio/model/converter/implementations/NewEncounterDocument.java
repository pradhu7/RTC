package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.*;
import com.apixio.model.owl.interfaces.donotimplement.ApixioDate;
import com.apixio.model.owl.interfaces.donotimplement.Human;

import java.net.URI;
import java.util.Collection;

/**
 * Created by jctoledo on 3/31/16.
 */
public class NewEncounterDocument implements EncounterDocument {

    private XUUID xuuid;
    private Collection<PatientEncounter> encounters;
    private Date signatureDate;
    private Boolean signedStatus;
    private Collection<MedicalProfessional> authors;
    private CareSite careSite;
    private File file;
    private Collection<Page> pages;
    private ApixioDate date;
    private String title;
    private ExternalIdentifier originalId;
    private Collection<ExternalIdentifier> otherOriginalIds;
    private Collection<DataProcessingDetail> dataProcDets;
    private Date lastEditDate;
    private Collection<XUUID> otherIds;
    private String logicalId;

    public NewEncounterDocument(){this.xuuid = XUUID.create("NewEncounterDocument");}


    @Override
    public Date getSignatureDate() {
        return this.signatureDate;
    }

    @Override
    public void setSignatureDate(Date newSignatureDate) {
        this.signatureDate = newSignatureDate;
    }

    @Override
    public Boolean getSignedStatus() {
        return this.signedStatus;
    }

    @Override
    public void setSignedStatus(Boolean newSignedStatus) {
        this.signedStatus= newSignedStatus;
    }

    @Override
    public URI getPatientEncounterURI() {
        return null;
    }

    @Override
    public void setPatientEncounterURI(URI newPatientEncounterURI) {

    }

    @Override
    public Collection<MedicalProfessional> getDocumentAuthors() {
        return this.authors;
    }

    @Override
    public Collection<MedicalProfessional> addDocumentAuthors(MedicalProfessional anItem) {
        if(this.authors!= null){
            this.authors.add(anItem);
            return this.authors;
        }
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
        return this.careSite;
    }

    @Override
    public void setDocumentCareSite(CareSite newDocumentCareSite) {
        this.careSite = newDocumentCareSite;
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
        return this.date;
    }

    @Override
    public void setDocumentDate(ApixioDate newDocumentDate) {
        this.date = newDocumentDate;
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
