package com.apixio.datasource.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.SerializationFeature;

import com.apixio.datasource.kafka.StreamRecord;

public class KafkaUtility
{
    private static final Logger logger = LoggerFactory.getLogger(KafkaUtility.class);

    private ObjectMapper mapper = new ObjectMapper();

    public KafkaUtility()
    {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, true);
        mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, true);
    }

    public String toJSON(StreamRecord s) throws JsonGenerationException, JsonMappingException, IOException
    {
        return mapper.writeValueAsString(s);
    }
}
