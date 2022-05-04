package com.apixio.model.aligner;

import java.io.IOException;
import java.util.Date;

import com.apixio.model.EitherStringOrNumber;
import com.apixio.model.event.AttributesType;
import com.apixio.model.event.adapters.EventAttributesTypeDeserializer;
import com.apixio.model.event.adapters.EventAttributesTypeSerializer;
import com.apixio.model.event.adapters.EventTypeDateDeserializer;
import com.apixio.model.event.adapters.EventTypeDateSerializer;
import com.apixio.model.utility.ApixioDateDeserialzer;
import com.apixio.model.utility.ApixioDateSerializer;
import com.apixio.model.utility.EitherStringOrNumberDeserializer;
import com.apixio.model.utility.EitherStringOrNumberSerializer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.joda.time.DateTime;

public class AlignerTypeJSONParser
{
    private static ObjectMapper objectMapper = new ObjectMapper();

    public AlignerTypeJSONParser()
    {
        // consolidated from event and patient class. If there are conflicts,
        // used the setting from events

        SimpleModule dateTimeModule = new SimpleModule("DateTimeModuleSerDer");
        dateTimeModule.addDeserializer(DateTime.class, new ApixioDateDeserialzer());
        dateTimeModule.addSerializer(DateTime.class, new ApixioDateSerializer()); // VK: I don't know why it is here!!!
        objectMapper.registerModule(dateTimeModule);

        SimpleModule eitherStringOrNumberModule = new SimpleModule("EitherStringOrNumberModuleSerDer");
        eitherStringOrNumberModule.addDeserializer(EitherStringOrNumber.class, new EitherStringOrNumberDeserializer());
        eitherStringOrNumberModule.addSerializer(EitherStringOrNumber.class, new EitherStringOrNumberSerializer());
        objectMapper.registerModule(eitherStringOrNumberModule);

        SimpleModule dateModule = new SimpleModule("DateSerDes");
        dateModule.addDeserializer(Date.class, new EventTypeDateDeserializer());
        dateModule.addSerializer(Date.class, new EventTypeDateSerializer());
        objectMapper.registerModule(dateModule);

        SimpleModule attrsModule = new SimpleModule("AttributesTypeSerDes");
        attrsModule.addSerializer(AttributesType.class, new EventAttributesTypeSerializer());
        attrsModule.addDeserializer(AttributesType.class, new EventAttributesTypeDeserializer());
        objectMapper.registerModule(attrsModule);

        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        objectMapper.configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, true);
        objectMapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);
    }

    public ObjectMapper getObjectMapper()
    {
        return objectMapper;
    }

    public String toJSON(AlignerType alignerType)
            throws IOException
    {
        return getObjectMapper().writeValueAsString(alignerType);
    }

    public AlignerType parseAlignerType(String jsonString)
            throws IOException
    {
        AlignerType alignerType = (AlignerType) objectMapper.readValue(jsonString, AlignerType.class);
        return alignerType;
    }
}
