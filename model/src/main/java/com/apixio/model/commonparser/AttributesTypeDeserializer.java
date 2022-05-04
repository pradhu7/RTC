package com.apixio.model.commonparser;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import com.apixio.model.event.AttributesType;
import com.apixio.model.event.transformer.EventTypeAttributesBuilder;
/**
 * Created by vvyas on 1/23/14.
 */
public class AttributesTypeDeserializer extends JsonDeserializer<AttributesType> {
    @Override
    public AttributesType deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        if(jsonParser == null)
            return null;


        EventTypeAttributesBuilder builder = new EventTypeAttributesBuilder();
        if(jsonParser.getCurrentToken() == JsonToken.START_OBJECT) {
            JsonToken token;
            while((token = jsonParser.nextToken()) != JsonToken.END_OBJECT) {
                if(token == JsonToken.VALUE_STRING) {
                    builder.add(jsonParser.getCurrentName(), jsonParser.getValueAsString());
                }
            }
        }

        return builder.build();
    }
}
