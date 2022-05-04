package com.apixio.model.event.adapters;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * EventTypeDateSerializer - Converts a Date object to a string which is valid in the event schema.
 * Created by vvyas on 1/23/14.
 */
public class EventTypeDateSerializer extends JsonSerializer<Date> {
    @Override
    public void serialize(Date date, JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider) throws IOException, JsonProcessingException {

        jsonGenerator.writeString(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(date));
    }
}
