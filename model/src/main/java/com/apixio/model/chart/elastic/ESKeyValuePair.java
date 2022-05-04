package com.apixio.model.chart.elastic;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@JsonSerialize(using = ESKeyValuePair.ESKeyValuePairSerializer.class)
@JsonDeserialize(using = ESKeyValuePair.ESKeyValuePairDeserializer.class)
public class ESKeyValuePair extends ESValue<ESStringValue> {

    protected final String key;

    public ESKeyValuePair(String key, ESStringValue value) {
        super(value, false);
        this.key = key;
    }

    @Override
    public String getJsonValue() {
        return value.getJsonValue();
    }

    public static Map<String, Object> getElasticTypeMapping() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("key", ImmutableMap.of("type", "text"));
        properties.put("value", ESStringValue.getElasticTypeMapping());
        //TODO: should type be object or nested??
        return ImmutableMap.of("type", "object", "properties", properties);
    }

    public static class ESKeyValuePairSerializer extends JsonSerializer<ESKeyValuePair> {

        @Override
        public void serialize(ESKeyValuePair pair, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeFieldName(pair.key);
            jsonGenerator.writeString(pair.getValue().getJsonValue());
            jsonGenerator.writeEndObject();
        }
    }

    public static class ESKeyValuePairDeserializer extends JsonDeserializer<ESKeyValuePair> {

        @Override
        public ESKeyValuePair deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
                throws IOException {
            JsonNode node = jsonParser.readValueAsTree();
            Map.Entry<String, JsonNode> keyValue = node.fields().next();
            return new ESKeyValuePair(keyValue.getKey(), new ESStringValue(keyValue.getValue().textValue()));
        }
    }
}
