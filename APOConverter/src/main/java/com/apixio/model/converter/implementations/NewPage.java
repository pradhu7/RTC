package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.*;
import org.joda.time.DateTime;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by jctoledo on 3/16/16.
 */
public class NewPage implements Page{

    private Collection<TextRendering> textRenderings;
    private Collection<ImageRendering> imageRenderings;
    private Date lastEditDate;
    private Collection<String> pageClassifications;
    private Integer pageNum;
    private String logicalId;
    private Collection<XUUID> otherIds;
    private XUUID xuuid;
    private URI uri;
    private ExternalIdentifier externalId;
    private Collection<ExternalIdentifier> otherOriginalIds;
    private Collection<DataProcessingDetail> dataProcDetails;


    @Override
    public ExternalIdentifier getOriginalID() {
        return this.externalId;
    }

    @Override
    public void setOriginalID(ExternalIdentifier newOriginalID) {
        this.externalId = newOriginalID;
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

    public NewPage(){
        this.xuuid = XUUID.create("NewPage");
    }

    @Override
    public Collection<TextRendering> getTextRenderings() {
        return this.textRenderings;
    }

    @Override
    public Collection<TextRendering> addTextRenderings(TextRendering anItem) {
        return null;
    }

    @Override
    public TextRendering getTextRenderingsById(XUUID anID) {
        return null;
    }

    @Override
    public void setTextRenderings(Collection<com.apixio.model.owl.interfaces.TextRendering> newTextRenderings) {
        this.textRenderings = newTextRenderings;
    }

    @Override
    public Collection<ImageRendering> getImageRenderings() {
        return this.imageRenderings;
    }

    @Override
    public Collection<ImageRendering> addImageRenderings(ImageRendering anItem) {
        return null;
    }

    @Override
    public ImageRendering getImageRenderingsById(XUUID anID) {
        return null;
    }

    @Override
    public void setImageRenderings(Collection<ImageRendering> newImageRenderings) {
        this.imageRenderings = newImageRenderings;
    }

    @Override
    public Integer getPageNumber() {
        return this.pageNum;
    }

    @Override
    public void setPageNumber(Integer newPageNumber) {
        this.pageNum = newPageNumber;
    }

    @Override
    public Collection<String> getPageClassifications() {
        return this.pageClassifications;
    }

    @Override
    public Collection<String> addPageClassifications(String anItem) {
        return null;
    }

    @Override
    public String getPageClassificationsById(XUUID anID) {
        return null;
    }

    @Override
    public void setPageClassifications(Collection<String> newPageClassifications) {
        this.pageClassifications = newPageClassifications;
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
