package com.apixio.model.aligner;

import java.io.IOException;
import java.util.Date;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

import com.apixio.model.event.adapters.EventTypeDateDeserializer;
import com.apixio.model.event.adapters.EventTypeDateSerializer;
import com.apixio.model.utility.ApixioDateDeserialzer;
import com.apixio.model.utility.ApixioDateSerializer;

public class AlignerConfigInfoJSONParser
{
    private static ObjectMapper objectMapper = new ObjectMapper();

    public AlignerConfigInfoJSONParser()
    {
        // initializer to setup our object mapper with the appropriate
        // serializers and deserializers for dates
        SimpleModule module1 = new SimpleModule("DateTimeDeserializerModule");
        module1.addDeserializer(DateTime.class, new ApixioDateDeserialzer());
        module1.addSerializer(DateTime.class, new ApixioDateSerializer());
        objectMapper.registerModule(module1);

        SimpleModule dateModule = new SimpleModule("DateSerDes");
        dateModule.addDeserializer(Date.class, new EventTypeDateDeserializer());
        dateModule.addSerializer(Date.class, new EventTypeDateSerializer());
        objectMapper.registerModule(dateModule);

        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, true);
        objectMapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, true);
    }

    public ObjectMapper getObjectMapper()
    {
        return objectMapper;
    }

    public String toJSON(AlignerConfigInfo alignerConfigInfo)
            throws IOException
    {
        return getObjectMapper().writeValueAsString(alignerConfigInfo);
    }

    public AlignerConfigInfo parseAlignerConfigInfo(String jsonString)
            throws IOException
    {
        AlignerConfigInfo config = (AlignerConfigInfo) objectMapper.readValue(jsonString, AlignerConfigInfo.class);
        return config;
    }
}

