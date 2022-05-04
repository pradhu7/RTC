package com.apixio.ifcwrapper.generator;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import java.io.IOException;

public class GeneratorListDeserializer extends JsonDeserializer<GeneratorListWrapper> {

    @Override
    public GeneratorListWrapper deserialize(
        JsonParser jp, DeserializationContext ctxt) throws IOException {
        ImmutableList.Builder<GeneratorWrapper> listBuilder = ImmutableList.builder();

        JsonNode node = jp.readValueAsTree();
        if (!node.has("generators")) {
            throw new GeneratorDeserializationError(
                "generator list has no key 'generators' at : " + node.toString());
        }
        JsonNode generatorsNode = node.get("generators");
        if (!generatorsNode.isArray()) {
            throw new GeneratorDeserializationError(
                "expected 'generators' to be an array at : " + generatorsNode.toString());
        }
        if (generatorsNode.get(0).isTextual() && generatorsNode.get(1).isArray()) {
            generatorsNode = generatorsNode.get(1);
        }
        for (JsonNode subNode : generatorsNode) {
            if (subNode.isObject()) {
                listBuilder.add(GeneratorUtil.fromJsonNode(subNode, null));
            }
        }
        return GeneratorListWrapper.builder().generators(listBuilder.build()).build();
    }

    private class GeneratorDeserializationError extends JsonProcessingException {

        protected GeneratorDeserializationError(String msg) {
            super(msg, null, null);
        }

        protected GeneratorDeserializationError(String msg, JsonLocation loc,
            Throwable rootCause) {
            super(msg, loc, rootCause);
        }
    }
}
