package com.apixio.ifcwrapper;

import com.apixio.ensemble.ifc.Generator;
import com.apixio.ensemble.ifc.Signal;
import com.apixio.ensemble.ifc.SignalType;
import com.apixio.ensemble.ifc.transport.Signals;
import com.apixio.ifcwrapper.errors.IfcTransportSerializationError;
import com.apixio.ifcwrapper.errors.SignalSerializationError;
import com.apixio.ifcwrapper.generator.GeneratorWrapper;
import com.apixio.ifcwrapper.signal.SignalDataWrapper;
import com.apixio.ifcwrapper.source.AbstractSourceWrapper;
import com.apixio.ifcwrapper.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SignalMarshal {

    private static final Logger LOG = LoggerFactory.getLogger(SignalMarshal.class);

    public static String toProtoJson(Signal signal) throws IOException {
        return toProtoJson(signal, false, false);
    }

    public static String toProtoJsonVerbose(Signal signal) throws IOException {
        return JsonFormat
            .printer()
            .sortingMapKeys()
            .includingDefaultValueFields()
            .omittingInsignificantWhitespace()
            .print(fromIfc(signal, false).getProto());
    }

    @VisibleForTesting
    static String toProtoJson(
        Signal signal, boolean includeDefaultFields, boolean forcewrap) throws IOException {
        return JsonFormat
            .printer()
            .omittingInsignificantWhitespace()
            .print(fromIfc(signal, forcewrap).getProto());
    }

    public static byte[] toProtoBytes(Signal signal) throws IOException {
        return toProtoBytes(signal, false);
    }

    @VisibleForTesting
    static byte[] toProtoBytes(Signal signal, boolean forcewrap) throws IOException {
        return fromIfc(signal, forcewrap).getProto().toByteArray();
    }

    public static SignalDataWrapper fromProtoJson(String protoAsJson) throws IOException {
        Signals.Signal.Builder builder = Signals.Signal.newBuilder();
        JsonFormat.parser().ignoringUnknownFields().merge(protoAsJson, builder);
        return new SignalDataWrapper(builder.build());
    }

    public static SignalDataWrapper fromProtoBytes(byte[] bytes) throws IOException {
        Signals.Signal.Builder builder = Signals.Signal.newBuilder();
        builder.mergeFrom(bytes);
        return new SignalDataWrapper(builder.build());
    }

    /**
     * Deserialize a legacy/v1 signal from bytes into a SignalDataWrapper
     */
    public static SignalDataWrapper fromBytes(byte[] bytes) throws IOException {
        return fromJson(new String(bytes));
    }

    /**
     * Deserialize a legacy/v1 json signal into a SignalDataWrapper
     *
     * <p>Use only to deserialize V1/legacy signals</p>
     */
    public static SignalDataWrapper fromJson(String signalJson) throws IOException {
        return JsonUtil.getMapper().readerFor(SignalDataWrapper.class).readValue(signalJson);
    }

    /**
     * Deserialize a scala serialized legacy/v1 json signal into a list of SignalDataWrapper
     *
     * <p>Use only to deserialize V1/legacy signals</p>
     */
    public static List<SignalDataWrapper> fromJsonList(String signalListJson) throws IOException {
        return JsonUtil.getMapper()
            .readerFor(new TypeReference<List<SignalDataWrapper>>() {
            })
            .readValue(signalListJson);
    }

    public static SignalDataWrapper fromIfc(Signal signal) throws SignalSerializationError {
        return fromIfc(signal, false);
    }

    @VisibleForTesting
    static SignalDataWrapper fromIfc(Signal signal, boolean forceWrap)
        throws SignalSerializationError {
        if (!forceWrap && (signal instanceof SignalDataWrapper)) {
            return (SignalDataWrapper) signal;
        }
        Signals.Signal.Builder builder = Signals.Signal.newBuilder();

        try {
            AbstractSourceWrapper source = SourceMarshal.wrapSource(signal.getSource());
            switch (SourceMarshal.getSourceType(source)) {
                case PAGE:
                    builder.setPageSource((Signals.PageSource) source.getProto());
                    break;
                case PATIENT:
                    builder.setPatientSource((Signals.PatientSource) source.getProto());
                    break;
                case DOCUMENT:
                    builder.setDocumentSource((Signals.DocumentSource) source.getProto());
                    break;
                case PAGE_WINDOW:
                    builder.setPageWindowSource((Signals.PageWindowSource) source.getProto());
                    break;
                default:
                    LOG.error("Error serializing ifc.Source : " +
                        JsonUtil.writeProtoForLog(source.getProto()));
                    throw new SignalSerializationError("Could not determine source type of signal");
            }
        } catch (IfcTransportSerializationError e) {
            throw new SignalSerializationError("Could not serialize source type of signal", e);
        }

        builder.setGenerator(wrapGenerator(signal.getGenerator(), forceWrap).getProto());

        mergeSignalValue(builder, signal.getValue(), signal.getType());

        builder.setWeight(signal.getWeight());
        builder.setName(signal.getName());

        return new SignalDataWrapper(builder.build());
    }

    public static GeneratorWrapper wrapGenerator(Generator generator) {
        return wrapGenerator(generator, false);
    }

    @VisibleForTesting
    static GeneratorWrapper wrapGenerator(Generator generator, boolean forceWrap) {
        if (!forceWrap && (generator instanceof GeneratorWrapper)) {
            return (GeneratorWrapper) generator;
        }
        return new GeneratorWrapper(Signals.Generator.newBuilder()
            .setClassName(generator.className())
            .setJarVersion(generator.jarVersion())
            .setModelVersion(generator.modelVersion())
            .setName(generator.getName())
            .setVersion(generator.getVersion())
            .build());
    }

    public static Signals.Signal.Builder mergeSignalValue(
        Signals.Signal.Builder builder, Object signalValue, SignalType signalType) {
        switch (signalType) {
            case NUMERIC:
                builder.setSignalType(Signals.SignalType.NUMERIC);
                builder.setNumericValue((Float) signalValue);
                break;
            case CATEGORY:
                builder.setSignalType(Signals.SignalType.CATEGORY);
                builder.setCategoryValue((String) signalValue);
                break;
            default:
                throw new IllegalArgumentException(
                    "SignalMarshal didn't recognize signal type: " + signalType);
        }
        return builder;
    }
}
