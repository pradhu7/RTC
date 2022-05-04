package com.apixio.ifcwrapper.util;

import com.apixio.XUUID;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonUtil {

    private static final Logger LOG = LoggerFactory.getLogger(JsonUtil.class);

    private static ObjectMapper mapper;

    public static ObjectMapper getMapper() {
        if (mapper == null) {
            mapper = new ObjectMapper();
            SimpleModule module = new SimpleModule();
            module.addDeserializer(XUUID.class, new XUUIDDeserializer());
            mapper.registerModule(module);
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }
        return mapper;
    }

    public static String writeProtoForLog(MessageOrBuilder obj) {
        try {
            return JsonFormat.printer().omittingInsignificantWhitespace().print(obj);
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalArgumentException("Invalid protocol buffer", e);
        }
    }

    public static String writeForLog(Object obj) {
        if (obj instanceof MessageOrBuilder) {
            return writeProtoForLog((MessageOrBuilder) obj);
        } else {
            try {
                return getMapper().writeValueAsString(obj);
            } catch (JsonProcessingException e) {
                LOG.error("[ERR: could not serialize " + obj.toString() + "]", e);
                return "[ERR: could not serialize " + obj.toString() + "]";
            }
        }
    }

    public static String prettyPrint(Object obj) {
        if (obj instanceof MessageOrBuilder) {
            try {
                return JsonFormat.printer()
                    .includingDefaultValueFields()
                    .sortingMapKeys()
                    .print((MessageOrBuilder) obj);
            } catch (InvalidProtocolBufferException e) {
                LOG.error("Error parsing protocol buffer", e);
                throw new RuntimeException(e);
            }
        } else {
            try {
                return getMapper().writerWithDefaultPrettyPrinter().writeValueAsString(obj);
            } catch (JsonProcessingException e) {
                LOG.error("[ERR: could not serialize " + obj.toString() + "]", e);
                return "[ERR: could not serialize " + obj.toString() + "]";
            }
        }
    }

    public static String parseJsonException(JsonProcessingException e) {

        String message = e.getOriginalMessage();
        if (message != null) {
            int pos = message.indexOf('\n');
            if (pos == -1) {
                pos = message.indexOf(" at [Source");
            }

            if (pos > 0) {
                message = message.substring(0, pos);
            }
        }

        if (e.getLocation() != null) {
            message = message + " location: " + String.format("line %d, column %d",
                e.getLocation().getLineNr(),
                e.getLocation().getColumnNr());

        }
        return message;
    }
}
