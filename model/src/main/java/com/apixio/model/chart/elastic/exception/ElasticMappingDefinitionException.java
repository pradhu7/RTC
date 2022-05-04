package com.apixio.model.chart.elastic.exception;

public class ElasticMappingDefinitionException extends Exception {

    public ElasticMappingDefinitionException(String reason) {
        super(reason);
    }

    public ElasticMappingDefinitionException(String reason, Throwable cause) {
        super(reason, cause);
    }
}
