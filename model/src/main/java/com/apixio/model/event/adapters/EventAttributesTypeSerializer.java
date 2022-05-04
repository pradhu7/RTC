package com.apixio.model.event.adapters;

import com.apixio.model.event.AttributesType;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Created by vvyas on 1/23/14.
 */
public class EventAttributesTypeSerializer extends JsonSerializer<AttributesType> {
    @Override
    public void serialize(AttributesType attributesType, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
        jsonGenerator.writeStartObject();
        for(com.apixio.model.event.AttributeType attr : attributesType.getAttribute()) {
            if (attr.getName()!= null && attr.getValue() != null)
                jsonGenerator.writeObjectField(attr.getName(),attr.getValue());
        }
        jsonGenerator.writeEndObject();
    }
}
