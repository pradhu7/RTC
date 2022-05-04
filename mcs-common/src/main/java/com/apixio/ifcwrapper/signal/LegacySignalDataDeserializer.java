package com.apixio.ifcwrapper.signal;

import com.apixio.ensemble.ifc.transport.Signals;
import com.apixio.ensemble.ifc.transport.Signals.SignalType;
import com.apixio.ifcwrapper.generator.GeneratorWrapper;
import com.apixio.ifcwrapper.source.AbstractSourceWrapper;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

public class LegacySignalDataDeserializer extends JsonDeserializer<SignalDataWrapper> {

    @Override
    public SignalDataWrapper deserialize(
        JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode dataNode = parser.readValueAsTree();
        Signals.Signal.Builder builder = Signals.Signal.newBuilder();

        JsonNode generatorNode = dataNode.get("generator");
        builder.setGenerator(
            parser.getCodec().treeToValue(generatorNode, GeneratorWrapper.class).getProto());

        JsonNode sourceNode = dataNode.get("source");
        AbstractSourceWrapper source =
            parser.getCodec().treeToValue(sourceNode, AbstractSourceWrapper.class);
        switch (source.getSourceType()) {
            case PATIENT:
                builder.setPatientSource((Signals.PatientSource) source.getProto());
                break;
            case DOCUMENT:
                builder.setDocumentSource((Signals.DocumentSource) source.getProto());
                break;
            case PAGE:
                builder.setPageSource((Signals.PageSource) source.getProto());
                break;
            case PAGE_WINDOW:
                builder.setPageWindowSource((Signals.PageWindowSource) source.getProto());
                break;
            default:
                throw new IOException(
                    "SignalDataWrapper found unknown source type : " + source.getSourceType());
        }

        builder.setName(dataNode.get("name").textValue());

        if (dataNode.hasNonNull("weight")) {
            builder.setWeight(dataNode.get("weight").floatValue());
        } else {
            if (dataNode.hasNonNull("wt")) {
                builder.setWeight(dataNode.get("wt").floatValue());
            }
        }
        SignalValueWrapper signalValueWrapper = getValue(dataNode);

        if (signalValueWrapper.isNumber()) {
            builder.setSignalType(SignalType.NUMERIC);
            builder.setNumericValue(signalValueWrapper.floatVal());
        } else {
            builder.setSignalType(SignalType.CATEGORY);
            builder.setCategoryValue(signalValueWrapper.stringVal());
        }

        return new SignalDataWrapper(builder.build());
    }

    private SignalValueWrapper getValue(JsonNode dataNode) {
        JsonNode valueNode = dataNode.get("value");
        if (valueNode.isArray()) {
            valueNode = valueNode.get(1);
        }

        SignalType signalType = null;

        if (!dataNode.hasNonNull("sigType")) {
            String typeVal = dataNode.get("sigType").asText().toUpperCase();
            if (typeVal == "NUMERIC") {
                signalType = SignalType.NUMERIC;
            } else if (typeVal == "CATEGORY") {
                signalType = SignalType.CATEGORY;
            }
        }

        if (signalType == SignalType.NUMERIC) {
            return SignalValueWrapper.builder()
                .isNumber(true)
                .floatVal(valueNode.floatValue())
                .build();
        }

        if (signalType == SignalType.CATEGORY) {
            return SignalValueWrapper.builder()
                .isNumber(false)
                .stringVal(valueNode.textValue())
                .build();
        }

        try {
            return SignalValueWrapper.builder()
                .isNumber(true)
                .floatVal(Float.parseFloat(valueNode.asText()))
                .build();
        } catch (NumberFormatException | NullPointerException e) {
            return SignalValueWrapper.builder()
                .isNumber(false)
                .stringVal(valueNode.asText())
                .build();
        }
    }
}
