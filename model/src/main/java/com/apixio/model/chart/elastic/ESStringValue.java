package com.apixio.model.chart.elastic;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.util.Map;

@JsonDeserialize(using = ESStringValue.ESStringValueDeserializer.class)
public class ESStringValue extends ESValue<String> {

    public ESStringValue(String value) {
        super(value, false);
    }

    @Override
    public String getJsonValue() {
        return value; //unencrypted
    }

    public static Map<String, Object> getElasticTypeMapping() {
        return ImmutableMap.of("type", "text");
    }

    public static class ESStringValueDeserializer extends JsonDeserializer<ESStringValue> {

        @Override
        public ESStringValue deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode node = jsonParser.readValueAsTree();
            return new ESStringValue(node.textValue());
        }
    }
}
