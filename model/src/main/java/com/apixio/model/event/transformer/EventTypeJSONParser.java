package com.apixio.model.event.transformer;

import java.io.IOException;
import java.util.Date;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.SerializationFeature;

import com.apixio.model.event.AttributesType;
import com.apixio.model.event.EventType;
import com.apixio.model.event.adapters.EventAttributesTypeDeserializer;
import com.apixio.model.event.adapters.EventAttributesTypeSerializer;
import com.apixio.model.event.adapters.EventTypeDateDeserializer;
import com.apixio.model.event.adapters.EventTypeDateSerializer;
import com.apixio.model.utility.ApixioDateDeserialzer;
import com.apixio.model.utility.ApixioDateSerializer;

public class EventTypeJSONParser
{
    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Factory methods so client can be explicit as to which one they want to deal with
     */
    public static EventTypeJSONParser getStandardParser()
    {
        return new EventTypeJSONParser(true);
    }

    public static EventTypeJSONParser getDeprecatedParser()
    {
        return new EventTypeJSONParser(false);
    }

    public EventTypeJSONParser()
    {
        this(true);
    }

    /**
     * Common initialization, with the only difference between "old" and "new" being the deserialization
     * of the dates (epoch time in old, ISO8601 in new) and attributes (array of name=value in old, object
     * fields in new).
     */
    private EventTypeJSONParser(boolean standard)
    {
        // initializer to setup our object mapper with the appropriate
        // serializers
        // and deserializers for dates
        SimpleModule module1 = new SimpleModule("DateTimeDeserializerModule");
        module1.addDeserializer(DateTime.class, new ApixioDateDeserialzer());
        module1.addSerializer(DateTime.class, new ApixioDateSerializer());
        objectMapper.registerModule(module1);

        SimpleModule dateModule = new SimpleModule("DateSerDes");
        if (standard)
            dateModule.addDeserializer(Date.class, new EventTypeDateDeserializer());
        dateModule.addSerializer(Date.class, new EventTypeDateSerializer());
        objectMapper.registerModule(dateModule);

        SimpleModule attrsModule = new SimpleModule("AttributesTypeSerDes");
        if (standard)
            attrsModule.addDeserializer(AttributesType.class, new EventAttributesTypeDeserializer());
        attrsModule.addSerializer(AttributesType.class, new EventAttributesTypeSerializer());
        objectMapper.registerModule(attrsModule);

        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, true);
        objectMapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, true);
    }


    public ObjectMapper getObjectMapper()
    {
        return objectMapper;
    }

    public String toJSON(EventType p) throws JsonGenerationException, JsonMappingException, IOException
    {
        return getObjectMapper().writeValueAsString(p);
    }

    /**
     * Parse the EventType data from a string
     * 
     * @param jsonString
     *            the json string containing the data
     * @return a EventType object
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    public EventType parseEventTypeData(String jsonString) throws JsonParseException, JsonMappingException, IOException
    {
        EventType p = (EventType) objectMapper.readValue(jsonString, EventType.class);
        return p;
    }
}
