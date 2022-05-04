package com.apixio.model.converter.bin.converters;

import com.apixio.model.converter.lib.RDFModel;
import com.apixio.model.event.EventType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jctoledo on 5/2/16.
 */
public class EventTypeToAPOModel {
    private List<EventType> eventTypeList = null;
    private RDFModel model = null;

    public EventTypeToAPOModel(){
        this.eventTypeList = new ArrayList<>();
        this.model = new RDFModel();
    }

    public EventTypeToAPOModel(List<EventType> listOfEvents){
        this();
        this.eventTypeList = listOfEvents;
        convertEvents();
    }

    public EventTypeToAPOModel(EventType et){
        this();
        this.eventTypeList.add(et);
        convertEvents();
    }


    private void convertEvents(){

    }
}
