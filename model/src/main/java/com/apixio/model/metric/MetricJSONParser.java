/*
 * 
 */
package com.apixio.model.metric;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

// TODO: Auto-generated Javadoc
/**
 * The Class MetricJSONParser.
 * @author Dan Dreon
 * @version 0.1
 */
public class MetricJSONParser
{
    
    /** The object mapper. */
    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Instantiates a new metric json parser.
     */
    public MetricJSONParser() {
    }

    /**
     * Gets the object mapper.
     *
     * @return the object mapper
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    /**
     * To json.
     *
     * @param p the p
     * @return the string
     * @throws JsonGenerationException the json generation exception
     * @throws JsonMappingException the json mapping exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public String toJSON(Metric p) throws JsonGenerationException, JsonMappingException, IOException {
        return getObjectMapper().writeValueAsString(p);
    }

    /**
     * Parses the event type data.
     *
     * @param jsonString the json string
     * @return the metric
     * @throws JsonParseException the json parse exception
     * @throws JsonMappingException the json mapping exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public Metric parseMetricTypeData(String jsonString) throws JsonParseException, JsonMappingException, IOException {
        return (Metric) objectMapper.readValue(jsonString, Metric.class);
    }
}
