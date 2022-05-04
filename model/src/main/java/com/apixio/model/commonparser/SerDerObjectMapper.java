package com.apixio.model.commonparser;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.joda.time.DateTime;

import com.apixio.model.EitherStringOrNumber;
import com.apixio.model.event.AttributesType;

public class SerDerObjectMapper
{
    public enum InitializeType{Basic, Compact, NonCompact}

    public static void initializeBasic(ObjectMapper objectMapper)
    {
        init(objectMapper);
    }

    public static void initializeNonCompact(ObjectMapper objectMapper)
    {
        initializeCommon(objectMapper);

        SimpleModule dateModule = new SimpleModule("DateSerDes");
        dateModule.addDeserializer(Date.class, new DateDeserializer());
        dateModule.addSerializer(Date.class, new DateSerializer());
        objectMapper.registerModule(dateModule);

        SimpleModule attrsModule = new SimpleModule("AttributesTypeSerDes");
        attrsModule.addSerializer(AttributesType.class, new AttributesTypeSerializer());
        attrsModule.addDeserializer(AttributesType.class, new AttributesTypeDeserializer());
        objectMapper.registerModule(attrsModule);

        objectMapper.configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, true);
        objectMapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, true);
    }

    public static void initializeCompact(ObjectMapper objectMapper)
    {
        initializeCommon(objectMapper);

        objectMapper.configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, false);
        objectMapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
    }

    private static void initializeCommon(ObjectMapper objectMapper)
    {
        init(objectMapper);

        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        objectMapper.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);
    }

    private static void init(ObjectMapper objectMapper)
    {
        SimpleModule dateTimeModule = new SimpleModule("DateTimeModuleSerDer");
        dateTimeModule.addDeserializer(DateTime.class, new DateTimeDeserialzer());
        dateTimeModule.addSerializer(DateTime.class, new DateTimeSerializer()); // VK: I don't know why it is here!!!
        objectMapper.registerModule(dateTimeModule);

        SimpleModule eitherStringOrNumberModule = new SimpleModule("EitherStringOrNumberModuleSerDer");
        eitherStringOrNumberModule.addDeserializer(EitherStringOrNumber.class, new EitherStringOrNumberDeserializer());
        eitherStringOrNumberModule.addSerializer(EitherStringOrNumber.class, new EitherStringOrNumberSerializer());
        objectMapper.registerModule(eitherStringOrNumberModule);
    }
}


