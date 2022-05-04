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
import java.util.UUID;

@JsonDeserialize(using = ESUUIDValue.ESUUIDValueDeserializer.class)
public class ESUUIDValue extends ESValue<UUID> {

    public ESUUIDValue(UUID value) {
        super(value, false);
    }

    @Override
    public String getJsonValue() {
        return value.toString();
    }

    public static Map<String, Object> getElasticTypeMapping() {
        return ImmutableMap.of("type", "keyword");
    }

    public static class ESUUIDValueDeserializer extends JsonDeserializer<ESUUIDValue> {

        @Override
        public ESUUIDValue deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode node = jsonParser.readValueAsTree();
            return new ESUUIDValue(UUID.fromString(node.textValue()));
        }
    }
}
