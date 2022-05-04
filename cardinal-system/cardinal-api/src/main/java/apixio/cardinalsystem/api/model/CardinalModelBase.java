package apixio.cardinalsystem.api.model;

import com.apixio.messages.MessageMetadata.XUUID;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.ContentsOrBuilder;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.DocumentInformation;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.FileInformation;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.PathInfo;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.ProcessEventOrBuilder;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.TransferEvent;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import org.apache.commons.validator.routines.UrlValidator;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Base Class for models using protobufs
 */
public class CardinalModelBase {
    @JsonIgnore
    private final static Pattern UUID_REGEX_PATTERN =
            Pattern.compile("^[{]?[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}[}]?$");
    @JsonIgnore
    private final static Pattern SHA512_PATTERN = Pattern.compile("^[a-fA-F0-9]{128}$");
    @JsonIgnore
    private final static Logger LOG = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());
    @JsonIgnore
    final String PROCESS_XUUID_TYPE = "PROCESSEVENT";
    @JsonIgnore
    final String TRANSFER_XUUID_TYPE = "TRANSFEREVENT";
    @JsonIgnore
    private final UrlValidator urlValidator = new UrlValidator();

    /**
     * @return ObjectMapper with needed modules and configuration
     */
    public static ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        configureObjectMapper(objectMapper);
        return objectMapper;
    }

    public static void configureObjectMapper(ObjectMapper objectMapper) {
        objectMapper.registerModule(new ProtobufModule());
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    }

    /**
     * @param event tracking event POJO based off protobuf
     * @return a modified tracking event record with auto-generated fields where applicable
     */
    public TrackingEventRecord validateRecord(TrackingEventRecord event) {
        TrackingEventRecord.Builder builder = event.toBuilder();
        builder.setEventTimestamp(validateTimestamp(event.getEventTimestamp()));
        switch (event.getEventCase()) {
            case PROCESS_EVENT:
                builder.setProcessEvent(validateProcessEvent(builder.getProcessEvent()));
                break;
            case TRANSFER_EVENT:
                builder.setTransferEvent(validateTransferEvent(builder.getTransferEvent()));
                break;
            default:
                throw new IllegalArgumentException(String.format("Unknown event type: %s", event.getEventCase()));
        }

        validateXUUID(builder.getOrgXuuid());

        return builder.build();
    }

    private TransferEvent validateTransferEvent(TransferEvent event) {

        if (Objects.isNull(event)) {
            throw new IllegalArgumentException("Can not validate null transfer event");
        }
        TrackingEventRecord.TransferEvent.Builder builder = event.toBuilder();
        if (builder.hasInitiatorXuuid()) {
            validateXUUID(builder.getInitiatorXuuid());
        } else {
            throw new IllegalArgumentException("Must have initiator_xuuid inside a transfer event");
        }
        if (builder.hasReceiverXuuid()) {
            validateXUUID(builder.getReceiverXuuid());
        } else {
            throw new IllegalArgumentException("Must have receiver_xuuid inside a process event");
        }
        if (builder.hasXuuid()) {
            LOG.warning(String.format("Request for transfer event specifies xuuid for process event. Will be overridden. Event type: %s", builder.getSubject().toString()));
        }
        XUUID xuuid = XUUID.newBuilder().setType(TRANSFER_XUUID_TYPE).setUuid(UUID.randomUUID().toString()).build();
        builder.setXuuid(xuuid);

        validateSubjectData(builder.getSubject());

        return builder.build();
    }

    /**
     * @param event tracking event to get the event id from
     * @return
     */
    public static String getEventId(TrackingEventRecord event) {
        switch (event.getEventCase()) {
            case PROCESS_EVENT:
                return getXUUIDString(event.getProcessEvent().getXuuid());
            case TRANSFER_EVENT:
                return getXUUIDString(event.getTransferEvent().getXuuid());
            default:
                throw new IllegalArgumentException(String.format("Unknown event type: %s", event.getEventCase()));
        }
    }

    /**
     * @param xuuid xuuid proto to translate into a string
     * @return
     */
    public static String getXUUIDString(XUUID xuuid) {
        return String.format("%s_%s", xuuid.getType(), xuuid.getUuid());
    }

    /**
     * @param timestamp an epoch timestam in seconds or milliseconds. assumes a recent time and will no work as expected
     *                  with a timestamp from say the 1980's
     * @return epoch time in milliseconds. will return the current time if set to 0
     */
    private Long validateTimestamp(Long timestamp) {
        if (timestamp == 0) {
            return Instant.now().toEpochMilli();
            // assume that less than 13 digits means it's a timestamp in seconds
        } else if (((long) Math.log10(timestamp) + 1) < 13) {
            return timestamp * 1000;
        }
        return timestamp;
    }

    /**
     * @param hash a hex string representing a sha512 hash
     *             the format will be validated to ensure it is 128 characters and contains only hex characters
     */
    private void validateSHA512(String hash) {
        if (!SHA512_PATTERN.matcher(hash).matches()) {
            throw new IllegalArgumentException(String.format("Invalid sha512 hash passed %s", hash));
        }
    }

    /**
     * @param xuuid an XUUID protobuf object. will validate that the type and uuid are set to a proper format
     */
    private void validateXUUID(XUUID xuuid) {
        String type = xuuid.getType();
        String uuid = xuuid.getUuid();
        if (type.isEmpty() || uuid.isEmpty()) {
            throw new IllegalArgumentException("Must have type and uuid defined for xuuid");
        }
        if (!UUID_REGEX_PATTERN.matcher(uuid).matches()) {
            throw new IllegalArgumentException(String.format("Invalid UUID format for xuuid: %s", uuid));
        }
    }

    /**
     * @param event validates that a process event has the appropriate fields and format
     * @return
     */
    private TrackingEventRecord.ProcessEvent validateProcessEvent(TrackingEventRecord.ProcessEvent event) {
        if (Objects.isNull(event)) {
            throw new IllegalArgumentException("Can not validate null process event");
        }
        TrackingEventRecord.ProcessEvent.Builder builder = event.toBuilder();
        if (builder.hasFromXuuid()) {
            validateXUUID(builder.getFromXuuid());
        } else {
            throw new IllegalArgumentException("Must have from_xuuid inside a process event");
        }
        if (builder.hasToXuuid()) {
            validateXUUID(builder.getToXuuid());
        } else {
            throw new IllegalArgumentException("Must have to_xuuid inside a process event");
        }
        if (builder.hasXuuid()) {
            LOG.warning(String.format("Request specifies xuuid for process event. Will be overridden. Event type: %s", builder.getDataCase().toString()));
        }
        XUUID xuuid = XUUID.newBuilder().setType(PROCESS_XUUID_TYPE).setUuid(UUID.randomUUID().toString()).build();
        builder.setXuuid(xuuid);

        validateProcessEventData(builder);
        validateSubjectData(builder.getSubject());

        return builder.build();
    }

    /**
     * @param event does basic checks to ensure metadata is defined for events
     */
    private void validateProcessEventData(ProcessEventOrBuilder event) {
        switch (event.getDataCase()) {
            case CDI_EVENT:
                if (!event.getCdiEvent().hasMetadata()) {
                    throw new IllegalArgumentException(String.format("Must have metadata for %s", event.getDataCase()));
                }
                break;
            case ETL_EVENT:
                if (!event.getEtlEvent().hasMetadata()) {
                    throw new IllegalArgumentException(String.format("Must have metadata for %s", event.getDataCase()));
                }
                break;
            case LOADER_EVENT:
                if (!event.getLoaderEvent().hasMetadata()) {
                    throw new IllegalArgumentException(String.format("Must have metadata for %s", event.getDataCase()));
                }
                break;
            case PREDICTION_EVENT:
                if (!event.getPredictionEvent().hasMetadata()) {
                    throw new IllegalArgumentException(String.format("Must have metadata for %s", event.getDataCase()));
                }
                break;
            case OPPORTUNITY_EVENT:
                if (!event.getOpportunityEvent().hasMetadata()) {
                    throw new IllegalArgumentException(String.format("Must have metadata for %s", event.getDataCase()));
                }
                break;
            case DATA_NOT_SET:
                throw new IllegalArgumentException("Must set data element for process event");
            default:
                LOG.warning(String.format("No validation defined for %s", event.getDataCase().toString()));
                break;
        }
    }

    /**
     * @param contents ensures subject data has appropriate data and fields
     */
    private void validateSubjectData(ContentsOrBuilder contents) {
        validateXUUID(contents.getXuuid());
        switch (contents.getDataCase()) {
            case DOC_INFO:
                DocumentInformation docInfo = contents.getDocInfo();
                validateSHA512(docInfo.getSha512());
                break;
            case FILE_INFO:
                FileInformation fileInfo = contents.getFileInfo();
                validateSHA512(fileInfo.getSha512());
                if (fileInfo.getFileSize() <= 0) {
                    throw new IllegalArgumentException(String.format("File size cannot be zero or negative: %d", fileInfo.getFileSize()));
                }
                if (!fileInfo.hasFilePath()) {
                    throw new IllegalArgumentException("Must specify file_path for file_info");
                }
                validateFilePath(fileInfo.getFilePath());
                break;
            case DATA_NOT_SET:
                throw new IllegalArgumentException("Must set data element for contents event");
            default:
                LOG.warning(String.format("No validation defined for %s", contents.getDataCase().toString()));
                break;
        }
    }

    /**
     * @param pathInfo ensures pathinfo constructs a valid URI
     */
    private void validateFilePath(PathInfo pathInfo) {
        String path = pathInfo.getPath();
        String hostname = pathInfo.getHost();
        String scheme = pathInfo.getScheme();
        if (hostname.isEmpty() || scheme.isEmpty() || path.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("Must specify scheme, host, and path for pathinfo. scheme: %s, host: %s, path: %s", scheme, hostname, path)
            );
        }
        if (!path.matches("^/.+")) {
            throw new IllegalArgumentException(String.format("Path must begin with '/'. Given: %s", path));
        }
        String uri = String.format("%s://%s%s", scheme, hostname, path);
        if (urlValidator.isValid(uri)) {
            throw new IllegalArgumentException(String.format("Invalid URI %s", uri));
        }
    }

}
