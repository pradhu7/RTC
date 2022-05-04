package com.apixio.model.chart.elastic;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.util.Map;

public class ESBooleanValue extends ESValue<Boolean> {

    public ESBooleanValue(Boolean value) {
        super(value, false);
    }

    @Override
    public String getJsonValue() {
        return String.valueOf(value);
    }

    public static Map<String, Object> getElasticTypeMapping() {
        return ImmutableMap.of("type", "boolean");
    }

    public static class ESBooleanValueDeserializer extends JsonDeserializer<ESBooleanValue> {

        @Override
        public ESBooleanValue deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode node = jsonParser.readValueAsTree();
            return new ESBooleanValue(Boolean.valueOf(node.textValue()));
        }
    }
}
