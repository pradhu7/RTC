package com.apixio.ifcwrapper.source;

import com.apixio.XUUID;
import com.apixio.ensemble.ifc.transport.Signals;
import com.apixio.messages.MessageMetadata;
import com.apixio.util.Protobuf;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractSourceDeserializer extends JsonDeserializer<AbstractSourceWrapper> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractSourceDeserializer.class);

    @Override
    public AbstractSourceWrapper deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException {
        String className = null;

        JsonNode dataNode;
        JsonNode node = jp.readValueAsTree();
        if (node.isArray()) {
            className = node.get(0).textValue();
            dataNode = node.get(1);
        } else {
            dataNode = node;
        }
        if (className == null && dataNode.hasNonNull("className")) {
            className = dataNode.get("className").textValue();
        }

        SourceType sourceType = null;
        try {
            sourceType = normalizeSourceType(jp, dataNode, className);
        } catch (JsonParseException e) {
            throw new JsonParseException(
                jp, "Could not get source type from " + node.toString(), e);
        }
        switch (sourceType) {
            case PATIENT:
                return toPatientWrapper(jp, dataNode);
            case DOCUMENT:
                return toDocumentWrapper(jp, dataNode);
            case PAGE:
                return toPageWrapper(jp, dataNode);
            case PAGE_WINDOW:
                return toPageWindowWrapper(jp, dataNode);
            default:
                throw new JsonParseException(jp,
                    "Don't know how to handle sourceType "
                        + sourceType + " in " + dataNode.toString());
        }
    }

    private PatientSourceWrapper toPatientWrapper(
        JsonParser jp, JsonNode dataNode) throws JsonProcessingException {
        Signals.PatientSource proto = Signals.PatientSource.newBuilder()
            .setPatientID(requirePatientId(jp, dataNode))
            .build();
        return new PatientSourceWrapper(proto);
    }

    private DocumentSourceWrapper toDocumentWrapper(
        JsonParser jp, JsonNode dataNode) throws JsonProcessingException {
        Signals.DocumentSource proto = Signals.DocumentSource.newBuilder()
            .setNumPages(requirePages(jp, dataNode))
            .setPatientID(requirePatientId(jp, dataNode))
            .setDocumentID(requireDocumentId(jp, dataNode))
            .build();
        return new DocumentSourceWrapper(proto);
    }

    private PageSourceWrapper toPageWrapper(
        JsonParser jp, JsonNode dataNode) throws JsonProcessingException {
        Signals.PageSource proto = Signals.PageSource.newBuilder()
            .setPage(requirePage(jp, dataNode))
            .setDocumentID(requireDocumentId(jp, dataNode))
            .setPatientID(requirePatientId(jp, dataNode))
            .build();
        Optional<String> locationDescriptor = maybeGetStringLocation(jp, dataNode);
        if (locationDescriptor.isPresent()) {
            Signals.StringLocation stringLocation = Signals.StringLocation.newBuilder()
                .setLocDescriptor(locationDescriptor.get())
                .build();
            proto = proto.toBuilder().setStringLocation(stringLocation).build();
        }
        return new PageSourceWrapper(proto);
    }

    private PageWindowSourceWrapper toPageWindowWrapper(
        JsonParser jp, JsonNode dataNode) throws JsonProcessingException {

        Signals.PageWindowSource.Builder builder = Signals.PageWindowSource.newBuilder()
            .setDocumentID(requireDocumentId(jp, dataNode))
            .setPatientID(requirePatientId(jp, dataNode));

        if (dataNode.hasNonNull("windowProps")) {
            JsonNode windowProps = dataNode.get("windowProps");
            builder.setCentroid(windowProps.get("centroid").asInt());
            builder.setStartPage(windowProps.get("start").asInt());
            builder.setEndPage(windowProps.get("end").asInt());
        } else {
            if (dataNode.hasNonNull("centroid")) {
                builder.setCentroid(dataNode.get("centroid").asInt());
            }
            if (dataNode.hasNonNull("startPage")) {
                builder.setStartPage(dataNode.get("startPage").asInt());
            }
            if (dataNode.hasNonNull("endPage")) {
                builder.setEndPage(dataNode.get("endPage").asInt());
            }
        }
        return new PageWindowSourceWrapper(builder.build());
    }

    private Integer requirePages(JsonParser jp, JsonNode dataNode) throws JsonProcessingException {
        if (dataNode.hasNonNull("pages")) {
            return dataNode.get("pages").asInt();
        } else {
            if (dataNode.hasNonNull("wholeDocProps")) {
                return dataNode.get("wholeDocProps").get("pages").asInt();
            }
        }
        throw new JsonParseException(jp, "Cannot get page from " + dataNode.toString());
    }

    private Integer requirePage(JsonParser jp, JsonNode dataNode) throws JsonProcessingException {
        if (dataNode.hasNonNull("page")) {
            return dataNode.get("page").asInt();
        } else {
            if (dataNode.hasNonNull("pageProps")) {
                return dataNode.get("pageProps").get("page").asInt();
            }
        }
        throw new JsonParseException(jp, "Cannot get pages from " + dataNode.toString());
    }

    private MessageMetadata.XUUID requireDocumentId(
        JsonParser jp, JsonNode dataNode) throws JsonProcessingException {
        if (dataNode.hasNonNull("documentId")) {
            JsonNode documentIdNode = dataNode.get("documentId");
            if (documentIdNode.isTextual()) {
                String documentIdValue = documentIdNode.textValue();
                if (!documentIdValue.startsWith("DOC_")) {
                    documentIdValue = "DOC_" + documentIdValue;
                    return Protobuf.XUUIDtoProtoXUUID(XUUID.fromString(documentIdValue));
                }
            } else {
                try {
                    return Protobuf
                        .XUUIDtoProtoXUUID(jp.getCodec().treeToValue(documentIdNode, XUUID.class));
                } catch (JsonProcessingException e) {
                    LOG.error("Could not deserialize documentId for source :" + e.getMessage(), e);
                    throw e;
                }
            }
        }
        throw new JsonParseException(jp, "Cannot get document id from " + dataNode.toString());

    }

    private MessageMetadata.XUUID requirePatientId(
        JsonParser jp, JsonNode dataNode) throws JsonProcessingException {
        if (dataNode.hasNonNull("patientId")) {
            JsonNode patientIdNode = dataNode.get("patientId");
            if (patientIdNode.isTextual()) {
                String patientIdValue = patientIdNode.textValue();
                if (!patientIdValue.startsWith("PAT_")) {
                    patientIdValue = "PAT_" + patientIdValue;
                    return Protobuf.XUUIDtoProtoXUUID(XUUID.fromString(patientIdValue));
                }
            } else {
                try {
                    return Protobuf.XUUIDtoProtoXUUID(
                        jp.getCodec().treeToValue(dataNode.get("patientId"), XUUID.class));
                } catch (JsonProcessingException e) {
                    LOG.error("Could not deserialize patientId for source : " + e.getMessage(), e);
                    throw e;
                }
            }
        }
        throw new JsonParseException(jp, "Cannot get document id from " + dataNode.toString());
    }

    private Optional<String> maybeGetStringLocation(JsonParser jp, JsonNode dataNode) {
        if (dataNode.hasNonNull("location")) {
            if (dataNode.get("location").hasNonNull("descriptor")) {
                return Optional.ofNullable(dataNode.get("location").get("descriptor").asText());
            }
        }
        return Optional.empty();
    }

    public static SourceType normalizeSourceType(
        JsonParser jp, JsonNode dataNode, String className) throws JsonParseException {
        String source = dataNode.hasNonNull("sourceType")
            ? dataNode.get("sourceType").asText() : null;
        if (source == null) {
            if (dataNode.hasNonNull("_interface_name")) {
                source = dataNode.get("_interface_name").asText();
            }
        }
        if (source == null) {
            if (source == null && className != null) {
                String[] parts = className.split("\\.");
                source = parts[parts.length - 1];
            }
        }

        if (source != null) {
            source = source.toLowerCase();
            if (("patient".equals(source)) || "patientsource".equals(source)) {
                return SourceType.PATIENT;
            }
            if ("window".equals(source) || "pagewindowsource".equals(source)) {
                return SourceType.PAGE_WINDOW;
            }
            if ("whole".equals(source) || "documentsource".equals(source)) {
                return SourceType.DOCUMENT;
            }
            if ("page".equals(source) || "pagesource".equals(source)) {
                return SourceType.PAGE;
            }
            if (source.startsWith("patient")) {
                return SourceType.PATIENT;
            }
            if (source.startsWith("window") || source.startsWith("pagewindow")) {
                return SourceType.PAGE_WINDOW;
            }
            if (source.startsWith("whole") || source.startsWith("document")) {
                return SourceType.DOCUMENT;
            }
            if (source.startsWith("page")) {
                return SourceType.PAGE;
            }
        }
        throw new JsonParseException(jp,
            "Could not find matching source for source node: "
                + dataNode.toString() + " className: " + className);
    }
}
