package com.apixio.ifcwrapper.generator;

import com.apixio.ensemble.ifc.Generator;
import com.apixio.ensemble.ifc.transport.Signals;
import com.apixio.ifcwrapper.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

public class GeneratorUtil {

    public static List<Generator> explodeGenString(String value) throws IOException {
        GeneratorListWrapper listWrapper = JsonUtil
            .getMapper().readerFor(GeneratorListWrapper.class).readValue(value);
        return new ArrayList<>(listWrapper.generators());
    }

    public static GeneratorWrapper fromLogicalId(String logicalId, String className) {
        Signals.Generator.Builder builder = Signals.Generator.newBuilder();

        String[] parts = logicalId.split(":");

        if (parts.length < 2) {
            return null;
        }

        builder.setName(parts[0]);

        String logicalVersion = parts[1];
        if (logicalVersion.indexOf('/') >= 0) {
            builder.setVersion(logicalVersion.split("/")[0]);
            builder.setModelVersion(logicalVersion.split("/")[1]);
        } else {
            builder.setVersion(logicalVersion);
        }
        builder.setClassName(className);

        GeneratorWrapper result = new GeneratorWrapper(builder.build());

        if (result.getName() != null && result.getVersion() != null && result.modelVersion() != null) {
            return result;
        }

        return null;
    }

    public static GeneratorWrapper fromJsonNode(JsonNode dataNode, @Nullable String className) {
        if (dataNode.hasNonNull("className")) {
            className = dataNode.get("className").textValue();
        }
        Signals.Generator.Builder builder = Signals.Generator.newBuilder();
        if (dataNode.hasNonNull("version")) {
            builder.setVersion(dataNode.get("version").textValue());
        }
        if (dataNode.hasNonNull("name")) {
            builder.setName(dataNode.get("name").textValue());
        }
        if (dataNode.hasNonNull("jarVersion")) {
            builder.setJarVersion(dataNode.get("jarVersion").textValue());
        }
        if (dataNode.hasNonNull("modelVersion")) {
            builder.setModelVersion(dataNode.get("modelVersion").textValue());
        }
        builder.setClassName(className);
        return new GeneratorWrapper(builder.build());
    }
}
