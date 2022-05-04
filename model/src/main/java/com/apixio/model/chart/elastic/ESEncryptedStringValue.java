package com.apixio.model.chart.elastic;

import com.apixio.security.Security;
import com.apixio.security.exception.ApixioSecurityException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.util.Map;

@JsonDeserialize(using = ESEncryptedStringValue.ESEncryptedStringValueDeserializer.class)
public class ESEncryptedStringValue extends ESValue<String> {

    public ESEncryptedStringValue(String value) {
        super(value, true);
    }

    @Override
    public String getJsonValue() {
        return value; //decrypted - will be encrypted by serializer
    }

    public static Map<String, Object> getElasticTypeMapping() {
        return ImmutableMap.of("type", "text");
    }

    public static class ESEncryptedStringValueDeserializer extends JsonDeserializer<ESEncryptedStringValue> {

        private final Security security = Security.getInstance();

        @Override
        public ESEncryptedStringValue deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode node = jsonParser.readValueAsTree();
            try {
                return new ESEncryptedStringValue(security.decrypt(node.textValue()));
            } catch (ApixioSecurityException e) {
                return new ESEncryptedStringValue("");
            }

        }
    }
}
