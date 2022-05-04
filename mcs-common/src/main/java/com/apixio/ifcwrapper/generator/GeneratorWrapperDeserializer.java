package com.apixio.ifcwrapper.generator;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

public class GeneratorWrapperDeserializer extends JsonDeserializer<GeneratorWrapper> {

    @Override
    public GeneratorWrapper deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException {
        String className = null;
        JsonNode dataNode;
        JsonNode node = jp.readValueAsTree();
        if (node.isArray()) {
            className = node.get(0).textValue();
            dataNode = node.get(1);
        } else {
            dataNode = node;
        }

        return GeneratorUtil.fromJsonNode(dataNode, className);
    }
}
