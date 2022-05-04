package com.apixio.model.external;

import com.apixio.model.external.AxmPatient;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.datatype.joda.JodaModule;

import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;

public class AxmUtils {

    private static ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper
                .setSerializationInclusion(Include.NON_NULL)
                .setSerializationInclusion(Include.NON_EMPTY)
                .setSerializationInclusion(Include.NON_DEFAULT)
                .disable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS)
                .disable(SerializationFeature.WRITE_NULL_MAP_VALUES)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .registerModule(new JodaModule())
                .setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
    }

    public static AxmPackage fromJson(String str) throws JsonParseException, JsonMappingException, IOException {
        try {
            return objectMapper.readValue(str, AxmPackage.class);
        }
        catch (JsonProcessingException jpe) {
            try {
                Field _location = JsonProcessingException.class.getDeclaredField("_location");
                _location.setAccessible(true);
                _location.set(jpe, null);
                throw jpe;
            }
            catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException("Failed to remove PHI from AxmPackage JSON parser exception", e);
            }
        }
    }

    public static String toJson(AxmPackage p) throws JsonProcessingException {
        return objectMapper.writeValueAsString(p);
    }

    public static String toJson(AxmPatient p) throws JsonProcessingException {
        return objectMapper.writeValueAsString(p);
    }
}
