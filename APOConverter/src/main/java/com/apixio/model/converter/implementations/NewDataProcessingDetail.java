package com.apixio.model.converter.implementations;

import com.apixio.XUUID;
import com.apixio.model.owl.interfaces.DataProcessingDetail;
import com.apixio.model.owl.interfaces.Date;
import com.apixio.model.owl.interfaces.Processor;
import com.apixio.model.owl.interfaces.Source;
import com.apixio.model.owl.interfaces.donotimplement.ParserType;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by jctoledo on 3/11/16.
 */
public class NewDataProcessingDetail implements DataProcessingDetail {

    private XUUID xuuid;
    private ParserType parserType;
    private URI uri;
    private Date parsingDate;
    private Collection<XUUID> otherIds;
    private Date editDate;
    private String version;
    private Collection<Source> sources;
    private String logicalId;
    private Processor processor;

    public NewDataProcessingDetail() {
        this.xuuid = XUUID.create("NewDataProcessingDetail");
    }


    @Override
    public Collection<Source> getSources() {
        return this.sources;
    }

    @Override
    public Collection<Source> addSources(Source anItem) {
        if(anItem != null && this.sources != null){
            this.sources.add(anItem);
        }else{
            this.sources = new ArrayList<>();
            this.sources.add(anItem);
        }
        return this.sources;
    }

    @Override
    public Source getSourcesById(XUUID anID) {
        if(this.sources != null && anID != null){
            for(Source s : this.sources){
                if(s.getInternalXUUID().equals(anID)){
                    return s;
                }
            }
        }
        return null;
    }

    @Override
    public void setSources(Collection<Source> newSources) {
        this.sources = newSources;
    }


    @Override
    public Date getDataProcessingDetailDate() {
        return this.parsingDate;
    }

    @Override
    public void setDataProcessingDetailDate(Date newDataProcessingDetailDate) {
        this.parsingDate = newDataProcessingDetailDate;
    }

    @Override
    public Processor getProcessor() {
        return this.processor;
    }

    @Override
    public void setProcessor(Processor newProcessor) {
        this.processor = newProcessor;
    }

    @Override
    public String getVersion() {
        return this.version;
    }

    @Override
    public void setVersion(String newVersion) {
        this.version = newVersion;
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
        return this.editDate;
    }

    @Override
    public void setLastEditDate(Date newLastEditDate) {
        this.editDate = newLastEditDate;
    }
}
