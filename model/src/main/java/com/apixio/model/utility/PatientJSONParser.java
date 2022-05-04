package com.apixio.model.utility;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.joda.time.DateTime;

import com.apixio.model.EitherStringOrNumber;
import com.apixio.model.patient.Patient;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;

public class PatientJSONParser {

    private static ObjectMapper objectMapper = new ObjectMapper();

    public PatientJSONParser()
    {
        // We need to register this one first because later we override DateTime de/serializers.
        objectMapper.registerModule(new JodaModule());

        SimpleModule module1 = new SimpleModule("DateTimeDeserializerModule");
        module1.addDeserializer(DateTime.class, new ApixioDateDeserialzer());
        objectMapper.registerModule(module1);

        SimpleModule module2 = new SimpleModule("EitherStringOrNumberDeserializerModule");
        module2.addDeserializer(EitherStringOrNumber.class, new EitherStringOrNumberDeserializer());
        objectMapper.registerModule(module2);

        SimpleModule module3 = new SimpleModule("DateTimeSerializerModule");
        module3.addSerializer(DateTime.class, new ApixioDateSerializer());
        objectMapper.registerModule(module3);

        SimpleModule module4 = new SimpleModule("EitherStringOrNumberSerializerModule");
        module4.addSerializer(EitherStringOrNumber.class, new EitherStringOrNumberSerializer());
        objectMapper.registerModule(module4);

        // This line below is superfluous since NON_EMPTY is a superset of NON_NULL
        objectMapper.setSerializationInclusion(Include.NON_NULL);

        objectMapper.setSerializationInclusion(Include.NON_EMPTY);
        objectMapper.configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, false);
        objectMapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
        objectMapper.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);
        objectMapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
    }

    /**
     * Parse the patient data from a string
     *
     * Intercepts Jackson parsing exceptions and strips PHI out of them.
     * Normally Jackson will print part of the PHI string where the parse error happened.
     * For backwards compatibility purposes we don't change public interface of this method
     * but rather intercept Jackson errors and hackish-ly set protected `_location` property to null.
     * `_location` property is where error context is kept.
     * Thus when parser error will be printed upstream it will not contain PHI.
     *
     * @param jsonString JSON string containing the data
     * @return a patient object
     */
    public Patient parsePatientData(String jsonString) throws JsonParseException, JsonMappingException, IOException
    {
        try
        {
            return objectMapper.readValue(jsonString, Patient.class);
        }
        catch (JsonProcessingException e)
        {
            try
            {
                Field _location = JsonProcessingException.class.getDeclaredField("_location");
                _location.setAccessible(true);
                _location.set(e, null);
                throw e;
            }
            catch (NoSuchFieldException | IllegalAccessException ex)
            {
                throw new RuntimeException("Failed to remove PHI from a patient JSON parser exception", ex);
            }
        }
    }

    public Patient parsePatientData(InputStream jsonStringStream) throws IOException
    {
        try
        {
            return objectMapper.readValue(jsonStringStream, Patient.class);
        }
        catch (JsonProcessingException e)
        {
            try
            {
                Field _location = JsonProcessingException.class.getDeclaredField("_location");
                _location.setAccessible(true);
                _location.set(e, null);
                throw e;
            }
            catch (NoSuchFieldException | IllegalAccessException ex)
            {
                throw new RuntimeException("Failed to remove PHI from a patient JSON parser exception", ex);
            }
        }
    }

    public String toJSON(Patient p) throws JsonProcessingException
    {
        return objectMapper.writeValueAsString(p);
    }

    public void serialize(OutputStream outputStream, Patient p) throws IOException
    {
        objectMapper.writeValue(outputStream, p);
    }
}
