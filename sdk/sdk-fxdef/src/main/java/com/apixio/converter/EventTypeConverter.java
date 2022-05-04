package com.apixio.converter;

import com.apixio.sdk.Converter;
import com.apixio.sdk.FxEnvironment;

import com.apixio.model.event.AttributeType;
import com.apixio.model.event.EventType; // TODO replace with implementation of ifc.X
import com.apixio.predictions.predictionbin.PredictionContainer;         // protobuf

import com.google.protobuf.Message;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.List;

import com.apixio.converter.util.PredictionBinUtils;


public class EventTypeConverter implements Converter
{
    @Override
    public Meta getMetadata()
    {
        return new Meta(EventType.class, PredictionContainer.PredictionBin.class, false);
    }

    @Override
    public void setEnvironment(FxEnvironment env)
    {
    }

    @Override
    public String getID()
    {
        return "eventTypeConverter";
    }

    /**
     * Convert from interface to protobuf.  No exceptions should be thrown.
     */
    @Override
    public Message convertToProtobuf(Object o)
    {
        EventType event = (EventType) o;

        try
        {
            List<AttributeType> attr = event.getAttributes().getAttribute();
            Map<String,String> amap = attr.stream().collect(Collectors.toMap(AttributeType::getName, at -> at.getValue()));
            final Map<String, AttributeType> attributeTypeMap = getAttributeMap(event);
            if (amap.size() == 0 || attributeTypeMap.size() == 0) {
                return null;
            }
            String modelVersion = amap.get("$modelVersion");
            return PredictionBinUtils.convertEventTypeToPredictionBin(event, amap.get("bucketType"), "", 
                                                                      amap.get("bucketName"), amap.get("bucketName"),
                                                                      modelVersion);
        }
        catch (Exception x)
        {
            x.printStackTrace();
            return null;
            // throw new RuntimeException("Failed to convert model.Event to protobuf", x);
        }
    }

    /**
     * Convert from protobuf to interface.  No exceptions should be thrown.
     */
    @Override
    public Object convertToInterface(Message pbin)
    {
        return (PredictionContainer.PredictionBin) null;
    }

    static private Map<String, AttributeType> getAttributeMap(final EventType event)
    {
        Map<String, AttributeType> attributeTypeMap = new HashMap<>();
        for(AttributeType at : event.getEvidence().getAttributes().getAttribute())
        {
            attributeTypeMap.put(at.getName(), at);
        }
        return attributeTypeMap;
    }
}
