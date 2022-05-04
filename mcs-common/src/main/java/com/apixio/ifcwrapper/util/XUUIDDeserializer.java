package com.apixio.ifcwrapper.util;

import com.apixio.XUUID;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XUUIDDeserializer extends JsonDeserializer<XUUID> {

    private static final Logger LOG = LoggerFactory.getLogger(XUUIDDeserializer.class);

    @Override
    public XUUID deserialize(
        JsonParser jp, DeserializationContext ctx) throws IOException, JsonProcessingException {
        JsonNode node = jp.readValueAsTree();
        if (node.isTextual()) {
            return XUUID.fromString(node.textValue());
        }
        if (node.hasNonNull("id")) {
            return XUUID.fromString(node.get("id").textValue());
        }
        String type =
            node.hasNonNull("pattern") ? node.get("pattern").textValue() : node.get("type").textValue();
        UUID value = node.hasNonNull("uuid") ?
            UUID.fromString(node.get("uuid").textValue()) : null;
        if (value == null && node.hasNonNull("uuidString")) {
            value = UUID.fromString(node.get("uuidString").textValue());
        }
        if (value == null) {
            throw new XUUIDDeserializationError(
                "Cannot deserialize XUUID with null value : " + node.textValue());
        }
        return XUUID.create(type, value, false, true);
    }

    class XUUIDDeserializationError extends JsonProcessingException {

        protected XUUIDDeserializationError(String msg) {
            super(msg, null, null);
        }

        protected XUUIDDeserializationError(String msg, JsonLocation loc,
            Throwable rootCause) {
            super(msg, loc, rootCause);
        }
    }
}
